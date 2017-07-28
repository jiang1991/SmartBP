package com.viatom.smartbp.items;

import com.viatom.smartbp.utility.LogUtils;

import java.util.Date;

/**
 * Created by wangjiang on 2017/7/27.
 */

public class Device {

    private DeviceInfo deviceInfo;

    public Device(DeviceInfo deviceInfo) {
        super();
        this.deviceInfo = deviceInfo;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public static class DeviceInfo {
        private String hardware_version, firmware_version, sn, protocol_version;
        private int device_type;
        private Date current_time;

        public DeviceInfo(byte[] buf) {
            if (buf == null) {
                LogUtils.d("device buf is null");
                return;
            }

            hardware_version = String.valueOf(buf[0]);

            StringBuilder fv_b = new StringBuilder();
            for (int i = 1; i<3; i++) {
                fv_b.append((char) buf[i]);
            }
            firmware_version = String.valueOf(fv_b);

            StringBuilder sn_b = new StringBuilder();
            for (int i=4; i<14; i++) {
                sn_b.append((char) buf[i]);
            }
            sn = sn_b.toString();

            LogUtils.d("hv: " + hardware_version + " fv: " + firmware_version + " SN: " + sn);
        }

        public String getSn() {
            return sn;
        }
    }


}
