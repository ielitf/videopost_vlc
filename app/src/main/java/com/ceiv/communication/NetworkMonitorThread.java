package com.ceiv.communication;

//import android.util.Log;

import com.ceiv.communication.utils.SystemInfoUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.ceiv.log4j.Log;

public class NetworkMonitorThread extends Thread {

    private final static String TAG = NetworkMonitorThread.class.getSimpleName();

    public interface ConnectSuccessCallBack {
        public void connectSuccess();
    }

    private String ipAddr;
    private ConnectSuccessCallBack callBack;
    private boolean stop;


    public NetworkMonitorThread(String ipAddr, ConnectSuccessCallBack callBack) throws Exception {
        if (ipAddr == null || !SystemInfoUtils.isIpAddr(ipAddr)|| callBack == null) {
            throw new Exception("Invalid Parameters!");
        }
        this.ipAddr = ipAddr;
        this.callBack = callBack;
    }

    public void stopMonitor() {
        stop = true;
        interrupt();
    }

    @Override
    public void run() {

        Process process = null;
        BufferedReader bufferedReader = null;
        String line = null;
        StringBuilder result = null;
        boolean needCallBack = false;
        stop = false;

        while (!stop) {
            try {
                Log.d(TAG, "start ping " + ipAddr);
                process = Runtime.getRuntime().exec("ping -c 4 " + ipAddr);
                result = new StringBuilder();
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line + "\n");
                }
                int ret = process.waitFor();
                Log.d(TAG, "ping result: \n" + result.toString());
                if (0 != ret) {
                    //网络不通，等待30s后继续尝试连接
                    Log.d(TAG, "ping cmd ret: " + ret);
                    sleep(30 * 1000);
                } else {
                    Log.d(TAG, "Connect " + ipAddr + " success!");
                    needCallBack = true;
                    stop = true;
                }
            } catch (Exception e) {
                if (stop) {
                    Log.d(TAG, "stop network monitor");
                    break;
                } else {
                    e.printStackTrace();
                }
            } finally {
                if (process != null) {
                    process.destroy();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (needCallBack) {
                //网络已经连通，调用回调之后，自动结束
                callBack.connectSuccess();
                break;
            }
        }
    }
}
