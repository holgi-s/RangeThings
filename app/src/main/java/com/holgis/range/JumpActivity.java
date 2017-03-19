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
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.holgis.net.NetServer;
import com.holgis.sensor.HCSR04;
import com.holgis.sensor.detector.JumpDetector;
import com.holgis.sensor.filter.SimpleEchoFilter;

import java.io.IOException;

public class JumpActivity extends Activity implements HCSR04.OnDistanceListener {

    private static final String TAG = JumpActivity.class.getSimpleName();

    private HCSR04 mSensor = null;
    private SimpleEchoFilter mFilter = null;
    private JumpDetector mDetector = null;

    private int mJumpCounter = 0;

    TextView distanceView = null;

    NetServer mServer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_range);
        distanceView = (TextView) findViewById(R.id.fullscreen_content);



        try {
            mSensor = new HCSR04("BCM17", "BCM27");
            mFilter = new SimpleEchoFilter();
            mDetector = new JumpDetector();
            mDetector.setMinDetection(5.0f); //cm
            mSensor.SetOnDistanceListener(this);

            mServer = new NetServer();
            mServer.startServer(this, 9786);
        }
        catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {

        if(mSensor!=null) {
            mServer.stopServer();
        }
        if(mSensor!=null) {
            mSensor.RemoveOnDistanceListener(this);
        }
        mSensor = null;
        mFilter = null;
        mDetector = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensor.Resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mSensor.Pause();
        } catch (IOException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void OnDistance(float distance) {

        float filtered = mFilter.filter(distance);
        boolean jump = mDetector.detect(filtered);

        mServer.AddMessage(new NetServer.DistanceMessage(distance, filtered, jump));

        if(jump){
            ++mJumpCounter;

            String txt = String.format("Jump Count: %d", mJumpCounter);
            Log.d(TAG, "OnDistance: " + txt);

            distanceView.setText(txt);
        }
    }
}
