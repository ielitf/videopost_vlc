package com.ceiv.BrtUtils;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrtInfoUtils {

    private final static String TAG = "BrtInfoUtils";

    public final static String defaultRouteID = "80002";

    private static RouteInfo curRouteInfo;

    static {
        gloRouteInfos = null;
        curRouteInfo = null;
        gloInfoLock = new Object();

    }

    public static ArrayList<RouteInfo> gloRouteInfos;

    private final static Object gloInfoLock;



    /*
    *   将JSON形式的路线信息的关键数据读取到RouteInfo对象中
    *   读取错误或者信息不完整返回null
    * */
    public static RouteInfo parseJsonRouteInfo(String jsonString) {

        JSONArray arrayStationList;
        int numStation;
        ArrayList<StationItem> stationItemList;
        StationItem stationItem;

        try {
            RouteInfo routeInfo = new RouteInfo();

            JSONArray resp = new JSONArray(jsonString);
            JSONObject jsonResp = resp.getJSONObject(0);
            Log.d(TAG, "RouteID:" + jsonResp.get("RouteID").toString());
            Log.d(TAG, "RouteName:" + jsonResp.get("RouteName").toString());
            routeInfo.setRouteID(jsonResp.get("RouteID").toString().trim());
            routeInfo.setRouteName(jsonResp.get("RouteName").toString().trim());

            JSONArray arraySegmentList = jsonResp.getJSONArray("SegmentList");

            //这里待确认
            routeInfo.setBRT(true);
            Log.d(TAG, "Segment Num: " + arraySegmentList.length());
            for(int i = 0; i < arraySegmentList.length(); i++) {
                JSONObject jsonSegment = arraySegmentList.getJSONObject(i);

                SegmentInfo segmentInfo = new SegmentInfo();
                segmentInfo.setSegmentID(jsonSegment.get("SegmentID").toString().trim());
                segmentInfo.setSegmentName(jsonSegment.get("SegmentName").toString().trim());
                segmentInfo.setFirstTime(jsonSegment.get("FirstTime").toString().trim());
                segmentInfo.setLastTime(jsonSegment.get("LastTime").toString().trim());
                segmentInfo.setRoutePrice(Float.valueOf(jsonSegment.get("RoutePrice").toString().trim()));
                segmentInfo.setFirtLastShiftInfo(jsonSegment.get("FirtLastShiftInfo").toString().trim());
                segmentInfo.setFirtLastShiftInfo2(jsonSegment.get("FirtLastShiftInfo2").toString().trim());

                int direction = 0;
                if ("1".equals(jsonSegment.get("RunDirection").toString().trim()) ||
                        jsonSegment.get("SegmentName").toString().contains("上行")) {
                    //上行
                    direction = 1;
                } else if ("2".equals(jsonSegment.get("RunDirection").toString().trim()) ||
                        jsonSegment.get("SegmentName").toString().contains("下行")) {
                    //下行
                    direction = 2;
                }
                if (direction == 0) {
                    //获取不到上下行信息
                    Log.d(TAG, "cat't get segment run direction info!");
                    continue;
                }
                segmentInfo.setRunDirection(direction);

                Log.d(TAG, "Segment[" + i + "] info: " +
                        "\nRunDirection: " + (segmentInfo.getRunDirection() == 1 ? "upline" : "downline") +
                        "\nID: " + segmentInfo.getSegmentID() +
                        "\nName: " + segmentInfo.getSegmentName() +
                        "\nFirstTime: " + segmentInfo.getFirstTime() +
                        "\nLastTime: " + segmentInfo.getLastTime() +
                        "\nRoutePrice: " + segmentInfo.getRoutePrice() +
                        "\nFirtLastShiftInfo: " + segmentInfo.getFirtLastShiftInfo() +
                        "\nFirtLastShiftInfo2: " + segmentInfo.getFirtLastShiftInfo2());

                arrayStationList = jsonSegment.getJSONArray("StationList");
                numStation = arrayStationList.length();
                stationItemList = new ArrayList<StationItem>();

                for(int j = 0; j < numStation; j++) {
                    JSONObject tmpJSONObject = arrayStationList.getJSONObject(j);
                    stationItem = new StationItem();
                    stationItem.setStationName(tmpJSONObject.get("StationName").toString().
                            replace("（上行）", "").replace("（下行）", "").trim());
                    stationItem.setDualSerial(Integer.valueOf(tmpJSONObject.get("DualSerial").toString().trim()));
                    stationItem.setStationID(tmpJSONObject.get("StationID").toString().trim());
                    stationItem.setStationNum(Integer.valueOf(stationItem.getStationID().substring(3, 5)));     //以“7201501”为例，中间15位站点编号
                    stationItem.setStationNO(tmpJSONObject.get("StationNO").toString().trim());
                    stationItem.setStationmemo(tmpJSONObject.get("Stationmemo").toString().trim());
                    int enameIndex = stationItem.getStationmemo().indexOf('|');
                    if (enameIndex >= 0) {
                        stationItem.setStationEName(stationItem.getStationmemo().substring(enameIndex + 1));
                    }
                    JSONObject StationPostion = tmpJSONObject.getJSONObject("StationPostion");
                    if (StationPostion != null) {
                        stationItem.setLongitude(Float.valueOf(StationPostion.get("Longitude").toString().trim()));
                        stationItem.setLatitude(Float.valueOf(StationPostion.get("Latitude").toString().trim()));
                    }
                    Log.d(TAG, "StationItem info: " +
                            "\nNum: " + stationItem.getStationNum() +
                            "\nName: " + stationItem.getStationName() +
                            "\nDualSerial: " + stationItem.getDualSerial() +
                            "\nEname: " + stationItem.getStationEName() +
                            "\nID: " + stationItem.getStationID() +
                            "\nNO: " + stationItem.getStationNO() +
                            "\nmemo: " + stationItem.getStationmemo() +
                            "\nLongitude: " + stationItem.getLongitude() +
                            "\nLatitude: " + stationItem.getLatitude());

                    if (!stationItem.isValid()) {
                        Log.e(TAG, "Get StationItem failed!");
                        return null;
                    }
                    stationItemList.add(stationItem);
                }
                segmentInfo.setStationList(stationItemList);
                if (!segmentInfo.isValid()) {
                    Log.e(TAG, "Get SegmentInfo failed!");
                    return null;
                }

                if (segmentInfo.getRunDirection() == 1) {
                    routeInfo.setUpline(segmentInfo);
                } else {
                    routeInfo.setDownline(segmentInfo);
                }
            }
            if (!routeInfo.isValid()) {
                Log.e(TAG, "Get RouteInfo failed!");
                return null;
            }

            //对站点信息根据双程号进行排序（升序）
            Collections.sort(routeInfo.getDownline().getStationList(), StationItem.StationItemComparator);
            Collections.sort(routeInfo.getUpline().getStationList(), StationItem.StationItemComparator);

            return routeInfo;
        } catch (Exception e) {
            Log.e(TAG, "Parse JsonRouteInfo failed!");
            e.printStackTrace();
            return null;
        }
    }

    //将路线信息RouteInfo持久化、保存到文件
    private static void saveRouteInfoToFile(RouteInfo routeInfo) {

        ObjectOutputStream oos = null;
        String RouteInfoFileName = "Route" + routeInfo.getRouteID() + ".dat";
        Log.d(TAG, "save RouteInfo to file: " + RouteInfoFileName);
        try {
            oos = new ObjectOutputStream(
                    new FileOutputStream(
                            Environment.getExternalStorageDirectory() + "/" + RouteInfoFileName));
            oos.writeObject(routeInfo);
            oos.flush();
        } catch (Exception e) {
            Log.e(TAG, "Write Route Info failed!");
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //保存新获取的路线信息
    public static void saveRouteInfo(RouteInfo routeInfo) {

        synchronized (gloInfoLock) {
            for (int i = 0; i < gloRouteInfos.size(); i++) {
                RouteInfo tmp = gloRouteInfos.get(i);
                if (tmp.getRouteID().equals(routeInfo.getRouteID())) {
                    gloRouteInfos.remove(i);
                    break;
                }
            }
            saveRouteInfoToFile(routeInfo);
            gloRouteInfos.add(routeInfo);
            if (curRouteInfo.getRouteID().equals(routeInfo.getRouteID())) {
                curRouteInfo = routeInfo;
            }
        }
    }

    public static RouteInfo readRouteInfoFromFile(String path, String name) {
        return readRouteInfoFromFile(path + "/" + name);
    }

    public static RouteInfo readRouteInfoFromFile(String filepath) {
        return readRouteInfoFromFile(new File(filepath));
    }

    public static RouteInfo readRouteInfoFromFile(File file) {

        ObjectInputStream ois = null;
        RouteInfo routeInfo = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            routeInfo = (RouteInfo) ois.readObject();
        } catch (Exception e) {
            Log.e(TAG, "Read RouteInfo from file failed!");
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return routeInfo;
    }

    /*
    *   整条线路至少有一条路线信息（RouteInfo）
    *   这个函数检查设备中是否含有这个默认的线路信息
    *   如果没有的话，会写入程序内置的线路信息
    *
    *   返回值：RouteInfo  默认的线路信息
    * */
    public static RouteInfo checkDefaultRouteInfo() {
        File defRouteInfoFile = null;
        RouteInfo routeInfo = null;
        boolean needWriteNewFile = false;
        String defRouteInfoFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Route" + defaultRouteID + ".dat";
        try {
            defRouteInfoFile = new File(defRouteInfoFileName);
            if (defRouteInfoFile.exists()) {
                routeInfo = readRouteInfoFromFile(defRouteInfoFile);
                if (routeInfo == null) {
                    needWriteNewFile = true;
                }
            } else {
                defRouteInfoFile.createNewFile();
                needWriteNewFile = true;
            }
            if (needWriteNewFile) {
                //默认数据不存在或者文件不完整，需要写入新的默认数据
                saveRouteInfoToFile(createDefRouteInfo());
                return readRouteInfoFromFile(defRouteInfoFileName);
            } else {
                return routeInfo;
            }
        } catch (Exception e) {
            Log.e(TAG, "Check default RouteInfo error!");
            e.printStackTrace();
            return null;
        }
    }

    //获取设备当前所在线路的线路信息
    public static RouteInfo getCurRouteInfo() {
        if (curRouteInfo != null) {
            return curRouteInfo;
        }
        for (RouteInfo tmp : getAllRouteInfo()) {
            if (tmp.getRouteID().equals(defaultRouteID)) {
                curRouteInfo = tmp;
                return curRouteInfo;
            }
        }
        Log.e(TAG, "Can't get curRouteInfo!");
        return null;
    }

    //获取本地设备中的所有路线信息
    public static ArrayList<RouteInfo> getAllRouteInfo() {
        if (gloRouteInfos != null) {
            return gloRouteInfos;
        }
        return getLatestAllRouteInfo();
    }

    //获取设备中最新的路线信息
    public static ArrayList<RouteInfo> getLatestAllRouteInfo() {
        ArrayList<RouteInfo> routeInfoList = new ArrayList<>();

        File dirFile = Environment.getExternalStorageDirectory();
        File[] files = dirFile.listFiles();
        RouteInfo tmpRouteInfo = null;
        Log.d(TAG, "Start to search RouteInfo...");
        for (File tmp : files) {
            if (tmp.isFile() && tmp.getName().startsWith("Route") && tmp.getName().endsWith(".dat")) {
                Log.d(TAG, "Find RouteInfo: " + tmp.getName());
                tmpRouteInfo = readRouteInfoFromFile(tmp);
                if (null != tmpRouteInfo) {
                    routeInfoList.add(tmpRouteInfo);
                } else {
                    Log.e(TAG, "Read RouteInfo from " + tmp.getName() + " failed!");
                }
            }
        }
        if (routeInfoList.size() == 0) {
            Log.d(TAG, "No RouteInfo Found in Device!\nUse default RouteInfo");
            routeInfoList.add(checkDefaultRouteInfo());
        }
        synchronized (gloInfoLock) {
            gloRouteInfos = routeInfoList;
        }
        return gloRouteInfos;
    }

    /*
    *   创建默认的路线信息，这里包含了规划中的所有站点信息
    * */
    private static RouteInfo createDefRouteInfo() {

        SegmentInfo upline = new SegmentInfo();
        upline.setRoutePrice(2.0f);
        upline.setRunDirection(SegmentInfo.UPLINE);
        upline.setFirstTime("2018-11-12 06:15:00");
        upline.setLastTime("2018-11-12 23:00:00");
        upline.setFirtLastShiftInfo("首末班：06:15--23:00");
        upline.setFirtLastShiftInfo2("");
        upline.setSegmentID("100052");
        upline.setSegmentName("银海玉洞路口站（上行）");

        ArrayList<StationItem> uplist = new ArrayList<StationItem>();
        uplist.add(new StationItem(18, 1, "园博园站|Garden Expo Park",
                "Garden Expo Park", "7201801", "园博园站", "7201801", 1.0f, 1.0f));
        uplist.add(new StationItem(17, 2, "园博园北门站|Garden Expo Park North Gate",
                "Garden Expo Park North Gate", "7201701", "园博园北门站", "7201701", 1.0f, 1.0f));
        uplist.add(new StationItem(16, 3, "梁村大桥西站|LIANGCUNDAQIAO West",
                "LIANGCUNDAQIAO West", "7201601", "梁村大桥西站", "7201601", 1.0f, 1.0f));
        uplist.add(new StationItem(15, 4, "玉洞永福路口站|YUDONGYONGFU LUKOU",
                "YUDONGYONGFU LUKOU", "7201501", "玉洞永福路口站", "7201501", 1.0f, 1.0f));
        uplist.add(new StationItem(14, 5, "五象火车站|WUXIANG Railway Station",
                "WUXIANG Railway Station", "7201401", "五象火车站", "7201401", 1.0f, 1.0f));
        uplist.add(new StationItem(13, 6, "玉洞新良路口站|YUDONGXINLIANG LUKOU",
                "YUDONGXINLIANG LUKOU", "7201301", "玉洞新良路口站", "7201301", 1.0f, 1.0f));
        uplist.add(new StationItem(12, 7, "玉洞延庆路口站|YUDONGYANQING LUKOU",
                "YUDONGYANQING LUKOU", "7201201", "玉洞延庆路口站", "7201201", 1.0f, 1.0f));
        uplist.add(new StationItem(11, 8, "玉洞丰庆路口站|YUDONGFENGQING LUKOU",
                "YUDONGFENGQING LUKOU", "7201101", "玉洞丰庆路口站", "7201101", 1.0f, 1.0f));
        uplist.add(new StationItem(10, 9, "玉洞瓦村路口站|YUDONGWACUN LUKOU",
                "YUDONGWACUN LUKOU", "7201001", "玉洞瓦村路口站", "7201001", 1.0f, 1.0f));
        uplist.add(new StationItem(9, 10, "玉洞那黄路口站|YUDONGNAHUANG LUKOU",
                "YUDONGNAHUANG LUKOU", "7200901", "玉洞那黄路口站", "7200901", 1.0f, 1.0f));
        uplist.add(new StationItem(8, 11, "五象湖（平乐玉洞立交）站|WUXIANGHU(PINGLEYUDONG Flyover)",
                "WUXIANGHU(PINGLEYUDONG Flyover)", "7200801", "五象湖（平乐玉洞立交）站", "7200801", 1.0f, 1.0f));
        uplist.add(new StationItem(7, 12, "五象湖公园站|WUXIANGHU Park",
                "WUXIANGHU Park", "7200701", "五象湖公园站", "7200701", 1.0f, 1.0f));
        uplist.add(new StationItem(6, 13, "玉洞玉象路口站|YUDONGYUXIANG LUKOU",
                "YUDONGYUXIANG LUKOU", "7200601", "玉洞玉象路口站", "7200601", 1.0f, 1.0f));
        uplist.add(new StationItem(4, 14, "玉洞东风路口东站|YUDONGDONGFENG LUKOU Eest",
                "YUDONGDONGFENG LUKOU Eest", "7200401", "玉洞东风路口东站", "7200401", 1.0f, 1.0f));
        uplist.add(new StationItem(3, 15, "玉洞东风路口西站|YUDONGDONGFENG LUKOU West",
                "YUDONGDONGFENG LUKOU West", "7200301", "玉洞东风路口西站", "7200301", 1.0f, 1.0f));
        uplist.add(new StationItem(2, 16, "玉洞红玉路口站|YUDONGHONGYU LUKOU",
                "YUDONGHONGYU LUKOU", "7200201", "玉洞红玉路口站", "7200201", 1.0f, 1.0f));
        uplist.add(new StationItem(1, 17, "银海玉洞路口站|YINHAIYUDONG LUKOU",
                "YINHAIYUDONG LUKOU", "7200101", "银海玉洞路口站", "7200101", 1.0f, 1.0f));

        //对站点信息根据双程号进行排序（升序）
        Collections.sort(uplist, StationItem.StationItemComparator);
        upline.setStationList(uplist);

        SegmentInfo downline = new SegmentInfo();
        downline.setRoutePrice(2.0f);
        downline.setRunDirection(SegmentInfo.DOWNLINE);
        downline.setFirstTime("2018-11-12 06:15:00");
        downline.setLastTime("2018-11-12 23:00:00");
        downline.setFirtLastShiftInfo("首末班：06:15--23:00");
        downline.setFirtLastShiftInfo2("");
        downline.setSegmentID("100053");
        downline.setSegmentName("园博园站（下行）");

        ArrayList<StationItem> downlist = new ArrayList<StationItem>();
        downlist.add(new StationItem(18, 34, "园博园站|Garden Expo Park",
                "Garden Expo Park", "7201802", "园博园站", "7201802", 1.0f, 1.0f));
        downlist.add(new StationItem(17, 33, "园博园北门站|Garden Expo Park North Gate",
                "Garden Expo Park North Gate", "7201702", "园博园北门站", "7201702", 1.0f, 1.0f));
        downlist.add(new StationItem(16, 32, "梁村大桥西站|LIANGCUNDAQIAO West",
                "LIANGCUNDAQIAO West", "7201602", "梁村大桥西站", "7201602", 1.0f, 1.0f));
        downlist.add(new StationItem(15, 31, "玉洞永福路口站|YUDONGYONGFU LUKOU",
                "YUDONGYONGFU LUKOU", "7201502", "玉洞永福路口站", "7201502", 1.0f, 1.0f));
        downlist.add(new StationItem(14, 30, "五象火车站|WUXIANG Railway Station",
                "WUXIANG Railway Station", "7201402", "五象火车站", "7201402", 1.0f, 1.0f));
        downlist.add(new StationItem(13, 29, "玉洞新良路口站|YUDONGXINLIANG LUKOU",
                "YUDONGXINLIANG LUKOU", "7201302", "玉洞新良路口站", "7201302", 1.0f, 1.0f));
        downlist.add(new StationItem(12, 28, "玉洞延庆路口站|YUDONGYANQING LUKOU",
                "YUDONGYANQING LUKOU", "7201202", "玉洞延庆路口站", "7201202", 1.0f, 1.0f));
        downlist.add(new StationItem(11, 27, "玉洞丰庆路口站|YUDONGFENGQING LUKOU",
                "YUDONGFENGQING LUKOU", "7201102", "玉洞丰庆路口站", "7201102", 1.0f, 1.0f));
        downlist.add(new StationItem(10, 26, "玉洞瓦村路口站|YUDONGWACUN LUKOU",
                "YUDONGWACUN LUKOU", "7201002", "玉洞瓦村路口站", "7201002", 1.0f, 1.0f));
        downlist.add(new StationItem(9, 25, "玉洞那黄路口站|YUDONGNAHUANG LUKOU",
                "YUDONGNAHUANG LUKOU", "7200902", "玉洞那黄路口站", "7200902", 1.0f, 1.0f));
        downlist.add(new StationItem(8, 24, "五象湖（平乐玉洞立交）站|WUXIANGHU(PINGLEYUDONG Flyover)",
                "WUXIANGHU(PINGLEYUDONG Flyover)", "7200802", "五象湖（平乐玉洞立交）站", "7200802", 1.0f, 1.0f));
        downlist.add(new StationItem(7, 23, "五象湖公园站|WUXIANGHU Park",
                "WUXIANGHU Park", "7200702", "五象湖公园站", "7200702", 1.0f, 1.0f));
        downlist.add(new StationItem(6, 22, "玉洞玉象路口站|YUDONGYUXIANG LUKOU",
                "YUDONGYUXIANG LUKOU", "7200602", "玉洞玉象路口站", "7200602", 1.0f, 1.0f));
        downlist.add(new StationItem(4, 21, "玉洞东风路口东站|YUDONGDONGFENG LUKOU Eest",
                "YUDONGDONGFENG LUKOU Eest", "7200402", "玉洞东风路口东站", "7200402", 1.0f, 1.0f));
        downlist.add(new StationItem(3, 20, "玉洞东风路口西站|YUDONGDONGFENG LUKOU West",
                "YUDONGDONGFENG LUKOU West", "7200302", "玉洞东风路口西站", "7200302", 1.0f, 1.0f));
        downlist.add(new StationItem(2, 19, "玉洞红玉路口站|YUDONGHONGYU LUKOU",
                "YUDONGHONGYU LUKOU", "7200202", "玉洞红玉路口站", "7200202", 1.0f, 1.0f));
        downlist.add(new StationItem(1, 18, "银海玉洞路口站|YINHAIYUDONG LUKOU",
                "YINHAIYUDONG LUKOU", "7200102", "银海玉洞路口站", "7200102", 1.0f, 1.0f));

        //对站点信息根据双程号进行排序（升序）
        Collections.sort(downlist, StationItem.StationItemComparator);
        downline.setStationList(downlist);

        return new RouteInfo(defaultRouteID, "B02路", upline, downline, true);
    }


    public static class BrtInfo {
        //车辆ID
        public String busID;
        //当前所处位置的双程号
        public int dualSerial;
        //当前所处的站点id
        public String stationId;
        //当前所处的站点id所在的索引，从0开始
        public int stationIdIndex;
        //到离站信息
        public int IsArrLeft;
        //最后一次更新的时间
        public long lastTime;

        public BrtInfo(String busID, int dualSerial, int IsArrLeft, long lastTime) {
            this.busID = busID;
            this.dualSerial = dualSerial;
            this.IsArrLeft = IsArrLeft;
            this.lastTime = lastTime;
        }
        public BrtInfo(String busID, String stationId,int stationIdIndex, int IsArrLeft, long lastTime) {
            this.busID = busID;
            this.stationId = stationId;
            this.stationIdIndex = stationIdIndex;
            this.IsArrLeft = IsArrLeft;
            this.lastTime = lastTime;
        }

        @Override
        public String toString() {
            return "BrtInfo{" +
                    "busID='" + busID + '\'' +
                    ", dualSerial=" + dualSerial +
                    ", stationId='" + stationId + '\'' +
                    ", stationIdIndex=" + stationIdIndex +
                    ", IsArrLeft=" + IsArrLeft +
                    ", lastTime=" + lastTime +
                    '}';
        }
    }


    //将新的brt信息按照由近及远的顺序添加到brtList中
    public static void InsertBrtInfo2(BrtInfo brtInfo, ArrayList<BrtInfo> brtList) {
        int size = brtList.size();
        if (0 == size) {
            brtList.add(brtInfo);
        } else {
            if (brtList.get(0).stationIdIndex < brtInfo.stationIdIndex) {
                //比第一个双程号还要大的话，就放在第一个位置
                brtList.add(0, brtInfo);
            } else if (brtList.get(size - 1).stationIdIndex > brtInfo.stationIdIndex) {
                //比最后一个双程号还要小的话，放在最后面
                brtList.add(size, brtInfo);
            } else {
                BrtInfo tmpInfo = null;
                for (int i = 0; i < size; i++) {
                    tmpInfo = brtList.get(i);
                    if (tmpInfo.stationIdIndex < brtInfo.stationIdIndex) {
                        //双程号大的（离显示屏所在的站点更近）的应该放在前面
                        brtList.add(i, brtInfo);
                        break;
                    } else if (tmpInfo.stationIdIndex == brtInfo.stationIdIndex) {
                        //双程号一致时需要检查到离站信息，“离站”的应放在“到站”的前面
                        if (tmpInfo.IsArrLeft == 2) {
                            if (brtInfo.IsArrLeft == 2) {
                                brtList.add(i, brtInfo);
                                break;
                            } else {
                                if (i == size - 1) {
                                    //如果已经是最后一个，则直接添加在后面
                                    brtList.add(size, brtInfo);
                                    break;
                                } else {
                                    continue;
                                }
                            }
                        } else {
                            brtList.add(i, brtInfo);
                            break;
                        }
                    } else {
                        //双程号较小时继续往后面寻找
                        if (i == size - 1) {
                            //如果已经是最后一个，则直接添加在后面
                            brtList.add(size, brtInfo);
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
    }

    //将新的brt信息按照由近及远的顺序添加到brtList中
    public static void InsertBrtInfo(BrtInfo brtInfo, ArrayList<BrtInfo> brtList) {
        int size = brtList.size();
        if (0 == size) {
            brtList.add(brtInfo);
        } else {
            if (brtList.get(0).dualSerial < brtInfo.dualSerial) {
                //比第一个双程号还要大的话，就放在第一个位置
                brtList.add(0, brtInfo);
            } else if (brtList.get(size - 1).dualSerial > brtInfo.dualSerial) {
                //比最后一个双程号还要小的话，放在最后面
                brtList.add(size, brtInfo);
            } else {
                BrtInfo tmpInfo = null;
                for (int i = 0; i < size; i++) {
                    tmpInfo = brtList.get(i);
                    if (tmpInfo.dualSerial < brtInfo.dualSerial) {
                        //双程号大的（离显示屏所在的站点更近）的应该放在前面
                        brtList.add(i, brtInfo);
                        break;
                    } else if (tmpInfo.dualSerial == brtInfo.dualSerial) {
                        //双程号一致时需要检查到离站信息，“离站”的应放在“到站”的前面
                        if (tmpInfo.IsArrLeft == 2) {
                            if (brtInfo.IsArrLeft == 2) {
                                brtList.add(i, brtInfo);
                                break;
                            } else {
                                if (i == size - 1) {
                                    //如果已经是最后一个，则直接添加在后面
                                    brtList.add(size, brtInfo);
                                    break;
                                } else {
                                    continue;
                                }
                            }
                        } else {
                            brtList.add(i, brtInfo);
                            break;
                        }
                    } else {
                        //双程号较小时继续往后面寻找
                        if (i == size - 1) {
                            //如果已经是最后一个，则直接添加在后面
                            brtList.add(size, brtInfo);
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
    }


}
