package com.example.testncnn;

import android.graphics.Bitmap;

public class NcnnWrapper {
    public native boolean Init(String param, byte[] bin); // 初始化函数
    public native float[] Detect(Bitmap bitmap); // 检测函数
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("NcnnWrapper");
    }
}
