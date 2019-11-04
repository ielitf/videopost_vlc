package com.ceiv.communication;

import android.util.Log;

import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.SystemInfoUtils;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import java.util.Date;

/**
 * Created by zhangdawei on 2018/8/9.
 */


/**
 *  心跳线程在初始化的时候一次性生成需要发送的消息，然后每次就直接发送，不需要再生成
 *  在其他地方，比如下载了新的文件（此时磁盘空间会变化），接收上位机设置的新的IP等需要调用更新回调函数，
 *  然后重新生成新的待发送的消息。
 *
 *
 * */
public class HeartBeatThread extends Thread {

    private final static String TAG = "HeartBeatThread";

    //用来更新心跳信息的接口
    public interface HeartBeatInfoUpdate {
        public void update();
    }

    //实现接口
    public HeartBeatInfoUpdate hbInfoUpdate = new HeartBeatInfoUpdate() {
        @Override
        public void update() {
            /*
            *   现在机制改为：通过上位机点击刷新，然后通过tcp发来HMulticast消息，然后我们做CMulticast反馈，
            *   这个CMulticast会重新生成最新的设备信息（包含设备序列号，flash剩余空间等），所以心跳消息是什么内容
            *   已经无所谓，这里就不浪费系统志愿重新生成心跳消息了
            *
            * */
//            //信息发生改变，重新生成消息
//            heartBeatMsg = makeHeartBeatMsg();
        }
    };

    private byte[] makeHeartBeatMsg(String appVer, String uiVer) {

        /*
         *   现在机制改为：通过上位机点击刷新，然后通过tcp发来HMulticast消息，然后我们做CMulticast反馈，
         *   这个CMulticast会重新生成最新的设备信息（包含设备序列号，flash剩余空间等），所以心跳消息是什么内容
         *   已经无所谓，这里就不浪费系统资源重新生成心跳消息了
         *
         * */
        String localIp = SystemInfoUtils.getNetworkInterfaceIp(null);
        String localMac = SystemInfoUtils.getNetworkInterfaceMac(null);
        if(localIp == null)
            localIp = SystemInfoUtils.getNetworkInterfaceIp("wlan0");
        if(localMac == null)
            localMac = SystemInfoUtils.getNetworkInterfaceMac("wlan0");
        builder_CHearBeat.setIp(localIp);
        builder_CHearBeat.setMac(localMac);
        builder_CHearBeat.setType(devInfo.getDevType());
        builder_CHearBeat.setIdentify(devInfo.getIdentify());
        //可用磁盘空间单位为：MB
        builder_CHearBeat.setDiskFree((int) (SystemInfoUtils.getAvailableExternalMemorySize() / 1024 / 1024));
        builder_CHearBeat.setVerApp(appVer);
        builder_CHearBeat.setVerUi(uiVer);

        cHeartBeat = builder_CHearBeat.build();
        byte[] msg = cHeartBeat.toByteArray();
        int c_len = msg.length + 4;
        int c_ver = ProtocolMessageProcess.PROTOCOL_VER;
        int c_type = ProtocolMessageProcess.TYPE_HeartBeat;
        byte[] msg_header = new byte[]{(byte)((c_len >> 8) & 0xff), (byte)(c_len & 0xff),
                (byte)((c_ver >> 8) & 0xff), (byte)(c_ver & 0xff),
                (byte)((c_type >> 8) & 0xff), (byte)(c_type & 0xff)};
        byte[] msg_total = new byte[msg.length + msg_header.length];
        System.arraycopy(msg_header, 0, msg_total, 0, msg_header.length);
        System.arraycopy(msg, 0, msg_total, msg_header.length, msg.length);
        return msg_total;

    }

    private NetMgrMsg.CHeartBeat.Builder builder_CHearBeat;
    private NetMgrMsg.CHeartBeat cHeartBeat;
    private byte[] heartBeatMsg;
    private IoSession session;
    private DeviceInfo devInfo;
    private int intervalTime;

    //intervalTime：单位：毫秒
    public HeartBeatThread(IoSession session, DeviceInfo deviceInfo, int intervalTime, String appVer, String uiVer) {
        //Log.d(TAG, "start");
        try {
            this.session = session;
            this.devInfo = deviceInfo;
            this.intervalTime = intervalTime;
            builder_CHearBeat = NetMgrMsg.CHeartBeat.newBuilder();

            heartBeatMsg = makeHeartBeatMsg(appVer, uiVer);
            //Log.d(TAG, "end");
        } catch (Exception e) {
            Log.e(TAG, "init error!");
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        Log.d(TAG, "running ...");
//        long oldTime = System.currentTimeMillis();
        while (true) {
            if (!session.isConnected()) {
                Log.d(TAG, "disconnect, heartbeat stop!");
                break;
            }
//            long newTime = System.currentTimeMillis();
//            Log.d(TAG, "Time:" + (newTime - oldTime) + "Send HeartBeat msg");
//            oldTime = newTime;
            session.write(IoBuffer.wrap(heartBeatMsg));
            try {
                Thread.sleep(intervalTime);
            } catch (InterruptedException e) {
                Log.e(TAG, "sleep error!");
                e.printStackTrace();
            }
        }
    }

}
