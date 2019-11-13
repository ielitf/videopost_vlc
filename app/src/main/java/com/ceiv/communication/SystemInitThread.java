package com.ceiv.communication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
//import android.util.Log;
import com.ceiv.log4j.Log;

import com.ceiv.BrtUtils.BrtInfoUtils;
import com.ceiv.BrtUtils.RouteInfo;
import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.DeviceInfoUtils;
import com.ceiv.communication.utils.NetworkProblemUtils;
import com.ceiv.communication.utils.SystemInfoUtils;
import com.ceiv.videopost.MainActivity;
import com.ceiv.videopost.StationInfo;
import com.ceiv.videopost.StationInfoOperation;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by zhangdawei on 2018/8/16.
 */

public class SystemInitThread implements Runnable, NetworkMonitorThread.ConnectSuccessCallBack {

    private final static String TAG = "SystemInitThread";

    /*
     *   说明：
     *   在检测到设备网络有问题时，例如：
     *   1、设备检测到网线未连接
     *   2、设备IP静态未配置成功
     *   3、设备默认路由未配置成功
     *   4、设备连不上服务器
     *   这时会尝试通过重启设备（最多3次）来解决这些问题，重启的方法是使用三全视讯提供的接口（发送广播）
     *   然而从发送广播后，到系统实际重启会有一段时间。因此在网络环境有问题的情况下：比如，网线未连接
     *   IP、路由未配置等，会出现设备无限重启的现象。
     *   经过测试发现原因如下：在我们调用重启的接口和实际开始重启这段时间内，我们的程序首先退出，然后
     *   系统先调用我们的Launcher的onDestroy然后再调用onCreate从而将Launcher启动起来，在Launcher的
     *   onCreate中会发送Action为START_APK的广播（正常情况下，我们的apk在系统上电后就是通过该广播来唤起），
     *   从而我们的apk在系统还未重启前再次启动起来，此时如果我们马上开始检查重启的原因
     *   （调用isRebootFromNetworkProblem）会自动清除掉“网络问题重启”的标志位（resetRebootFlag），
     *   该标志位的目的是区分正常的系统重启和因为网络问题导致的重启。但是后面还来不及再次检查网络的环境
     *   （网线连接、IP、路由配置等）就已经真正进行重启了，所以会出现了每次重启都是正常重启的判断，
     *   但实际重启完后，又检测到网络环境的异常，又会继续尝试重启系统，所以会出现无限循环重启的问题。
     *   在apk启动起来后延时一段时间再检测网络环境是否有异常，这里取30s的时间
     *
     * */
    //延时30s，这个时间不能太短
    private final static int delayTime = 30 * 1000;

    private Context mContext;

    private StationInfo stationInfo = null;

    private DeviceInfo deviceInfo = null;

    private NetworkMonitorThread netWorkMonitorThread = null;

    private SystemInfoUtils.ApplicationOperation applicationOperation;

    //SystemInfoUtils.ApplicationOperation该接口目的是为了能够获取上下文Context，Activity
    //主要用在发送广播、截图等功能
    public SystemInitThread(SystemInfoUtils.ApplicationOperation applicationOperation) {
        this.applicationOperation = applicationOperation;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public void run() {

        Log.d(TAG, "SystemInitThread start... ");

        mContext = applicationOperation.getActivity().getApplicationContext();

        //延时一段时间
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (NetworkProblemUtils.isRebootFromNetworkProblem()) {
                    //如果是因为网络原因导致的重启，首先清除flag
                    com.ceiv.log4j.Log.d(TAG, "Reboot from NetworkProblem");
                    NetworkProblemUtils.resetRebootFlag();
                } else {
                    //如果不是因为网络原因导致的重启，则清零重启记录
                    com.ceiv.log4j.Log.d(TAG, "Normal Reboot");
                    NetworkProblemUtils.resetRebootTimes();
                }

                //apk启动3s后（防止系统还未初始化完全）进行网络状况的检查
                if (networkCheckTimer != null) {
                    networkCheckTimer.cancel();
                    networkCheckTimer = null;
                }
                networkCheckTimer = new Timer();
                networkCheckTimer.schedule(createNetworkCheckTask(), 3000);

                //添加广播接收器，接收网络变化的广播
                ConnectivityManager mConnectivityManager = (ConnectivityManager) applicationOperation.getActivity().
                        getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                applicationOperation.getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                        new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        }, delayTime);

        //读取本地配置文件，没有的话生成默认的配置。
        if (!DeviceInfoUtils.DeviceInfoUtilsInit(applicationOperation.getActivity())) {
            Log.e(TAG, "DeviceInfoUtilsInit failed!");
            return;
        }
        deviceInfo = DeviceInfoUtils.getDeviceInfoFromFile();
        if (null == deviceInfo) {
            Log.e(TAG, "Can't get Device information from file");
            return;
        } else {
            //初始化设备信息
            Message msg = Message.obtain();
            msg.what = MainActivity.MsgInitDevInfo;
            msg.obj = deviceInfo;
            applicationOperation.getMainHandler().sendMessage(msg);
        }

        //检查本地媒体目录，如果没有相关目录，则创建
        if (!DeviceInfoUtils.checkDeviceMediaDirectory()) {
            Log.e(TAG, "check device media directory error!");
            return;
        }

        //下面是耗时操作，所以不放在主线程中进行，在初始化线程操作完后，
        //通过Message通知主线程可以更新界面了
        ArrayList<RouteInfo> routeInfoList = BrtInfoUtils.getAllRouteInfo();
        if (routeInfoList == null) {
            Log.e(TAG, "Get Local RouteInfo failed!");
            return;
        }
        RouteInfo routeInfo = BrtInfoUtils.getCurRouteInfo();
        if (routeInfo == null) {
            Log.e(TAG, "Get curRouteInfo failed!");
            return;
        }
        Message routeMsg = Message.obtain();
        routeMsg.what = MainActivity.MsgInitRouteInfo;
        applicationOperation.getMainHandler().sendMessage(routeMsg);


        //获取站点信息
        StationInfoOperation.setResourcesContext(applicationOperation.getActivity().getApplicationContext());
        stationInfo = StationInfoOperation.getStationInfoFromXml();
        /* 这里有个问题需要处理：不管怎么样都必须保证stationInfo中有站点信息可用，
         * 所以若读取配置文件失败，需要用代码手动写死数据 */
        if (stationInfo == null) {
            Log.e(TAG, "Get StationInfo from xml file failed!");
        } else {
            //请求界面主线程将初始的站点信息显示在界面上
            Message stationInitMsg = Message.obtain();
            stationInitMsg.what = MainActivity.MsgInitStation;
            //当前的站点详细信息
            stationInitMsg.obj = stationInfo;
            applicationOperation.getMainHandler().sendMessage(stationInitMsg);
        }

        //默认调试模式关闭
        SystemInfoUtils.debugModeInit();

        //开启组播线程
        Log.d(TAG, "Start MulticastThread... ");
        new MulticastThread("238.10.21.100", 8999, deviceInfo, applicationOperation).start();

    }

    @Override
    public void connectSuccess() {
        //和服务器连接成功后，重新开启一个检测网络环境的定时任务
        Log.d(TAG, "ping server ok, going to start a new networkCheckTask.");
        if (networkCheckTimer != null) {
            networkCheckTimer.cancel();
            networkCheckTimer = null;
        }
        networkCheckTimer = new Timer();
        networkCheckTimer.schedule(createNetworkCheckTask(), 3000);
    }

    //连不上服务器时会开启的监控线程
    private NetworkMonitorThread netMonitor = null;
    //网络环境检查的延时定时器
    private Timer networkCheckTimer = null;
    //网络环境检查定时任务
    private TimerTask createNetworkCheckTask() {
        return new TimerTask() {
            @Override
            public void run() {
                String serverIpStr []  = deviceInfo.getServerIp().split(":");
                Log.i(TAG, "===getServerIp:"+deviceInfo.getServerIp() );
                if(serverIpStr !=null){
                    Log.i(TAG, "===serverIpStr:" + serverIpStr[0]);
                }
                else{
                    Log.i(TAG, "===serverIpStr:"+serverIpStr );
                    return;
                }

                //检查设备的以太网口网线是否连接
                if (!SystemInfoUtils.isEthernetConnected(mContext)) {
                    com.ceiv.log4j.Log.e(TAG, "Ethernet disconnected!");
                    if (NetworkProblemUtils.getRebootTimes() >= NetworkProblemUtils.RebootMaxTimes2) {
                        //重启次数达到最大数，后面不再尝试重启，而是在屏幕界面显示当前的状态
                        com.ceiv.log4j.Log.e(TAG, "NetworkProblem<Ethernet disconnected>: RebootTimes -> MaxTimes");
                        Message msg = Message.obtain();
                        Bundle data = new Bundle();
                        //是否显示网络问题的信息
                        data.putBoolean("isShow", true);
                        data.putString("info", "网线连接异常");
                        msg.what = MainActivity.MsgUpdateNetworkInfo;
                        msg.setData(data);
                        applicationOperation.getMainHandler().sendMessage(msg);
                    } else {
                        com.ceiv.log4j.Log.e(TAG, "gona to reboot!");
                        //记录重启
                        NetworkProblemUtils.rebootRecord();
                        SystemInfoUtils.rebootDevice(mContext);
                    }
                    return;
                } else {
                    com.ceiv.log4j.Log.d(TAG, "Ethernet connection OK");
                }

                //检查设备是否配置了IP
                if (null == SystemInfoUtils.getNetworkInterfaceIp(null)) {
                    com.ceiv.log4j.Log.e(TAG, "IpAddress was not configured!");
                    if (NetworkProblemUtils.getRebootTimes() >= NetworkProblemUtils.RebootMaxTimes2) {
                        //重启次数达到最大数，后面不再尝试重启，而是在屏幕界面显示当前的状态
                        com.ceiv.log4j.Log.e(TAG, "NetworkProblem<IpAddress was not configured>: RebootTimes -> MaxTimes");
                        Message msg = Message.obtain();
                        Bundle data = new Bundle();
                        //是否显示网络问题的信息
                        data.putBoolean("isShow", true);
                        data.putString("info", "设备IP配置异常");
                        msg.what = MainActivity.MsgUpdateNetworkInfo;
                        msg.setData(data);
                        applicationOperation.getMainHandler().sendMessage(msg);
                    } else {
                        com.ceiv.log4j.Log.e(TAG, "gona to reboot!");
                        //记录重启
                        NetworkProblemUtils.rebootRecord();
                        SystemInfoUtils.rebootDevice(mContext);
                    }
                    return;
                } else {
                    com.ceiv.log4j.Log.d(TAG, "Device IP OK");
                }

                //检查设备是否设置了默认路由
                if (!SystemInfoUtils.isEthernetInterfaceDefaultRouteSet()) {
                    com.ceiv.log4j.Log.e(TAG, "Default route was not set!");
                    if (NetworkProblemUtils.getRebootTimes() >= NetworkProblemUtils.RebootMaxTimes2) {
                        //重启次数达到最大数，后面不再尝试重启，而是在屏幕界面显示当前的状态
                        com.ceiv.log4j.Log.e(TAG, "NetworkProblem<Default route was not set>: RebootTimes -> MaxTimes");
                        Message msg = Message.obtain();
                        Bundle data = new Bundle();
                        //是否显示网络问题的信息
                        data.putBoolean("isShow", true);
                        data.putString("info", "设备路由配置异常");
                        msg.what = MainActivity.MsgUpdateNetworkInfo;
                        msg.setData(data);
                        applicationOperation.getMainHandler().sendMessage(msg);
                    } else {
                        com.ceiv.log4j.Log.e(TAG, "gona to reboot!");
                        //记录重启
                        NetworkProblemUtils.rebootRecord();
                        SystemInfoUtils.rebootDevice(mContext);
                    }
                    return;
                } else {
                    com.ceiv.log4j.Log.d(TAG, "Default Route OK");
                }

                //检查和服务器的连接性
                if (!SystemInfoUtils.testConnectivityWithAddress(serverIpStr[0])) {
//                if (!SystemInfoUtils.testConnectivityWithAddress("172.16.30.254")) {
                    com.ceiv.log4j.Log.e(TAG, "Can't connect to server: " + serverIpStr[0]);
                    if (NetworkProblemUtils.getRebootTimes() >= NetworkProblemUtils.RebootMaxTimes2) {
                        //重启次数达到最大数，后面不再尝试重启，而是在屏幕界面显示当前的状态
                        com.ceiv.log4j.Log.e(TAG, "NetworkProblem<Can't connect to server: "+
                                serverIpStr[0] + ">: RebootTimes -> MaxTimes");
                        Log.e(TAG, "服务器连接异常");
                        Message msg = Message.obtain();
                        Bundle data = new Bundle();
                        //是否显示网络问题的信息
                        data.putBoolean("isShow", true);
                        data.putString("info", "服务器连接异常");
                        msg.what = MainActivity.MsgUpdateNetworkInfo;
                        msg.setData(data);
                        applicationOperation.getMainHandler().sendMessage(msg);

                        /*
                         *   如果前面的网线连接、IP和路由都配置的没问题，只是和服务器连接不上，
                         *   而且重启三次后都无效，则只能把状态显示在界面上，帮助我们判断问题所在，
                         *   并且后面启动一个检测和服务器连接的线程，该线程一直运行，直到连通
                         *   服务器后自动终止，并开启一个重新检测网络环境的定时任务
                         *
                         * */
                        if (netMonitor != null) {
                            netMonitor.stopMonitor();
                            netMonitor = null;
                        }
                        try {
                            netMonitor = new NetworkMonitorThread(serverIpStr[0],
                                    SystemInitThread.this);
                            netMonitor.start();
                        }catch (Exception e) {
                            com.ceiv.log4j.Log.e(TAG, "Start new NetworkMonitorThread error!", e);
                        }
                    } else {
                        com.ceiv.log4j.Log.e(TAG, "gona to reboot!");
                        //记录重启
                        NetworkProblemUtils.rebootRecord();
                        SystemInfoUtils.rebootDevice(mContext);
                    }
                    return;
                } else {
                    com.ceiv.log4j.Log.d(TAG, "Connection with server OK");
                }

                //当一切网络参数都没问题时，清零重启计数
                com.ceiv.log4j.Log.d(TAG, "All Network Parameters OK!");
                NetworkProblemUtils.resetRebootTimes();
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                //是否显示网络问题的信息
                data.putBoolean("isShow", false);
                data.putString("info", "");
                msg.what = MainActivity.MsgUpdateNetworkInfo;
                msg.setData(data);
                applicationOperation.getMainHandler().sendMessage(msg);
            }
        };
    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                //广播接收器是在主线程中运行，这里不做可能会有阻塞、延迟的任务
                com.ceiv.log4j.Log.d(TAG, "BroadcastReceiver onReceive: CONNECTIVITY_ACTION");
                if (networkCheckTimer != null) {
                    networkCheckTimer.cancel();
                    networkCheckTimer = null;
                }
                networkCheckTimer = new Timer();
                //重新设定3s后检查
                networkCheckTimer.schedule(createNetworkCheckTask(), 3000);

            }
        }
    };
}
