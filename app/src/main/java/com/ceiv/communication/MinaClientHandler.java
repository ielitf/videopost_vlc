package com.ceiv.communication;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.SystemInfoUtils;

import com.ceiv.communication.FileDownload.FileDownloadCallBack;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.ceiv.communication.NetMgrMsg.HCommonSetting;

/**
 * Created by zhangdawei on 2018/8/9.
 */

public class MinaClientHandler extends IoHandlerAdapter {

    private final static String TAG = "MinaClientHandler";
    private final static String DEBUG_TAG = "SEND_AND_RECV";

    private final static String VIDEO_FILE_PATH = "video";
    private final static String PICTURE_FILE_PATH = "picture";
    private final static String TEXT_FILE_PATH = "text";

    private MinaClientThread minaClientThread;
    private DeviceInfo deviceInfo;
    private SystemInfoUtils.ApplicationOperation applicationOperation;
    private HeartBeatThread heartBeatThread = null;
    private HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate = null;

    private AppDownloadCallBack appDownloadCallBack = null;

    public MinaClientHandler(MinaClientThread minaClientThread, DeviceInfo deviceInfo, SystemInfoUtils.ApplicationOperation applicationOperation) {
        this.minaClientThread = minaClientThread;
        this.deviceInfo = deviceInfo;
        this.applicationOperation = applicationOperation;
    }

    class AppDownloadCallBack implements FileDownloadCallBack {

        private Context context = null;
        DeviceInfo devInfo = null;
        private NetMgrMsg.HCommonSetting hCommonSetting = null;
        private IoSession session = null;
        byte[] respone = null;

        public AppDownloadCallBack(Context context, DeviceInfo devInfo, HCommonSetting hCommonSetting, IoSession session) {
            this.context = context;
            this.devInfo = devInfo;
            this.hCommonSetting = hCommonSetting;
            this.session = session;
        }
        @Override
        public void download(int status, int result) {

            if (FileDownload.DOWNLOAD_FINISHED == status) {
                if (FileDownload.SUCCESS == result) {
                    respone = ProtocolMessageProcess.makeMsgCCommonSetting(devInfo.getIdentify(),
                            hCommonSetting.getDevCtrlOpt(), null, 100);
                    session.write(IoBuffer.wrap(respone));
                    //发送消息后进行升级
                    SystemInfoUtils.requestUpdateApk(context, Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/" + SystemInfoUtils.UPDATE_APK_NAME);
                } else {
                    respone = ProtocolMessageProcess.makeMsgCCommonSetting(devInfo.getIdentify(),
                            hCommonSetting.getDevCtrlOpt(), null, -1);
                    session.write(IoBuffer.wrap(respone));
                }
            } else if (FileDownload.DOWNLOAD_RUNNING == status) {
                respone = ProtocolMessageProcess.makeMsgCCommonSetting(devInfo.getIdentify(),
                        hCommonSetting.getDevCtrlOpt(), null, result);
                session.write(IoBuffer.wrap(respone));
            }
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Log.d(TAG, "exceptionCaught", cause);
        //cause.printStackTrace();
        if (minaClientThread.getRequestQuitStatus()) {
            Log.d(TAG, "Receive MinaClientThread Quit Request! gona to close Session!");
            session.closeNow();
        }
    }

    //Session对象被创建时调用
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        Log.d(TAG, "sessionCreated");
    }

    //连接建立成功时调用
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        Log.d(TAG, "sessionOpened");

        heartBeatThread = new HeartBeatThread(session, deviceInfo, 200,
                SystemInfoUtils.getApkVersionName(applicationOperation.getActivity()),
                SystemInfoUtils.getApkVersionName(applicationOperation.getActivity()));
        heartBeatThread.start();
        heartBeatInfoUpdate = heartBeatThread.hbInfoUpdate;

        Log.d(TAG, "send msg_CMulticast");
        String localIp = SystemInfoUtils.getNetworkInterfaceIp(null);
        String localMac = SystemInfoUtils.getNetworkInterfaceMac(null);
        if(localIp == null)
            localIp = SystemInfoUtils.getNetworkInterfaceIp("wlan0");
        if(localMac == null)
            localMac = SystemInfoUtils.getNetworkInterfaceMac("wlan0");
        byte[] msg_CMulticast = ProtocolMessageProcess.makeMsgCMulticast(
                localIp, localMac, deviceInfo.getDevType(), deviceInfo.getIdentify(),
                (int) (SystemInfoUtils.getAvailableExternalMemorySize() / 1024 / 1024),
                SystemInfoUtils.getApkVersionName(applicationOperation.getActivity()),
                SystemInfoUtils.getApkVersionName(applicationOperation.getActivity()),
                null, null);

        session.write(IoBuffer.wrap(msg_CMulticast));

        Log.d(TAG, "send msg_CMulticast finished");
    }

    //接收到消息时调用
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        Log.d(TAG, "messageReceived");

        IoBuffer in = (IoBuffer) message;
        byte[] msg_total = in.array();

        if (msg_total.length <= 6) {
            Log.d(TAG, "Receive message too short!");
            return;
        }
        Log.d(TAG, "Received msg total length:" + msg_total.length);

        int msg_length = ((msg_total[0] << 8) & 0xff00) | (msg_total[1] & 0xff);
        int msg_ver = ((msg_total[2] << 8) & 0xff00) | (msg_total[3] & 0xff);
        int msg_type = ((msg_total[4] << 8) & 0xff00) | (msg_total[5] & 0xff);

        if (msg_length > (msg_total.length - 2)) {
            Log.d(TAG, "Receive incomplete msg, drop it!");
            return;
        }
        byte[] msg_body = new byte[msg_length - 4];
        Log.d(TAG, "Received protobuf msg length:" + msg_body.length);
        System.arraycopy(msg_total, 6, msg_body, 0, msg_body.length);

        if (msg_ver != ProtocolMessageProcess.PROTOCOL_VER) {
            Log.d(TAG, "Protocol version wrong!");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for(byte tmp : msg_body) {
            sb.append(String.format("%02x ", tmp));
        }
        Log.d(TAG, "Received protobuf msg :" + sb.toString());

        switch (msg_type) {
            case ProtocolMessageProcess.TYPE_Multicast:
                Log.d(TAG, "TYPE_Multicast");

                NetMgrMsg.HMulticast hMulticast = NetMgrMsg.HMulticast.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHMulticast(hMulticast, session, deviceInfo, applicationOperation);

                break;
            case ProtocolMessageProcess.TYPE_NetSetting:
                Log.d(TAG, "TYPE_NetSetting");
                NetMgrMsg.HNetSetting hNetSetting = NetMgrMsg.HNetSetting.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHNetSetting(hNetSetting, session, deviceInfo,
                        applicationOperation, heartBeatInfoUpdate);

                break;
            case ProtocolMessageProcess.TYPE_DecoderSetting:
                Log.d(TAG, "TYPE_DecoderSetting");
                NetMgrMsg.HDecoderSetting hDecoderSetting = NetMgrMsg.HDecoderSetting.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHDecoderSetting(hDecoderSetting, session, deviceInfo);

                break;
            case ProtocolMessageProcess.TYPE_SeederSetting:
                Log.d(TAG, "TYPE_SeederSetting");
                NetMgrMsg.HSeederSetting hSeederSetting = NetMgrMsg.HSeederSetting.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHSeederSetting(hSeederSetting, session, deviceInfo);

                break;
            case ProtocolMessageProcess.TYPE_CommonSetting:
                Log.d(TAG, "TYPE_CommonSetting");
                NetMgrMsg.HCommonSetting hCommonSetting = NetMgrMsg.HCommonSetting.parseFrom(msg_body);
                appDownloadCallBack = new AppDownloadCallBack(applicationOperation.getActivity(), deviceInfo, hCommonSetting, session);

                ProtocolMessageProcess.msgProcessHCommonSetting(hCommonSetting, session, deviceInfo,
                        appDownloadCallBack, applicationOperation, heartBeatInfoUpdate);

                break;
            case ProtocolMessageProcess.TYPE_MediaSetting:
                Log.d(TAG, "TYPE_MediaSetting");
                NetMgrMsg.HMediaSetting hMediaSetting = NetMgrMsg.HMediaSetting.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHMediaSetting(hMediaSetting, session, deviceInfo, heartBeatInfoUpdate, applicationOperation);
                break;

            case ProtocolMessageProcess.TYPE_DispSetting:
                Log.d(TAG, "TYPE_DispSetting");
                NetMgrMsg.HDispSetting hDispSetting = NetMgrMsg.HDispSetting.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHDispSetting(hDispSetting, session, deviceInfo, applicationOperation);

                break;
            case ProtocolMessageProcess.TYPE_PlayListSetting:
                Log.d(TAG, "TYPE_PlayListSetting");
                NetMgrMsg.HPlayListSetting hPlayListSetting = NetMgrMsg.HPlayListSetting.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHPlayListSetting(hPlayListSetting, session, deviceInfo);

                break;
            case ProtocolMessageProcess.TYPE_PlayStatistic:
                Log.d(TAG, "TYPE_PlayStatistic");
                NetMgrMsg.HPlayStatistic hPlayStatistic = NetMgrMsg.HPlayStatistic.parseFrom(msg_body);
                ProtocolMessageProcess.msgProcessHPlayStatistic(hPlayStatistic, session, deviceInfo);

                break;
            default:
                Log.d(TAG, "TYPE_Unsupport");
                break;
        }
    }

    //消息发送成功后会调用
    @Override
    public void messageSent(IoSession session, Object message) {
        //Log.d(TAG, "messageSent");
        ProtocolMessageProcess.printSendMessage(message, 16,false, true);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {

        //连接建立时会启动心跳线程
        Log.d(TAG, "Connection to Server[Idle] - " + status.toString());

        if (session != null) {
            session.closeNow();
        }
    }

    //TCP: 连接被关闭时调用， UDP: IoSession的close方法被调用时调用
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        Log.d(TAG, "sessionClosed");
        //连接关闭时停止心跳线程
        heartBeatThread.interrupt();
        heartBeatThread = null;
    }


}
