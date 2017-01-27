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


import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class HCSR04 implements AutoCloseable {

    private static final String TAG = HCSR04.class.getSimpleName();

    private Gpio mTriggerPin;
    private Gpio mEchoPin;

    private Handler mHandler;

    private float mDistance;
    private long mTriggerTime;


    public HCSR04(String triggerPin, String echoPin) throws IOException {

        PeripheralManagerService pioService = new PeripheralManagerService();

        mTriggerPin = pioService.openGpio(triggerPin);
        mTriggerPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        mTriggerPin.setActiveType(Gpio.ACTIVE_HIGH);

        mEchoPin = pioService.openGpio(echoPin);
        mEchoPin.setDirection(Gpio.DIRECTION_IN);
        mEchoPin.setActiveType(Gpio.ACTIVE_HIGH);

        mEchoPin.setEdgeTriggerType(Gpio.EDGE_BOTH);
        mEchoPin.registerGpioCallback(mGpioCallback);

        mHandler = new Handler();

        Resume();
    }


    @Override
    public void close() throws IOException {

        mHandler.removeCallbacks(mTrigger);

        mTriggerPin.close();
        mTriggerPin = null;

        mEchoPin.close();
        mEchoPin = null;
    }

    public void Pause() throws IOException {

        mHandler.removeCallbacks(mTrigger);

    }

    public void Resume() {

        mHandler.removeCallbacks(mTrigger);

        mTriggerTime = System.nanoTime();

        mTrigger.run();
    }

    public interface OnDistanceListener {
        void OnDistance(float distance);
    }

    private List<OnDistanceListener> mOnDistanceReadings = new ArrayList<>();

    public void SetOnDistanceListener(OnDistanceListener onDistance) {
        mOnDistanceReadings.add(onDistance);
    }

    public void RemoveOnDistanceListener(OnDistanceListener onDistance) {
        mOnDistanceReadings.remove(onDistance);
    }

    @SuppressWarnings("unused")
    public float GetDistance(){
        return mDistance;
    }

    private Runnable mTrigger = new Runnable() {
        @Override
        public void run() {
            try {
                try {
                    mTriggerPin.setValue(true);
                    Thread.sleep(0, 10 * 1000);  //10 µs pulse
                    mTriggerPin.setValue(false);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (NullPointerException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mTrigger, 100);
            }
        }
    };

    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {

            boolean rising = false;
            long nano = System.nanoTime();
            try {
                rising = gpio.getValue();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            if (rising) {
                mTriggerTime = nano;
            } else {
                echo(nano - mTriggerTime);
            }

            return super.onGpioEdge(gpio);
        }
    };

    //echoTime in [ns]
    private void echo(long peakTime) {

        float echoDelay = peakTime / 1000000000.0f; // [nsec] -> [sec]
        float soundVelocity = 343.2f; // [m/sec] TODO: adjust for temperature
        float meter = echoDelay * soundVelocity / 2; // [m], (roundtrip -> /2)

        //valid senor results
        if(meter >= 0.1f && meter <= 4.0f) {

            float centimeter = meter * 100.f; //[cm]
            mDistance = centimeter;

            Log.d(TAG, "Distance: " + String.format("%.2f", centimeter) + " cm");

            for(OnDistanceListener listener : mOnDistanceReadings) {
                listener.OnDistance(mDistance);
            }
        }
    }
}