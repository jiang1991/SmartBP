package com.viatom.smartbp.profile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by wangjiang on 2017/6/8.
 */

public class BPManager extends BleManager<BleManagerCallbacks> {
//    public final static UUID DEVICE_SERVICE_UUID = UUID.fromString("0x1800");
//    private static final UUID DEVICE_NAME_C_UUID = UUID.fromString("0x2A00");
//
//    public final static UUID BATTERY_SERVICE_UUID = UUID.fromString("0x180F");
//    private static final UUID BATTERY_VALUE_C_UUID = UUID.fromString("0x2A19");
//
//    public final static UUID DEVICE_INFO_SERVICE = UUID.fromString("0x180A");
//    private static final UUID DEVICE_SN_C_UUID = UUID.fromString("0x2A24");
//    private static final UUID DEVICD_VERSION_C_UUID = UUID.fromString("0x2A26");
//
//    public final static UUID DFU_SERVICE_UUID = UUID.fromString("0xFE59");
//    private final static UUID DUF_C_UUID = UUID.fromString("8ec90003-f315-4f60-9fb8-838830daea50");

    private BluetoothGattCharacteristic nameCharacteristic, batteryCharacteristic;

    private static BPManager managerInstance = null;

    public static synchronized BPManager getBPManager(final Context context) {
        if (managerInstance == null){
            managerInstance = new BPManager(context);
        }
        return managerInstance;
    }

    private BPManager(final Context context) {
        super(context);
    }

    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
        @Override
        protected boolean isRequiredServiceSupported(BluetoothGatt gatt) {
//            BluetoothGattService service = gatt.getService(DEVICE_SERVICE_UUID);
//            if (service != null) {
//                nameCharacteristic = service.getCharacteristic(DEVICE_NAME_C_UUID);
//            }
//            BluetoothGattService batteryService = gatt.getService(BATTERY_SERVICE_UUID);
//            if (batteryService != null) {
//                batteryCharacteristic = service.getCharacteristic(BATTERY_VALUE_C_UUID);
//            }
            return nameCharacteristic != null;
        }

        @Override
        protected Deque<Request> initGatt(BluetoothGatt gatt) {
            final LinkedList<Request> requests = new LinkedList<>();
            if (nameCharacteristic != null)
                requests.add(Request.newReadRequest(batteryCharacteristic));
                requests.add(Request.newReadRequest(nameCharacteristic));
            return requests;
        }

        @Override
        protected void onDeviceDisconnected() {
            batteryCharacteristic = null;
            nameCharacteristic = null;
        }
    };

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }
}
