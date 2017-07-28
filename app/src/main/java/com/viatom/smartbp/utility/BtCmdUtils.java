package com.viatom.smartbp.utility;

/**
 * Created by wangjiang on 2017/7/27.
 */

public class BtCmdUtils {

        public static byte[] getDeviceInfo() {
            byte[] cmd = new byte[Constant.CMD_GET_DEVICE_INFO_LENGTH];

            // header
            cmd[0] = (byte) 0xA5;
            cmd[1] = (byte) 0x00; // package number
            cmd[2] = (byte) 0x00; // if need resend response

            cmd[3] = (byte) 0xE1; // cmd
            cmd[4] = (byte) 0x00;
            cmd[5] = (byte) 0x00;
            cmd[6] = (byte) 0x00;
            cmd[7] = (byte) 0x00;

            return cmd;
        }

}
