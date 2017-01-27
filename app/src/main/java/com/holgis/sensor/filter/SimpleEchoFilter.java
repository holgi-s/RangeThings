package com.holgis.sensor.filter;
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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//mixture of 'simple moving median' and 'moving average'
//by using a moving window and then averaging the values in the middle of the window

public class SimpleEchoFilter implements IDistanceFilter {

    private static final String TAG = SimpleEchoFilter.class.getSimpleName();

    private static final int FILER_WINDOW_= 3;
    private static final int FILER_UPPER_CUTOFF = 2;
    private static final int FILER_LOWER_CUTOFF = 0;

    private List<Float> distanceEchos = new LinkedList<>();

    @Override
    public float filter(float value) {
        float filtered = 0.0f;

        if(distanceEchos.size() >= FILER_WINDOW_){
            distanceEchos.remove(0);
        }
        distanceEchos.add(new Float(value));

        List<Float> sortedEchos = new ArrayList<>(distanceEchos);
        Collections.sort(sortedEchos);

        if(sortedEchos.size() > (FILER_LOWER_CUTOFF + FILER_UPPER_CUTOFF)) {
            filtered = average(sortedEchos,
                    FILER_LOWER_CUTOFF, sortedEchos.size() - FILER_UPPER_CUTOFF);
        } else {
            filtered = average(sortedEchos, 0, sortedEchos.size());
        }

        Log.d(TAG, "Filtered: " +
                String.format("%d; %.2f", System.currentTimeMillis(), filtered) + " cm");

        return filtered;
    }

    private float average(List<Float> echos, int begin, int end) {
        float avg = 0;
        for(int i = begin; i < end; ++i){
            avg += echos.get(i);
        }
        if(end-begin != 0) {
            return avg / (end - begin);
        } else {
            return avg;
        }
    }



}

