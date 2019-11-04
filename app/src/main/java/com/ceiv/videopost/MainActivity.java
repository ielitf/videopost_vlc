package com.ceiv.videopost;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.ceiv.ApplicationMonitorService;
import com.ceiv.BrtUtils.BrtInfoUtils;
import com.ceiv.BrtUtils.RouteInfo;
import com.ceiv.BrtUtils.StationItem;
import com.ceiv.BrtUtils.bean.SearchBasicInfo;
import com.ceiv.BrtUtils.bean.Stations;
import com.ceiv.communication.NetMgrDefine;
import com.ceiv.communication.NetMgrMsg;
import com.ceiv.communication.ProtocolMessageProcess;
import com.ceiv.communication.SystemInitThread;
import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.DeviceInfoUtils;
import com.ceiv.communication.utils.SystemInfoUtils;
import com.ceiv.log4j.Log;
import com.ceiv.videopost.HisenseMsg.MsgService;
import com.ceiv.videopost.fragment.ButtomFragment;
import com.ceiv.videopost.fragment.LeftFragment;
import com.ceiv.videopost.fragment.QLeftFragment;
import com.ceiv.videopost.fragment.QRightFragment;
import com.ceiv.videopost.fragment.RightFragment;
import com.ceiv.videopost.fragment.TopFragment;
import com.ceiv.videopost.fragment.ZLeftFragment;
import com.ceiv.videopost.fragment.ZRightFragment;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//import android.util.Log;


public class MainActivity extends FragmentActivity implements SystemInfoUtils.ApplicationOperation {
    private Intent intent, bindIntent;
    private static boolean isSystemInitThread;

    static {
        isSystemInitThread = false;
    }

    private ViceDisplay mViceDisplay;//副屏
    //private VideoView videoView;

    private RelativeLayout mRelativeLayout = null;
    private SurfaceView mVideoSurface = null;


    private VideoController videoController;
    private TextView textDebugInfo;

    private TextView networkInfo;

    private MsgService msgService;
    Message message;
    private final static String TAG = "MainActivity";
    private final static String TestTag = "TestTag";

    public static final String SERVICE_CLASSNAME = "com.ceiv.videopost.HisenseMsg.MsgService";
    private final static String cityCode = "101300101";
    //private final static String HisenseServerIP = "124.227.197.82";
    private final static String HisenseServerIP = "172.16.10.105";
    private final static int HisenseServerPort = 1001;
    private final static String secretKey = "3fff3349acbe4fffb1680ce8ad4b37d1";

    //界面是否被初始化过
    boolean isFragmentAdded = false;
    //界面各Fragment
    TopFragment topFragment;
    ButtomFragment buttomFragment;
    LeftFragment leftFragment;
    RightFragment rightFragment;
    //如果当前站点为终点站时的Fragment
    ZLeftFragment zLeftFragment;
    ZRightFragment zRightFragment;

    /*
     *  表示时间是否联网校准过的标志位，程序开始运行后，会主动通过网络请求进行时间校准，
     *  一旦校准过一次后就不在进行网络请求
     */
    private boolean reqTimeFlag = false;
    private Timer reqTimeTimer = null;
    private TimerTask reqTimeTask = null;

    /*
     *  表示是否成功获取过路线信息的标志位，程序开始运行后，会主动通过网络请求获取路线信息，
     *  一旦成功获取一次路线信息后就会停止请求
     */
    private boolean reqRouteInfoFlag = false;
    private Timer reqRouteInfoTimer = null;
    private TimerTask reqRouteInfoTask = null;

    //请求获取天气信息
    private Timer reqWeatherTimer = null;
    private TimerTask reqWeatherTask = null;

    //网络lag重启
    private Timer restartTimer = null;
    private TimerTask restartDevice = null;
    RestartTimesOperation mRestartTimesOperation = new RestartTimesOperation();
    JSONObject mJson = null;
    //当前设备所在的位置（所在的站点编号）
    private int DevicePosition = -1;
    //当前设备所在位置的线路方向（上行、下行），对于某些在中间的设备不区分上下行
    private int DeviceDirection = -1;

    //更新网络状态
    public final static int MsgUpdateNetworkInfo = 0xf9;
    //更新路线信息
    public final static int MsgUpdateRouteInfo = 0xfa;
    //初始化路线信息
    public final static int MsgInitRouteInfo = 0xfb;
    //初始化设备信息
    public final static int MsgInitDevInfo = 0xfc;
    //更新BRT车辆信息
    public final static int MsgUpdateBRTInfo = 0xfd;
    //更新站点信息
    public final static int MsgUpdateStation = 0xfe;
    //初始化站点信息
    public final static int MsgInitStation = 0xff;

    //设备信息
    private DeviceInfo deviceInfo = null;
    /*
     *  stationID指当前屏幕所在站点位置（双程号）
     *  对于背靠背屏来说，主要有两种情况，线路区间中大多数屏都是：中间是站台，两边是线路，分别是上行和下行，
     *  站台上屏幕上左右两块区域分别显示两条线路的情况，此时stationID为该站点的上/下双程号都行；
     *  另一种情况是：上下行路线在中间，路的两边有分别针对上/下行线路的屏幕，起点、终点和区间中间的某些站点
     *  是这种情况；
     *
     */
    private int stationID = -1;
    //屏幕显示的主题，和主副屏安装的位置、上面所述的两种情况有关系
    private int themeStyle = -1;
    //包含上下行线路的所有站点信息
    private StationInfo stationInfo = null;
    //初始化的stationID是否是下行线路的双程号，true：是；false：否
    boolean isDownline = false;
    //站点位置标志, 0x01: 起点  0x02: 终点  0x03: 中间站点
    int posFlag = 0x0;

    private String basicInfoStr = "";//开机收到的站点相关信息；
    private List<com.ceiv.BrtUtils.bean.StationInfo> stationInfoList = new ArrayList<>();
    private List<Stations> stationUpList = new ArrayList<>();
    private List<Stations> stationDownList = new ArrayList<>();
//    private List<Stations> stationList = new ArrayList<>();
//    private SearchBasicInfo searchBasicInfo;

    private String routeOfDevice = "";//配置文件中,当前设备所在的线路
    private String routeId;//公交线路，网络获取的
    private String routeIdFromSp;//公交线路，网络获取的
    private String direction;//方向
    private String busName;//公交线路名称
    private int position_stationInfo;
    private int position_station;//当前站的id在Stations集合里的位置
    private int curDevPos;//当前站点ID
    boolean findCurStation = false;//能否在路线信息中找到设备坐在位置的站点
    private String nextStation = "", nextStationEn = "", direStation = "", direStationEn = "";
    private QLeftFragment qLeftFragment;

    //全局字体
    public static Typeface gloTypeface;
    //信号中断时重启
    private HttpRequest httpRequest = null;
    private HttpRequest.RequestCallBack callBack = new HttpRequest.RequestCallBack() {
        @Override
        public void requestTime(String respone, boolean success) {
            Log.d(TAG, "RequestTime: " + (success ? "success" : "failed"));
            Log.d("request_debug", "time: " + (success ? respone : "failed"));
            if (success) {
                String[] data = respone.split(" ");
                String[] date = data[0].split("-");
                String[] time = data[1].split(":");
                int year = Integer.valueOf(date[0]);
                int month = Integer.valueOf(date[1]);
                int day = Integer.valueOf(date[2]);
                int hour = Integer.valueOf(time[0]);
                int minute = Integer.valueOf(time[1]);
                int second = Integer.valueOf(time[2]);
                //进行简单的检查
                if (year < 2018 || year > 2118 || month > 12 || month < 1 || day < 1 || day > 31 ||
                        hour < 0 || hour > 24 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                    Log.d(TAG, "Invalid Time Value!");
                    return;
                }
                reqTimeFlag = true;
                //成功获取时间后停止定时任务
                if (reqTimeTimer != null) {
                    reqTimeTimer.cancel();
                    reqTimeTimer = null;
                }
                SystemInfoUtils.setSystemTime(MainActivity.this, year, month, day, hour, minute, second);
            }
        }

        @Override
        public void requestWeather(String respone, boolean success) {
            Log.d(TAG, "RequestWeather: " + (success ? "success" : "failed"));
            Log.d("request_debug", "weather: " + (success ? respone : "failed"));
            if (success) {
                //更新主屏天气信息
                Log.d(TAG, "gona to update display[1] weather info!");
                topFragment.updateTemp(respone);
                //更新副屏天气信息
                if (mViceDisplay != null) {
                    Log.d(TAG, "gona to update display[2] weather info!");
                    mViceDisplay.updateTemp(respone);
                }
            }
        }

        @Override
        public void requestRouteInfo(RouteInfo routeInfo, boolean success) {
            Log.d(TAG, "RequestRouteInfo: " + (success ? "success" : "failed"));
            if (success) {
                /*
                 *  如果http获取到的路线信息不包含本设备所在的位置的
                 *  站点的信息，表明本站点暂未开通，所以不采用该获取的
                 *  新的路线信息
                 *  获取路线信息后要和系统已经存储的路线信息进行对比
                 *  如果不一致的话，需要更新配置文件中的信息
                 */
                if (!DeviceInfoUtils.DeviceInfoUtilsInit(MainActivity.this)) {
                    return;
                }
                boolean findDevPos = false;
                int tmpDevPos = DeviceInfoUtils.getDevicePositionFromIdentify(DeviceInfoUtils.getDeviceInfoFromFile().getIdentify());
//                ArrayList<StationItem> upline = BrtInfoUtils.getCurRouteInfo().getUpline().getStationList();
//                ArrayList<StationItem> downline = BrtInfoUtils.getCurRouteInfo().getDownline().getStationList();

                ArrayList<StationItem> upline = routeInfo.getUpline().getStationList();
                ArrayList<StationItem> downline = routeInfo.getDownline().getStationList();

                for (int i = 0; i < upline.size(); i++) {
                    Log.d(TAG, "up " + upline.get(i).getStationNum());
                    if (upline.get(i).getStationNum() == tmpDevPos) {
                        for (int j = 0; j < downline.size(); j++) {
                            Log.d(TAG, "down " + downline.get(j).getStationNum());
                            if (downline.get(j).getStationNum() == tmpDevPos) {
                                findDevPos = true;
                                break;
                            }
                        }
                        break;
                    }
                }
                if (!findDevPos) {
                    Log.d(TAG, "没有找到该站点: " + tmpDevPos);
                } else {
                    if (!BrtInfoUtils.getCurRouteInfo().equals(routeInfo)) {
                        Log.d(TAG, "Receive new RouteInfo!");
                        BrtInfoUtils.saveRouteInfo(routeInfo);
                        //通知系统路线信息已经更改
                        Message msg = Message.obtain();
                        msg.what = MsgUpdateRouteInfo;
                        msg.obj = routeInfo;
                        handler.sendMessage(msg);
                    } else {
                        Log.d(TAG, "Receive RouteInfo == local RouteInfo!");
                    }
                }
                //成功获取路线信息后停止定时任务
                reqRouteInfoFlag = true;
                if (reqRouteInfoTimer != null) {
                    reqRouteInfoTimer.cancel();
                    reqRouteInfoTimer = null;
                }
            }
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgUpdateBRTInfo://MQTT发来的更信息
//                    Log.d(TAG, "Receive msg: MsgUpdateBRTInfo");
//                    if (isFragmentAdded) {
//                        ArrayList<String> brtInfo = (ArrayList<String>) msg.obj;
//                        if (brtInfo.size() == 4) {
//                            updateBRTInfo(brtInfo);
//                        } else {
//                            Log.e(TAG, "Invalid BRT Info!");
//                        }
//                    }
                    Log.i(TAG, "updateBRTInfo" + (String) msg.obj);
                    updateBRTInfo((String) msg.obj);
                    break;

                case MsgInitRouteInfo://初始化，主要是fragment的添加和初始化（开机）
                    Log.d(TAG, "Receive msg: MsgInitRouteInfo");
//                    InitRouteInfo2();//担心改错了，故而复制一下
                    InitRouteInfo();
                    break;

                case MsgUpdateRouteInfo:
                    Log.d(TAG, "Receive msg: MsgUpdateRouteInfo");
                    updateRouteInfo();
                    break;

                case MsgUpdateStation:
                    Log.d(TAG, "Receive msg: MsgUpdateStation");
                    updateStationInfo((StationInfo) msg.obj);
                    break;

                case MsgInitStation://初始化站点信息（开机）
                    Log.d(TAG, "Receive msg: MsgInitStation");
                    stationInfoInit((StationInfo) msg.obj);
                    break;

                case MsgInitDevInfo://初始化设备信息（开机）
                    Log.d(TAG, "Receive msg: MsgInitDevInfo");
                    deviceInfo = (DeviceInfo) msg.obj;
                    routeOfDevice = DeviceInfoUtils.getDeviceRouteFromIdentify(deviceInfo.getIdentify());
                    //将配置文件中的线路存储
                    SpUtils.putString(CodeConstants.ROUTE_ID, routeOfDevice);
                    //当前该显示屏所在的站点位置
                    stationID = DeviceInfoUtils.getStationIDFromIdentify(deviceInfo.getIdentify());
                    Log.d(TAG, "站点ID：stationID: " + stationID + "/当前线路：routeOfDevice：" + routeOfDevice);
                    //当前该显示屏的显示样式
                    themeStyle = deviceInfo.getThemeStyle();
                    Log.d(TAG, "themeStyle: " + themeStyle);

                    //获取设备位置
                    DevicePosition = DeviceInfoUtils.getDevicePositionFromIdentify(deviceInfo.getIdentify());
                    Log.d(TAG, "Device Position: " + DevicePosition);
                    //获取设备位置所在的线路方向
                    DeviceDirection = DeviceInfoUtils.getDeviceDirectionFromIdentify(deviceInfo.getIdentify());
                    Log.d(TAG, "Device Direction: " + (DeviceDirection == 1 ? "upline" : "downline"));

                    //设备信息初始化成功，开始校准时间、请求天气、路线信息等
                    //获取天气信息
                    reqWeatherTask = new TimerTask() {
                        @Override
                        public void run() {
                            httpRequest.requestWeatherInfo(cityCode);
                        }
                    };
                    //5分钟获取一次
                    reqWeatherTimer = new Timer();
                    reqWeatherTimer.schedule(reqWeatherTask, 0, 5 * 60 * 1000);

                    //请求获取网络时间
                    reqTimeTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (!reqTimeFlag) {
                                httpRequest.requestTimeInfo();
                            }
                        }
                    };
                    //不成功的话隔一分钟再请求
                    reqTimeTimer = new Timer();
                    reqTimeTimer.schedule(reqTimeTask, 0, 60 * 1000);

                    //请求获取路线信息
                    reqRouteInfoTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (!reqRouteInfoFlag) {
                                httpRequest.requestRouteInfo("http://" + HisenseServerIP + ":" + HisenseServerPort, "80002", secretKey);
                            }
                        }
                    };
                    //不成功的话隔一分钟再请求
                    reqRouteInfoTimer = new Timer();
                    reqRouteInfoTimer.schedule(reqRouteInfoTask, 0, 60 * 1000);

                    break;

                case MsgUpdateNetworkInfo:
                    Log.d(TAG, "Receive MsgUpdateNetworkInfo");
                    Bundle data = msg.getData();

                    boolean isShow = data.getBoolean("isShow");
                    String info = data.getString("info");
                    if (isShow) {
                        networkInfo.setVisibility(View.VISIBLE);
                        networkInfo.setText(info);
                    } else {
                        networkInfo.setVisibility(View.INVISIBLE);
                    }
                    break;

                case ProtocolMessageProcess.MsgWhatDebugMode:
                    //调试模式请求
                    boolean debugOn = false;
                    debugOn = msg.getData().getBoolean("debug_mode");
                    Log.d(TAG, "DebugMode " + (debugOn ? "On" : "Off"));
                    if (textDebugInfo != null) {
                        if (debugOn) {
                            textDebugInfo.setText(SystemInfoUtils.getNetworkInterfaceIp(null));
                            textDebugInfo.setVisibility(View.VISIBLE);
                            //调整到最前面
                            textDebugInfo.bringToFront();
                        } else {
                            textDebugInfo.setVisibility(View.INVISIBLE);
                        }
                    }
                    break;
                case ProtocolMessageProcess.MsgWhatMediaOpt:
                    Log.d(TAG, "Request Media operation");
                    NetMgrMsg.HMediaSetting hMediaSetting = null;
                    //媒体文件相关操作
                    switch (msg.arg1) {
                        case ProtocolMessageProcess.MsgArg1ReloadMedia:
                            //子线程已完成媒体操作，现在需要扫描新的媒体文件和播放参数，重新进行显示
                            Log.d(TAG, "Reload media");
                            if (null != videoController) {
                                videoController.refreshVideoList();
                            }
                            if (null != buttomFragment) {
                                buttomFragment.updateDisplayContent();
                            }
                            if (mViceDisplay != null) {
                                mViceDisplay.updateTips();
                                mViceDisplay.refreshVideoList();
                            }
                            break;
                        case ProtocolMessageProcess.MsgArg1MediaRemoveByName:
                            /*
                             *  马上需要进行媒体文件删除操作，现在请先做相应的预处理，
                             *  比如，暂停正在播放的视屏，关闭打开的文件
                             */
                            Log.d(TAG, "Remove media by name preprocess");
                            hMediaSetting = (NetMgrMsg.HMediaSetting) msg.obj;
                            if (null != hMediaSetting) {
                                boolean hasImage = false;
                                boolean hasVideo = false;
                                for (NetMgrMsg.MediaResouce tmp : hMediaSetting.getMediaList()) {
                                    if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.VIDEO) {
                                        hasVideo = true;
                                    } else if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.PIC) {
                                        hasImage = true;
                                    }
                                }
                                if (hasImage) {
                                    //videopost程序不含有需要更新的图片
                                    //picOpt.stopPicDisp(true);
                                }
                                if (hasVideo) {
                                    if (videoController != null) {
                                        videoController.stopVideo(true);
                                    }
                                    if (mViceDisplay != null) {
                                        mViceDisplay.stopVideo(true);
                                    }
                                }
                            }
                            synchronized (SystemInfoUtils.getMediaOptObject()) {
                                //通知子线程可以进行后续的操作了
                                SystemInfoUtils.getMediaOptObject().notify();
                            }

                            break;
                        case ProtocolMessageProcess.MsgArg1MediaRemoveByType:
                            /*
                             *  马上需要进行媒体文件删除操作，现在请先做相应的预处理，
                             *  比如，暂停正在播放的视屏，关闭打开的文件
                             */
                            Log.d(TAG, "Remove media by type preprocess");
                            hMediaSetting = (NetMgrMsg.HMediaSetting) msg.obj;
                            if (null != hMediaSetting) {
                                switch (hMediaSetting.getMedia(0).getMediaType()) {
                                    case TEXT:
                                        //删除文本内容不影响显示，这里不做操作
                                        break;
                                    case PIC:
                                        //videopost程序不含有需要更新的图片
                                        //Log.d(TAG, "gona to delete all local image file!");
                                        //picOpt.stopPicDisp(true);
                                        break;
                                    case VIDEO:
                                        Log.d(TAG, "gona to delete all local video file!");
                                        if (videoController != null) {
                                            videoController.stopVideo(true);
                                        }
                                        if (mViceDisplay != null) {
                                            mViceDisplay.stopVideo(true);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            synchronized (SystemInfoUtils.getMediaOptObject()) {
                                //通知子线程可以进行后续的操作了
                                SystemInfoUtils.getMediaOptObject().notify();
                            }
                            break;
                        case ProtocolMessageProcess.MsgArg1MediaDownload:
                            /*
                             *  马上需要进行媒体文件删除操作，现在请先做相应的预处理，
                             *  比如，暂停正在播放的视屏，关闭打开的文件
                             */
                            Log.d(TAG, "Download media preprocess");
                            hMediaSetting = (NetMgrMsg.HMediaSetting) msg.obj;
                            if (null != hMediaSetting) {
                                boolean hasImage = false;
                                boolean hasVideo = false;
                                for (NetMgrMsg.MediaResouce tmp : hMediaSetting.getMediaList()) {
                                    if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.VIDEO) {
                                        hasVideo = true;
                                    } else if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.PIC) {
                                        hasImage = true;
                                    }
                                }
                                if (hasImage) {
                                    //picOpt.stopPicDisp(true);
                                }
                                if (hasVideo) {
                                    if (videoController != null) {
                                        videoController.stopVideo(true);
                                    }
                                    if (mViceDisplay != null) {
                                        mViceDisplay.stopVideo(true);
                                    }
                                }
                            }
                            synchronized (SystemInfoUtils.getMediaOptObject()) {
                                //通知子线程可以进行后续的操作了
                                SystemInfoUtils.getMediaOptObject().notify();
                            }
                            break;
                        case ProtocolMessageProcess.MsgArg1DispSetting:
                            /*
                             *   即将修改相应的媒体播放参数，先做相应的预处理
                             */
                            Log.d(TAG, "DispSetting preprocess");
//                            synchronized (SystemInfoUtils.getMediaOptObject()) {
//                                //通知子线程可以进行后续的操作了
//                                SystemInfoUtils.getMediaOptObject().notify();
//                            }
                            if (null != buttomFragment) {
                                buttomFragment.updateDisplayParameters();
                            }
                            if (null != mViceDisplay) {
                                mViceDisplay.updateTips();
                            }
                            break;
                    }

                    break;
                default:
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TestTag, "onCreate");
        super.onCreate(savedInstanceState);

        //不在需要设置这些window属性
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //hideVirtualKey();
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //启动监护Service，该服务在程序崩溃退出后将apk重启
        if (!isServiceRunning(ApplicationMonitorService.class.getName())) {
            intent = new Intent(this, ApplicationMonitorService.class);
            startService(intent);
        }

        //测试是否解决界面卡顿问题
        getWindow().setBackgroundDrawable(null);


        final FragmentManager fm = getSupportFragmentManager();
        topFragment = (TopFragment) fm.findFragmentById(R.id.top_fragment);
        buttomFragment = (ButtomFragment) fm.findFragmentById(R.id.buttom_fragment);
        httpRequest = new HttpRequest(callBack);

        //调试信息文本初始化
        textDebugInfo = findViewById(R.id.textDebugInfo);
        textDebugInfo.setVisibility(View.INVISIBLE);

        //网络状态显示初始化
        networkInfo = findViewById(R.id.ethernetInfo);
        networkInfo.setVisibility(View.INVISIBLE);

        //开始播放视频
        //videoView = (VideoView) findViewById(R.id.videoview);
//        Log.d("video_debug", "video getWidth: " + videoView.getWidth());
//        Log.d("video_debug", "video getHeight: " + videoView.getHeight());
//        Log.d("video_debug", "video getMeasuredWidth: " + videoView.getMeasuredWidth());
//        Log.d("video_debug", "video getMeasuredHeight: " + videoView.getMeasuredHeight());


        mRelativeLayout = findViewById(R.id.re_mvideo);
        mVideoSurface = new SurfaceView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(950, 710);
        layoutParams.setMargins(0, 0, 0, 0);
        mVideoSurface.setLayoutParams(layoutParams);
        mVideoSurface.layout(0, 0, 950, 710);
        mRelativeLayout.addView(mVideoSurface);


        try {
            videoController = new VideoController(this, mVideoSurface,
                    "android.resource://" + getActivity().getPackageName() + "/" + R.raw.nanning,
                    Environment.getExternalStorageDirectory() + "/media/video");
            videoController.startVideo();
        } catch (Exception e) {
            Log.d(TAG, "play video error!", e);
            //e.printStackTrace();
        }


        if (!isSystemInitThread) {
            isSystemInitThread = true;
            Log.d(TestTag, "Start a new SystemInitThread!");
            //启动初始化线程 ，初始化设备信息
            new Thread(new SystemInitThread(this)).start();
        }

        Log.d(TAG, "gona to start MQTT service!");
        //启动MQTT Service
        if (!isServiceRunning(MsgService.class.getName())) {
            bindIntent = new Intent(this, MsgService.class);
            bindService(bindIntent, conn, BIND_AUTO_CREATE);
            Log.d(TAG, "bind MQTT service");
        }

        //开机从网络获取闪电信息
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                checkStationData();
            }
        }).start();

    }

    private void checkStationData() {
        Log.i(TAG, "开机：检查更新路线");
        String url = "http://172.16.30.254:8082/message/searchBasicInfo";
        OkGo.<String>get(url).execute(new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                String responseStr = response.body();
                Log.i(TAG, response.code() + responseStr);
                if (response.code() == 200) {
                    Log.i(TAG, "searchBasicInfo请求成功");
                    basicInfoStr = responseStr;
                    SearchBasicInfo searchBasicInfo = JSON.parseObject(basicInfoStr, SearchBasicInfo.class);
                    Log.i(TAG, "searchBasicInfo:" + searchBasicInfo.toString());
                    initStationData(searchBasicInfo, basicInfoStr);
                }
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                Log.i(TAG, "searchBasicInfo请求失败");
                basicInfoStr = SpUtils.getString(CodeConstants.SEARCH_BASIC_INFO);
                if (!basicInfoStr.equals("")) {
                    SearchBasicInfo searchBasicInfo = JSON.parseObject(basicInfoStr, SearchBasicInfo.class);
                    Log.i(TAG, "searchBasicInfo:" + searchBasicInfo.toString());
                    initStationData(searchBasicInfo, basicInfoStr);
                }
            }
        });

    }

    /**
     * 开机获取，或者从数据库中拿到公交站信息后，解析
     * 注意：此时配置文件中的线路编号如果发生改变，此时就和本地SP存储的数据
     * 就不对应了，这时候，返回 null
     */
    private List<Stations> initStation(SearchBasicInfo searchBasicInfo, String basicInfoStr) {
        int curDevPos = DeviceInfoUtils.getDevicePositionFromIdentify(DeviceInfoUtils.getDeviceInfoFromFile().getIdentify());
        Log.i(TAG, "searchBasicInfo：开始解析数据");
        Log.i(TAG, "当前站ID：" + curDevPos);
        routeIdFromSp = SpUtils.getString(CodeConstants.ROUTE_ID);
        stationInfoList = searchBasicInfo.getStationInfo();
        for (int i = 0; i < stationInfoList.size(); i++) {
            if (stationInfoList.get(i).getDirection().equals("up")) {
                stationUpList = stationInfoList.get(i).getStations();//上行站点集合
                for (int j = 0; j < stationUpList.size(); j++) {
                    if (curDevPos == Integer.parseInt(stationUpList.get(j).getId())) {
                        List<Stations> stationList = stationInfoList.get(i).getStations();
                        position_stationInfo = i;
                        position_station = j;
                        direction = stationInfoList.get(i).getDirection();
                        routeId = stationInfoList.get(i).getRouteId();
                        Log.i(TAG, "position_station:" + position_station);
                        Log.i(TAG, "线路：" + routeId + "/方向：" + direction + "/当前站id:" + curDevPos);
                        if (routeIdFromSp.equals(routeId)) {//如果配置文件中(和SP中一致)的线路编号和收到的线路一致，储存更新数据库
                            SpUtils.putString(CodeConstants.SEARCH_BASIC_INFO, basicInfoStr);
                            SpUtils.putString(CodeConstants.ROUTE_ID, routeId);
                            findCurStation = true;
                            return stationList;
                        } else {
                            findCurStation = false;
                            return null;
                        }
                    }
                }

            } else if (stationInfoList.get(i).getDirection().equals("down")) {
                stationDownList = stationInfoList.get(i).getStations();//下行站点集合
                for (int k = 0; k < stationDownList.size(); k++) {
                    if (curDevPos == Integer.parseInt(stationDownList.get(k).getId())) {
                        List<Stations> stationList = stationInfoList.get(i).getStations();
                        position_stationInfo = i;
                        position_station = k;
                        direction = stationInfoList.get(i).getDirection();
                        routeId = stationInfoList.get(i).getRouteId();
                        Log.i(TAG, "position_station:" + position_station);
                        Log.i(TAG, "线路：" + routeId + "/方向：" + direction + "/当前站id:" + curDevPos);

                        if (routeOfDevice.equals(routeId)) {//如果配置文件中(和SP中一致)的线路编号和收到的线路一致，储存更新数据库
                            SpUtils.putString(CodeConstants.SEARCH_BASIC_INFO, basicInfoStr);
                            SpUtils.putString(CodeConstants.ROUTE_ID, routeId);
                            findCurStation = true;
                            return stationList;
                        } else {
                            findCurStation = false;
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 开机获取公交站信息后，更新页面
     */
    private void initStationData(SearchBasicInfo searchBasicInfo, String basicInfoStr) {
        List<Stations> stationList = initStation(searchBasicInfo, basicInfoStr);
        if (findCurStation && stationList != null && stationList.size() > 0) {
            int count = stationList.size();
            if (!isFragmentAdded) {//如果fragment没有初始化，那么就初始化
                addFragment(stationList);
            }
            Log.i(TAG, "searchBasicInfo：" + "开始更新页面");
            topFragment.setCurStationName(stationList.get(position_station).getName());
            topFragment.setCurStationEName(stationList.get(position_station).getNameEn());
            topFragment.setCurLine(SpUtils.getString(CodeConstants.ROUTE_ID));
            if (position_station == 0) {//起始站点
                nextStation = stationList.get(position_station + 1).getName();
                nextStationEn = stationList.get(position_station + 1).getNameEn();
                direStation = stationList.get(count - 1).getName();
                direStationEn = stationList.get(count - 1).getNameEn();
                qLeftFragment.setDirectionAndNestStation(direStation, direStationEn, nextStation, nextStationEn);
            } else if (position_station < (count - 1)) {//中间站点
                nextStation = stationList.get(position_station + 1).getName();
                nextStationEn = stationList.get(position_station + 1).getNameEn();
                direStation = stationList.get(count - 1).getName();
                direStationEn = stationList.get(count - 1).getNameEn();
                Log.i(TAG, "direStation：" +direStation+ "/direStationEn:"+direStationEn+"/nextStation:"+nextStation+"/nextStationEn:"+nextStationEn);
                leftFragment.setDirectionAndNestStation(direStation, direStationEn, nextStation, nextStationEn);
            } else if (position_station == (count - 1)) {//终点站点
//            nextStation = stationList.get(position_station - 1).getName();
//            nextStationEn = stationList.get(position_station - 1).getNameEn();
//            direStation = stationList.get(0).getName();
//            direStationEn = stationList.get(0).getNameEn();
            }
        } else {
            Log.i(TAG, "线路信息发生变化，或没有找到当前站：");
        }
    }


    private void hideVirtualKey() {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        window.setAttributes(params);
    }

    private void updateRouteInfo() {
        //这边最快速的方法是重启系统或者重启apk
        Log.d(TAG, "gona to updateRouteInfo(reboot system)");
//        SystemInfoUtils.rebootDevice(getActivity());
        //TODO 更新路线
    }

    /**
     * 开机初始化
     * 注意：此时配置文件中的线路编号如果发生改变，此时就和本地SP存储的数据
     * 就不对应了，这时候，只根据站点信息加载对应的fragment，而不显示数据
     */
    private void InitRouteInfo() {
        Log.d(TAG, "启动app，开始初始化");
        String basicInfoStr = SpUtils.getString(CodeConstants.SEARCH_BASIC_INFO);
        if (basicInfoStr.equals("")) {
            //第一次运行app时候，SP里面数据为空，但若此时网络网络获取数据成功或，则会加载相应数据，
            // 可是，此处若没有初始化Fragment，会报错
            //但由于不知道设备站点ID是初始、中间、还是终点站，故而无法初始化相应的fragment。
            // 坑啊.故而网络获取数据的时候，判断一下，在那里面初始化。
            isFragmentAdded = false;
            return;
        } else {
            SearchBasicInfo searchBasicInfo = JSON.parseObject(basicInfoStr, SearchBasicInfo.class);
            //当配置文件中的线路发生变化的时候，一下方法返回NULL
            List<Stations> stationList = initStation(searchBasicInfo, basicInfoStr);
            if (stationList == null) {//线路信息发生改变，只加载fragment
                Log.d(TAG, "线路信息发生改变，只加载fragment");
                isFragmentAdded = false;
                return;
            } else {
                if (!findCurStation) {
                    Log.e(TAG, "没有找到当前站点信息");
                    return;
                }
                addFragment(stationList);
            }
        }
    }

    /**
     * 添加fragment并且初始化
     *
     * @param stationList
     */
    private void addFragment(List<Stations> stationList) {
        int count = stationList.size();
        Log.i(TAG, "searchBasicInfo：" + "开始初始化fragment");
        if (position_station == 0) {
            //起始站
            posFlag = 0x01;
        } else if (position_station == (count - 1)) {
            //终点站
            posFlag = 0x02;
        } else {
            //中间站点
            posFlag = 0x03;
        }

        //初始化TopFragment的显示内容
        if (topFragment != null) {
            topFragment.setCurStationName(stationList.get(position_station).getName());
            topFragment.setCurStationEName(stationList.get(position_station).getNameEn());
            topFragment.setCurLine(routeOfDevice);
        }
        if (posFlag == 0x01) {
            //显示起点fragment
            qLeftFragment = QLeftFragment.newInstance(stationList.get(1).getName(), stationList.get(1).getNameEn(),stationList.get(count-1).getName(),stationList.get(count-1).getNameEn());
            replaceleftFragment(qLeftFragment);

            QRightFragment qRightFragment = QRightFragment.newInstance(stationList.get(1).getName(), stationList.get(1).getNameEn());
            replacerightFragment(qRightFragment);

        } else if (posFlag == 0x02) {
            //显示终点fragment
            zLeftFragment = new ZLeftFragment();
            zLeftFragment.fragmentInit2(stationList);
            replaceleftFragment(zLeftFragment);
            zRightFragment = new ZRightFragment();
//                zRightFragment.fragmentInit(isDownline ? downline : upline);
            replacerightFragment(zRightFragment);

        } else {
            //中间站点需要额外判断显示样式
            themeStyle = 1;
            Log.d("debug_test", "themeStyle: 1");
            //样式1：站台在中间，上下行在两边。且面向主屏时左边是下行，右边是上行。
            leftFragment = new LeftFragment();
            leftFragment.fragmentInit(stationList, position_station);
            rightFragment = new RightFragment();
//                rightFragment.fragmentInit(themeStyle, upline, curIndexUpline);
            replaceleftFragment(leftFragment);
        }
        //界面初始化完毕
        isFragmentAdded = true;
    }

    /*private void InitRouteInfo2() {

        RouteInfo routeInfo = BrtInfoUtils.getCurRouteInfo();

        if (DevicePosition < 1 || DevicePosition > 18 || DevicePosition == 5) {
            //一站
            Log.d(TAG, "Device Position uninitialized, set default value: 1");
            DevicePosition = 1;
        }
        if (DeviceDirection != 1 && DeviceDirection != 2) {
            //上行
            Log.d(TAG, "Device Direction uninitialized, set default value: 1(upline)");
            DeviceDirection = 2;
        }

        ArrayList<StationItem> upline = routeInfo.getUpline().getStationList();
        ArrayList<StationItem> downline = routeInfo.getDownline().getStationList();

        int uplineCount = upline.size();
        int downlineCount = downline.size();

        if (uplineCount != downlineCount) {
            //如果上下行线路的站点数目不一致，表明有错误
            Log.e(TAG, "Downline Station Count not equal to Upline Station Count!");
            return;
        }

        //能否在路线信息中找到设备坐在位置的站点
        boolean findCurStation = false;

        //当前设备所在站点在上行线路中的位置
        int curIndexUpline = -1;
        //当前设备所在站点在下行线路中的位置
        int curIndexDownline = -1;
        //检查上下行线路中有无当前站点
        for (int i = 0; i < upline.size(); i++) {
            StationItem tmpUpline = upline.get(i);
            if (tmpUpline.getStationNum() == DevicePosition) {
                curIndexUpline = i;
                for (int j = 0; j < downline.size(); j++) {
                    StationItem tmpDownline = downline.get(j);
                    if (tmpDownline.getStationNum() == DevicePosition) {
                        findCurStation = true;
                        curIndexDownline = j;
                        break;
                    }
                }
                break;
            }
        }
        if (!findCurStation) {
            Log.e(TAG, "Can't find current Station from RouteInfo!");
            return;
        }

        *//*  由于实际站点序号和downline或者upline中的双程号并不是一一对应的，
     *  所以先生成一个映射表，方便后面根据双程号查找站点信息
     *  *//*
        SparseIntArray ds2downlineIndex = new SparseIntArray();
        SparseIntArray ds2uplineIndex = new SparseIntArray();
        for (int i = 0; i < downlineCount; i++) {
            ds2downlineIndex.put(downline.get(i).getDualSerial(), i);
        }
        for (int i = 0; i < uplineCount; i++) {
            ds2uplineIndex.put(upline.get(i).getDualSerial(), i);
        }

        if (DeviceDirection == 1) {
            //设备对应上行线路
            isDownline = false;
            if (curIndexUpline == 0) {
                //起始站
                posFlag = 0x01;
            } else if (curIndexUpline == uplineCount - 1) {
                //终点站
                posFlag = 0x02;
            } else {
                //中间站点
                posFlag = 0x03;
            }
        } else {
            //设备对应下行线路
            isDownline = true;
            if (curIndexDownline == 0) {
                //起始站
                posFlag = 0x01;
            } else if (curIndexDownline == downlineCount - 1) {
                //终点站
                posFlag = 0x02;
            } else {
                //中间站点
                posFlag = 0x03;
            }
        }
        //初始化TopFragment的显示内容
        if (topFragment != null) {
            if (isDownline) {
//                topFragment.setCurStationName(downline.get(curIndexDownline).getStationName());
//                topFragment.setCurStationEName(downline.get(curIndexDownline).getStationEName());
            } else {
//                topFragment.setCurStationName(upline.get(curIndexUpline).getStationName());
//                topFragment.setCurStationEName(upline.get(curIndexUpline).getStationEName());
            }
        }
        if (posFlag == 0x01) {
            //显示起点fragment
            StationItem tmp = isDownline ? downline.get(1) : upline.get(1);
            qLeftFragment = QLeftFragment.newInstance(tmp.getStationName(), tmp.getStationEName());
            replaceleftFragment(qLeftFragment);

            QRightFragment qRightFragment = QRightFragment.newInstance(tmp.getStationName(), tmp.getStationEName());
            replacerightFragment(qRightFragment);

        } else if (posFlag == 0x02) {
            //显示终点fragment
            zLeftFragment = new ZLeftFragment();
            zLeftFragment.fragmentInit(isDownline ? downline : upline);
            replaceleftFragment(zLeftFragment);
            zRightFragment = new ZRightFragment();
            zRightFragment.fragmentInit(isDownline ? downline : upline);
            replacerightFragment(zRightFragment);

        } else {
            //中间站点需要额外判断显示样式
            switch (themeStyle) {
                case 1:
                    Log.d("debug_test", "themeStyle: 1");
                    //样式1：站台在中间，上下行在两边。且面向主屏时左边是下行，右边是上行。
                    leftFragment = new LeftFragment();
                    leftFragment.fragmentInit(themeStyle, downline, curIndexDownline);
                    rightFragment = new RightFragment();
                    rightFragment.fragmentInit(themeStyle, upline, curIndexUpline);
                    replaceleftFragment(leftFragment);
                    replacerightFragment(rightFragment);
                    break;
                case 2:
                    Log.d("debug_test", "themeStyle: 2");
                    //样式2：站台在中间，上下行在两边。且面向主屏时左边是上行，右边是下行。
                    leftFragment = new LeftFragment();
                    leftFragment.fragmentInit(themeStyle, upline, curIndexUpline);
                    rightFragment = new RightFragment();
                    rightFragment.fragmentInit(themeStyle, downline, curIndexDownline);
                    replaceleftFragment(leftFragment);
                    replacerightFragment(rightFragment);
                    break;
                case 3:
                    Log.d("debug_test", "themeStyle: 3");
                    *//*  样式3：中间是上/下行线路，两边是站台。且面向主屏时线路在右侧，具体屏幕是处在上行边上的站台还是
     *  下行边上的站台由stationID来决定 *//*
                case 4:
                    Log.d("debug_test", "themeStyle: 4");
                    *//*  样式4：中间是上/下行线路，两边是站台。且面向主屏时线路在左侧，具体屏幕是处在上行边上的站台还是
     *  下行边上的站台由stationID来决定 *//*

                    //左边fragment和右边fragment相同
                    leftFragment = new LeftFragment();
                    leftFragment.fragmentInit(themeStyle, isDownline ? downline : upline,
                            isDownline ? curIndexDownline : curIndexUpline);
                    rightFragment = new RightFragment();
                    rightFragment.fragmentInit(themeStyle, isDownline ? downline : upline,
                            isDownline ? curIndexDownline : curIndexUpline);
                    replaceleftFragment(leftFragment);
                    replacerightFragment(rightFragment);
                    break;

                default:
                    Log.e(TAG, "Unknown themeStyle!");
                    break;
            }

        }

//        Log.d(TAG, "new ViceDisplay!");
//        mViceDisplay = new ViceDisplay(MainActivity.this);
//        Log.d(TAG, "new ViceDisplay finished!");
//        mViceDisplay.ViceDisplayInit(posFlag, themeStyle, isDownline, routeInfo, curIndexDownline, curIndexUpline);
//        Log.d(TAG, "ViceDisplay Init finished!");
//        mViceDisplay.Show();
//        Log.d(TAG, "ViceDisplay show!");

        //界面初始化完毕
        isFragmentAdded = true;

    }*/

    //当通过http获取到新的站点信息后需要更新系统使用的站点信息
    private void updateStationInfo(StationInfo stations) {

        //重新初始化站点信息
        stationInfoInit(stations);
    }

    //初始化站点信息
    private void stationInfoInit(StationInfo stations) {
        //TODO 初始化站点信息
        this.stationInfo = stations;


//
//        /*
//         *  stationID指的是站牌所在位置双程号，对于站牌在中间，上下行线路在两边的情况，上下行的双程号效果是一样的
//         *  对于上下行线路在中间，两边分别有上行/下行的站牌的情况，会根据这个stationID来确认是上行还是下行
//         *
//         */
//        if (stationID <= 0) {
//            //如果未初始化，默认是起始站点
//            Log.d(TAG, "stationID uninitialized, set default value: 1");
//            stationID = 1;
//        }
//        if (themeStyle <= 0) {
//            //
//            Log.d(TAG, "themeStyle uninitialized, set default value: 1");
//            themeStyle = 1;
//        }
//
//        //上下行数据都是根据双程号升序排列的
//        ArrayList<StationInfoItem> downline = stationInfo.downline;
//        ArrayList<StationInfoItem> upline = stationInfo.upline;
//        //下行线路站点总数
//        int downlineCount = downline.size();
//        //上行线路站点总数，正常应该和下行的数目一样
//        int uplineCount = upline.size();
//
//        //当前站点在下行线路的信息
//        StationInfoItem curStaDownline = null;
//        //当前站点在上行线路的信息
//        StationInfoItem curStaUpline = null;
//
//        /*  由于实际站点序号和downline或者upline中的双程号并不是一一对应的，
//         *  所以先生成一个映射表，方便后面根据双程号查找站点信息
//         *  */
//        HashMap<Integer, Integer> dS2DownlineListIndex = new HashMap<>();
//        HashMap<Integer, Integer> dS2UplineListIndex = new HashMap<>();
//        for (int i = 0; i < downlineCount; i++) {
//            dS2DownlineListIndex.put(downline.get(i).dualSerial, i);
//        }
//        for (int i = 0; i < uplineCount; i++) {
//            dS2UplineListIndex.put(upline.get(i).dualSerial, i);
//        }
//
//        //查找当前站点在上下行线路中对应的信息
//        if (dS2DownlineListIndex.containsKey(stationID)) {
//            //如果stationID是下行线路的双程号
//            isDownline = true;
//            curStaDownline = downline.get(dS2DownlineListIndex.get(stationID));
//            Log.d("debug_test", "stationID:" + stationID);
//            Log.d("debug_test", "downlineCount:" + downlineCount);
//            Log.d("debug_test", "index:" + dS2DownlineListIndex.get(stationID));
//            if (dS2DownlineListIndex.get(stationID) == 0) {
//                //如果是下行线路的起点站
//                Log.d("debug_test", "start");
//                posFlag = 0x01;
//            } else if (dS2DownlineListIndex.get(stationID) == downlineCount - 1) {
//                //如果是下行线路的终点站
//                Log.d("debug_test", "end");
//                posFlag = 0x02;
//            } else {
//                //如果是中间站点
//                Log.d("debug_test", "mid");
//                posFlag = 0x03;
//            }
//
//        } else if (dS2UplineListIndex.containsKey(stationID)) {
//            //如果stationID是上行线路的双程号
//            isDownline = false;
//            curStaUpline = upline.get(dS2UplineListIndex.get(stationID));
//            if (dS2UplineListIndex.get(stationID) == 0) {
//                //如果是上行线路的起始站点
//                posFlag = 0x01;
//            } else if (dS2UplineListIndex.get(stationID) == uplineCount - 1) {
//                //如果是上行线路的终点站
//                posFlag = 0x02;
//            } else {
//                //如果是中间站点
//                posFlag = 0x03;
//            }
//
//        } else {
//            Log.e(TAG, "Can't find StationID[" + stationID + "] 's StationInfo!");
//            //如果并不属于任意方向的双程号，则出现异常，这时手动赋值，认为是下行线路起始站点
//            isDownline = true;
//            posFlag = 0x01;
//            curStaDownline = downline.get(0);
//            stationID = downline.get(0).dualSerial;
//        }
//        //获取当前站点在上行/下行ArrayList中的标号
//        int downlineIndex = -1;
//        int uplineIndex = -1;
//        if (curStaDownline != null) {
//            downlineIndex = dS2DownlineListIndex.get(stationID);
//            for (int i = 0; i < uplineCount; i++) {
//                if (upline.get(i).name.equals(downline.get(downlineIndex).name)) {
//                    uplineIndex = i;
//                    break;
//                }
//            }
//        } else {
//            if (curStaUpline != null) {
//                uplineIndex = dS2UplineListIndex.get(stationID);
//                for (int i = 0; i < downlineCount; i++) {
//                    if (downline.get(i).name.equals(upline.get(uplineIndex).name)) {
//                        downlineIndex = i;
//                        break;
//                    }
//                }
//            }
//        }
//        if (downlineIndex < 0 || uplineIndex < 0) {
//            Log.e(TAG, "");
//        }
//        //初始化TopFragment的显示内容
//        if (topFragment != null) {
//            if (isDownline) {
//                Log.d("debug_test", "downlineIndex:" + downlineIndex + " name:" +
//                        downline.get(downlineIndex).name + " ename:" + downline.get(downlineIndex).ename);
//                topFragment.setCurStationName(downline.get(downlineIndex).name);
//                topFragment.setCurStationEName(downline.get(downlineIndex).ename);
//            } else {
//                topFragment.setCurStationName(upline.get(uplineIndex).name);
//                topFragment.setCurStationEName(upline.get(uplineIndex).ename);
//            }
//        }
//        if (posFlag == 0x01) {
//            //显示起点fragment
//            StationInfoItem tmp = isDownline ? downline.get(1) : upline.get(1);
//            QLeftFragment qLeftFragment = QLeftFragment.newInstance(tmp.name, tmp.ename);
//            replaceleftFragment(qLeftFragment);
////            QLeftFragment qLeftFragment = new QLeftFragment();
////            replaceleftFragment(qLeftFragment);
////            qLeftFragment.setStaName(isDownline ? downline.get(0).name : upline.get(0).name);
////            qLeftFragment.setStaEName(isDownline ? downline.get(0).ename : upline.get(0).ename);
//
//            QRightFragment qRightFragment = QRightFragment.newInstance(tmp.name, tmp.ename);
//            replacerightFragment(qRightFragment);
////            QRightFragment qRightFragment = new QRightFragment();
////            replacerightFragment(qRightFragment);
////            qRightFragment.setStaName(isDownline ? downline.get(0).name : upline.get(0).name);
////            qRightFragment.setStaEName(isDownline ? downline.get(0).ename : upline.get(0).ename);
//
//        } else if (posFlag == 0x02) {
//            //显示终点fragment
//            zLeftFragment = new ZLeftFragment();
//            zLeftFragment.fragmentInit(isDownline ? downline : upline);
//            replaceleftFragment(zLeftFragment);
//            zRightFragment = new ZRightFragment();
//            zRightFragment.fragmentInit(isDownline ? downline : upline);
//            replacerightFragment(zRightFragment);
//
//        } else {
//            //中间站点需要额外判断显示样式
//            switch (themeStyle) {
//                case 1:
//                    Log.d("debug_test", "themeStyle: 1");
//                    //样式1：站台在中间，上下行在两边。且面向主屏时左边是下行，右边是上行。
//                    leftFragment = new LeftFragment();
//                    leftFragment.fragmentInit(themeStyle, downline, downlineIndex);
//                    rightFragment = new RightFragment();
//                    rightFragment.fragmentInit(themeStyle, upline, uplineIndex);
//                    replaceleftFragment(leftFragment);
//                    replacerightFragment(rightFragment);
//                    break;
//                case 2:
//                    Log.d("debug_test", "themeStyle: 2");
//                    //样式2：站台在中间，上下行在两边。且面向主屏时左边是上行，右边是下行。
//                    leftFragment = new LeftFragment();
//                    leftFragment.fragmentInit(themeStyle, upline, uplineIndex);
//                    rightFragment = new RightFragment();
//                    rightFragment.fragmentInit(themeStyle, downline, downlineIndex);
//                    replaceleftFragment(leftFragment);
//                    replacerightFragment(rightFragment);
//                    break;
//                case 3:
//                    Log.d("debug_test", "themeStyle: 3");
//                    /*  样式3：中间是上/下行线路，两边是站台。且面向主屏时线路在右侧，具体屏幕是处在上行边上的站台还是
//                     *  下行边上的站台由stationID来决定 */
//                case 4:
//                    Log.d("debug_test", "themeStyle: 4");
//                    /*  样式4：中间是上/下行线路，两边是站台。且面向主屏时线路在左侧，具体屏幕是处在上行边上的站台还是
//                     *  下行边上的站台由stationID来决定 */
//
//                    //左边fragment和右边fragment相同
//                    leftFragment = new LeftFragment();
//                    leftFragment.fragmentInit(themeStyle, isDownline ? downline : upline,
//                            isDownline ? downlineIndex : uplineIndex);
//                    rightFragment = new RightFragment();
//                    rightFragment.fragmentInit(themeStyle, isDownline ? downline : upline,
//                            isDownline ? downlineIndex : uplineIndex);
//                    replaceleftFragment(leftFragment);
//                    replacerightFragment(rightFragment);
//                    break;
//
//                default:
//                    Log.e(TAG, "Unknown themeStyle!");
//                    break;
//            }
//
//        }
//
//        Log.d(TAG, "new ViceDisplay!");
//        mViceDisplay = new ViceDisplay(MainActivity.this);
//        Log.d(TAG, "new ViceDisplay finished!");
//        mViceDisplay.ViceDisplayInit(posFlag, themeStyle, isDownline, stations, downlineIndex, uplineIndex);
//        Log.d(TAG, "ViceDisplay Init finished!");
//        mViceDisplay.Show();
//        Log.d(TAG, "ViceDisplay show!");
//
//        //界面初始化完毕
//        isFragmentAdded = true;

        /*
        //模拟测试，两秒更新一次BRT信息
        TimerTask MqttTest1 = new TimerTask() {
            private int testDs = 1;
            private int testIsArrLeft = 1;
            @Override
            public void run() {
                Message msg = Message.obtain();
                msg.what = MsgUpdateBRTInfo;
                ArrayList<String> brtInfo = new ArrayList<>();
                //路线ID
                brtInfo.add("80002");
                //车辆ID
                brtInfo.add("506");
                //双程号
                brtInfo.add("" + testDs);
                //到离站信息
                brtInfo.add("" + testIsArrLeft);
                testIsArrLeft++;
                if (testIsArrLeft > 2) {
                    testDs++;
                    if (testDs == 5) {
                        testDs = 6;
                    } else if (testDs == 32) {
                        testDs = 33;
                    } else if (testDs > 36) {
                        testDs = 1;
                    }
                }
                if (testIsArrLeft > 2) {
                    testIsArrLeft = 1;
                }
                msg.obj = brtInfo;
                handler.sendMessage(msg);
            }
        };
        Timer MqttTimer1 = new Timer();
        MqttTimer1.schedule(MqttTest1, 2000, 2000);

        //模拟测试，两秒更新一次BRT信息
        TimerTask MqttTest2 = new TimerTask() {
            private int testDs = 1;
            private int testIsArrLeft = 1;
            @Override
            public void run() {
                Message msg = Message.obtain();
                msg.what = MsgUpdateBRTInfo;
                ArrayList<String> brtInfo = new ArrayList<>();
                //路线ID
                brtInfo.add("80002");
                //车辆ID
                brtInfo.add("509");
                //双程号
                brtInfo.add("" + testDs);
                //到离站信息
                brtInfo.add("" + testIsArrLeft);
                testIsArrLeft++;
                if (testIsArrLeft > 2) {
                    testDs++;
                    if (testDs == 5) {
                        testDs = 6;
                    } else if (testDs == 32) {
                        testDs = 33;
                    } else if (testDs > 36) {
                        testDs = 1;
                    }
                }
                if (testIsArrLeft > 2) {
                    testIsArrLeft = 1;
                }
                msg.obj = brtInfo;
                handler.sendMessage(msg);
            }
        };
        Timer MqttTimer2 = new Timer();
        MqttTimer2.schedule(MqttTest2, 1000, 1500);
        */
    }

    private void updateBRTInfo(String brtInfo) {

        if (!isFragmentAdded) {
            return;
        }
//        //线路ID
//        String RouteID = brtInfo.get(0);
//        //车辆ID
//        String ProductID = brtInfo.get(1);
//        //双程号
//        int dualSerialID = Integer.valueOf(brtInfo.get(2));
//        //到离站数据 1: 表示到站  2: 表示离站
//        int IsArrLeft = Integer.valueOf(brtInfo.get(3));
//
//        //过滤线路ID
//        if (!"80002".equals(RouteID)) {
//            Log.d(TAG, "Data from other Routes have been received!");
//            return;
//        }
        if (posFlag == 0x02) {
            //终点
            zLeftFragment.updateBRTInfo(brtInfo, mViceDisplay);
//            zRightFragment.updateBRTInfo(brtInfo, mViceDisplay);
//            zLeftFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
//            zRightFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
        } else if (posFlag == 0x03) {
            //中间站点
            leftFragment.updateBRTInfo(brtInfo, mViceDisplay);
//            rightFragment.updateBRTInfo(brtInfo, mViceDisplay);
//            leftFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
//            rightFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
        }

    }


//    private void updateBRTInfo(ArrayList<String> brtInfo) {
//
//        if (!isFragmentAdded) {
//            return;
//        }
//        //线路ID
//        String RouteID = brtInfo.get(0);
//        //车辆ID
//        String ProductID = brtInfo.get(1);
//        //双程号
//        int dualSerialID = Integer.valueOf(brtInfo.get(2));
//        //到离站数据 1: 表示到站  2: 表示离站
//        int IsArrLeft = Integer.valueOf(brtInfo.get(3));
//
//        //过滤线路ID
//        if (!"80002".equals(RouteID)) {
//            Log.d(TAG, "Data from other Routes have been received!");
//            return;
//        }
//        if (posFlag == 0x02) {
//            //终点
//            zLeftFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
//            zRightFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
//        } else if (posFlag == 0x03) {
//            //中间站点
//            leftFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
//            rightFragment.updateBRTInfo(ProductID, dualSerialID, IsArrLeft, mViceDisplay);
//        }
//
//    }

    private void replaceleftFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.left_layout, fragment);
        transaction.commit();
    }

    private void replacerightFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.right_layout, fragment);
        transaction.commit();
    }

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "MQTT service disconnect!");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "MQTT service connect!");
            //返回一个MsgService对象
            msgService = ((MsgService.MsgBinder) service).getService();
            //注册回调接口来接收下载进度的变化
            msgService.setMqttListener(new MsgService.OnMqttListener() {
                //                @Override
//                public void onRecvMsg(ArrayList brtInfo) {
//                    //mProgressBar.setProgress(progress);
//                    if (null != brtInfo && brtInfo.size() == 4) {
//                        Log.d(TAG, "Receive MQTT msg: " + brtInfo.toString());
//                        Message msg = Message.obtain();
//                        msg.what = MsgUpdateBRTInfo;
//                        msg.obj = brtInfo;
//                        handler.sendMessage(msg);
//                    }
//                }
                @Override
                public void onRecvMsg(String brtInfo) {
                    Message msg = Message.obtain();
                    msg.what = MsgUpdateBRTInfo;
                    msg.obj = brtInfo;
                    handler.sendMessage(msg);
                }
            });
        }
    };

    @Override
    protected void onStart() {
        Log.d(TestTag, "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TestTag, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TestTag, "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TestTag, "onDestroy");
        unbindService(conn);
        System.exit(0);
    }

    @Override
    protected void onResume() {
        Log.d(TestTag, "onResume");
        super.onResume();
        //updateMqttdata();
    }
//    private void updateMqttdata() {
//        if (!serviceIsRunning()) {
//            startBlackIceService();
//            updateMqttdata();
//        }
//    }
//
//    private void startBlackIceService() {
//        final Intent intent = new Intent(this, MsgService.class);
//        startService(intent);
//    }
//
//    private boolean serviceIsRunning() {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (SERVICE_CLASSNAME.equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * 判断服务是否运行
     */
    private boolean isServiceRunning(final String className) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (info == null || info.size() == 0) return false;
        for (ActivityManager.RunningServiceInfo aInfo : info) {
            if (className.equals(aInfo.service.getClassName())) return true;
        }
        return false;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Handler getMainHandler() {
        return handler;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "new orientation: LANDSCAPE");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "new orientation: PORTRAIT");
        }
    }

}
