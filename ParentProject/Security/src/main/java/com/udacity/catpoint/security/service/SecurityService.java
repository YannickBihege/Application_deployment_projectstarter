package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {
    /**
     * I updated the code based on the answers given on knowledge.
     */

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }
    private Boolean catDetection = false;
    Set<Sensor> getActiveSensors(){
        return getSensors()
                .stream()
                .filter(Sensor::getActive)
                .collect(Collectors.toSet());
    }
    private boolean allSensorsInactive() {
        return getSensors()
                .stream()
                .noneMatch(Sensor::getActive);
    }
    boolean systemArmed(ArmingStatus armingStatus){
        return List.of(ArmingStatus.ARMED_HOME, ArmingStatus.ARMED_AWAY)
                .contains(armingStatus);
    }
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }
    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }
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
    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }


    /**
     * -------------------Behavioral method  CAT DETECTION-------------------------------------------
     */
    /**
     * Internal method that handles alarm status changes based on whether the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && allSensorsInactive()){
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }


    /**
     * ---------------------Behavioral method ARMING STATUS-------------------------------------------------------
     */
    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(catDetection && armingStatus == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());
            sensors.forEach(sensor -> changeSensorActivationStatus(sensor, false));
        }
        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }


    /**
     * -------------------Behavioral methodS handle SENSOR activation and deactivation and ALARM STATUS-------------------------------------------
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }


    /**
     * -------------------Behavioral methods change ACTIVATION STATUS-------------------------------------------
     */
    public void changeSensorActivationStatus(Sensor sensor) {
        AlarmStatus actualAlarmStatus = this.getAlarmStatus();
        ArmingStatus actualArmingStatus = this.getArmingStatus();

        if (actualAlarmStatus == AlarmStatus.PENDING_ALARM && !sensor.getActive()) {
            handleSensorDeactivated();
        } else if (actualAlarmStatus == AlarmStatus.ALARM && actualArmingStatus == ArmingStatus.DISARMED) {
            handleSensorDeactivated();
        }
        securityRepository.updateSensor(sensor);
    }
    /**
     * @param sensor @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus actualAlarmStatus = securityRepository.getAlarmStatus();
        if(actualAlarmStatus != AlarmStatus.ALARM) {
            if(active) {
                handleSensorActivated();
            } else if (sensor.getActive()) {
                handleSensorDeactivated();
            }
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

}