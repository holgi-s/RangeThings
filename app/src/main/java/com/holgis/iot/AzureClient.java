package com.holgis.iot;
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

import com.google.gson.Gson;
import com.holgis.range.BuildConfig;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import java.io.IOException;
import java.net.URISyntaxException;

public class AzureClient {

    private String connString = BuildConfig.AZURE_CONNECTION_STRING;
    private IotHubClientProtocol protocol = IotHubClientProtocol.AMQPS;
    private DeviceClient client = null;

    private int sendContext = 0;

    public void open() throws IOException, URISyntaxException  {

        client = new DeviceClient(connString, protocol);
        try {
            client.open();
        } catch(IOException e) {
            client = null;
            throw e;
        }

    }

    public void close() throws IOException {
        client.close();
    }


    public void send(float distance) {
        if (client != null) {

            Gson gson = new Gson();

            String json = gson.toJson(new DistanceMessage(distance));
            Message msg = new Message(json);

            client.sendEventAsync(msg, new IotHubEventCallback() {
                @Override
                public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
                    Integer i = (Integer) callbackContext;
                    System.out.println("IoT Hub responded to message "+i.toString()
                            + " with status " + responseStatus.name());
                }
            }, sendContext++);
        }
    }

    private static class DistanceMessage {
        public long Timestamp;
        public float Distance;

        public DistanceMessage(float distance){
            this.Timestamp = System.currentTimeMillis();
            this.Distance = distance;
        }
    }
}


