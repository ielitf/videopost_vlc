package com.ceiv.communication.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Formatter;
//import android.util.Log;
import com.ceiv.log4j.Log;

import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangdawei on 2018/8/15.
 */

public class SystemInfoUtils {

    private final static String TAG = "SystemInfoUtils";

    public static final String UPDATE_APK_NAME = "update.apk";

    public final static String ACTION_CONFIG_IP = "com.ceiv.CONFIG_IP";
    public final static String ACTION_UPDATE_APK = "com.ceiv.UPDATE_APK";
//    public final static String ACTION_CHANGE_BL = "com.ceiv.CHANGE_BL";
//    public final static String ACTION_REBOOT = "com.ceiv.REBOOT";

    //调试模式是否开启
    private static boolean debug_mode_on;

    //媒体操作使用的Object
    private static Object mediaSyncObj;
    public static Object getMediaOptObject() {
        if (null == mediaSyncObj) {
            mediaSyncObj = new Object();
        }
        return mediaSyncObj;
    }

    public static HashMap<String, String> fontToName = null;

    static {
        fontToName = new HashMap<>();
        fontToName.put("heiti", "heiti.ttf");
        fontToName.put("kaiti", "kaiti.ttf");
        fontToName.put("lishu", "lishu.ttf");
        fontToName.put("songti", "songti.ttf");
    }


    private final static long KB = 1024;
    private final static long MB = 1024 * 1024;
    private final static long GB = 1024 * 1024 * 1024;
    private final static long TB = 1024 * 1024 * 1024 * 1024;

    //屏幕方向相关参数
    //真实的旋转方向
    private static int realOrientation;
    //表面上的旋转方向
    private static int fakeOrientation;

    /*
    *   调试模式控制初始化， 默认关闭
    * */
    public static void debugModeInit() {
        debug_mode_on = false;
    }

    /*
    *   handler: Activity中定义的Handler，用来向app主线程发送消息，实现界面的变化（例如增加本机IP的显示）
    *   trunOn: 是否打开调试模式
    * */
    public static void debugModeControl(Handler handler, boolean turnOn) {
        if (turnOn != debug_mode_on) {
            debug_mode_on = turnOn;
            Message debug_msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putBoolean("debug_mode", debug_mode_on);
            debug_msg.what = com.ceiv.communication.ProtocolMessageProcess.MsgWhatDebugMode;
            debug_msg.setData(bundle);
            handler.sendMessage(debug_msg);
        }
    }

    //判断外部存储/SD卡是否可用
    public static boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    //获取手机内部存储空间
    public static String getInternalMemorySize(Context content) {
        File file = Environment.getDataDirectory();
        //    /data目录
        Log.d(TAG, "getDataDirectory:" + file.getPath());
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long blockCountLong = statFs.getBlockCountLong();
        long size = blockCountLong * blockSizeLong;
        return Formatter.formatFileSize(content, size);
    }

    //获取手机内部可用存储空间
    public static String getAvailableInternalMemorySize(Context content) {
        File file = Environment.getDataDirectory();
        //    /data目录
        Log.d(TAG, "getDataDirectory:" + file.getPath());
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long availableBlockCountLong = statFs.getAvailableBlocksLong();
        long size = availableBlockCountLong * blockSizeLong;
        return Formatter.formatFileSize(content, size);
    }

    //获取手机外部存储空间
    public static String getExternalMemorySize(Context content) {
        File file = Environment.getExternalStorageDirectory();
        //    /mnt/sdcard -> /mnt/internal_sd目录
        Log.d(TAG, "getExternalStorageDirectory:" + file.getPath());
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long blockCountLong = statFs.getBlockCountLong();
        long size = blockCountLong * blockSizeLong;
        return Formatter.formatFileSize(content, size);
    }

    //获取手机外部可用存储空间，以字符串形式输出
    public static String getAvailableExternalMemorySize(Context content) {
        File file = Environment.getExternalStorageDirectory();
        //    /mnt/sdcard -> /mnt/internal_sd目录
        Log.d(TAG, "getExternalStorageDirectory:" + file.getPath());
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long availableBlockCountLong = statFs.getAvailableBlocksLong();
        long size = availableBlockCountLong * blockSizeLong;
        return Formatter.formatFileSize(content, size);
    }

    //获取手机外部可用存储空间，以字节数目返回
    public static long getAvailableExternalMemorySize() {
        File file = Environment.getExternalStorageDirectory();
        //    /mnt/sdcard -> /mnt/internal_sd目录
        Log.d(TAG, "getExternalStorageDirectory:" + file.getPath());
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long availableBlockCountLong = statFs.getAvailableBlocksLong();
        return availableBlockCountLong * blockSizeLong;
    }

    //获取指定接口IP地址，默认接口为：eth0
    public static String getNetworkInterfaceIp(String netIfName) {
        String netInterfaceName;
        if ("".equals(netIfName) || null == netIfName) {
            netInterfaceName = "eth0";
        } else {
            netInterfaceName = netIfName;
        }

        String ipAddr = null;
        try {
            NetworkInterface ni = NetworkInterface.getByName(netInterfaceName);

            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                InetAddress inetAddress = address.nextElement();
                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                    ipAddr = inetAddress.getHostAddress().toString();
                    Log.d(TAG, "Network Interface \"" + netInterfaceName + "\" has IP:" + ipAddr);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "get Network Interface\"" + netInterfaceName + "\" info failed");
            e.printStackTrace();
            return null;
        }
        return ipAddr;
    }

    //获取指定接口MAC地址，默认接口为：eth0
    public static String getNetworkInterfaceMac(String netIfName) {
        String netInterfaceName;
        if ("".equals(netIfName) || null == netIfName) {
            netInterfaceName = "eth0";
        } else {
            netInterfaceName = netIfName;
        }

        String macStr = null;
        try {
            NetworkInterface ni = NetworkInterface.getByName(netInterfaceName);
            byte[] mac = ni.getHardwareAddress();
            macStr = MACByte2Hex(mac);
        } catch (Exception e) {
            Log.e(TAG, "get Network Interface\"" + netInterfaceName + "\" info failed");
            e.printStackTrace();
            return null;
        }
        return macStr;
    }

    //将字节数组存储的mac地址装换成字符串形式
    private static String MACByte2Hex(byte[] mac) {

        StringBuilder stringBuilder = new StringBuilder();
        String stmp = null;
        int len = mac.length;
        for (int n = 0; n < len; n++) {
            stmp = Integer.toHexString(mac[n] & 0xFF);
            if (stmp.length() == 1) {
                stringBuilder.append("0").append(stmp);
            } else {
                stringBuilder.append(stmp);
            }
            if (n < len - 1) {
                stringBuilder.append(":");
            }
        }
        return stringBuilder.toString().toUpperCase().trim();
    }

    /*
    *   配置系统网络参数
    *
    *   context:    上下文
    *   ip:         要设置的静态IP地址
    *   nm:         子网掩码
    *   gw:         网关地址
    *   dns:        域名服务器地址
    *
    * */
    public static void setSystemNetworkInfo(Context context, String ip, String nm, String gw, String dns) {

        if (null == context) {
            Log.e(TAG, "Context can't be null");
            return;
        }

        if (!checkIPConfigration(ip, nm, gw, dns)) {
            Log.e(TAG, "Invalid arguments!");
            return;
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_CONFIG_IP);
        intent.putExtra("ip", ip);
        intent.putExtra("nm", nm);
        intent.putExtra("gw", gw);
        intent.putExtra("dns", dns);
        Log.d(TAG, "send broadcast to Launcher to change net config!");
        context.sendBroadcast(intent);
    }

    public static boolean checkIPConfigration(String ip, String nm, String gw, String dns) {

        //判断IP地址是否是合法的
        if (!isIpAddr(ip) || !isIpAddr(gw) || !isIpAddr(dns) || !isNetmask(nm)) {
            return false;
        }
        return true;
    }

    /*
    *   调整系统背光参数
    *
    *   context:    上下文
    *   value:      背光值 最小0 最大255
    *   save        是否保存
    *
    * */
    public static void setSystemBackLight(Context context, int value, boolean save) {

        //南宁项目的设备背光是由单片机控制的，所以这里我们不做操作。
//        if (null == context) {
//            Log.e(TAG, "Context can't be null");
//            return;
//        }
//        if (value < 0) {
//            value = 0;
//        }
//        if (value > 255) {
//            value = 255;
//        }
//        Intent intent = new Intent();
//        intent.setAction(ACTION_CHANGE_BL);
//        intent.putExtra("blValue", value);
//        intent.putExtra("save", save);
//
//        context.sendBroadcast(intent);
    }

    /*
    *   请求进行apk升级
    *   会对待升级的apk进行检验，如果其包名不和当前程序的包名，则不进行升级，并删除之
    *   context:    上下文
    *   apkPath:    待升级的apk路径
    *
    * */
    public static void requestUpdateApk(Context context, String apkPath) {

        if (null == context) {
            Log.e(TAG, "Context can't be null");
            return;
        }
        if (null == apkPath || "".equals(apkPath)) {
            Log.e(TAG, "Invalid argument: apkFile path");
        }
        PackageManager packageManager = context.getPackageManager();
        PackageInfo pinfo = packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        String curName = getCurrentPkgName(context);
        boolean needDeleteApk = false;
        if (null == pinfo) {
            Log.e(TAG, "Can't obtain apk information");
            needDeleteApk = true;
        } else {
            if (!pinfo.packageName.equals(curName)) {
                Log.e(TAG, "Current pkg name: " + curName + ", update apk pkg name: " + pinfo.packageName);
                needDeleteApk = true;
            }
        }
        if (needDeleteApk) {
            try {
                new File(apkPath).delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_APK);
            intent.putExtra("path", apkPath);
            context.sendBroadcast(intent);
        }
    }

    //获取当前应用包名
    public static String getCurrentPkgName(Context context) {
        //获取当前应用pid
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (info.pid == pid) {
                return info.processName;
            }
        }
        return "";
    }

    //重启设备
    public static void rebootDevice(Context context) {

        //设备重启采用三全视讯提供的接口，所以这里不在向我们的Launcher发送广播
//        Intent intent = new Intent();
//        intent.setAction("android.intent.action.sendkey");
//        intent.putExtra("keycode", 1234);
//        context.sendBroadcast(intent);


//        if (null == context) {
//            Log.e(TAG, "Context can't be null");
//            return;
//        }
//
//        Intent intent = new Intent();
//        intent.setAction(ACTION_REBOOT);
//        context.sendBroadcast(intent);
    }

//    public static void screenShotRequest(Activity activity, String path) {
//
//        if (null == activity || null == path) {
//            Log.e(TAG, "");
//            return;
//        }
//
//
//        Bitmap bitmap = shotActivity(activity);
//
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
//        String date = simpleDateFormat.format(new Date());
//        String fileName = "Screenshot_" + date + ".jpg";
//        File file = new File(path + "/" + fileName);
//        FileOutputStream out = null;
//        try {
//            out = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//
//        } catch (Exception e) {
//            Log.e(TAG, "");
//        } finally {
//            try {
//                if (null != out) {
//                    out.flush();
//                    out.close();
//                }
//            } catch (IOException e) {
//
//            }
//
//        }
//
//    }

//    private static Bitmap shotActivity(Activity activity) {
//        View view = activity.getWindow().getDecorView();
//        view.setDrawingCacheEnabled(true);
//        view.buildDrawingCache();
//
//        Bitmap bp = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
//
//        view.setDrawingCacheEnabled(false);
//        view.destroyDrawingCache();
//
//
//        return bp;
//    }

    //将文件大小自动转换成字符串模式
    public static String fileSizeToString(long size) {

        if (size < 0) {
            return null;
        }
        if (size > KB) {
            if (size > MB) {
                if (size > GB) {
                    if (size > TB) {
                        return String.format("%.2f", (size / (double)TB)) + "TB";
                    } else {
                        return String.format("%.2f", (size / (double)GB)) + "GB";
                    }
                } else {
                    return String.format("%.2f", (size / (double)MB)) + "MB";
                }
            } else {
                return String.format("%.2f", (size / (double)KB)) + "KB";
            }
        } else {
            return size + "Byte";
        }
    }

    //IP地址是否符合规则
    public static boolean isIpAddr(String ip) {
        try {
            InetAddress ia = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

    //子网掩码是否符合规则
    public static boolean isNetmask(String mask) {

        String regEx1 = "255\\.255\\.255\\.(0|128|192|224|240|248|252|254)";
        String regEx2 = "255\\.255\\.(0|128|192|224|240|248|252|254|255)\\.0";
        String regEx3 = "255\\.(0|128|192|224|240|248|252|254|255)\\.0\\.0";

        Pattern pattern1 = Pattern.compile(regEx1);
        Pattern pattern2 = Pattern.compile(regEx2);
        Pattern pattern3 = Pattern.compile(regEx3);

        return pattern1.matcher(mask).matches() || pattern2.matcher(mask).matches() || pattern3.matcher(mask).matches();
    }

    public static boolean orientationInit(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        String rotStr = null;
        boolean getOrientation = false;
        if (rotation == Surface.ROTATION_0) {
            getOrientation = true;
            rotStr = "ROTATION_0";
        } else if (rotation == Surface.ROTATION_90) {
            getOrientation = true;
            rotStr = "ROTATION_90";
        } else if (rotation == Surface.ROTATION_180) {
            getOrientation = true;
            rotStr = "ROTATION_180";
        } else if (rotation == Surface.ROTATION_270) {
            getOrientation = true;
            rotStr = "ROTATION_270";
        } else {
            getOrientation = false;
        }
        if (getOrientation) {
            realOrientation = rotation;
            fakeOrientation = rotation;
            Log.d(TAG, "Current Rotation: " + rotStr);
            return true;
        } else {
            Log.d(TAG, "Can't get Start Rotation");
            return false;
        }
    }

    public static void setRotation(Activity activity, int rotation) {
        String action = "android.intent.action.sendkey";

        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra("keycode", 1242);
        switch (rotation) {
            case 0:
                intent.putExtra("screen_num", Surface.ROTATION_0);
                break;
            case 90:
                intent.putExtra("screen_num", Surface.ROTATION_90);
                break;
            case 180:
                intent.putExtra("screen_num", Surface.ROTATION_180);
                break;
            case 270:
                intent.putExtra("screen_num", Surface.ROTATION_270);
                break;
            default:
                Log.d(TAG, "Invalid rotation!");
                return;
        }
        activity.sendBroadcast(intent);
    }

    public static void changeOrientation(Activity activity, int orientation) {

        String action = "android.intent.action.sendkey";

        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                if (realOrientation == Surface.ROTATION_270) {
                    realOrientation = Surface.ROTATION_90;
                    fakeOrientation = Surface.ROTATION_270;
                    Intent intent = new Intent();
                    intent.setAction(action);
                    intent.putExtra("keycode", 1242);
                    intent.putExtra("screen_num", realOrientation);
                    activity.sendBroadcast(intent);
                }
                activity.setRequestedOrientation(orientation);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                if (realOrientation == Surface.ROTATION_90) {
                    realOrientation = Surface.ROTATION_270;
                    fakeOrientation = Surface.ROTATION_90;
                    Intent intent = new Intent();
                    intent.setAction(action);
                    intent.putExtra("keycode", 1242);
                    intent.putExtra("screen_num", realOrientation);
                    activity.sendBroadcast(intent);
                }
                activity.setRequestedOrientation(orientation);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                if (realOrientation == Surface.ROTATION_180) {
                    realOrientation = Surface.ROTATION_0;
                    fakeOrientation = Surface.ROTATION_180;
                    Intent intent = new Intent();
                    intent.setAction(action);
                    intent.putExtra("keycode", 1242);
                    intent.putExtra("screen_num", realOrientation);
                    activity.sendBroadcast(intent);
                }
                activity.setRequestedOrientation(orientation);
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                if (realOrientation == Surface.ROTATION_0) {
                    realOrientation = Surface.ROTATION_180;
                    fakeOrientation = Surface.ROTATION_0;
                    Intent intent = new Intent();
                    intent.setAction(action);
                    intent.putExtra("keycode", 1242);
                    intent.putExtra("screen_num", realOrientation);
                    activity.sendBroadcast(intent);
                }
                activity.setRequestedOrientation(orientation);
                break;
            default:
                Log.d(TAG, "Unsupport orientaion!");
                break;
        }
    }

    /*
     *  该功能仅在三全视讯RK3288板卡上有效
     *  目前函数没有进行参数检验，需调用者自己确保数据合法、准确
     *
     */
    public static void setSystemTime(Context context, int year, int mon, int day, int hour, int min, int sec) {

        Intent intent = new Intent("android.intent.action.sendkey");
        intent.putExtra("keycode", 1243);
        intent.putExtra("year", year);
        intent.putExtra("month", mon);
        intent.putExtra("day", day);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", min);
        intent.putExtra("second", sec);
        context.sendBroadcast(intent);
    }

    /*
     *  目前该功能仅在三全视讯RK3288板卡上有效
     *  函数暂时不进行参数检验，需调用者自己确保数据合法、准确
     *  timeZone的例子：GMT+02:00
     */
    public static void setSystemTimeZone(Context context, String timeZone) {

        Intent intent = new Intent("android.intent.action.sendkey");
        intent.putExtra("keycode", 1244);
        intent.putExtra("timezone", timeZone);
        context.sendBroadcast(intent);
    }

    private static HashMap<String, Typeface> gloTypefaceMap;

    public static Typeface getTypeface(Context context, String name) {

        if (gloTypefaceMap == null) {
            gloTypefaceMap = new HashMap<>();
        }
        if (gloTypefaceMap.containsKey(name)) {
            return gloTypefaceMap.get(name);
        }
        try {
            AssetManager mgr = context.getAssets();
            Typeface tmp = Typeface.createFromAsset(mgr, "fonts/" + name);
            gloTypefaceMap.put(name, tmp);
            return gloTypefaceMap.get(name);
        } catch (Exception e) {
            Log.e(TAG, "get Typeface: " + name + "error!");
            e.printStackTrace();
            return null;
        }
    }

    public interface ApplicationOperation {
        //该接口用来实现子线程获取Activity或者上下文Context
        //主要用来在子线程中发送广播，进行截图
        public Activity getActivity();

        //该接口用来获取界面Activity的handler，然后子线程可以通知主界面
        //那些显示的内容需要改变。
        public Handler getMainHandler();
    }

    public static String getMqttClientId() {
        return getNetworkInterfaceIp(null) + getNetworkInterfaceMac(null);
    }

    //-1表示未获取到真正的版本编号
    private static int versionCode = -1;
    //获取APK版本编号
    public static int getApkVersionCode(Context context) {
        if (versionCode > 0) {
            return versionCode;
        }
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            return versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't find Apk Version Code!");
            e.printStackTrace();
            versionCode = -1;
            return 0;
        }
    }

    private static String versionName = null;
    //获取APK版本名称
    public static String getApkVersionName(Context context) {
        if (null != versionName) {
            return versionName;
        }
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't find Apk Version Name!");
            e.printStackTrace();
            versionName = null;
            return "";
        }
    }

    /* 以太网口网线是否连接 */
    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netWorkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        /*
         *   经测试通过isConnected确实可以检查设备的以太网口网线是否连接，
         *   而不管网线是否连接，isAvailable都返回true，所以不能用isAvailable来判断网线状态
         *
         *   还有一种检查以太网口网线是否连接的方法是调用命令：
         *   cat /sys/class/net/eth0/carrier
         *   如果连接了网线，返回1，否则返回0
         * */
        return netWorkInfo != null && netWorkInfo.isConnected();
    }

    /*
     *   用来判断以太网口eth0的默认路由是否真的添加了
     *   目前该函数通过调用命令行"ip route list table all"，并分析其结果来
     *   判断以太网口的默认路由是否添加了
     *   需要注意的是该函数可能会阻塞，不应该在主线程中调用！
     * */
    public static boolean isEthernetInterfaceDefaultRouteSet() {

        Process process = null;
        BufferedReader bufferedReader = null;
        String line;
        StringBuilder result;
        boolean isRouteSet = false;

        try {
            process = Runtime.getRuntime().exec("ip route list table all");
            result = new StringBuilder();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("default via")) {
                    result.append(line);
                }
            }
            process.waitFor();

            Log.d(TAG, "result: \n" + result.toString());
            //任意开头，任意结尾，中间的格式如下
            String patterns = ".*default via (\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}) dev eth0.*";
            Pattern p1 = Pattern.compile(patterns);
            Matcher matcher = p1.matcher(result);
            if (matcher.matches()) {
                String routeIp = matcher.group(1);
                Log.d(TAG, "ethernet route ip: " + routeIp);
                isRouteSet = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "exec<ip route list table all> failed!", e);
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "close bufferReader failed!", e);
                    e.printStackTrace();
                }
            }
        }
        return isRouteSet;
    }

    /*
     *   检查和某一地址的网络连通性，通过ping命令来判断
     *   所以本函数会出现阻塞，不应该在主线程（界面线程）中调用
     *
     *   参数：ipAddr  对方的IP地址，注意调用者需要保证
     *                   该参数实际值的合法性、有效性
     *
     * */
    public static boolean testConnectivityWithAddress(String ipAddr) {

        Process process = null;
        BufferedReader bufferedReader = null;
        String line = null;
        StringBuilder result = null;
        boolean isConnected = true;

        if (null == ipAddr || !isIpAddr(ipAddr)){
            Log.e(TAG, "Invalid Ip Address: " + ipAddr);
            return false;
        }
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
                Log.d(TAG, "ping cmd ret: " + ret);
                isConnected = false;
            } else {
                Log.d(TAG, "Network connectivity with address:" + ipAddr + " ok!");
                isConnected = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "ping cmd error!", e);
            isConnected = false;
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "close BufferReader error!", e);
                    e.printStackTrace();
                }
            }
        }
        return isConnected;
    }

    /*
     *   png图片转jpg图片，实现图片压缩，便于图片上传（主要用在设备“信息回显”功能上）
     *   参数： pngPic  输入的PNG图片完整路径
     *          jpgPic  输出的JPG图片完整路径
     *
     *   返回值： 是否压缩成功
     * */
    public static boolean pngPictureCompressedToJpg(String pngPic, String jpgPic) {
        //默认quality值为60
        return pngPictureCompressedToJpg(pngPic, jpgPic, 40);
    }

    /*
     *   png图片转jpg图片，实现图片压缩，便于图片上传（主要用在设备“信息回显”功能上）
     *   参数： pngPic  输入的PNG图片完整路径
     *          jpgPic  输出的JPG图片完整路径
     *          quality 压缩的程度，0 - 100  0代表最高压缩率，100代表最高质量
     *
     *   返回值： 是否压缩成功
     * */
    public static boolean pngPictureCompressedToJpg(String pngPic, String jpgPic, int quality) {

        if (pngPic == null || jpgPic == null || quality < 0 || quality > 100) {
            Log.e(TAG, "pngPictureCompressedToJpg: Invalid arguments!");
            return false;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(pngPic);
        BufferedOutputStream bos = null;
        boolean compressedSuccess = true;

        try {
            bos = new BufferedOutputStream(new FileOutputStream(jpgPic));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)) {
                compressedSuccess = true;
            } else {
                compressedSuccess = false;
            }
            bos.flush();
        } catch (Exception e) {
            Log.e(TAG, "Compress Image failed!", e);
            compressedSuccess = false;
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close output stream error!", e);
                compressedSuccess = false;
            }
        }
        return compressedSuccess;
    }

}













