package com.viatom.smartbp.scanner;

import android.bluetooth.BluetoothDevice;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

/**
 * Created by wangjiang on 2017/7/10.
 */

public class SmartBPDevice {
    static final int NO_RSSI = -1000;

    public final BluetoothDevice device;
    public String name;
    public int rssi;

    public SmartBPDevice(final ScanResult scanResult) {
        this.device = scanResult.getDevice();
        this.name = scanResult.getScanRecord() != null ? scanResult.getScanRecord().getDeviceName() : null;
        this.rssi = scanResult.getRssi();
    }

    public SmartBPDevice(final BluetoothDevice device) {
        this.device = device;
        this.name = device.getName();
        this.rssi = NO_RSSI;
    }

    public boolean matches(final ScanResult scanResult) {
        return device.getAddress().equals(scanResult.getDevice().getAddress());
    }
}
