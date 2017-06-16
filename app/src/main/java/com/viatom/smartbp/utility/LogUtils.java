package com.viatom.smartbp.utility;

import android.util.Log;

/**
 * Created by wangjiang on 2017/6/14.
 */

public class LogUtils {
    /**
     *  Debug flag
     */
    public static boolean DEBUG = true;

    /**
     * Log debug information with tag "VD" if DEBUG flag is true
     * @param string
     */
    public static void d(String string){
        if(DEBUG)
            Log.d("VD",string);
    }

    /**
     * Print debug information if DEBUG flag is true
     * @param string
     */
    public static void println(String string){
        if(DEBUG)
            System.out.println(string);
    }
}
