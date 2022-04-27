package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
import org.junit.jupiter.params.provider.MethodSource;
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


    private Sensor getNewSensor(){
        return new Sensor(randomString, SensorType.DOOR);
    }

    @BeforeEach
    void setUp() {
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


    @Test
    void ifSystemArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifSystemArmedAndSensorActivatedAndPendingState_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    @Test
    void changeAlarmStatus_alarmAlreadyPendingAndSensorActivated_alarmStatusAlarm(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM); //first call up
    }








}