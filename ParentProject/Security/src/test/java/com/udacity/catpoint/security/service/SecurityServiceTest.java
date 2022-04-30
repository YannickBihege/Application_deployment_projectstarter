package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Stream;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;
    private final String randomString = UUID.randomUUID().toString();
    @Mock
    SecurityRepository securityRepository;
    @Mock
    ImageService imageService;
    private
    SecurityService securityService;
    @Mock
    private StatusListener statusListener;

    private Sensor getNewSensor(){
        return new Sensor(randomString, SensorType.DOOR);
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getNewSensor();
    }

    static Stream<Arguments> booleanMethodSource(){
        return Stream.of(
                Arguments.of(false, true),
                Arguments.of(true, false)
        );
    }

    private Set<Sensor> getSensors(boolean active, int count){
        String randomString = UUID.randomUUID().toString();
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i <= count; i++){
            sensors.add(new Sensor(randomString, SensorType.DOOR));
        }
        sensors.forEach(it -> it.setActive(active));
        return sensors;
    }
    private Set<Sensor> getAllSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor(randomString, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }


/**
 * 1)
 *If alarm is armed and a sensor becomes activated,
 * put the system into pending alarm status.
 * 2)
 * If alarm is armed and a sensor becomes activated
 * and the system is already pending alarm, set the alarm status to alarm.
 * 3)
 * If pending alarm and all sensors are inactive,
 * return to no alarm state.
 * 4)
 * If alarm is active, change in sensor state should
 * not affect the alarm state.
 * 5)
 * If a sensor is activated while already active
 * and the system is in pending state, change it to alarm state.
 * 6)
 * If a sensor is deactivated while already inactive,
 * make no changes to the alarm state.
 * 7)
 * If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
 * 8)
 * If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
 * 9)
 * If the system is disarmed, set the status to no alarm.
 * 10)
 * If the system is armed, reset all sensors to inactive.
 * 11)
 * If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
 */

    @Test // 2
    @DisplayName("If alarm is armed and a sensor becomes activated\n" +
            " * and the system is already pending alarm, " +
            "set the alarm status to alarm")
    void changeAlarmStatus_alarmAlreadyPendingAndSensorActivated_alarmStatusAlarm(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test // 5
    @DisplayName(" If a sensor is activated while already active\n"+
            "and the system is in pending state, " +
            "change it to alarm state.")
    void changeAlarmState_systemActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @ParameterizedTest // 6
    @DisplayName("If a sensor is deactivated while already inactive,\n" +
            " * make no changes to the alarm state.")
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void changeAlarmState_sensorDeactivateWhileInactive_noChangeToAlarmState(AlarmStatus alarmStatus){
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test // 7
    @DisplayName("If the image service identifies an image containing a cat while the system is " +
            "armed-home, put the system into alarm status.")
    void changeAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test // 9
    @DisplayName("If the system is disarmed, set the status to no alarm.")
    void changeAlarmStatus_systemDisArmed_changeToAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @Test // 11
    @DisplayName("If the system is armed-home while the camera shows a cat, " +
            "set the alarm status to alarm.")
    void changeAlarmStatus_systemArmedHomeAndCatDetected_changeToAlarmStatus(){
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

}