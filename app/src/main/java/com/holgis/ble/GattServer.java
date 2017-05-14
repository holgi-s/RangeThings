package com.holgis.ble;
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import com.holgis.data.DistanceMessage;

import java.util.ArrayList;

import static android.content.Context.BLUETOOTH_SERVICE;

public class GattServer implements AutoCloseable {

    private static final String TAG = GattServer.class.getName();

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;

    private ArrayList<BluetoothDevice> mConnectedDevices;
    private BluetoothGattCharacteristic mDistanceCharacteristic;

    static public boolean checkBluetooth(Context context){
        BluetoothManager bm = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter ba = bm.getAdapter();
        if (ba == null) {
            //Bluetooth is disabled
            Log.e(TAG, "BluetoothAdapter not available!");
            return false;
        }

        if(!ba.isEnabled()) {
            Log.w(TAG, "BluetoothAdapter not enabled!");
            ba.enable();
        }

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Bluetooth LE is not supported");
            return false;
        }

        if(!ba.isMultipleAdvertisementSupported()){
            Log.i(TAG, "No Multiple Advertisement Support!");
        }

        return ba.isEnabled();
    }

    public GattServer(Context context){

        mContext = context;
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mConnectedDevices = new ArrayList<>();

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        initServer();
        startAdvertising();
    }

    public void close(){
        stopAdvertising();
        shutdownServer();
    }

    private final Object mLock = new Object();
    private DistanceMessage mMessage;

    public void addMessage(DistanceMessage message){
            synchronized (mLock) {
                mMessage = message;
            }
            notifyConnectedDevices();
    }

    public byte[] getMessage() {
        synchronized (mLock) {
            if(mMessage!=null) {
                return mMessage.getBytes();
            } else {
                return new byte[0];
            }
        }
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(GattProfile.SERVICE_UUID))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    /*
     * Terminate the advertiser
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Peripheral Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "Peripheral Advertise Failed: "+errorCode);
        }
    };


    private void initServer() {
        BluetoothGattService service =new BluetoothGattService(GattProfile.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //BluetoothGattCharacteristic distanceCharacteristic =
        mDistanceCharacteristic = new BluetoothGattCharacteristic(GattProfile.CHARACTERISIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        service.addCharacteristic(mDistanceCharacteristic);

        mGattServer.addService(service);
    }

    /*
     * Terminate the server and any running callbacks
     */
    private void shutdownServer() {

        if (mGattServer == null) return;

        mGattServer.close();
    }

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.i(TAG, "onConnectionStateChange "
                    +GattProfile.getStatusDescription(status)+" "
                    +GattProfile.getStateDescription(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Device Connected: " + device.getName());
                mConnectedDevices.add(device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Device Disconnected: " + device.getName());
                mConnectedDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.i(TAG, "onCharacteristicReadRequest " + characteristic.getUuid().toString());

            if (GattProfile.CHARACTERISIC_UUID.equals(characteristic.getUuid())) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, getMessage());
            } else {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.i(TAG, "onCharacteristicWriteRequest "+characteristic.getUuid().toString());
        }

    };

    private void notifyConnectedDevices() {
        for (BluetoothDevice device : mConnectedDevices) {
            if(mDistanceCharacteristic!=null) {
                mDistanceCharacteristic.setValue(getMessage());
                mGattServer.notifyCharacteristicChanged(device, mDistanceCharacteristic, false);
            }
        }
    }
}
