package com.holgis.data;
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

import com.holgis.net.NetServer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DistanceMessage {

    static public final int SIZE = 8+4+4+1; //Long.BYTES + Float.BYTES*2 + Byte.BYTES;

    public int version = 1;
    public long timestamp = 0;
    public float distance = 0.0f;
    public float filtered = 0.0f;
    public boolean jump = false;

    public DistanceMessage() {
    }

    public DistanceMessage(float distance, float filtered, boolean jump){
        this.timestamp = System.currentTimeMillis();
        this.distance = distance;
        this.filtered = filtered;
        this.jump = jump;
    }

    public byte[] getBytes(){
        byte[] b = ByteBuffer.allocate(DistanceMessage.SIZE).
                order(ByteOrder.LITTLE_ENDIAN).putLong(timestamp).
                putFloat(distance).putFloat(filtered).
                put((byte)(jump?1:0)).array();
        return b;
    }
}
