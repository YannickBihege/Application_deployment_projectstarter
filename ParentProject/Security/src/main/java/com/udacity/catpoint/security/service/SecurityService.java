package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * <p>
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private Boolean catDetection = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * These methods should be applied to track the number of active and inactive  sensors
     * getActive is implemented by the Sensor class
     */
    private Boolean allSensorsInactive() {
        return getSensors().stream().noneMatch(Sensor::getActive);
    }

    Set<Sensor> getActiveSensors() {
        return getSensors().stream().filter(Sensor::getActive).collect(toSet());
    }

    boolean systemArmed() {
        return List.of(ArmingStatus.ARMED_HOME, ArmingStatus.ARMED_AWAY).contains(this.securityRepository.getArmingStatus());
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     *
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        switch (armingStatus) {
            case DISARMED -> {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
            case ARMED_HOME, ARMED_AWAY -> {
                if (catDetection) {
                    setAlarmStatus(AlarmStatus.ALARM);
                }
                ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());
                sensors.forEach(sensor -> changeSensorActivationStatus(sensor, false));
            }
        }
        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }


    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     *
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catDetection = cat;

        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && getSensors().stream().allMatch(sensor -> !sensor.getActive())) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     *
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }


    /**
     * Change the alarm status of the system and notify all listeners.
     *
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }


    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     *
     * @param sensor
     * @param active
     */

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if (!sensor.getActive() && active) {  //1
            handleSensorActivated();
        } else if (!sensor.getActive() && !active) {
            handleSensorActivated();
        } else if (sensor.getActive() && !active) { //3 Deactivated
            handleSensorDeactivated();
        } else if (sensor.getActive() && active) { // 4
            //TODO Two systems are active  the system should go to ALARM state.
            switch (securityRepository.getAlarmStatus()) {
                case NO_ALARM, PENDING_ALARM, ALARM -> setAlarmStatus(AlarmStatus.ALARM);
                // Do nothing, basically stay in the same state
            }
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }


    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        // int numberActiveSensors = getSensors().stream().filter(Sensor::getActive).collect(toSet()).size();
        if (getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        } else {
            switch (securityRepository.getAlarmStatus()) {
                case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
                case PENDING_ALARM, ALARM -> setAlarmStatus(AlarmStatus.ALARM);
                // Do nothing, basically stay in the same state
            }
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch (securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.ALARM); // Do nothing, basically stay in the same state
        }
    }


    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     *
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    public boolean hasStatusListener(StatusListener statusListener) {
        return statusListeners.contains(statusListener);
    }
}