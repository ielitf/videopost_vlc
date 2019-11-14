package com.ceiv.communication.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Created by zhangdawei on 2018/8/16.
 */

public class DeviceInfoUtils {

    private final static String TAG = "DeviceInfoUtils";

    private final static String deviceInfoXmlName = "DeviceInfo.xml";
    private final static String defaultDeviceInfoFile = "DefaultDeviceInfo.xml";
    private final static String deviceInfoFormat = "DeviceInfoFormat.xml";

    public static Context context = null;
    public final static String MediaPath = "media";
    public final static String VideoPath = "video";
    public final static String PicturePath = "picture";
    public final static String TextPath = "text";

    public final static int IdentifyLen = 20;

    private static String formatString;

    private static DeviceInfo defaultDevInfo = null;

    //确保读的时候文件不会被改变
    private static Object rwLock;
    private static HashMap<String, Integer> contentNameToIndex = null;
    static {
        Log.d(TAG, "static func run start");
        rwLock = new Object();
        contentNameToIndex = new HashMap<String, Integer>();
        contentNameToIndex.put("Identify", 1);
        contentNameToIndex.put("RouteLine", 17);
        contentNameToIndex.put("StationID", 18);
        contentNameToIndex.put("ServerIp", 2);
        contentNameToIndex.put("ServerPort2", 3);
        contentNameToIndex.put("ServerPort", 4);
        contentNameToIndex.put("InfoPublishServer", 5);
        contentNameToIndex.put("DevType", 6);
        contentNameToIndex.put("CurrentStationID", 7);
        contentNameToIndex.put("NextStationID", 8);
        contentNameToIndex.put("ThemeStyle", 9);
        contentNameToIndex.put("TextType", 10);
        contentNameToIndex.put("TextColor", 11);
        contentNameToIndex.put("TextSize", 12);
        contentNameToIndex.put("TextFont", 13);
        contentNameToIndex.put("TextSpeed", 14);
        contentNameToIndex.put("PicDispTime", 15);
        contentNameToIndex.put("VideoPlayTime", 16);
        Log.d(TAG, "static func run end");
    }

    //必须先调用
    public static boolean DeviceInfoUtilsInit(Context con) {
        if (context != null) {
            //已经初始化
            return true;
        }
        if (null == con) {
            Log.e(TAG, "Invalid Context!");
            return false;
        }
        context = con;
        boolean readSuccess = true;
        BufferedInputStream bis = null;
        byte[] format;
        int readSize = 0;
        int fileSize = 0;
        int ret;
        try {
            bis = new BufferedInputStream(context.getAssets().open(deviceInfoFormat));
            fileSize = bis.available();
            if (fileSize <= 0) {
                throw new Exception("invalid format file size");
            }
            Log.d(TAG, "format file size: " + fileSize);
            format = new byte[fileSize];
            while ((ret = bis.read(format, readSize, fileSize - readSize)) > 0) {
                Log.d(TAG, "read ret: " + ret);
                readSize += ret;
            }
            if (readSize != fileSize) {
                throw new Exception("read incomplete file");
            }
            formatString = new String(format, "UTF-8");
            Log.d(TAG, "formatString: " + formatString);
        } catch (Exception e) {
            Log.e(TAG, "Read format string error!");
            e.printStackTrace();
            readSuccess = false;
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close format file error!");
                e.printStackTrace();
                readSuccess = false;
            }
        }
        if (!readSuccess) {
            return readSuccess;
        }
        //后面继续读取默认的设备配置文件
        if (getDefaultDevInfo() == null) {
            readSuccess = false;
        } else {
            readSuccess = true;
        }
        return readSuccess;
    }


//    //必须先调用
//    public static boolean DeviceInfoUtilsInit(Context con) {
//        if (context != null) {
//            //已经初始化
//            return true;
//        }
//        if (null == con) {
//            Log.e(TAG, "Invalid Context!");
//            return false;
//        }
//        context = con;
//        boolean readSuccess = true;
//        BufferedInputStream bis = null;
//        byte[] format;
//        int readSize = 0;
//        int fileSize = 0;
//        int ret;
//        try {
//            bis = new BufferedInputStream(context.getAssets().open(deviceInfoFormat));
//            fileSize = bis.available();
//            if (fileSize <= 0) {
//                throw new Exception("invalid format file size");
//            }
//            Log.d(TAG, "format file size: " + fileSize);
//            format = new byte[fileSize];
//            while ((ret = bis.read(format, readSize, fileSize - readSize)) > 0) {
//                Log.d(TAG, "read ret: " + ret);
//                readSize += ret;
//            }
//            if (readSize != fileSize) {
//                throw new Exception("read incomplete file");
//            }
//            formatString = new String(format, "UTF-8");
//            Log.d(TAG, "formatString: " + formatString);
//        } catch (Exception e) {
//            Log.e(TAG, "Read format string error!");
//            e.printStackTrace();
//            readSuccess = false;
//        } finally {
//            try {
//                if (bis != null) {
//                    bis.close();
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "close format file error!");
//                e.printStackTrace();
//                readSuccess = false;
//            }
//        }
//        return readSuccess;
//    }

    private static DeviceInfo gloDeviceInfo = null;

    //解析设备配置xml文件
    public static DeviceInfo getDeviceInfoFromFile() {

        if (gloDeviceInfo != null) {
            return gloDeviceInfo;
        }
        if (!deviceInfoFileCheck()) {
            Log.e(TAG, "Device Info File Check error!");
            return null;
        }
        String configFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + deviceInfoXmlName;
        File devInfoXmlFile = null;
        byte[] fileContent = null;
        synchronized (rwLock) {
            devInfoXmlFile = new File(configFile);
            Long fileLen = devInfoXmlFile.length();
            fileContent = new byte[fileLen.intValue()];
            try {
                FileInputStream fileInputStream = new FileInputStream(devInfoXmlFile);
                fileInputStream.read(fileContent);
                fileInputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Read deviceInfo xml file failed");
                e.printStackTrace();
                return null;
            }
        }
        Log.e(TAG, "配置文件："+ new String(fileContent));
        DeviceInfo deviceInfo = parseDeviceInfoFromXmlByteArray(fileContent);
        if (deviceInfo == null) {
            deviceInfo = new DeviceInfo();
        }
        Log.d(TAG, "Read complete Info(device Info flag:" +
                String.format("%x", deviceInfo.getInfoFlag()) + ")? " +
                (deviceInfo.isInfoComplete() ? "yes" : "no"));
        if (!deviceInfo.isInfoComplete()) {////////////////////////////////////////////////////////////
            return null;
        }
        gloDeviceInfo = deviceInfo;
        if (gloDeviceInfo.getDevType() != defaultDevInfo.getDevType()) {
            //修正设备类型
            gloDeviceInfo.setDevType(defaultDevInfo.getDevType());
            saveDeviceInfoToFile(gloDeviceInfo);
        }
        return gloDeviceInfo;
    }

    /*
     *   兼容旧版本配置文件的设备信息读取接口
     *   考虑到软件升级后设备的配置文件可能会添加/删除某些字段
     *   而每次都删除旧版本的配置文件，然后重新写入新的配置文件后再重新配置
     *   这样会造成很大的工作量，所以这个接口相比上面的接口会将新旧版本都有的
     *   配置参数保留下来，写入到新的配置文件中，以此减少工作量
     *
     * */
    public static DeviceInfo getDevInfFrmFileCompatibleOldVer() {

        if (gloDeviceInfo != null) {
            return gloDeviceInfo;
        }
        if (!deviceInfoFileCheck()) {
            return null;
        }
        String configFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + deviceInfoXmlName;
        File devInfoXmlFile = null;
        byte[] fileContent = null;
        DeviceInfo deviceInfo = null;
        try {
            synchronized (rwLock) {
                devInfoXmlFile = new File(configFile);
                Long fileLen = devInfoXmlFile.length();
                fileContent = new byte[fileLen.intValue()];

                FileInputStream fileInputStream = new FileInputStream(devInfoXmlFile);
                fileInputStream.read(fileContent);
                fileInputStream.close();
                fileInputStream = null;
            }
            deviceInfo = parseDeviceInfoFromXmlByteArray(fileContent);
            if (deviceInfo == null) {
                deviceInfo = new DeviceInfo();
            }
            Log.d(TAG, "Read complete Info(device Info flag:" +
                    String.format("%x", deviceInfo.getInfoFlag()) + ")? " +
                    (deviceInfo.isInfoComplete() ? "yes" : "no"));
            if (!deviceInfo.isInfoComplete()) {
                /*
                 *   如果数据不完整，需要将缺失的字段填充入默认的值
                 *   这里我们可以确保默认值是不为空的，所以不检测默认值是否完整
                 * */
                DeviceInfo defDeviceInfo = getDefaultDevInfo();
                if (defDeviceInfo == null) {
                    throw new Exception("Get Default Device Info failed!");
                }
                if (!deviceInfo.isIdentifySet()) {
                    deviceInfo.setIdentify(defDeviceInfo.getIdentify());
                }
                if (!deviceInfo.isServerIpSet()) {
                    deviceInfo.setServerIp(defDeviceInfo.getServerIp());
                }
                if (!deviceInfo.isServerPortSet()) {
                    deviceInfo.setServerPort(defDeviceInfo.getServerPort());
                }
                if (!deviceInfo.isInfoPublishServerSet()) {
                    deviceInfo.setInfoPublishServer(defDeviceInfo.getInfoPublishServer());
                }
                if (!deviceInfo.isDevTypeSet()) {
                    deviceInfo.setDevType(defDeviceInfo.getDevType());
                }
                if (!deviceInfo.isCurrentStationIdSet()) {
                    deviceInfo.setCurrentStationId(defDeviceInfo.getCurrentStationId());
                }
                if (!deviceInfo.isNextStationIdSet()) {
                    deviceInfo.setNextStationId(defDeviceInfo.getNextStationId());
                }
                if (!deviceInfo.isThemeStyleSet()) {
                    deviceInfo.setThemeStyle(defDeviceInfo.getThemeStyle());
                }
                if (!deviceInfo.isTextTypeSet()) {
                    deviceInfo.setTextType(defDeviceInfo.getTextType());
                }
                if (!deviceInfo.isTextColorSet()) {
                    deviceInfo.setTextColor(defDeviceInfo.getTextColor());
                }
                if (!deviceInfo.isTextSizeSet()) {
                    deviceInfo.setTextSize(defDeviceInfo.getTextSize());
                }
                if (!deviceInfo.isTextFontSet()) {
                    deviceInfo.setTextFont(defDeviceInfo.getTextFont());
                }
                if (!deviceInfo.isTextSpeedSet()) {
                    deviceInfo.setTextSpeed(defDeviceInfo.getTextSpeed());
                }
                if (!deviceInfo.isPicDispTimeSet()) {
                    deviceInfo.setPicDispTime(defDeviceInfo.getPicDispTime());
                }
                if (!deviceInfo.isVideoPlayTimeSet()) {
                    deviceInfo.setVideoPlayTime(defDeviceInfo.getVideoPlayTime());
                }
                defDeviceInfo = null;
                /*
                 *   填充完默认的值后必须首先将新的配置保存到文件中
                 * */
                if (!saveDeviceInfoToFile(deviceInfo)) {
                    throw new Exception("save new Device Info to file failed!");
                }
            }
            gloDeviceInfo = deviceInfo;
            if (gloDeviceInfo.getDevType() != defaultDevInfo.getDevType()) {
                gloDeviceInfo.setDevType(defaultDevInfo.getDevType());
                saveDeviceInfoToFile(gloDeviceInfo);
            }
            return gloDeviceInfo;

        } catch (Exception e) {
            Log.e(TAG, "Get Device Info from file failed!");
            e.printStackTrace();
            return null;
        }
    }

    /*
     *   从assets中读取默认的设备配置文件，这个主要用在getDevInfFrmFileCompatibleOldVer接口中
     * */
    private static DeviceInfo getDefaultDevInfo() {

        if (defaultDevInfo != null) {
            return defaultDevInfo;
        }
        if (null == context) {
            Log.e(TAG, "must call DeviceInfoUtilsInit first!");
            return null;
        }
        BufferedInputStream bis = null;
        String contentString = null;
        byte[] content;
        int readSize = 0;
        int fileSize = 0;
        int ret;
        try {
            bis = new BufferedInputStream(context.getAssets().open(defaultDeviceInfoFile));
            fileSize = bis.available();
            if (fileSize <= 0) {
                throw new Exception("invalid default Device Info file size");
            }
            Log.d(TAG, "Device Info file size: " + fileSize);
            content = new byte[fileSize];
            while ((ret = bis.read(content, readSize, fileSize - readSize)) > 0) {
                Log.d(TAG, "read ret: " + ret);
                readSize += ret;
            }
            if (readSize != fileSize) {
                throw new Exception("read incomplete file");
            }
            defaultDevInfo = parseDeviceInfoFromXmlByteArray(content);
            if (defaultDevInfo == null) {
                throw new Exception("parse DeviceInfoFromXmlByteArray failed!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Read default Device Info error!");
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close format file error!");
                e.printStackTrace();
            }
            return defaultDevInfo;
        }
    }

    /*
     *   将设备配置信息保存到文件中
     * */
    public static boolean saveDeviceInfoToFile(DeviceInfo deviceInfo) {

        if (!deviceInfoFileCheck()) {
            return false;
        }
        if (!deviceInfo.isInfoComplete()) {
            return false;
        }
        gloDeviceInfo = deviceInfo;

        String configFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + deviceInfoXmlName;
        FileOutputStream fileOutputStream = null;
//        FileWriter fileWriter = null;
        //构建配置文本
        String content = String.format(formatString, deviceInfo.getIdentify(), deviceInfo.getServerIp(), deviceInfo.getServerPort(),
                deviceInfo.getInfoPublishServer(), deviceInfo.getDevType(), deviceInfo.getCurrentStationId(), deviceInfo.getNextStationId(),
                deviceInfo.getThemeStyle(), deviceInfo.getTextType(), deviceInfo.getTextColor(), deviceInfo.getTextSize(), deviceInfo.getTextFont(),
                deviceInfo.getTextSpeed(), deviceInfo.getPicDispTime(), deviceInfo.getVideoPlayTime());
        synchronized (rwLock) {
            try {
                fileOutputStream = new FileOutputStream(configFile);
                fileOutputStream.write(content.getBytes());
                fileOutputStream.flush();
                fileOutputStream.getFD().sync();
//                //以覆盖形式写入
//                fileWriter = new FileWriter(configFile, false);
//                fileWriter.write(content);
//                fileWriter.flush();
            } catch (IOException e) {
                Log.d(TAG, "write device information to xml file failed");
                e.printStackTrace();
                return false;
            } finally {
                try {
//                    if (fileWriter != null) {
//                        fileWriter.close();
//                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "close device information file failed");
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     *   检查设备配置文件是否存在，如果不存在则写入默认的新的配置文件
     * */
    private static boolean deviceInfoFileCheck() {

        if (context == null) {
            Log.e(TAG, "should run setContext first!");
            return false;
        }
        String configFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + deviceInfoXmlName;
        File devInfoXmlFile = new File(configFile);
        if (!devInfoXmlFile.exists()) {
            try {
                File fileParent = devInfoXmlFile.getParentFile();
                if (fileParent != null) {
                    if (!fileParent.exists()) {
                        fileParent.mkdirs();
                    }
                }
                devInfoXmlFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Create new deviceInfoFile failed");
                e.printStackTrace();
                return false;
            }

            boolean writeSuccess = true;
            InputStream is = null;
            FileOutputStream fos = null;
            byte[] buffer = new byte[1024];
            int bt = 0;
            try {
                is = context.getResources().getAssets().open(defaultDeviceInfoFile);
                fos = new FileOutputStream(devInfoXmlFile);
                while ((bt = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bt);
                }
                fos.flush();
                fos.getFD().sync();
            } catch (Exception e) {
                Log.e(TAG, "Write default DeviceInfo to XmlFile failed!");
                e.printStackTrace();
                writeSuccess = false;
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close DeviceInfo file failed!");
                    e.printStackTrace();
                    writeSuccess = false;
                }
            }
            if (!writeSuccess) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static boolean checkDeviceMediaDirectory() {

        if (!SystemInfoUtils.isExternalStorageAvailable()) {
            Log.e(TAG, "Device media directory unavailable!");
            return false;
        }

        String videoFullPath = Environment.getExternalStorageDirectory() + "/" + MediaPath + "/" + VideoPath;
        String pictureFullPath = Environment.getExternalStorageDirectory() + "/" + MediaPath + "/" + PicturePath;
        String textFullPath = Environment.getExternalStorageDirectory() + "/" + MediaPath + "/" + TextPath;

        File[] files = new File[]{new File(videoFullPath), new File(pictureFullPath), new File(textFullPath)};

        for (File tmp : files) {
            if (tmp.exists()) {
                //如果已经存在
                if (!tmp.isDirectory()) {
                    //如果不是目录
                    tmp.delete();
                    tmp.mkdirs();
                }
            } else {
                //如果不存在
                tmp.mkdirs();
            }
        }

        return true;
    }

    /*
     *   从xml byte[]中解析设备信息
     * */
    public static DeviceInfo parseDeviceInfoFromXmlByteArray(byte[] content) {
        String contentString = null;
        try {
            contentString = new String(content, "UTF-8");
            Log.d(TAG, "XML: " + contentString);
            return parseDeviceInfoFromXmlString(contentString);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupport byte[] content!");
            e.printStackTrace();
            return null;
        }
    }

    /*
     *   从xml String中解析设备信息
     * */
    public static DeviceInfo parseDeviceInfoFromXmlString(String xmlContent) {
        Log.d(TAG, "xmlContent:"+ xmlContent);
        DeviceInfo deviceInfo = new DeviceInfo();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(xmlContent));

            int eventType = xmlPullParser.getEventType();
            Log.d(TAG, "start parse xml");
            while (eventType != xmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        Log.d(TAG, "start doc");
                        break;
                    case XmlPullParser.START_TAG:
                        if (!contentNameToIndex.containsKey(xmlPullParser.getName())) {
                            break;
                        }
                        int index = contentNameToIndex.get(xmlPullParser.getName());
                        Log.i(TAG, "contentNameToIndex:" + index+ "/ name:"+ xmlPullParser.getName());
                        switch (index) {
                            case 1:
                                String identify = xmlPullParser.nextText();
                                Log.d(TAG, "Identify:" + identify);
                                deviceInfo.setIdentify(identify.trim());
                                break;
                            case 17:
                                String routeLine = xmlPullParser.nextText();
                                Log.d(TAG, "routeLine:" + routeLine);
                                deviceInfo.setRouteLine(routeLine.trim());
                                break;
                            case 18:
                                String stationID = xmlPullParser.nextText();
                                Log.d(TAG, "stationID:" + stationID);
                                deviceInfo.setStationID(stationID.trim());
                                break;
                            case 2:
                                String serverIp = xmlPullParser.nextText();
                                Log.d(TAG, "ServerIp:" + serverIp);
                                deviceInfo.setServerIp(serverIp.trim());
                                break;
                            case 3:
                                int serverPort2 = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "ServerPort2:" + serverPort2);
                                deviceInfo.setServerPort2(serverPort2);
                                break;
                            case 4:
                                int serverPort = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "ServerPort:" + serverPort);
                                deviceInfo.setServerPort(serverPort);
                                break;
                            case 5:
                                String infoPublishServer = xmlPullParser.nextText();
                                Log.d(TAG, "InfoPublishServer:" + infoPublishServer);
                                deviceInfo.setInfoPublishServer(infoPublishServer.trim());
                                break;
                            case 6:
                                //十六进制转换
                                int devType = Integer.valueOf(xmlPullParser.nextText(), 16);
                                Log.d(TAG, "DevType:" + devType);
                                deviceInfo.setDevType(devType);
                                break;
                            case 7:
                                int currentStationID = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "CurrentStationID:" + currentStationID);
                                deviceInfo.setCurrentStationId(currentStationID);
                                break;
                            case 8:
                                int nextStationID = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "NextStationID:" + nextStationID);
                                deviceInfo.setNextStationId(nextStationID);
                                break;
                            case 9:
                                int themeStyle = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "ThemeStyle:" + themeStyle);
                                deviceInfo.setThemeStyle(themeStyle);
                                break;
                            case 10:
                                int textType = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "TextType:" + textType);
                                deviceInfo.setTextType(textType);
                                break;
                            case 11:
                                String textColorHex = xmlPullParser.nextText();
                                int textColor = Color.parseColor(textColorHex);
                                Log.d(TAG, "TextColor:" + textColorHex);
                                deviceInfo.setTextColor(textColor);
                                break;
                            case 12:
                                int textSize = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "TextSize:" + textSize);
                                deviceInfo.setTextSize(textSize);
                                break;
                            case 13:
                                String textFont = xmlPullParser.nextText().trim();
                                Log.d(TAG, "TextFont:" + textFont);
                                deviceInfo.setTextFont(textFont);
                                break;
                            case 14:
                                int textSpeed = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "TextSpeed:" + textSpeed);
                                deviceInfo.setTextSpeed(textSpeed);
                                break;
                            case 15:
                                int picDispTime = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "PicDispTime:" + picDispTime);
                                deviceInfo.setPicDispTime(picDispTime);
                                break;
                            case 16:
                                int videoPlayTime = Integer.valueOf(xmlPullParser.nextText());
                                Log.d(TAG, "VideoPlayTime:" + videoPlayTime);
                                deviceInfo.setVideoPlayTime(videoPlayTime);
                                break;
                            default:
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse XML file error!");
            e.printStackTrace();
            return null;
        }
        return deviceInfo;
    }

    public static int getStationIDFromIdentify(String identify)
    {
        int len = identify.length();
        Log.d(TAG, "Identify: " + identify);
        if (IdentifyLen != len) {
            Log.d(TAG, "Invalid Identify!");
            return 1;
        }
        String sub = identify.substring(len - 5, len - 3);
        Log.d(TAG, "substring:" + sub);
        Log.d(TAG, "stationID:" + Integer.valueOf(sub));
        return Integer.valueOf(sub);
    }

    //从识别号中提取设备所属的线路，如，1路，B12等
    public static String getDeviceRouteFromIdentify(String identify) {
        try {
            int len = identify.length();
            if (IdentifyLen != len) {
                throw new Exception("Invalid Identify: " + identify);
            }
            String sub = identify.substring(len - 8, len - 5);
            return sub;
        } catch (Exception e) {
            e.printStackTrace();
            //出现异常则默认是位于1路
            return "";
        }
    }

    //从识别号中提取设备所在的未知（站点编号）
    public static String getDevicePositionFromIdentify(String identify) {
        try {
            int len = identify.length();
            if (IdentifyLen != len) {
                throw new Exception("Invalid Identify: " + identify);
            }
            String sub = identify.substring(len - 5, len - 3);
            return sub;
        } catch (Exception e) {
            e.printStackTrace();
            //出现异常则默认是位于1站
            return "1";
        }
    }

    //从识别号中提取设备所在的方向（上行、下行）
    // 1: 上行（例如18站到1站方向）   2: 下行（例如1站到18站方向）
    public static int getDeviceDirectionFromIdentify(String identify) {
        int dir = 1;
        try {
            int len = identify.length();
            if (IdentifyLen != len) {
                throw new Exception("Invalid Identify: " + identify);
            }
            String sub = identify.substring(len - 3, len - 2);
            dir = Integer.valueOf(sub);
            if (dir != 1) {
                //不为1（上行），则认为是2（下行）
                dir = 2;
            }
            return dir;
        } catch (Exception e) {
            e.printStackTrace();
            //出现异常则默认是上行
            return 1;
        }
    }


    public static class StationName {
        String name;
        String ename;
        public StationName(String name, String ename) {
            this.name = name;
            this.ename = ename;
        }
    }

    //站点编号和站点名称的映射表
    private static SparseArray<StationName> devicePositionInfo;

    static {
        devicePositionInfo = new SparseArray<>();
        devicePositionInfo.put(1, new StationName("银海玉洞路口站", "YINHAIYUDONG LUKOU"));
        devicePositionInfo.put(2, new StationName("玉洞红玉路口站", "YUDONGHONGYU LUKOU"));
        devicePositionInfo.put(3, new StationName("玉洞东风路口西站", "YUDONGDONGFENG LUKOU West"));
        devicePositionInfo.put(4, new StationName("玉洞东风路口东站", "YUDONGDONGFENG LUKOU Eest"));
        devicePositionInfo.put(6, new StationName("玉洞玉象路口站", "YUDONGYUXIANG LUKOU"));
        devicePositionInfo.put(7, new StationName("五象湖公园站", "WUXIANGHU Park"));
        devicePositionInfo.put(8, new StationName("五象湖（平乐玉洞立交）站", "WUXIANGHU(PINGLEYUDONG Flyover)"));
        devicePositionInfo.put(9, new StationName("玉洞那黄路口站", "YUDONGNAHUANG LUKOU"));
        devicePositionInfo.put(10, new StationName("玉洞瓦村路口站", "YUDONGWACUN LUKOU"));
        devicePositionInfo.put(11, new StationName("玉洞丰庆路口站", "YUDONGFENGQING LUKOU"));
        devicePositionInfo.put(12, new StationName("玉洞延庆路口站", "YUDONGYANQING LUKOU"));
        devicePositionInfo.put(13, new StationName("玉洞新良路口站", "YUDONGXINLIANG LUKOU"));
        devicePositionInfo.put(14, new StationName("五象火车站", "WUXIANG Railway Station"));
        devicePositionInfo.put(15, new StationName("玉洞永福路口站", "YUDONGYONGFU LUKOU"));
        devicePositionInfo.put(16, new StationName("梁村大桥西站站", "LIANGCUNDAQIAO West"));
        devicePositionInfo.put(17, new StationName("园博园北门站", "Garden Expo Park North Gate"));
        devicePositionInfo.put(18, new StationName("园博园站", "Garden Expo Park"));
    }

    //是否包含指定的站点编号
    public static boolean isContainStationNum(int num) {
        if (null != devicePositionInfo.get(num, null)) {
            return true;
        }
        return false;
    }

    //获取指定站点编号的站点名称
    public static StationName getStationNameByNum(int num) {
        return devicePositionInfo.get(num);
    }

}
