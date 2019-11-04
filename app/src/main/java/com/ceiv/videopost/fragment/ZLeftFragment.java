package com.ceiv.videopost.fragment;

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
import android.widget.TextView;

import com.ceiv.BrtUtils.StationItem;
import com.ceiv.videopost.HisenseMsg.MsgService;
import com.ceiv.videopost.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ceiv.BrtUtils.BrtInfoUtils;
import com.ceiv.BrtUtils.BrtInfoUtils.BrtInfo;
import com.ceiv.videopost.ViceDisplay;


/**
 * Created by chu on 2018/8/16.
 */

public class ZLeftFragment  extends Fragment {

    private final static String TAG = "ZLeftFragment";

    private final static int MsgSetBrtInfo = 0x01;
    //线路站点信息
    private ArrayList<StationItem> stationList = null;
    private List<Stations> customStationsList = null;//添加一个 by litf
    //当前站点在列表中的标号
    private int curStaIndex = -1;
    //站点数目
    private int stationCount = -1;

//    //线路站点信息
//    private ArrayList<StationInfoItem> stationList = null;
//    //当前站点在列表中的标号
//    private int curStaIndex = -1;
//    //站点数目
//    private int stationCount = -1;

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

    //双程号到stationList中站点的各站点index的映射
    private HashMap<Integer, Integer> ds2ListIndex = null;

    private final static String UnknownInfo = "暂无车辆";
    private int stationId;//当前站的ID
//    //车辆信息TextView正常大小
//    private final int NormalSize = 45;
//    //车辆信息TextView显示内容较多时的大小
//    private final int SmallSize = 35;

    //车辆信息TextView正常大小
    private final int NormalSize = 120;
    //车辆信息TextView显示内容较多时的大小
    private final int SmallSize = 90;

    //本次车信息View
    private TextView ZLto_station;
    //本次车信息内容
    private String ZLto_content;
    //本次车信息颜色
    private int ZLto_color;
    //本次车信息大小
    private int ZLto_size;
    //下次车信息View
    private TextView ZLnextto_station;
    //下次车信息内容
    private String ZLnextto_content;
    //下次车信息颜色
    private int ZLnextto_color;
    //下次车信息大小
    private int ZLnextto_size;

    private ViceDisplay mViceDisplay = null;

    private final Object BusInfoLock = new Object();

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgSetBrtInfo:
                    ZLto_station.setText(msg.getData().getString("tt"));
                    ZLto_station.setTextColor(msg.getData().getInt("tc"));
                    ZLto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, msg.getData().getInt("ts"));
//                    ZLnextto_station.setText(msg.getData().getString("ntt"));
//                    ZLnextto_station.setTextColor(msg.getData().getInt("ntc"));
//                    ZLnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, msg.getData().getInt("nts"));
                    break;
                default:
                    break;
            }
        }
    };

//    public void fragmentInit(ArrayList<StationInfoItem> stationList) {
////        this.stationList = stationList;
////        this.stationCount = stationList.size();
////        //终点站
////        this.curStaIndex = stationCount - 1;
////        brtList = new ArrayList<>();
////        ds2ListIndex = new HashMap<Integer, Integer>();
////        //初始化映射表
////        for (int i = 0; i < stationCount; i++) {
////            ds2ListIndex.put(stationList.get(i).dualSerial, i);
////        }
////
////        cleanBRTInfoThread.start();
////    }

    public void fragmentInit(ArrayList<StationItem> stationList) {
        this.stationList = stationList;
        this.stationCount = stationList.size();
        //终点站
        this.curStaIndex = stationCount - 1;
        brtList = new ArrayList<>();
        leftBusMap = new HashMap<String, Long>();
        ds2ListIndex = new HashMap<Integer, Integer>();
        //初始化映射表
        for (int i = 0; i < stationCount; i++) {
            ds2ListIndex.put(stationList.get(i).getDualSerial(), i);
        }

        cleanBRTInfoThread.start();
    }
    /**
     * 新加的By litf,可能有问题，所以没有在上面那个构造方法改
     *
     * @param stationList
     */
    public void fragmentInit2(List<Stations> stationList) {
        this.customStationsList = stationList;
        this.stationCount = stationList.size();
        //终点站
        this.curStaIndex = stationCount - 1;
        stationId = Integer.parseInt(stationList.get(curStaIndex).getId());
        brtList = new ArrayList<>();
        leftBusMap = new HashMap<String, Long>();
        ds2ListIndex = new HashMap<Integer, Integer>();
        //初始化映射表
        for (int i = 0; i < stationCount; i++) {
            ds2ListIndex.put(Integer.parseInt(stationList.get(i).getId()), i);
        }

        cleanBRTInfoThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.zleft_fragment,container,false);
        Log.e(TAG, "终点ZLeftFragment");
        ZLto_station=(TextView)view.findViewById(R.id.ZLto_station);
//        ZLnextto_station=(TextView)view.findViewById(R.id.ZLnextto_station);

        //初始化BRT信息
        ZLto_station.setText(UnknownInfo);
        ZLto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, SmallSize);
//        ZLnextto_station.setText(UnknownInfo);
//        ZLnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
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
                    for (int i = 0; i < brtList.size(); i++) {
                        if (Math.abs(brtList.get(i).lastTime - System.currentTimeMillis()) > 10 * 60 * 1000) {
                            //长时间没有收到该车的任何数据，则认为该车已经离线
                            final BrtInfoUtils.BrtInfo tmpBrtInfo = brtList.remove(i);
                            Log.d(TAG, "BusID: " + tmpBrtInfo.busID + " dualSerialID: " + tmpBrtInfo.dualSerial + " offline!");
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
        Log.i(TAG, "ZLeftFragment MQTT消息：brtInfo:" + brtInfo);
        if (mViceDisplay == null) {
            mViceDisplay = viceDisplay;
        }
        //$是特殊符号，必须用[$]来分割
        String[] data = brtInfo.split("[" + MsgService.MsgSeparator + "]");
        if (data.length != 7 || !(data[1].equals(MsgService.routeId))) {
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
            if (data[0].equals(MsgService.MsgTypeArrLeft)) {
                //到离站数据
                String ProductID = data[2];
                int dualSerial = Integer.valueOf(data[3]);
                int IsArrLeft = Integer.valueOf(data[4]);
                //补发标志 0 正常  1 GPRS补发  2 场站DSRC补发  5 站台上报到离站
                int isReissue = Integer.valueOf(data[5]);
                //是否是新加入的车辆
                boolean newBus = true;
                //如果是原本就在线上的车，那么此次信息中车辆状态是否改变
                boolean newPos = false;
                //车次类型，这里如果不是4：上行、 5：下行 则表明该车辆已处于非运营/离开本线路的状态
                //需要剔除该车辆信息
                int sequenceType = Integer.valueOf(data[6]);

//        //查看是否是当前线路的数据
//        if (!ds2ListIndex.containsKey(dualSerial)) {
//            return;
//        }

                //当前站点的信息
//                StationItem curStation = stationList.get(curStaIndex);
//                StationInfoItem curStation = stationList.get(curStaIndex);
                sequenceType = 4;
                if (isReissue == 0 && MsgService.BrtUplineStat != sequenceType && MsgService.BrtDownlineStat != sequenceType) {
                    //既不是上行状态、也不是下行状态，需要剔除该信息
                    for (int i = 0; i < brtList.size(); i++) {
                        if (brtList.get(i).busID.equals(ProductID)) {
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
                                if (tmp.dualSerial != dualSerial || tmp.IsArrLeft != IsArrLeft) {
                                    //状态改变了
                                    newPos = true;
                                    //先移除该车辆信息
                                    BrtInfo oldInfo = brtList.remove(i);
                                    oldInfo.dualSerial = dualSerial;
                                    oldInfo.IsArrLeft = IsArrLeft;
                                    oldInfo.lastTime = System.currentTimeMillis();
//                                if (dualSerial < curStation.dualSerial || (dualSerial == curStation.dualSerial && IsArrLeft == 1)) {
                                    if (dualSerial < stationId || (dualSerial == stationId && IsArrLeft == 1)) {
                                        //如果还在往本站行驶的路上，则调整位置
                                        BrtInfoUtils.InsertBrtInfo(oldInfo, brtList);
                                    } else {
                                        //如果还在本线路但是已经驶离本站，则添加到离站车辆列表中
                                        leftBusMap.put(ProductID, System.currentTimeMillis());
                                    }
                                    needUpdate = true;
                                }
                            } else {
                                //添加到离站车辆表中
                                leftBusMap.put(ProductID, System.currentTimeMillis());
                                brtList.remove(i);
                                needUpdate = true;
                            }

                            break;
                        }
                    }
                    //是新加入路线的车辆
                    if (newBus) {
                        //判断是否是刚才已经离站的车辆，如果是，则忽略该消息
                        if (leftBusMap.containsKey(ProductID)) {
                            return;
                        }
                        //判断双程号是否在当前线路内
                        if (ds2ListIndex.containsKey(dualSerial)) {
//                        if (dualSerial < curStation.dualSerial || (dualSerial == curStation.dualSerial && IsArrLeft == 1)) {
                            if (dualSerial < stationId || (dualSerial == stationId && IsArrLeft == 1)) {
                                //如果在往本站行驶的路上
                                BrtInfoUtils.InsertBrtInfo(new BrtInfo(ProductID, dualSerial, IsArrLeft, System.currentTimeMillis()), brtList);
                                needUpdate = true;
                            } else {
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
                int dualSerialID = Integer.valueOf(data[3]);
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
                StationItem curStation = stationList.get(curStaIndex);

                if (MsgService.BrtUplineStat != sequenceType && MsgService.BrtDownlineStat != sequenceType) {
                    for (int i = 0; i < brtList.size(); i++) {
                        if (brtList.get(i).busID.equals(ProductID)) {
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
                            return;
                        }
                        //如果是新加入的车辆，由于GPS数据不含有到离站信息，所以这里假设是处在两站之间，由GPS数据引入的新的车辆
                        //只有在设备第一次启动（后面的到离站数据会校准当前车辆位置）和前面遗漏到离站数据的情况下才会出现
                        if (ds2ListIndex.containsKey(dualSerialID)) {
                            int tmpDualSerialID;
                            int tmpIsArrLeft;

//                            if (stationList.get(0).getDualSerial() == dualSerialID) {
//                                //如果是当前行进方向上的第一个站的双程号，则表明车辆刚进入线路，还未驶出第一个站
//                                tmpDualSerialID = dualSerialID;
//                                tmpIsArrLeft = 1;
//                            } else {
//                                //取前一个站的双程号
////                                tmpDualSerialID = stationList.get(ds2ListIndex.get(dualSerialID) - 1).dualSerial;
//                                tmpDualSerialID = stationList.get(ds2ListIndex.get(dualSerialID) - 1).getDualSerial();
//                                tmpIsArrLeft = 2;   //离站， 这里只是估计（大多数情况如此，即便不是这样，会有后面的到离站数据进行校正）
//                            }
                            tmpIsArrLeft = 2;   //离站， 这里只是估计（大多数情况如此，即便不是这样，会有后面的到离站数据进行校正）
                            if (dualSerialID < curStation.getDualSerial() || (dualSerialID == curStation.getDualSerial() && tmpIsArrLeft == 1)) {
                                //如果在往本站行驶的路上
                                BrtInfoUtils.InsertBrtInfo(new BrtInfo(ProductID, dualSerialID, tmpIsArrLeft, System.currentTimeMillis()), brtList);
                                needUpdate = true;
                            } else {
                                //如果已经越过本站
                                needUpdate = false;
                            }
//                            BrtInfoUtils.InsertBrtInfo(new BrtInfo(ProductID, tmpDualSerialID, tmpIsArrLeft, System.currentTimeMillis()), brtList);
//                            //brtList.add(new BrtInfo(ProductID, tmpDualSerialID, tmpIsArrLeft, System.currentTimeMillis()));
//                            needUpdate = true;
                        }
                    }
                }
            }
            if (needUpdate) {
                setBrtInfo(viceDisplay);
            }
        }
    }

    //取brtList的前两个信息显示
    private void setBrtInfo(ViceDisplay viceDisplay){
        Log.i("BrtTest", "MQTT：开始更新页面");
        int size = brtList.size();
        Log.i("BrtTest", "brtList.size():"+size+"/brtList:"+brtList.toString());
        if (size == 0) {
            //当前还没有BRT车辆信息暂无车辆字体为小
            ZLto_content = UnknownInfo;
            ZLto_color = Color.WHITE;
            ZLto_size = SmallSize;
            ZLnextto_content = UnknownInfo;
            ZLnextto_color = Color.WHITE;
            ZLnextto_size = SmallSize;
        } else if (size == 1) {
            //当前只有一辆BRT车辆信息
            int distance = curStaIndex - ds2ListIndex.get(brtList.get(0).dualSerial);
            Log.i("BrtTest", "distance:"+distance);
            if (distance > 1) {
                ZLto_content = distance + "站";
                ZLto_color = Color.WHITE;
                ZLto_size = NormalSize;
            } else if (distance == 1) {
                if (brtList.get(0).IsArrLeft == 1) {//IsArrLeft：1到站；2离站；3途中
                    ZLto_content = distance + "站";
                    ZLto_color = Color.WHITE;
                    ZLto_size = NormalSize;
                } else {//IsArrLeft：1到站；2离站；3途中。
                    ZLto_content = "即将到站";
                    ZLto_color = Color.GREEN;
                    ZLto_size = SmallSize;
                }
            } else if (distance == 0 && brtList.get(0).IsArrLeft == 1) {//IsArrLeft：1到站；2离站；3途中。目前不显示离站
                ZLto_content = "到站";
                ZLto_color = Color.GREEN;
                ZLto_size = NormalSize;
            }
            ZLnextto_content = UnknownInfo;
            ZLnextto_color = Color.WHITE;
            ZLnextto_size = NormalSize;
        } else {
            //当前有至少两辆BRT车辆信息
            int distance = curStaIndex - ds2ListIndex.get(brtList.get(0).dualSerial);
            if (distance > 1) {
                ZLto_content = distance + "站";
                ZLto_color = Color.WHITE;
                ZLto_size = NormalSize;
            } else if (distance == 1) {
                if (brtList.get(0).IsArrLeft == 1) {
                    ZLto_content = distance + "站";
                    ZLto_color = Color.WHITE;
                    ZLto_size = NormalSize;
                } else {
                    ZLto_content = "即将到站";
                    ZLto_color = Color.GREEN;
                    ZLto_size = SmallSize;
                }
            } else if (distance == 0 && brtList.get(0).IsArrLeft == 1) {
                ZLto_content = "到站";
                ZLto_color = Color.GREEN;
                ZLto_size = NormalSize;
            }
            distance = curStaIndex - ds2ListIndex.get(brtList.get(1).dualSerial);
            if (distance > 1) {
                ZLnextto_content = distance + "站";
                ZLnextto_color = Color.WHITE;
                ZLnextto_size = NormalSize;
            } else if (distance == 1) {
                if (brtList.get(1).IsArrLeft == 1) {
                    ZLnextto_content = distance + "站";
                    ZLnextto_color = Color.WHITE;
                    ZLnextto_size = NormalSize;
                } else {
                    ZLnextto_content = "即将到站";
                    ZLnextto_color = Color.GREEN;
                    ZLnextto_size = SmallSize;
                }
            } else if (distance == 0 && brtList.get(1).IsArrLeft == 1) {
                ZLnextto_content = "到站";
                ZLnextto_color = Color.GREEN;
                ZLnextto_size = NormalSize;
            }
        }
        //更新主屏信息
//        if(ZLto_station != null && ZLnextto_station != null){
            Message msg = Message.obtain();
        if(ZLto_station != null){
            msg.what = MsgSetBrtInfo;
            Bundle bundle = new Bundle();
            bundle.putString("tt", ZLto_content);
            bundle.putInt("tc", ZLto_color);
            bundle.putInt("ts", ZLto_size);
            bundle.putString("ntt", ZLnextto_content);
            bundle.putInt("ntc", ZLnextto_color);
            bundle.putInt("nts", ZLnextto_size);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
        //更新副屏信息， 主屏左边对应副屏右边
        if (viceDisplay != null) {
            viceDisplay.updateRightBrtInfo(ZLto_content, ZLto_color, ZLto_size*100/95,
                    ZLnextto_content, ZLnextto_color, ZLnextto_size*100/95);
        }
    }

    /*
     *  下面的代码是同时显示本次车和下次车的信息，现在由于海信那边要求只显示本次车的信息
     *  且将“即将到站”状态去除
     *
     */
//    //取brtList的前两个信息显示
//    private void setBrtInfo(ViceDisplay viceDisplay){
//        int size = brtList.size();
//        if (size == 0) {
//            //当前还没有BRT车辆信息
//            ZLto_content = UnknownInfo;
//            ZLto_color = Color.WHITE;
//            ZLto_size = NormalSize;
//            ZLnextto_content = UnknownInfo;
//            ZLnextto_color = Color.WHITE;
//            ZLnextto_size = NormalSize;
//        } else if (size == 1) {
//            //当前只有一辆BRT车辆信息
//            int distance = curStaIndex - ds2ListIndex.get(brtList.get(0).dualSerial);
//            if (distance > 1) {
//                ZLto_content = distance + "站";
//                ZLto_color = Color.WHITE;
//                ZLto_size = NormalSize;
//            } else if (distance == 1) {
//                if (brtList.get(0).IsArrLeft == 1) {
//                    ZLto_content = distance + "站";
//                    ZLto_color = Color.WHITE;
//                    ZLto_size = NormalSize;
//                } else {
//                    ZLto_content = "即将到站";
//                    ZLto_color = Color.GREEN;
//                    ZLto_size = SmallSize;
//                }
//            } else if (distance == 0 && brtList.get(0).IsArrLeft == 1) {
//                ZLto_content = "到站";
//                ZLto_color = Color.GREEN;
//                ZLto_size = NormalSize;
//            }
//            ZLnextto_content = UnknownInfo;
//            ZLnextto_color = Color.WHITE;
//            ZLnextto_size = NormalSize;
//        } else {
//            //当前有至少两辆BRT车辆信息
//            int distance = curStaIndex - ds2ListIndex.get(brtList.get(0).dualSerial);
//            if (distance > 1) {
//                ZLto_content = distance + "站";
//                ZLto_color = Color.WHITE;
//                ZLto_size = NormalSize;
//            } else if (distance == 1) {
//                if (brtList.get(0).IsArrLeft == 1) {
//                    ZLto_content = distance + "站";
//                    ZLto_color = Color.WHITE;
//                    ZLto_size = NormalSize;
//                } else {
//                    ZLto_content = "即将到站";
//                    ZLto_color = Color.GREEN;
//                    ZLto_size = SmallSize;
//                }
//            } else if (distance == 0 && brtList.get(0).IsArrLeft == 1) {
//                ZLto_content = "到站";
//                ZLto_color = Color.GREEN;
//                ZLto_size = NormalSize;
//            }
//            distance = curStaIndex - ds2ListIndex.get(brtList.get(1).dualSerial);
//            if (distance > 1) {
//                ZLnextto_content = distance + "站";
//                ZLnextto_color = Color.WHITE;
//                ZLnextto_size = NormalSize;
//            } else if (distance == 1) {
//                if (brtList.get(1).IsArrLeft == 1) {
//                    ZLnextto_content = distance + "站";
//                    ZLnextto_color = Color.WHITE;
//                    ZLnextto_size = NormalSize;
//                } else {
//                    ZLnextto_content = "即将到站";
//                    ZLnextto_color = Color.GREEN;
//                    ZLnextto_size = SmallSize;
//                }
//            } else if (distance == 0 && brtList.get(1).IsArrLeft == 1) {
//                ZLnextto_content = "到站";
//                ZLnextto_color = Color.GREEN;
//                ZLnextto_size = NormalSize;
//            }
//        }
//        //更新主屏信息
//        if(ZLto_station != null && ZLnextto_station != null){
//            Message msg = Message.obtain();
//            msg.what = MsgSetBrtInfo;
//            Bundle bundle = new Bundle();
//            bundle.putString("tt", ZLto_content);
//            bundle.putInt("tc", ZLto_color);
//            bundle.putInt("ts", ZLto_size);
//            bundle.putString("ntt", ZLnextto_content);
//            bundle.putInt("ntc", ZLnextto_color);
//            bundle.putInt("nts", ZLnextto_size);
//            msg.setData(bundle);
//            handler.sendMessage(msg);
//        }
//        //更新副屏信息， 主屏左边对应副屏右边
//        if (viceDisplay != null) {
//            viceDisplay.updateRightBrtInfo(ZLto_content, ZLto_color, ZLto_size,
//                    ZLnextto_content, ZLnextto_color, ZLnextto_size);
//        }
//    }

}