package com.ceiv.videopost;

import android.content.Context;
import android.os.Environment;
//import android.util.Log;
import com.ceiv.log4j.Log;

import android.util.Xml;

import com.ceiv.videopost.StationInfo.StationInfoItem;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zhangdawei on 2018/9/17.
 */

public class StationInfoOperation {

    private final static String TAG = "StationInfoOperation";

    private final static String stationInfoFile = "StationInfo.xml";
    private final static String defaultStationInfoFile = "DefaultStationInfo.xml";

    private final static String stationInfoFormat = "<stations>\n" +
            "\t<segment dir=\"downline\">\n" +
            "%s" +
            "\t</segment>\n" +
            "\t<segment dir=\"upline\">\n" +
            "%s" +
            "\t</segment>\n" +
            "</stations>\n";

    private final static String stationItemFormat = "\t\t<station>\n" +
            "\t\t\t<name>%s</name>\n" +
            "\t\t\t<ename>%s</ename>\n" +
            "\t\t\t<dualSerial>%d</dualSerial>\n" +
            "\t\t</station>\n";

    private static Context context = null;

    public static void setResourcesContext(Context con) {
        if (context == null) {
            context = con;
        }
    }

    public static StationInfo getStationInfoFromXml() {

        File stationFile = new File(Environment.getExternalStorageDirectory() + "/" + stationInfoFile);
        if (!stationInfoFileCheck()) {
            return null;
        }

        StationInfo stationInfo = null;
        ArrayList<StationInfoItem> lineInfo = null;
        StationInfoItem stationInfoItem = null;
        //信息读取成功
        boolean readSuccess = false;
        //-1: 当前还没处理到上下行数据，1: 代表当前在操作下行线路的数据，2: 则正在操作上行线路的数据
        int directionFlag = -1;
        //判断信息是否完整的标志位，为1时表示有数据，0表示没有数据，bit0: 下行信息， bit1: 上行信息
        int infoCompleteFlag = 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(stationFile);
            // 获得pull解析器对象
            XmlPullParser parser = Xml.newPullParser();
            // 指定解析的文件和编码格式
            parser.setInput(fis, "utf-8");
            int eventType = parser.getEventType(); // 获得事件类型
            Log.d(TAG, "start parse xml");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    //开始标签
                    case XmlPullParser.START_TAG:
                        if ("stations".equals(parser.getName())) {
                            stationInfo = new StationInfo();
                        } else if ("segment".equals(parser.getName())) {
                            if (-1 != directionFlag) {
                                //当前正在处理上行/下行的数据，此时又检测到"segment"起始字段，出现异常！
                                throw new Exception("Read another <segment> before </segment>");
                            }
                            String direction = parser.getAttributeValue(null, "dir");
                            if ("downline".equals(direction)) {
                                directionFlag = 1;
                                lineInfo = new ArrayList<>();
                            } else if ("upline".equals(direction)) {
                                directionFlag = 2;
                                lineInfo = new ArrayList<>();
                            } else {
                                throw new Exception("segment don't has direction attribute");
                            }
                        } else if ("station".equals(parser.getName())) {
                            stationInfoItem = new StationInfoItem();
                        } else if ("name".equals(parser.getName())) {
                            stationInfoItem.name = parser.nextText();
                        } else if ("ename".equals(parser.getName())) {
                            stationInfoItem.ename = parser.nextText();
                        } else if ("dualserial".equals(parser.getName())) {
                            stationInfoItem.dualSerial = Integer.valueOf(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("station".equals(parser.getName())) {
                            if (stationInfoItem.isValid()) {
                                lineInfo.add(stationInfoItem);
                                stationInfoItem = null;
                            } else {
                                throw new Exception("Invalid StationInfo[name:" + stationInfoItem.name + ", ename:" +
                                        stationInfoItem.ename + ", dualSerial:" + stationInfoItem.dualSerial + "]!");
                            }
                        } else if ("segment".equals(parser.getName())) {
                            if (1 == directionFlag) {
                                //下行数据处理完毕
                                stationInfo.downline = lineInfo;
                                infoCompleteFlag |= 0x01;
                                directionFlag = -1;
                            } else if (2 == directionFlag){
                                //上行数据处理完毕
                                stationInfo.upline = lineInfo;
                                infoCompleteFlag |= 0x02;
                                directionFlag = -1;
                            } else {
                                //当前没在处理上行/下行数据时收到"segment"结束字段，出现异常！
                                throw new Exception("Read </segment> before <segment>");
                            }
                            lineInfo = null;
                        } else if ("stations".equals(parser.getName())) {
                            if (0x03 == infoCompleteFlag) {
                                readSuccess = true;
                            } else {
                                readSuccess = false;
                            }
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next(); // 获得下一个事件类型
            }
        }catch (Exception e) {
            Log.e(TAG, "Get StationInfo from xml file error!", e);
            //e.printStackTrace();
            readSuccess = false;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close StationInfo xml file error!", e);
                //e.printStackTrace();
            }
            if (readSuccess) {
                //上下行数据都能够读取出来
                //先进行排序（升序）
                Collections.sort(stationInfo.downline, new StationInfo.StationInfoComparator());
                Collections.sort(stationInfo.upline, new StationInfo.StationInfoComparator());
                return stationInfo;
            } else {
                //信息读取出错或者不完整
                return null;
            }
        }
    }

    public static boolean saveStationInfoToXml(StationInfo stations) {

        if (null == stations || null == stations.upline || null == stations.downline) {
            Log.e(TAG, "Invalid arguments!");
            return false;
        }

        if (!stationInfoFileCheck()) {
            return false;
        }

        File file = new File(Environment.getExternalStorageDirectory() + "/" + stationInfoFile);
        FileWriter fileWriter = null;
        StringBuilder downline = new StringBuilder();
        StringBuilder upline = new StringBuilder();
        try {
            fileWriter = new FileWriter(file, false);
            for (StationInfoItem item : stations.downline) {
                if (item.dualSerial < 0 || item.name == null || "".equals(item.name) || item.ename == null) {
                    throw new Exception("Invalid StationInfo item!");
                }
                downline.append(String.format(stationItemFormat, item.name, item.ename, item.dualSerial));
            }
            for (StationInfoItem item : stations.upline) {
                if (item.dualSerial < 0 || item.name == null || "".equals(item.name) || item.ename == null) {
                    throw new Exception("Invalid StationInfo item!");
                }
                upline.append(String.format(stationItemFormat, item.name, item.ename, item.dualSerial));
            }

            fileWriter.write(String.format(stationInfoFormat, downline.toString(), upline.toString()));
            fileWriter.flush();
        } catch (Exception e) {
            Log.d(TAG, "Write StationInfo to xml file failed!", e);
            //e.printStackTrace();
            return false;
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "close stationInfo file failed!", e);
                //e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private static boolean stationInfoFileCheck() {

        File stationFile = new File(Environment.getExternalStorageDirectory() + "/" + stationInfoFile);
        if (!stationFile.exists()) {
            Log.d(TAG, stationInfoFile + " does't exist! pepare to create default StationInfo file!");
            try {
                File fileParent = stationFile.getParentFile();
                if (fileParent != null) {
                    if (!fileParent.exists()) {
                        fileParent.mkdirs();
                    }
                }
                stationFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Create default StationInfo file failed!", e);
                //e.printStackTrace();
                return false;
            }
            boolean writeSuccess = true;
//            File defaultFile = null;
            InputStream is = null;
//            FileInputStream fis = null;
            FileOutputStream fos = null;
            byte[] buffer = new byte[1024];
            int bt = 0;
            try {
                is = context.getResources().getAssets().open(defaultStationInfoFile);
                //defaultFile = new File(URI.create("file:///android_asset/" + defaultStationInfoFile));
                //fis = new FileInputStream(defaultFile);
                fos = new FileOutputStream(stationFile);
                while ((bt = is.read(buffer)) != -1) {
                //while ((bt = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bt);
                }
                fos.flush();
            } catch (Exception e) {
                Log.e(TAG, "Write default StationInfo to XmlFile failed!", e);
                //e.printStackTrace();
                writeSuccess = false;
            } finally {
                try {
//                    if (fis != null) {
//                        fis.close();
//                    }
                    if (is != null) {
                        is.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close StationInfo file failed!", e);
                    //e.printStackTrace();
                    writeSuccess = false;
                }
            }
            if (!writeSuccess) {
                return false;
            } else {
                return true;
            }
        } else {
            if (!stationFile.isFile()) {
                if (stationFile.isDirectory()) {
                    if (removeDir(stationFile.getAbsolutePath())) {
                        Log.d(TAG, "remove directory:" + stationFile.getAbsolutePath() + "failed!");
                        return false;
                    }
                } else {
                    Log.d(TAG, stationFile.getName() +
                            "belong to neither regular file nor directory file!");
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean removeDir(String path) {

        return true;
    }
}
