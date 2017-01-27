/*
 * Copyright 2017 Holger Schmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.holgis.sensor;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.UserSensor;
import com.google.android.things.userdriver.UserSensorDriver;
import com.google.android.things.userdriver.UserSensorReading;
import com.holgis.sensor.filter.IDistanceFilter;

import java.io.IOException;


public class HCSR04Driver implements AutoCloseable {

    private static final String TAG = HCSR04Driver.class.getSimpleName();

    private static final String SENSOR_NAME = "HC-SR04";
    private static final String SENSOR_VENDOR = "Sparkfun";
    private static final int SENSOR_VERSION = 1;
    private static final int SENSOR_MIN_DELAY = 100000; //µsec
    private static final float  SENSOR_MAX_RANGE = 400f; //cm
    private static final float SENSOR_RESOLUTION = 0.5f; //cm

    private DistanceUserDriver mDistanceUserDriver = null;

    private String mTrigger = null;
    private String mEcho = null;
    private IDistanceFilter mFilter;

    public HCSR04Driver(String gpioTrigger, String gpioEcho, IDistanceFilter filter) {
        mTrigger = gpioTrigger;
        mEcho = gpioEcho;
        mFilter = filter;
    }

    @Override
    public void close() throws IOException {
        unregisterSensor();
    }

    public void registerSensor() throws IOException{
        if (mDistanceUserDriver == null) {
            mDistanceUserDriver = new DistanceUserDriver(mTrigger, mEcho, mFilter);
            UserDriverManager.getManager().registerSensor(mDistanceUserDriver.getUserSensor());
        }
    }

    public void unregisterSensor() throws IOException {
        if (mDistanceUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mDistanceUserDriver.getUserSensor());
            mDistanceUserDriver.close();
            mDistanceUserDriver = null;
        }
    }

    private class DistanceUserDriver extends UserSensorDriver {

        private UserSensor mUserSensor;
        private HCSR04 mHCSR04 = null;
        private float mDistance = 0.0f;
        private IDistanceFilter mFilter = null;

        public DistanceUserDriver(String gpioTrigger, String gpioEcho, IDistanceFilter filter) throws  IOException {
            mFilter = filter;
            mHCSR04 = new HCSR04(gpioTrigger, gpioEcho);
            mHCSR04.SetOnDistanceListener(mOnDistanceListener);
        }

        public void close() throws IOException {
            mHCSR04.RemoveOnDistanceListener(mOnDistanceListener);
            mHCSR04.close();
        }

        private HCSR04.OnDistanceListener mOnDistanceListener = new HCSR04.OnDistanceListener() {
            @Override
            public void OnDistance(float distance) {
                if(mFilter != null) {
                    mDistance = mFilter.filter(distance);
                } else {
                    mDistance = distance;
                }
            }
        };
        
        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = UserSensor.builder()
                        .setType(Sensor.TYPE_PROXIMITY)
                        .setName(SENSOR_NAME)
                        .setVendor(SENSOR_VENDOR)
                        .setVersion(SENSOR_VERSION)
                        .setMaxRange(SENSOR_MAX_RANGE)
                        .setResolution(SENSOR_RESOLUTION)
                        .setMinDelay(SENSOR_MIN_DELAY)
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public void setEnabled(boolean enabled) throws IOException {
            if(enabled){
                mHCSR04.Resume();
            } else {
                mHCSR04.Pause();
            }
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{ mDistance });
        }
    }




}