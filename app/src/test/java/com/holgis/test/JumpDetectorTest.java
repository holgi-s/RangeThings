package com.holgis.test;
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

import com.holgis.sensor.detector.JumpDetector;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JumpDetectorTest {

    @Test
    public void jumpSequenceTest() {

        JumpDetector jd = new JumpDetector();

        assertFalse(jd.detect(20));
        assertFalse(jd.detect(18));
        assertFalse(jd.detect(15));
        assertFalse(jd.detect(12));
        assertFalse(jd.detect(15));
        assertFalse(jd.detect(18));
        assertFalse(jd.detect(21));
        assertFalse(jd.detect(22));
        assertTrue(jd.detect(19)); //JUMP
        assertFalse(jd.detect(15));
        assertFalse(jd.detect(13));
        assertFalse(jd.detect(11));
        assertFalse(jd.detect(9));
        assertFalse(jd.detect(8));
        assertFalse(jd.detect(11));
        assertFalse(jd.detect(10));
        assertFalse(jd.detect(9));// NO JUMP
        assertFalse(jd.detect(11));
        assertFalse(jd.detect(15));
        assertFalse(jd.detect(17));
        assertFalse(jd.detect(21));
        assertTrue(jd.detect(20));
        assertFalse(jd.detect(21));
        assertFalse(jd.detect(23));
        assertFalse(jd.detect(21));// NO JUMP
        assertFalse(jd.detect(18));
        assertFalse(jd.detect(16));
        assertFalse(jd.detect(14));
        assertFalse(jd.detect(15));
        assertFalse(jd.detect(13));
        assertFalse(jd.detect(11));
        assertFalse(jd.detect(9));
        assertFalse(jd.detect(10));
        assertFalse(jd.detect(13));
        assertFalse(jd.detect(15));
        assertFalse(jd.detect(17));
        assertTrue(jd.detect(14)); //JUMP
        assertFalse(jd.detect(13));
        assertFalse(jd.detect(11));

    }

}
