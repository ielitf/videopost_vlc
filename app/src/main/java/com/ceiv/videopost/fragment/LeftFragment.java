package com.ceiv.videopost.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
//import android.util.Log;
import com.ceiv.BrtUtils.bean.Stations;
import com.ceiv.log4j.Log;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ceiv.BrtUtils.StationItem;
import com.ceiv.communication.SystemInitThread;
import com.ceiv.videopost.HisenseMsg.MsgService;
import com.ceiv.videopost.R;
import com.ceiv.videopost.StationInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ceiv.BrtUtils.BrtInfoUtils;
import com.ceiv.BrtUtils.BrtInfoUtils.BrtInfo;
import com.ceiv.videopost.ViceDisplay;

/**
 * Created by chu on 2018/8/14.
 */

public class LeftFragment extends Fragment {

    private final static String TAG = "LeftFragment";

    private final static int MsgSetBrtInfo = 0x01;

    /*
     *  当本次车刚好路过本站后，需要及时将本次车信息剔除，然后将下次车信息移到“本次车距本站”位置，
     *  同时将下下次车显示到“下次车距本站”的位置，所里这里要维持一个当前线路的车辆信息表，
     *  该List中车辆的顺序按照离本站由近及远的顺序，同时其双程号也是由大到小
     */
    private ArrayList<BrtInfo> brtList = null;

    /*
     *  离站车辆列表，刚离开本站的车会做一个短时短时间内的记录，在这段时间内收到该车的消息会忽略
     *  这么做的原因是可能会出现这样的情况：车辆刚离站后，车辆信息会被从brtList中剔除，但是如果后
     *  面又收到之前的GPS数据，那么本来已经驶离本站的车辆，又会被加入到brtList中，切状态变为“即将到站”
     *  虽然这个状态在1分钟后会消失，但是还是会误导乘客；其次到离站数据有补发的数据，当车辆已经离站
     *  但是有收到了到站的补发消息，就会显示又到站了的状态，这样也是有问题的
     */
    private HashMap<String, Long> leftBusMap = null;

    //本次车信息View
    private TextView Lto_station, distance_time;
    //到达本站的时间
    private String Lto_distance_time;
    //本次车信息内容
    private String Lto_content;
    //本次车信息颜色
    private int Lto_color;
    //本次车信息大小
    private int Lto_size;
    //下次车信息View
    private TextView Lnextto_station;
    //下次车信息内容
    private String Lnextto_content;
    //下次车信息颜色
    private int Lnextto_color;
    //下次车信息大小
    private int Lnextto_size;

    private final static String UnknownInfo = "暂无车辆";
    private final static String UnknownInfo_time = "（ 时间未知 ）";
//    //车辆信息TextView正常大小
//    private final int NormalSize = 45;
//    //车辆信息TextView显示内容较多时的大小
//    private final int SmallSize = 35;

    //车辆信息TextView正常大小
    private final int NormalSize = 100;
    //车辆信息TextView显示内容较多时的大小
    private final int SmallSize = 90;

    //开往方向的图片
    private ImageView lineDirImg = null;
    //显示样式
    private int themeStyle = 1;
    //是否是下行
    private boolean isDownline = false;
    //线路站点信息
    private ArrayList<StationItem> stationList = null;
    private List<Stations> customStationsList = null;//添加一个 by litf
    String stationId;//当前设备所在站的ID
    //    //线路站点信息
//    private ArrayList<StationInfoItem> stationList = null;
    //当前站点在列表中的标号
    private int curStaIndex = -1;
    //站点数目
    private int stationCount = -1;
    //终点站名字
    private String dstStationName = null;
    //终点站英文名
    private String dstStationEName = null;
    //下一站名字
    private String nxtStationName = null;
    //下一站英文名
    private String nxtStationEName = null;

    //双程号到stationList中站点的各站点index的映射
    private HashMap<String, Integer> ds2ListIndex = null;

    private ViceDisplay mViceDisplay = null;

    private final Object BusInfoLock = new Object();
    private TextView nextStaName, nextStaEName, dstStationNameTV, dstStationENameTV;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgSetBrtInfo:
                    distance_time.setText(msg.getData().getString("time"));
                    Lto_station.setText(msg.getData().getString("tt"));
                    Lto_station.setTextColor(msg.getData().getInt("tc"));
                    Lto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, msg.getData().getInt("ts"));
//                    Lnextto_station.setText(msg.getData().getString("ntt"));
//                    Lnextto_station.setTextColor(msg.getData().getInt("ntc"));
//                    Lnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, msg.getData().getInt("nts"));
//                    break;
                default:
                    break;
            }
        }
    };


    public LeftFragment() {

    }

//    public void fragmentInit(int themeStyle, ArrayList<StationInfoItem> stationList, int curStaIndex) {
//        this.themeStyle = themeStyle;
//        this.stationList = stationList;
//        this.stationCount = stationList.size();
//        this.curStaIndex = curStaIndex;
//        brtList = new ArrayList<>();
//        ds2ListIndex = new HashMap<Integer, Integer>();
//        //初始化映射表
//        for (int i = 0; i < stationCount; i++) {
//            ds2ListIndex.put(stationList.get(i).dualSerial, i);
//        }
//
//        cleanBRTInfoThread.start();
//    }

    public void fragmentInit(int themeStyle, ArrayList<StationItem> stationList, int curStaIndex) {
        this.themeStyle = themeStyle;
        this.stationList = stationList;
        this.stationCount = stationList.size();
        this.curStaIndex = curStaIndex;
        brtList = new ArrayList<>();
        leftBusMap = new HashMap<String, Long>();
        ds2ListIndex = new HashMap<String, Integer>();
        //初始化映射表
        for (int i = 0; i < stationCount; i++) {
//            ds2ListIndex.put(stationList.get(i).getDualSerial(), i);
        }

        cleanBRTInfoThread.start();
    }

    /**
     * 新加的By litf,可能有问题，所以没有在上面那个构造方法改
     *
     * @param stationList
     * @param curStaIndex
     */
    public void fragmentInit(List<Stations> stationList, int curStaIndex) {
        Log.d(TAG, "curStaIndex:" + curStaIndex + "/stationList:" + stationList.toString());
        this.customStationsList = stationList;
        this.stationCount = stationList.size();
        this.curStaIndex = curStaIndex;
        stationId = stationList.get(curStaIndex).getId();
        brtList = new ArrayList<>();
        leftBusMap = new HashMap<String, Long>();
        ds2ListIndex = new HashMap<String, Integer>();
        //初始化映射表
        for (int i = 0; i < stationCount; i++) {
            ds2ListIndex.put(stationList.get(i).getId(), i);
        }
        cleanBRTInfoThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.left_fragment, container, false);
        Log.d(TAG, "中间LeftFragment");
        //设置目的站点信息
        dstStationNameTV = view.findViewById(R.id.LdstStationName);
//        dstStationNameTV.setText("开往" + customStationsList.get(stationCount - 1).name.replace("站", "") + "方向");
//        dstStationNameTV.setText("开往" + customStationsList.get(stationCount - 1).getStationName().replace("站", "") + "方向");
        dstStationENameTV = view.findViewById(R.id.LdstStationEName);
//        dstStationENameTV.setText("Bound to " + customStationsList.get(stationCount - 1).ename);
//        dstStationENameTV.setText("Bound to " + customStationsList.get(stationCount - 1).getStationEName());

        //设置下一站信息
        //正常情况下，该View显示中文名称
        nextStaName = view.findViewById(R.id.LnextStaName);
        //正常情况下，该View显示英文名称，当中文名称太长的话，该View显示后半部的中文
        nextStaEName = view.findViewById(R.id.LnextStaEName);
        //当中文名称太长需要分行显示的时候，该View显示英文名称
        TextView nextStaEName2 = view.findViewById(R.id.LnextStaEName2);

//        String tmpName = customStationsList.get(curStaIndex + 1).name;
//        String tmpEName = customStationsList.get(curStaIndex + 1).ename;

        setDirectionAndNestStation(customStationsList.get(stationCount - 1).getName(), customStationsList.get(stationCount - 1).getNameEn(),
                customStationsList.get(curStaIndex + 1).getName(), customStationsList.get(curStaIndex + 1).getNameEn()
        );

        Lto_station = view.findViewById(R.id.Lto_station);
        distance_time = view.findViewById(R.id.distance_time);
        /*
        String tmpName = customStationsList.get(curStaIndex + 1).getStationName();
        String tmpEName = customStationsList.get(curStaIndex + 1).getStationEName();
        int bracketLeftIndex = -1;
        int bracketRightIndex = -1;
        //每行中文最多显示8个字符
        if (tmpName.length() > 8) {
            String name1 = null;
            String name2 = null;
            //查看是否有中文括号的内容
            bracketLeftIndex = tmpName.indexOf("（");
            bracketRightIndex = tmpName.indexOf("）");
            if (bracketLeftIndex < 0 || bracketRightIndex < 0 || bracketLeftIndex > bracketRightIndex) {
                //没有括号或者括号有问题则直接分行显示
                name1 = tmpName.substring(0, (int) (tmpName.length() / 2));
                name2 = tmpName.substring((int) (tmpName.length() / 2), tmpName.length());
            } else {
                //有括号的话，将括号中的内容放到第二行显示
                StringBuilder sb = new StringBuilder();
                //括号两边的凑在一起
                sb.append(tmpName.split("（")[0]).append(tmpName.split("）")[1]);
                name1 = sb.toString();
                //括号内的单独一行
                name2 = "（" + tmpName.split("（")[1].split("）")[0] + "）";
            }
//            nextStaName.setText(name1);
            nextStaEName.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50);
//            nextStaEName.setText(name2);
            nextStaEName2.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20);
            nextStaEName2.setText(tmpEName);
//            nextStaEName2.setVisibility(View.VISIBLE);
        } else {
//            nextStaName.setText(tmpName);
//            nextStaEName.setText(tmpEName);
//            nextStaEName2.setVisibility(View.INVISIBLE);
        }

        lineDirImg = view.findViewById(R.id.LDirImg);

        //本次车到站信息
        Lto_station = view.findViewById(R.id.Lto_station);
        //下次车到站信息
//        Lnextto_station = view.findViewById(R.id.Lnextto_station);
        //初始化
        Lto_station.setText(UnknownInfo);
        Lto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, SmallSize);
//        Lnextto_station.setText(UnknownInfo);
//        Lnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);

        //初始化目的站点旁边的线路方向图标
        switch (themeStyle) {
            case 1:
            case 2:
            case 4:
                lineDirImg.setImageResource(R.drawable.left);
                break;
            case 3:
                lineDirImg.setImageResource(R.drawable.right);
                break;
            default:
                break;
        }
        */

        return view;
    }

    Thread cleanBRTInfoThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                //5s进行一次清理检查
                try {
                    Thread.sleep(5 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (BusInfoLock) {
                    //清理离站车辆列表
                    ArrayList<String> cleanList = new ArrayList<>();
                    for (String bus : leftBusMap.keySet()) {
                        //离站10s内不接受该车的信息，防止补发数据和GPS数据的影响
                        if (Math.abs(leftBusMap.get(bus) - System.currentTimeMillis()) > 10 * 1000) {
                            cleanList.add(bus);
                        }
                    }
                    for (String bus : cleanList) {
                        leftBusMap.remove(bus);
                    }
                    //清理车辆列表
                    for (int i = 0; i < brtList.size(); i++) {
                        if (Math.abs(brtList.get(i).lastTime - System.currentTimeMillis()) > 10 * 60 * 1000) {
                            //长时间没有收到该车的任何数据，则认为该车已经离线
                            final BrtInfoUtils.BrtInfo tmpBrtInfo = brtList.remove(i);
                            Log.d(TAG, "BusID: " + tmpBrtInfo.busID + " dualSerialID: " + tmpBrtInfo.stationId + " offline!");
                            if (mViceDisplay != null) {
                                setBrtInfo(mViceDisplay);
                            }
//                            getActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (mViceDisplay != null) {
//                                        setBrtInfo(mViceDisplay);
//                                    }
//                                }
//                            });
                        }
                    }
                }
            }
        }
    });

    /*
     *  brtInfo: BRT信息
     *  viceDisplay: 当主屏修改完BRT信息后通知副屏也做相应的修改
     * */
    public void updateBRTInfo(String brtInfo, ViceDisplay viceDisplay) {
        Log.i(TAG, "LeftFragment MQTT消息：brtInfo:" + brtInfo);
        if (mViceDisplay == null) {
            mViceDisplay = viceDisplay;
        }
        //$是特殊符号，必须用[$]来分割
        String[] data = brtInfo.split("[" + MsgService.MsgSeparator + "]");
        if (data.length != 8 || !(data[1].equals(MsgService.routeId))) {
            Log.e(TAG, "Invalid BrtInfo Data!");
            Log.e(TAG, "data len: " + data.length);
            for (String tmp : data) {
                Log.e(TAG, tmp);
            }
            return;
        }
        //是否需要更新界面车辆信息
        boolean needUpdate = false;
        synchronized (BusInfoLock) {
            int arrivalTime = Integer.parseInt(data[7]);
            if(arrivalTime >= 60){
                Lto_distance_time = "（ " + arrivalTime/60 + "分钟 ）";
            }else{
                Lto_distance_time = "（ 不足1分钟 ）";
            }
            if (data[0].equals(MsgService.MsgTypeArrLeft)) {
                //到离站数据
                String ProductID = data[2];//车的ID
                String dualSerial = data[3];//站点ID
                int stationIdIndex = ds2ListIndex.get(dualSerial);
                int IsArrLeft = Integer.valueOf(data[4]);
                //补发标志 0 正常  1 GPRS补发  2 场站DSRC补发  5 站台上报到离站
                int isReissue = Integer.valueOf(data[5]);
                //是否是新加入的车辆
                boolean newBus = true;
                //如果是原本就在线上的车，那么此次信息中车辆状态是否改变
                boolean newPos = false;
                //车次类型，这里如果不是   4：上行、 5：下行 则表明该车辆已处于非运营/离开本线路的状态
                //需要剔除该车辆信息
                int sequenceType = Integer.valueOf(data[6]);

                //当前站点的信息
//                StationInfoItem curStation = stationList.get(curStaIndex);
//                StationItem curStation = stationList.get(curStaIndex);
//                Stations curStation = customStationsList.get(curStaIndex);
                sequenceType = 4;
                if (isReissue == 0 && MsgService.BrtUplineStat != sequenceType && MsgService.BrtDownlineStat != sequenceType) {
                    //既不是上行状态、也不是下行状态，需要剔除该信息
                    for (int i = 0; i < brtList.size(); i++) {
                        if (brtList.get(i).busID.equals(ProductID)) {
                            Log.d("BusTest", "车辆：" + ProductID + " 离开运营状态");
                            brtList.remove(i);
                            needUpdate = true;
                            break;
                        }
                    }
                } else {
                    //首先查看是否是新加入的车辆
                    for (int i = 0; i < brtList.size(); i++) {
                        BrtInfo tmp = brtList.get(i);
                        if (tmp.busID.equals(ProductID)) {
                            //不是新车，则更新该车的信息
                            newBus = false;
                            if (ds2ListIndex.containsKey(dualSerial)) {
                                //如果双程号在当前线路内
                                if (tmp.stationIdIndex != stationIdIndex || tmp.IsArrLeft != IsArrLeft || tmp.arrivalTime != arrivalTime) {
                                    //状态改变了:该车辆所在站点改变、到离站改变、到达时间改变
                                    newPos = true;
                                    //先移除该车辆信息
                                    BrtInfo oldInfo = brtList.remove(i);
                                    Log.d("BusTest", "车辆：" + ProductID + " 原状态：双程号：" +
                                            oldInfo.stationId + " 到离站：" + oldInfo.IsArrLeft);
                                    oldInfo.stationId = dualSerial;
                                    oldInfo.stationIdIndex = stationIdIndex;
                                    oldInfo.IsArrLeft = IsArrLeft;
                                    oldInfo.lastTime = System.currentTimeMillis();
                                    oldInfo.arrivalTime = arrivalTime;
//                                if (dualSerial < curStation.dualSerial || (dualSerial == curStation.dualSerial && IsArrLeft == 1)) {
                                    if (stationIdIndex < ds2ListIndex.get(stationId) || (stationIdIndex == ds2ListIndex.get(stationId) && IsArrLeft == 1)) {
                                        //如果还在往本站行驶的路上，则调整位置
                                        Log.d("BusTest", "车辆：" + ProductID + " 新状态：双程号：" + dualSerial + " 到离站：" + IsArrLeft);
                                        BrtInfoUtils.InsertBrtInfo2(oldInfo, brtList);
                                    } else {
                                        Log.d("BrtTest", "车辆：" + ProductID + " 驶过本站");
                                        //如果还在本线路但是已经驶离本站，则添加到离站车辆列表中
                                        leftBusMap.put(ProductID, System.currentTimeMillis());
                                    }
                                    needUpdate = true;
                                }
                            } else {
                                Log.d("BrtTest", "车辆：" + ProductID + "驶出本线路");
                                //如果双程号不在当前线路内，表明已驶出当前线路
                                //添加到离站车辆表中
                                leftBusMap.put(ProductID, System.currentTimeMillis());
                                //移除该车辆信息
                                brtList.remove(i);
                                needUpdate = true;
                            }
                            break;
                        }
                    }
                    //如果是新加入路线的车辆
                    if (newBus) {
                        //判断是否是刚才已经离站的车辆，如果是，则忽略该消息
                        if (leftBusMap.containsKey(ProductID)) {
                            Log.d("BrtTest", "车辆：" + ProductID + "已处于离站状态后又收到到离站信息");
                            return;
                        }
                        //判断双程号在不在线路范围
                        if (ds2ListIndex.containsKey(dualSerial)) {
//                        if (dualSerial < curStation.dualSerial || (dualSerial == curStation.dualSerial && IsArrLeft == 1)) {
                            if (stationIdIndex < ds2ListIndex.get(stationId) || (stationIdIndex == ds2ListIndex.get(stationId) && IsArrLeft == 1)) {
                                Log.d("BrtTest", "车辆：" + ProductID + " 新加入本线路，状态：双程号：" + dualSerial + " 到离站：" + IsArrLeft);
                                //如果在往本站行驶的路上
                                BrtInfoUtils.InsertBrtInfo2(new BrtInfo(ProductID, dualSerial, stationIdIndex, IsArrLeft, System.currentTimeMillis(),arrivalTime), brtList);
                                needUpdate = true;
                            } else {
                                Log.d("BrtTest", "车辆：" + ProductID + " 新加入本线路，但已越过本站");
                                //如果已经越过本站
                                needUpdate = false;
                            }
                        }
                    }
                }
            } else if (data[0].equals(MsgService.MsgTypeGps)) {
                //GPS数据
                //线路ID
                String RouteID = data[1];
                //车辆ID
                String ProductID = data[2];
                //双程号
                String dualSerialID = data[3];
                int stationIdIndex = ds2ListIndex.get(dualSerialID);
                //车次类型，这里如果不是4：上行、 5：下行 则表明该车辆已处于非运营/离开本线路的状态
                //需要剔除该车辆信息
                int sequenceType = Integer.valueOf(data[4]);

//            Log.d(TAG, "GPS Data: RouteID: " + RouteID + " ProductID: " +
//                    ProductID + " dualSerialID: " + dualSerialID + " sequenceType: " + sequenceType);

                //是否是新加入的车辆
                boolean newBus = true;
                //如果是原本就在线上的车，那么此次信息中车辆状态是否改变
                boolean newPos = false;
                //当前站点的信息
//                StationInfoItem curStation = stationList.get(curStaIndex);
//                StationItem curStation = stationList.get(curStaIndex);

                if (MsgService.BrtUplineStat != sequenceType && MsgService.BrtDownlineStat != sequenceType) {
                    //既不是上行状态、也不是下行状态，需要剔除该信息
                    for (int i = 0; i < brtList.size(); i++) {
                        if (brtList.get(i).busID.equals(ProductID)) {
                            Log.d("BrtTest", "车辆：" + ProductID + " GPS数据表明已处于非运营状态");
                            brtList.remove(i);
                            needUpdate = true;
                            break;
                        }
                    }
                } else {
                    //如果是上下行的车辆，需要检查是否是新加入的车辆
                    for (int i = 0; i < brtList.size(); i++) {
                        BrtInfoUtils.BrtInfo tmpBrtInfo = brtList.get(i);
                        if (tmpBrtInfo.busID.equals(ProductID)) {
                            //更新时间戳
                            tmpBrtInfo.lastTime = System.currentTimeMillis();
                            newBus = false;
                            break;
                        }
                    }
                    if (newBus) {
                        //检查是否是刚刚离站的车辆，如果是则忽略此次GPS数据
                        if (leftBusMap.containsKey(ProductID)) {
                            Log.d("BrtTest", "车辆：" + ProductID + "刚离站，忽略收到的GPS数据");
                            return;
                        }
                        //如果是新加入的车辆，由于GPS数据不含有到离站信息，所以这里假设是处在两站之间，由GPS数据引入的新的车辆
                        //只有在设备第一次启动（后面的到离站数据会校准当前车辆位置）和前面遗漏到离站数据的情况下才会出现
                        if (ds2ListIndex.containsKey(dualSerialID)) {
//                            int tmpDualSerialID;
                            int tmpIsArrLeft;
                            tmpIsArrLeft = 2;   //离站， 这里只是估计（大多数情况如此，即便不是这样，会有后面的到离站数据进行校正）
                            if (stationIdIndex < ds2ListIndex.get(stationId) || (stationIdIndex < ds2ListIndex.get(stationId) && tmpIsArrLeft == 1)) {
                                //如果在往本站行驶的路上
                                Log.d("BrtTest", "车辆：" + ProductID + " 新加入本线路，状态：双程号：" +
                                        dualSerialID + " 到离站：" + tmpIsArrLeft + " GPS");
                                BrtInfoUtils.InsertBrtInfo2(new BrtInfo(ProductID, dualSerialID, stationIdIndex, tmpIsArrLeft, System.currentTimeMillis(),arrivalTime), brtList);
                                needUpdate = true;
                            } else {
                                //如果已经越过本站
                                Log.d("BrtTest", "车辆：" + ProductID + " 新加入本线路，但已越过本站 GPS");
                                needUpdate = false;
                            }
                        }
                    }
                }
            }
            if (needUpdate) {
                setBrtInfo(viceDisplay);
            }
        }
    }

    /**
     * 新添加的方法 by litf
     * 设置开往什么方向，以及下一站是什么车
     */
    public void setDirectionAndNestStation(String directionStation, String directionStationEn, String nextStation, String nextStationEn) {
        Log.i(TAG, "direStation：" + directionStation + "/direStationEn:" + directionStationEn + "/nextStation:" + nextStation + "/nextStationEn:" + nextStationEn);
        if (dstStationNameTV != null && dstStationENameTV != null && nextStaName != null && nextStaEName != null) {
            nextStaName.setText(nextStation);
            nextStaEName.setText(nextStationEn);
            dstStationNameTV.setText("开往" + directionStation + "方向");
            dstStationENameTV.setText("to " + directionStationEn);
        } else {
            Log.i(TAG, "textView：没有初始化");
        }
    }

    //取brtList的前两个信息显示
    private void setBrtInfo(ViceDisplay viceDisplay) {
        Log.i("BrtTest", "MQTT：开始更新页面");
        int size = brtList.size();
        Log.i("BrtTest", "brtList.size():" + size + "/brtList:" + brtList.toString());
        if (size == 0) {
            //当前还没有BRT车辆信息暂无车辆字体为小
            Lto_distance_time = UnknownInfo_time;
            Lto_content = UnknownInfo;
            Lto_color = Color.WHITE;
            Lto_size = SmallSize;
            Lnextto_content = UnknownInfo;
            Lnextto_color = Color.WHITE;
            Lnextto_size = SmallSize;
        } else if (size == 1) {
            //当前只有一辆BRT车辆信息
            int distance = curStaIndex - ds2ListIndex.get(brtList.get(0).stationId);
            Log.i("BrtTest", "distance:" + distance);
            if (distance > 1) {
                Lto_content = distance + "站";
                Lto_color = Color.WHITE;
                Lto_size = NormalSize;
            } else if (distance == 1) {
                if (brtList.get(0).IsArrLeft == 1) {
                    Lto_content = distance + "站";
                    Lto_color = Color.WHITE;
                    Lto_size = NormalSize;
                } else {
                    Lto_content = "即将到站";
                    Lto_color = Color.GREEN;
                    Lto_size = SmallSize;

                }
            } else if (distance == 0 && brtList.get(0).IsArrLeft == 1) {
                Lto_content = "到站";
                Lto_color = Color.GREEN;
                Lto_size = NormalSize;
            }
            Lnextto_content = UnknownInfo;
            Lnextto_color = Color.WHITE;
            Lnextto_size = NormalSize;
        } else {
            //当前有至少两辆BRT车辆信息
            int distance = curStaIndex - ds2ListIndex.get(brtList.get(0).stationId);
            Log.i("BrtTest", "distance:" + distance);
            if (distance > 1) {
                Lto_content = distance + "站";
                Lto_color = Color.WHITE;
                Lto_size = NormalSize;
            } else if (distance == 1) {
                if (brtList.get(0).IsArrLeft == 1) {
                    Lto_content = distance + "站";
                    Lto_color = Color.WHITE;
                    Lto_size = NormalSize;
                } else {
                    Lto_content = "即将到站";
                    Lto_size = SmallSize;
                    Lto_color = Color.GREEN;
                }
            } else if (distance == 0 && brtList.get(0).IsArrLeft == 1) {
                Lto_content = "到站";
                Lto_color = Color.GREEN;
                Lto_size = NormalSize;
            }
            distance = curStaIndex - ds2ListIndex.get(brtList.get(1).stationId);
            if (distance > 1) {
                Lnextto_content = distance + "站";
                Lnextto_color = Color.WHITE;
                Lnextto_size = NormalSize;
            } else if (distance == 1) {
                if (brtList.get(1).IsArrLeft == 1) {
                    Lnextto_content = distance + "站";
                    Lnextto_color = Color.WHITE;
                    Lnextto_size = NormalSize;
                } else {
                    Lnextto_content = "即将到站";
                    Lnextto_size = SmallSize;
                    Lnextto_color = Color.GREEN;
                }
            } else if (distance == 0 && brtList.get(1).IsArrLeft == 1) {
                Lnextto_content = "到站";
                Lnextto_color = Color.GREEN;
                Lnextto_size = NormalSize;
            }
        }
        //更新主屏信息
//        if(Lto_station != null && Lnextto_station != null){
        if (Lto_station != null) {
            Log.i("BrtTest", "要更新显示的内容：" + Lto_content + "/" + Lto_color + "/" + Lto_size);
            Message msg = Message.obtain();
            msg.what = MsgSetBrtInfo;
            Bundle bundle = new Bundle();
            bundle.putString("tt", Lto_content);
            bundle.putInt("tc", Lto_color);
            bundle.putInt("ts", Lto_size);
            bundle.putString("ntt", Lnextto_content);
            bundle.putInt("ntc", Lnextto_color);
            bundle.putInt("nts", Lnextto_size);
            bundle.putString("time", Lto_distance_time);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
        //更新副屏信息， 主屏左边对应副屏右边
        if (viceDisplay != null) {
            viceDisplay.updateRightBrtInfo(Lto_content, Lto_color, Lto_size * 100 / 95,
                    Lnextto_content, Lnextto_color, Lnextto_size * 100 / 95);
        }
    }
}
