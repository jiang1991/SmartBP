package com.viatom.smartbp.utility;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;

/**
 * Created by wangjiang on 2017/6/14.
 */

public class Constant {
    private static Context mContext;

    public static void init(Context context) {
        if (context == null) {
            mContext = context;
        }
    }

    public static void initDir() {
        download_dir =new File(Environment.getExternalStorageDirectory(), "Download");
    }

    // local directory
    public static File download_dir;

    public static BluetoothDevice selectedDevice;
    public static String selectedDeviceName;

    public static String DEVICE_UPDATE_EN_URL = "https://api.viatomtech.com.cn/update/smartbp/en";
    public static String DEVICE_UPDATE_ZH_URL = "https://api.viatomtech.com.cn/update/smartbp/zh";



    /* Bluetooth Constant */

    final public static String WRTITE_CHARACTERISTIC = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    final public static String READ_CHARACTERISTIC = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";

    final public static int CMD_GET_DEVICE_INFO_LENGTH = 8;

}
