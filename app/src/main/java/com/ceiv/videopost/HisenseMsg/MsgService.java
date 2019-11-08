package com.ceiv.videopost.HisenseMsg;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
//import android.util.Log;
import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.DeviceInfoUtils;
import com.ceiv.log4j.Log;

import com.ceiv.communication.utils.SystemInfoUtils;
import com.ceiv.videopost.CodeConstants;
import com.ceiv.videopost.utils.SpUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author made by ceiv
 */
public  class MsgService extends Service {

    private final static String TAG = "MsgService";
    private final static String ArrLeftDataTag = "ArrLeftDataTag";
    private final static String GpsDataTag = "GpsDataTag";

    public final static String MsgSeparator = "$";
    public final static String MsgTypeArrLeft = "1";
    public final static String MsgTypeGps = "2";

    public final static int BrtUplineStat = 4;
    public final static int BrtDownlineStat = 5;

    //海信服务器地址
//    public static final String BROKER_URL = "tcp://172.16.10.37:1883";
    public static  String BROKER_URL = "";//见初始化
//    public static  String BROKER_URL = "tcp://aids.zdhs.com.cn:1883";
//    public static final String BROKER_URL = "tcp://172.16.30.254:1883";
    //    public static final String clientId = "ceiv-client";
    public String clientId;
    public static final String TopicArrLev = "TopicArrLev";
    public static final String TopicGPS = "TopicGPS";
    public static final String TopicBrt = "com/ceiv/busmsg/zzbrt";
//        private String userName = "admin"; // 连接的用户名
//    private String passWord = "pi@1415"; //连接的密码
    private String userName = "zzx"; // 连接的用户名
    private String passWord = "zzx"; //连接的密码

//    public static final String BROKER_URL = "tcp://192.168.43.45:1883";
//    public static final String clientId = "android-client";
//    public static final String TOPIC = "de/eclipsemagazin/blackice/warnings";
//    private String userName = "admin"; // 连接的用户名
//    private String passWord = "admin"; //连接的密码

    private Context appContext;

    public MqttAndroidClient mqttClient;

    public MqttConnectOptions options;
    private ScheduledExecutorService scheduler;
    private ConnectivityManager mConnectivityManager; // To check for connectivity changes
    private OnMqttListener onMqttListener;
    public static String routeId = "";//公交线路名称
    //到离站数据
    private String ArrLeftData = "";
    private String ArrLeftDataOld = "";

    //GPS数据
    private String GpsData = "";
    private String GpsDataOld = "";

    private Boolean mRetained;

    //尝试连接是否在运行
    private boolean tryConnRuning;
    //是否需要连接mqtt
    private boolean needConn;

    /*函数接口，在mainactivity重写该函数*/
    public interface OnMqttListener {
        //        void onRecvMsg(ArrayList hxdata);
        void onRecvMsg(String data);
    }

    public void setOnMqttListener(OnMqttListener onMqttListener) {
        this.onMqttListener = onMqttListener;
    }
    /**
     * 注册回调接口的方法，供外部调用
     * @param onMqttListener
     */
    public void setMqttListener(OnMqttListener onMqttListener) {
        this.setOnMqttListener(onMqttListener);
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return new MsgBinder();
    }
    public class MsgBinder extends Binder {
        public MsgService getService(){
            return MsgService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        needConn = true;
        tryConnRuning = false;
        appContext = getApplicationContext();
        clientId = SystemInfoUtils.getMqttClientId()+"111";
        routeId = SpUtils.getString(CodeConstants.ROUTE_ID);
        DeviceInfo deviceInfo = DeviceInfoUtils.getDeviceInfoFromFile();
        String serverIp = deviceInfo.getServerIp();
        Log.d(TAG, "serverIp: " + serverIp);
        BROKER_URL = "tcp://"+ serverIp + ":1883";
        Log.d(TAG, "BROKER_URL: " + BROKER_URL +"/Mqtt Client ID: " + clientId);
        init();           //初始化相关配置
        startConnect();        //连接mqtt服务器
        super.onCreate();
    }

    /**
     * 初始化相关数据
     */
    public void init() {
        try {
            mqttClient = new MqttAndroidClient(this, BROKER_URL, clientId);
            //MQTT的连接设置
            options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);

            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调

            mqttClient.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    //连接丢失，5s后进行重连
                    Log.d(TAG,"MQTT connection lost...\n" + cause);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startConnect();
                        }
                    }, 5 * 1000);
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    routeId = SpUtils.getString(CodeConstants.ROUTE_ID);
                    //subscribe后得到的消息会执行到这里面
                    // System.out.println("Msg" + message);
                    Log.d(TAG,"MQTT 收到消息111：");
                    String str = new String(message.getPayload()) ;
                    Log.d(TAG,"MQTT str："+ str);
                    byte[] msgbyte = message.getPayload();
                    int i=msgbyte.length;
                    Log.d(TAG,"MQTT 收到消息111："+i);
                    int length_msg = (msgbyte[0] & 0xff) << 8 | (msgbyte[1] & 0xff);
                    int type = (msgbyte[2] & 0xff) << 8 | (msgbyte[3] & 0xff);
                    // System.out.println("Msg type:" + type);
                    byte[] msgbyte_body = new byte[length_msg-2];
                    System.arraycopy(msgbyte, 4, msgbyte_body, 0, length_msg-2);
                    //到离站信息
                    Log.d(TAG,"MQTT 收到消息："+type);
                    if (type == 0x03) {
                        ProtoBufMsgGpsArrleaNn.BusArrLeftMsg busArrLeftMsg = ProtoBufMsgGpsArrleaNn.BusArrLeftMsg.parseFrom(msgbyte_body);
                        //将数据添加到list
                        Log.d(TAG,"本机路线:"+routeId+"/收到消息的路线："+ busArrLeftMsg.getRouteID());
                        if(busArrLeftMsg.getRouteID().equals(routeId)) {
                            String ss = busArrLeftMsg.getIsArrLeft();
                            Log.d(TAG,"MQTT Arr："+ss);
                            int isArrLeft = Integer.valueOf(ss);
                            Log.d(ArrLeftDataTag, "车辆：" + busArrLeftMsg.getProductID() +
                                    " 双程号：" + busArrLeftMsg.getDualSerialid() + " 补发标志：" + busArrLeftMsg.getIsReissue() +
                                    " 上报时间：" + busArrLeftMsg.getMsgTime() + " 车次类型：" + busArrLeftMsg.getSequenceType() +
                                    ((isArrLeft == 1) ? " 到了 " : (isArrLeft == 2) ? " 离开 " : " 未知 "));
                            //40s之前的数据忽略
                            //                          Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(busArrLeftMsg.getMsgTime());
//                            if (Math.abs(System.currentTimeMillis() - date.getTime()) > 40 * 1000) {
//                                Log.d(ArrLeftDataTag, "Data Timeout, discard");
//                                return;
//                            } else
//                           {
                            ArrLeftData = MsgTypeArrLeft +
                                    MsgSeparator + busArrLeftMsg.getRouteID() +
                                    MsgSeparator + busArrLeftMsg.getProductID() +
                                    MsgSeparator + busArrLeftMsg.getDualSerialid() +
                                    MsgSeparator + busArrLeftMsg.getIsArrLeft() +
                                    MsgSeparator + busArrLeftMsg.getIsReissue() +
                                    MsgSeparator + busArrLeftMsg.getSequenceType();
                            Log.d(TAG,"MQTT ArrLeftData："+ArrLeftData);
                            if (!ArrLeftData.equals(ArrLeftDataOld)) {
                                if (null != onMqttListener) {
                                    onMqttListener.onRecvMsg(ArrLeftData);
                                }
                                ArrLeftDataOld = ArrLeftData;
                            }
//                            }
                        }
                    } else if (type == 0x02) {
                        /*
                         *   GPS数据，不管车辆的状态是否改变了，该数据都会以一定频率定时发送
                         *   这里接受这个数据主要是随时监控车辆的状态，例如GPS数据中含有车次类型，
                         *   当车次类型不为4：上行   5：下行时，我们认为车辆已经处于非运营/不在线路内行驶
                         *   的状态了，必须将车辆信息剔除
                         * */
                        ProtoBufMsgGpsArrleaNn.BusGpsMsg busGpsMsg = ProtoBufMsgGpsArrleaNn.BusGpsMsg.parseFrom(msgbyte_body);
                        if (busGpsMsg.getRouteID().equals(routeId)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("\nGPS数据：");
                            sb.append("车辆：" + busGpsMsg.getProductID() + " 双程号：" + busGpsMsg.getDualSerialid() +
                                    " 补发标志：" + busGpsMsg.getIsReissue() + " 上报时间：" + busGpsMsg.getMsgTime() + " 车次类型：" + busGpsMsg.getSequenceType());
                            sb.append("\n");
                            Log.d(GpsDataTag, sb.toString());
                            //40s之前的数据忽略
                            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(busGpsMsg.getMsgTime());
                            if (Math.abs(System.currentTimeMillis() - date.getTime()) > 40 * 1000) {
                                Log.d(GpsDataTag, "Data Timeout, discard");
                                return;
                            } else {
                                GpsData = MsgTypeGps +
                                        MsgSeparator + busGpsMsg.getRouteID() +
                                        MsgSeparator + busGpsMsg.getProductID() +
                                        MsgSeparator + busGpsMsg.getDualSerialid() +
                                        MsgSeparator + busGpsMsg.getSequenceType() +
                                        MsgSeparator + "null" +
                                        MsgSeparator + "null";
                                if (!GpsData.equals(GpsDataOld)) {
                                    if (null != onMqttListener) {
                                        onMqttListener.onRecvMsg(GpsData);
                                    }
                                    GpsDataOld = GpsData;
                                }
                            }
                        }
                    }
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private IMqttActionListener subscribeListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken iMqttToken) {
            Log.d(TAG, "Subscribe Topic success!");
        }

        @Override
        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
            Log.d(TAG, "Subscribe Topic failed!\n" + throwable);
            //订阅失败，2s后尝试重新连接
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (mqttClient.isConnected()) {
                            mqttClient.disconnect();
                        }
                        startConnect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }, 2 * 1000);
        }
    };

    private IMqttActionListener connectListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken iMqttToken) {
            Log.i(TAG, "Mqtt connect success!");
            try {
                Log.d(TAG, "Try to Subscribe Topic...");
                mqttClient.subscribe(TopicBrt, 1, null, subscribeListener);
            } catch (MqttException e) {
                Log.e(TAG, "Subscribe Topic error!", e);
                //e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
            Log.d(TAG, "Mqtt connect failed: \n" + throwable);
            //连接失败，5s后重新尝试连接
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    startConnect();
                }
            }, 5 * 1000);
        }
    };

    /**
     *  调用init() 方法之后，调用此方法。
     */
    public synchronized void startConnect() {
        Log.d(TAG, "startConnect");
        if (needConn && !mqttClient.isConnected()) {
            try {
                mqttClient.connect(options, null, connectListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Receiver that listens for connectivity changes
     * via ConnectivityManager
     * 网络状态发生变化接收器
     */
    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (SystemInfoUtils.isEthernetConnected(appContext)) {
                    //网络正常
                    Log.d(TAG, "Network connected!");
                    needConn = true;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startConnect();
                        }
                    }, 5 * 1000);

                } else {
                    //网络断开
                    Log.d(TAG, "Network disconnected!");
                    needConn = false;
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        unregisterReceiver(mConnectivityReceiver);
        try {
            mqttClient.disconnect(0);
        } catch (MqttException e) {
            Log.e(TAG, "MQTT disconnect error!", e);
            //e.printStackTrace();
        }
    }
}
