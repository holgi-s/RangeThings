/*
 * Copyright 2016 Holger Schmidt
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

package com.holgis.range;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.holgis.sensor.HCSR04Driver;
import com.holgis.sensor.filter.SimpleEchoFilter;

import java.io.IOException;

public class RangeActivity extends Activity implements SensorEventListener {

    private static final String TAG = RangeActivity.class.getSimpleName();

    private HCSR04Driver mHCSR04Driver = null;
    private SensorManager mSensorManager;

    TextView distanceView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_range);

        distanceView = (TextView) findViewById(R.id.fullscreen_content);

        mHCSR04Driver = new HCSR04Driver("BCM17", "BCM27", new SimpleEchoFilter());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerDynamicSensorCallback(new SensorCallback());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mHCSR04Driver.registerSensor();
        } catch (IOException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mHCSR04Driver.unregisterSensor();
        } catch (IOException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    // Listen for registration events from the sensor driver
    private class SensorCallback extends SensorManager.DynamicSensorCallback {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            Log.i(TAG, sensor.getName() + " has been connected");

            // Begin listening for sensor readings
            mSensorManager.registerListener(RangeActivity.this, sensor,
                    SensorManager.SENSOR_DELAY_GAME);
        }

        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            Log.i(TAG, sensor.getName() + " has been disconnected");

            // Stop receiving sensor readings
            mSensorManager.unregisterListener(RangeActivity.this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(sensorEvent.sensor.getType()==Sensor.TYPE_PROXIMITY) {
            if (sensorEvent.values.length > 0) {

                String text = "Proximity: " + Math.round(sensorEvent.values[0]) + " cm";
                Log.i(TAG, text);

                if(distanceView!=null){
                    distanceView.setText(text);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
