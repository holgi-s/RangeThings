package com.holgis.sensor.detector;
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

public class JumpDetector implements IDetector {

    private float minDetection = 5.0f;

    private float minDistance = 0.0f;
    private float maxDistance = 400.0f;

    private float prevDistance = 0.0f;

    private final boolean up = true;
    private final boolean down = false;

    private boolean currentDirection = down;
    private boolean initialDistance = true;

    public void setMinDetection(float minJumpDistance){
        minDetection = minJumpDistance;
    }

    public boolean detect(float curDistance) {
        boolean detected  = false;
        if(initialDistance){
            maxDistance = minDistance = prevDistance = curDistance;
            initialDistance = false;
        }
        if(currentDirection == down) {
            if(curDistance > prevDistance) {
                currentDirection = up;
                minDistance = prevDistance;
                // change to up
            } else if(curDistance < prevDistance) {
                //continue down
            }
        } else { //currentDirection == up
            if(curDistance > prevDistance) {
                //continue up
            } else if(curDistance < prevDistance) {
                //change to down
                currentDirection = down;
                maxDistance = prevDistance;
                if(maxDistance - minDistance > minDetection){
                    detected = true;
                }
            }
        }
        prevDistance = curDistance;
        return detected;
    }
}
