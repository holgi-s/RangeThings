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



import com.holgis.iot.AzureClient;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertTrue;


public class AzureTest {
    @Test
    public void connectionTest() {
        AzureClient ac = new AzureClient();

        try {
            ac.open();
        } catch (IOException e) {
            assertTrue(false);
        } catch (URISyntaxException e) {
            assertTrue(false);
        }

        try {
            ac.close();
        } catch (IOException e) {
            assertTrue(false);
        }

    }
    @Test
    public void eventTest() {
        AzureClient ac = new AzureClient();
        try {
            ac.open();

            for(int i=0;i<10;++i) {
                ac.send(2.3f+i*1.1f);
            }

            ac.close();

        } catch (IOException e) {
        } catch (URISyntaxException e) {
        }
    }
}
