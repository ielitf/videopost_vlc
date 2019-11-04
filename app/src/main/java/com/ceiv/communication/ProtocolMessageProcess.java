package com.ceiv.communication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.os.Message;
//import android.util.Log;
import com.ceiv.log4j.Log;
import android.view.View;

import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.DeviceInfoUtils;
import com.ceiv.communication.utils.SystemInfoUtils;

import com.ceiv.communication.FileDownload.FileDownloadCallBack;
import com.ceiv.videopost.MainActivity;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by zhangdawei on 2018/8/9.
 */

public class ProtocolMessageProcess {

    private final static boolean printDebugInfo = true;

    private final static String TAG = "ProtocolMessageProcesss";

    //传递给界面主线程的msg的命令
    public final static int MsgWhatDebugMode = 0x01;
    public final static int MsgWhatMediaOpt = 0x02;

    //msg相应的参数
    public final static int MsgArg1ReloadMedia = 0x01;
    public final static int MsgArg1MediaRemoveByName = 0x02;
    public final static int MsgArg1MediaRemoveByType = 0x03;
    public final static int MsgArg1MediaDownload = 0x04;
    public final static int MsgArg1DispSetting = 0x05;

    //指定需要更新那些种类的媒体文件，这里是位掩码
    public final static int MsgArg2VideoBit = 0x01;
    public final static int MsgArg2PicBit = 0x02;
    public final static int MsgArg2TextBit = 0x04;

    //协议版本
    public final static int PROTOCOL_VER = 0x01;

    //消息类型
    public final static int TYPE_Multicast = 0x11;
    public final static int TYPE_HeartBeat = 0x21;
    public final static int TYPE_NetSetting = 0x31;
    public final static int TYPE_DecoderSetting = 0x32;
    public final static int TYPE_SeederSetting = 0x33;
    public final static int TYPE_CommonSetting = 0x41;
    public final static int TYPE_MediaSetting = 0x51;
    public final static int TYPE_DispSetting = 0x52;
    public final static int TYPE_PlayListSetting = 0x61;
    public final static int TYPE_PlayStatistic = 0x62;


    //HMulticast中的MulticastOptEnum为DBG_MODE时，需要content的内容
    public final static int DEBUG_MODE_ON = 1;
    public final static int DEBUG_MODE_OFF = 0;

    //10: 十进制, 16: 十六进制
    public static void printSendMessage(Object msg, int formatType, boolean hasHeartBeat, boolean hasMsgHead) {

        IoBuffer in = (IoBuffer) msg;
        byte[] msg_total = in.array();
        if (msg_total.length <= 6) {
            Log.d(TAG, "Message too short!");
            return;
        }
        int len = (((msg_total[0] << 8) & 0xff00) + (msg_total[1] & 0xff)) + 2;
        //如果要求不打印心跳包
        if (!hasHeartBeat) {
            int protocolType = (((msg_total[4] << 8) & 0xff00) + (msg_total[5] & 0xff));
            if (ProtocolMessageProcess.TYPE_HeartBeat == protocolType) {
                return;
            }
        }

        StringBuilder sb = new StringBuilder();
        String format = null;
        if (formatType == 10) {
            format = "%02d ";
        } else if (formatType == 16) {
            format = "%02x ";
        } else {
            Log.d(TAG, "type error!");
            return;
        }
        //是否打印消息头
        if (hasMsgHead) {
            for (int i = 0; i < len; i++) {
                sb.append(String.format(format, msg_total[i]));
            }
        } else {
            for (int i = 6; i < len; i++) {
                sb.append(String.format(format, msg_total[i]));
            }
        }
        Log.d(TAG, "msg content: " + sb.toString());
    }

    public static byte[] makeMsgCMulticast(String ip, String mac, int type, String identify,
                                             int diskFree, String verApp, String verUi, String ipEth1, String macEth1) {

        NetMgrMsg.CMulticast.Builder builder = NetMgrMsg.CMulticast.newBuilder();
        builder.setIp(ip);
        builder.setMac(mac);
        builder.setType(type);
        builder.setIdentify(identify);
        builder.setDiskFree(diskFree);
        if (verApp != null) {
            builder.setVerApp(verApp);
        }
        if (verUi != null) {
            builder.setVerUi(verUi);
        }
        if (ipEth1 != null) {
            builder.setIpEth1(ipEth1);
        }
        if (macEth1 != null) {
            builder.setMacEth1(macEth1);
        }

        NetMgrMsg.CMulticast cMulticast = builder.build();
        byte[] msg = cMulticast.toByteArray();

        int c_len = msg.length + 4;
        int c_ver = PROTOCOL_VER;
        int c_type = TYPE_Multicast;
        byte[] msg_header = new byte[]{(byte)((c_len >> 8) & 0xff), (byte)(c_len & 0xff),
                (byte)((c_ver >> 8) & 0xff), (byte)(c_ver & 0xff),
                (byte)((c_type >> 8) & 0xff), (byte)(c_type & 0xff)};
        byte[] msg_total = new byte[msg.length + msg_header.length];
        System.arraycopy(msg_header, 0, msg_total, 0, msg_header.length);
        System.arraycopy(msg, 0, msg_total, msg_header.length, msg.length);

        return msg_total;
    }

    public static byte[] makeMsgCHeartBeat(String ip, String mac, int devType, String devIdentify,
                                           int diskFree, String verApp, String verUi, String ipEth1, String macEth1) {

        NetMgrMsg.CHeartBeat.Builder builder = NetMgrMsg.CHeartBeat.newBuilder();
        builder.setIp(ip);
        builder.setMac(mac);
        builder.setType(devType);
        builder.setIdentify(devIdentify);
        builder.setDiskFree(diskFree);
        if (verApp != null) {
            builder.setVerApp(verApp);
        }
        if (verUi != null) {
            builder.setVerUi(verUi);
        }
        if (ipEth1 != null) {
            builder.setIpEth1(ipEth1);
        }
        if (macEth1 != null) {
            builder.setMacEth1(macEth1);
        }

        NetMgrMsg.CHeartBeat cHeartBeat = builder.build();
        byte[] msg = cHeartBeat.toByteArray();

        int c_len = msg.length + 4;
        int c_ver = PROTOCOL_VER;
        int c_type = TYPE_HeartBeat;
        byte[] msg_header = new byte[]{(byte)((c_len >> 8) & 0xff), (byte)(c_len & 0xff),
                (byte)((c_ver >> 8) & 0xff), (byte)(c_ver & 0xff),
                (byte)((c_type >> 8) & 0xff), (byte)(c_type & 0xff)};
        byte[] msg_total = new byte[msg.length + msg_header.length];
        System.arraycopy(msg_header, 0, msg_total, 0, msg_header.length);
        System.arraycopy(msg, 0, msg_total, msg_header.length, msg.length);

        return msg_total;
    }

    public static byte[] makeMsgCCommonSetting(String identify, NetMgrDefine.DevCtrlEnum devCtrlOpt, String feedBackContent, int status) {

        NetMgrMsg.CCommonSetting.Builder builder = NetMgrMsg.CCommonSetting.newBuilder();

        builder.setIdentify(identify);
        builder.setDevCtrlOpt(devCtrlOpt);
        if (null != feedBackContent) {
            builder.setFeedBackContent(feedBackContent);
        }
        builder.setStatus(status);

        NetMgrMsg.CCommonSetting cCommonSetting = builder.build();
        byte[] msg = cCommonSetting.toByteArray();

        int c_len = msg.length + 4;
        int c_ver = PROTOCOL_VER;
        int c_type = TYPE_CommonSetting;
        byte[] msg_header = new byte[]{(byte)((c_len >> 8) & 0xff), (byte)(c_len & 0xff),
                (byte)((c_ver >> 8) & 0xff), (byte)(c_ver & 0xff),
                (byte)((c_type >> 8) & 0xff), (byte)(c_type & 0xff)};
        byte[] msg_total = new byte[msg.length + msg_header.length];
        System.arraycopy(msg_header, 0, msg_total, 0, msg_header.length);
        System.arraycopy(msg, 0, msg_total, msg_header.length, msg.length);

        return msg_total;
    }

    public static byte[] makeMsgCMediaSetting(String identify, NetMgrDefine.MediaOptEnum opt,
                                              ArrayList<NetMgrMsg.MediaResouce> feedBackMedia, int status) {

        NetMgrMsg.CMediaSetting.Builder builder = NetMgrMsg.CMediaSetting.newBuilder();

        NetMgrMsg.MediaResouce mediaResouce = null;
        switch (opt) {
            case Inquire_Media_All:
                builder.setIdentify(identify);
                Log.d(TAG, "Identify:" + identify);
                builder.setOpt(NetMgrDefine.MediaOptEnum.Inquire_Media_All);
                Log.d(TAG, "opt:" + opt);
                builder.setStatus(status);
                Log.d(TAG, "status:" + status);
                for (NetMgrMsg.MediaResouce tmpResouce : feedBackMedia) {
                    builder.addFeedBackMedia(tmpResouce);
                }
                Log.d(TAG, "Resouce count:" + builder.getFeedBackMediaCount());
                for (int i = 0; i < builder.getFeedBackMediaCount(); i++) {
                    NetMgrMsg.MediaResouce tmp = builder.getFeedBackMedia(i);
                    Log.d(TAG, "name:" + tmp.getName() + ", size:" + tmp.getSize() + ", type:" + tmp.getMediaType());
                }

                break;
            case Remove_Media:
            case Empty_Media_ByType:
            case Download_Media_ByName:
            case Upload_Media_ByAddr:
                builder.setIdentify(identify);
                builder.setOpt(opt);
                builder.setStatus(status);
                break;
            default:
                return null;
        }

        NetMgrMsg.CMediaSetting cMediaSetting = builder.build();
        byte[] msg = cMediaSetting.toByteArray();

        int c_len = msg.length + 4;
        int c_ver = PROTOCOL_VER;
        int c_type = TYPE_MediaSetting;
        byte[] msg_header = new byte[]{(byte)((c_len >> 8) & 0xff), (byte)(c_len & 0xff),
        (byte)((c_ver >> 8) & 0xff), (byte)(c_ver & 0xff),
        (byte)((c_type >> 8) & 0xff), (byte)(c_type & 0xff)};
        byte[] msg_total = new byte[msg.length + msg_header.length];
        System.arraycopy(msg_header, 0, msg_total, 0, msg_header.length);
        System.arraycopy(msg, 0, msg_total, msg_header.length, msg.length);

        return msg_total;
    }

    //HMulticast 消息的处理函数
    public static void msgProcessHMulticast(NetMgrMsg.HMulticast hMulticast, IoSession session, DeviceInfo devInfo,
                                            SystemInfoUtils.ApplicationOperation applicationOperation) {

        if (printDebugInfo) {
            Log.d(TAG, "****************  HMulticast  *****************");
            Log.d(TAG, "* serverIp:" + hMulticast.getServerIp());
            Log.d(TAG, "* serverPort:" + hMulticast.getServerPort());
            Log.d(TAG, "* opt:" + hMulticast.getOpt());
            Log.d(TAG, "* content:" + hMulticast.getContent());
            Log.d(TAG, "***********************************************");
        }

        String localIp = SystemInfoUtils.getNetworkInterfaceIp(null);
        if (localIp == null) {
            localIp = SystemInfoUtils.getNetworkInterfaceIp("wlan0");
        }
        String localMac = SystemInfoUtils.getNetworkInterfaceMac(null);
        if (localMac == null) {
            localMac = SystemInfoUtils.getNetworkInterfaceMac("wlan0");
        }
        //查看是否有调试模式的请求
        if (hMulticast.getOpt() == NetMgrDefine.MulticastOptEnum.DBG_MODE) {
            switch (hMulticast.getContent()) {
                case ProtocolMessageProcess.DEBUG_MODE_ON:
                    SystemInfoUtils.debugModeControl(applicationOperation.getMainHandler(), true);
                    break;
                case ProtocolMessageProcess.DEBUG_MODE_OFF:
                    SystemInfoUtils.debugModeControl(applicationOperation.getMainHandler(), false);
                    break;
                default:
                    Log.d(TAG, "Unsupport debug_mode content!");
                    break;
            }
        }

        byte[] respone = ProtocolMessageProcess.makeMsgCMulticast(
                localIp, localMac, devInfo.getDevType(), devInfo.getIdentify(),
                (int) (SystemInfoUtils.getAvailableExternalMemorySize() / 1024 / 1024),
                SystemInfoUtils.getApkVersionName(applicationOperation.getActivity()),
                SystemInfoUtils.getApkVersionName(applicationOperation.getActivity()),
                null, null);
        Log.d(TAG, "Send CMulticast feedback!");
        session.write(IoBuffer.wrap(respone));
    }

    //NetSetting 消息的处理函数
    public static void msgProcessHNetSetting(NetMgrMsg.HNetSetting hNetSetting, IoSession session,
                                             DeviceInfo devInfo, SystemInfoUtils.ApplicationOperation applicationOperation,
                                             HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  NetSetting  *****************");
            Log.d(TAG, "* type:" + hNetSetting.getType());
            Log.d(TAG, "* serverIp:" + hNetSetting.getServerIp());
            Log.d(TAG, "* serverPort:" + hNetSetting.getServerPort());
            Log.d(TAG, "* serverIpSpare:" + hNetSetting.getServerIpSpare());
            Log.d(TAG, "* serverPortSpare:" + hNetSetting.getServerPortSpare());
            Log.d(TAG, "* clientIpEth0:" + hNetSetting.getClientIpEth0());
            Log.d(TAG, "* clientMaskEth0:" + hNetSetting.getClientMaskEth0());
            Log.d(TAG, "* clientGwEth0:" + hNetSetting.getClientGwEth0());
            Log.d(TAG, "* clientIpEth1:" + hNetSetting.getClientIpEth1());
            Log.d(TAG, "* clientMaskEth1:" + hNetSetting.getClientMaskEth1());
            Log.d(TAG, "* clientGwEth1:" + hNetSetting.getClientGwEth1());
            Log.d(TAG, "***********************************************");
        }

        //首先对设备类型进行判断
        int devType = hNetSetting.getType();
        if (0 != devType && devInfo.getDevType() != devType) {
            Log.d(TAG, "wrong device type! our device type:" + devInfo.getDevType());
            return;
        }

        boolean saveConfig = false;
        //查看是否要设置上位机地址
        String serverIp = hNetSetting.getServerIp();
        int serverPort = hNetSetting.getServerPort();
        if (!SystemInfoUtils.isIpAddr(serverIp) || serverPort <= 0) {
            Log.d(TAG, "No need to set serverIp");
        } else {
            saveConfig = true;
            devInfo.setServerIp(serverIp);
            devInfo.setServerPort(serverPort);
        }

        //查看是否要设置海信服务器地址
        String serverIpSpare = hNetSetting.getServerIpSpare();
        int serverPortSapre = hNetSetting.getServerPortSpare();
        if (!SystemInfoUtils.isIpAddr(serverIpSpare) || serverPortSapre <= 0) {
            Log.d(TAG, "No need to set InfoPublishServer");
        } else {
            saveConfig = true;
            devInfo.setInfoPublishServer("tcp://" + serverIpSpare + ":" + serverPortSapre);
        }

        //保存配置到文件中
        if (saveConfig) {
            DeviceInfoUtils.saveDeviceInfoToFile(devInfo);
        }

        //查看是否要设置以太网网口
        String ipEth0 = hNetSetting.getClientIpEth0();
        String maskEth0 = hNetSetting.getClientMaskEth0();
        String gwEth0 = hNetSetting.getClientGwEth0();
        String dnsEth0 = hNetSetting.getClientDnsEth0();
        if (null == ipEth0 || null == maskEth0 || null == gwEth0 || null == dnsEth0) {
            Log.d(TAG, "No need to set eth0 config!");
            return;
        }
        if (SystemInfoUtils.isIpAddr(ipEth0) && SystemInfoUtils.isNetmask(maskEth0) &&
                SystemInfoUtils.isIpAddr(gwEth0) && SystemInfoUtils.isIpAddr(dnsEth0)) {
            Log.d(TAG, "going to set eth0, ip:" + ipEth0 + ", mask:" + maskEth0 + ", gateway:" + gwEth0 + ", dns:" + dnsEth0);
            SystemInfoUtils.setSystemNetworkInfo(applicationOperation.getActivity(), ipEth0, maskEth0, gwEth0, dnsEth0);
            //修改以太网网络参数后需要重新连接上位机。
            session.closeOnFlush();
        } else {
            Log.d(TAG, "Invalid eth0 config!");
        }

        //我们的android设备没有第二个以太网口，所以不需要检测eth1设置

    }

    //DecoderSetting 消息的处理函数
    public static void msgProcessHDecoderSetting(NetMgrMsg.HDecoderSetting hDecoderSetting, IoSession session, DeviceInfo devInfo) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  DecoderSetting  *****************");
            Log.d(TAG, "* type:" + hDecoderSetting.getType());
            Log.d(TAG, "* volume:" + hDecoderSetting.getVolume());
            Log.d(TAG, "* playCtrl:" + hDecoderSetting.getPlayCtrl());
            Log.d(TAG, "* position:" + hDecoderSetting.getPosition());
            Log.d(TAG, "* masterStreamAddr:" + hDecoderSetting.getMasterStreamAddr());
            Log.d(TAG, "* spareStreamAddr:" + hDecoderSetting.getSpareStreamAddr());
            Log.d(TAG, "* switch:" + hDecoderSetting.getSwitch());
            Log.d(TAG, "***********************************************");
        }
    }

    //SeederSetting 消息的处理函数
    public static void msgProcessHSeederSetting(NetMgrMsg.HSeederSetting hSeederSetting, IoSession session, DeviceInfo devInfo) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  SeederSetting  *****************");
            Log.d(TAG, "* type:" + hSeederSetting.getType());
            Log.d(TAG, "* isMaster:" + hSeederSetting.getIsMaster());
            Log.d(TAG, "* spareIp:" + hSeederSetting.getSpareIp());
            Log.d(TAG, "* sparePort:" + hSeederSetting.getSparePort());
            Log.d(TAG, "* isAutoSwitch:" + hSeederSetting.getIsAutoSwitch());
            Log.d(TAG, "* isLive:" + hSeederSetting.getIsLive());
            Log.d(TAG, "* recvStreamAddr:" + hSeederSetting.getRecvStreamAddr());
            Log.d(TAG, "* recvStreamPhy:" + hSeederSetting.getRecvStreamPhy());
            Log.d(TAG, "* sendStreamAddr:" + hSeederSetting.getSendStreamAddr());
            Log.d(TAG, "* sendStreamPhy:" + hSeederSetting.getSendStreamPhy());
            Log.d(TAG, "* switch:" + hSeederSetting.getSwitch());
            Log.d(TAG, "***********************************************");
        }
    }

    //CommonSetting 消息的处理函数
    public static void msgProcessHCommonSetting(NetMgrMsg.HCommonSetting hCommonSetting, IoSession session,
                                                DeviceInfo devInfo, FileDownloadCallBack appDownloadCallBack,
                                                SystemInfoUtils.ApplicationOperation applicationOperation,
                                                HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate) {

        if (printDebugInfo) {
            Log.d(TAG, "****************  CommonSetting  *****************");
            Log.d(TAG, "* type:" + hCommonSetting.getType());
            Log.d(TAG, "* identify:" + hCommonSetting.getIdentify());
            Log.d(TAG, "* blOption:" + hCommonSetting.getBlOption());
            Log.d(TAG, "* backlight:" + hCommonSetting.getBacklight());
            Log.d(TAG, "* gamma:" + hCommonSetting.getGamma());
            Log.d(TAG, "* loggerLevel:" + hCommonSetting.getLoggerLevel());
            Log.d(TAG, "* themeStyle:" + hCommonSetting.getThemeStyle());
            Log.d(TAG, "* devCtrlOpt:" + hCommonSetting.getDevCtrlOpt());
            Log.d(TAG, "* devCtrlContent:" + hCommonSetting.getDevCtrlContent());
            Log.d(TAG, "* rotation:" + hCommonSetting.getRotation());
            Log.d(TAG, "***********************************************");
        }

        //首先对设备类型进行判断
        int devType = hCommonSetting.getType();
        if (0 != devType && devInfo.getDevType() != devType) {
            Log.d(TAG, "wrong device type! our device type:" + devInfo.getDevType());
            return;
        }

        boolean saveIdentify = false;
        boolean saveThemeStyle = false;
        String devIdentify = hCommonSetting.getIdentify();
        //判断是否是需要设置设备编号
        if (null != devIdentify) {
            devInfo.setIdentify(devIdentify);
            saveIdentify = true;
        }
        //保存镜像设置
        int themeStyle = hCommonSetting.getThemeStyle();
        if (1 == themeStyle || 2 == themeStyle || 3 == themeStyle || 4 == themeStyle) {
            devInfo.setThemeStyle(themeStyle);
            saveThemeStyle = true;
        }
        if (saveIdentify || saveThemeStyle) {
            if (!DeviceInfoUtils.saveDeviceInfoToFile(devInfo)) {
                Log.d(TAG, "save device info failed!");
            }
        }

        //不在更新心跳消息
//        //如果修改了Identify，则需要更新心跳消息
//        if (saveIdentify) {
//            heartBeatInfoUpdate.update();
//        }

        //现在设备背光由专门的单片机控制
        //设置背光
//        int blOption = hCommonSetting.getBlOption();
//        int backlight = hCommonSetting.getBacklight();
//        //blOption, 1:预览, 2:保存
//        if (1 == blOption || 2 == blOption) {
//            if (backlight <= 255 && backlight >= 0) {
//                SystemInfoUtils.setSystemBackLight(applicationOperation.getActivity(), backlight, ((1 == blOption) ? false : true));
//            }
//        }

        //设置gamma忽略

        //设置日志登记忽略

        //设备控制
        byte[] respone = null;
        NetMgrDefine.DevCtrlEnum devCtrlEnum = hCommonSetting.getDevCtrlOpt();
        switch (devCtrlEnum) {
            case RESET_DEV:
                respone = makeMsgCCommonSetting(hCommonSetting.getIdentify(), NetMgrDefine.DevCtrlEnum.RESET_DEV, null, 100);
                session.write(IoBuffer.wrap(respone));
                SystemInfoUtils.rebootDevice(applicationOperation.getActivity());
                break;
            case RESET_APP:
                respone = makeMsgCCommonSetting(hCommonSetting.getIdentify(), NetMgrDefine.DevCtrlEnum.RESET_APP, null, 100);
                session.write(IoBuffer.wrap(respone));
                Intent intentActivity = new Intent(applicationOperation.getActivity(), MainActivity.class);
                int mPendingIntentId = 12345;
                PendingIntent mPendingIntent = PendingIntent.getActivity(applicationOperation.getActivity(), mPendingIntentId,
                        intentActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) applicationOperation.getActivity().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
                Log.d(TAG, "exit...");
                System.exit(0);
                break;
            case UPDATE_APP:
                String appUrl = hCommonSetting.getDevCtrlContent();
                if (null == appUrl || "".equals(appUrl)) {
                    Log.e(TAG, "can't get app download url!");
                } else {
                    FileDownload appDownload = new FileDownload(appDownloadCallBack);
                    appDownload.download(appUrl, Environment.getExternalStorageDirectory().getAbsolutePath(), SystemInfoUtils.UPDATE_APK_NAME);
                }
                break;
            case UPDATE_SYS:
                //不做处理
                respone = makeMsgCCommonSetting(hCommonSetting.getIdentify(), NetMgrDefine.DevCtrlEnum.UPDATE_SYS, null, 100);
                session.write(IoBuffer.wrap(respone));
                break;
            case SCREEN_SHOT:
                //获取当前Activity截图

                //首先判断是否传来了截图上传地址
                String uploadUrl = hCommonSetting.getDevCtrlContent();
                if (null == uploadUrl || "".equals(uploadUrl)) {
                    respone = makeMsgCCommonSetting(devInfo.getIdentify(), hCommonSetting.getDevCtrlOpt(), null, -1);
                    session.write(IoBuffer.wrap(respone));
                    break;
                }


                final String pictureName =  devInfo.getIdentify() + "-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".png";
                String picturePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String picFullName = picturePath + "/" + pictureName;

                Log.d(TAG, "gona to send broadcast to request screenshot!");
                //发送广播通知系统进行截图
                Intent shIntent = new Intent();
                shIntent.setAction("com.ceiv.SCREEN_SHOT");
                shIntent.putExtra("fullName", picFullName);
                applicationOperation.getActivity().sendBroadcast(shIntent);

                try {
                    synchronized (SystemInfoUtils.getMediaOptObject()) {
                        //这里设置时间稍微长点，否则截图未成功，就继续运行了
                        SystemInfoUtils.getMediaOptObject().wait(4000);
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Request screen shot time out!");
                    e.printStackTrace();
                    byte[] tmpRespone = makeMsgCCommonSetting(devInfo.getIdentify(), hCommonSetting.getDevCtrlOpt(), pictureName, -1);
                    session.write(IoBuffer.wrap(tmpRespone));
                    break;
                }
                Log.d(TAG, "Screen shot finish, start compressing picture");
                final String compressedFile = pictureName.replace("png", "jpg");
                if (!SystemInfoUtils.pngPictureCompressedToJpg(picFullName,
                        picturePath + File.separator + compressedFile)) {
                    Log.e(TAG, "Picture Compression Failed!");
                    respone = makeMsgCCommonSetting(devInfo.getIdentify(), hCommonSetting.getDevCtrlOpt(), null, -1);
                    session.write(IoBuffer.wrap(respone));
                } else {
                    try {
                        final File file = new File(picturePath + File.separator + compressedFile);

                        final String tmpIdentify = devInfo.getIdentify();
                        final IoSession tmpSession = session;
                        final NetMgrDefine.DevCtrlEnum tmpDevOpt = hCommonSetting.getDevCtrlOpt();
                        FileUpload.FileUploadCallBack picUploadCB = new FileUpload.FileUploadCallBack() {
                            @Override
                            public void upload(int status, int result) {
                                byte[] tmpRespone = null;
                                if (FileUpload.UPLOAD_FINISHED == status) {
                                    if (FileUpload.SUCCESS == result) {
                                        Log.d(TAG, "Screenshot upload success!");
                                        tmpRespone = makeMsgCCommonSetting(tmpIdentify, tmpDevOpt, compressedFile, 100);
                                    } else {
                                        Log.d(TAG, "Screenshot upload failed!");
                                        tmpRespone = makeMsgCCommonSetting(tmpIdentify, tmpDevOpt, compressedFile, -1);
                                    }
                                    Log.d(TAG, "Screenshot upload finished, delete local file!");
                                    file.delete();
                                    tmpSession.write(IoBuffer.wrap(tmpRespone));
                                }
                            }
                        };
                        FileUpload picUpload = new FileUpload(picUploadCB);
                        picUpload.upload(uploadUrl, compressedFile, picturePath, compressedFile);

                    } catch (Exception e) {
                        Log.d(TAG, "Screenshot saving or screenshot upload failed!", e);
                        //e.printStackTrace();
                        respone = makeMsgCCommonSetting(devInfo.getIdentify(), hCommonSetting.getDevCtrlOpt(), null, -1);
                        session.write(IoBuffer.wrap(respone));
                    }
                }
                //删除压缩前的文件
                //先删除压缩前的文件
                try {
                    File oldFile = new File(picFullName);
                    oldFile.delete();
                } catch (Exception e) {
                    Log.e(TAG, "delete uncompressed picture error!", e);
                }


                //下面的方法截取不到videoView、WebView等的内容，所以放弃
//                //获取当前Activity截图
//
//                //首先判断是否传来了截图上传地址
//                String uploadUrl = hCommonSetting.getDevCtrlContent();
//                if (null == uploadUrl || "".equals(uploadUrl)) {
//                    respone = makeMsgCCommonSetting(devInfo.identify, hCommonSetting.getDevCtrlOpt(), null, -1);
//                    session.write(IoBuffer.wrap(respone));
//                    break;
//                }
//                View dView = applicationOperation.getActivity().getWindow().getDecorView();
//                dView.setDrawingCacheEnabled(true);
//                dView.buildDrawingCache();
//                Bitmap bitmap = Bitmap.createBitmap(dView.getDrawingCache());
//
//                final String pictureName = devInfo.identify + "-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".png";
//
//                if (bitmap == null) {
//                    Log.e(TAG, "Can't get screenshot bitmap!");
//                    respone = makeMsgCCommonSetting(devInfo.identify, hCommonSetting.getDevCtrlOpt(), null, -1);
//                    session.write(IoBuffer.wrap(respone));
//                } else {
//                    try {
//                        String picturePath = Environment.getExternalStorageDirectory().getAbsolutePath();
//                        final File file = new File(picturePath + "/" + pictureName);
//                        FileOutputStream fos = new FileOutputStream(file);
//                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                        fos.flush();
//                        fos.close();
//                        Log.d(TAG, "Save screenshot to file success");
//
//                        final String tmpIdentify = devInfo.identify;
//                        final IoSession tmpSession = session;
//                        final NetMgrDefine.DevCtrlEnum tmpDevOpt = hCommonSetting.getDevCtrlOpt();
//                        FileUpload.FileUploadCallBack picUploadCB = new FileUpload.FileUploadCallBack() {
//                            @Override
//                            public void upload(int status, int result) {
//                                byte[] tmpRespone = null;
//                                if (FileUpload.UPLOAD_FINISHED == status) {
//                                    if (FileUpload.SUCCESS == result) {
//                                        Log.d(TAG, "Screenshot upload success!");
//                                        tmpRespone = makeMsgCCommonSetting(tmpIdentify, tmpDevOpt, pictureName, 100);
//                                    } else {
//                                        Log.d(TAG, "Screenshot upload failed!");
//                                        tmpRespone = makeMsgCCommonSetting(tmpIdentify, tmpDevOpt, pictureName, -1);
//                                    }
//                                    Log.d(TAG, "Screenshot upload finished, delete local file!");
//                                    file.delete();
//                                    tmpSession.write(IoBuffer.wrap(tmpRespone));
//                                }
//                            }
//                        };
//                        FileUpload picUpload = new FileUpload(picUploadCB);
//                        picUpload.upload(uploadUrl, pictureName, picturePath, pictureName);
//
//                    } catch (Exception e) {
//                        Log.d(TAG, "Screenshot saving or screenshot upload failed!");
//                        e.printStackTrace();
//                        respone = makeMsgCCommonSetting(devInfo.identify, hCommonSetting.getDevCtrlOpt(), null, -1);
//                        session.write(IoBuffer.wrap(respone));
//                    }
//                }

                break;
            case LOG_EXPORT:
                //不做处理
                respone = makeMsgCCommonSetting(hCommonSetting.getIdentify(), NetMgrDefine.DevCtrlEnum.LOG_EXPORT, null, 100);
                session.write(IoBuffer.wrap(respone));
                break;
            default:
                break;
        }

        //检查是否需要旋转屏幕
        int rotation = hCommonSetting.getRotation();
        if (rotation != -2) {
            SystemInfoUtils.setRotation(applicationOperation.getActivity(), rotation);
        }

    }


    //MediaSetting 消息的处理函数
    public static void msgProcessHMediaSetting(NetMgrMsg.HMediaSetting hMediaSetting, IoSession session, DeviceInfo devInfo,
                                               HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate,
                                               SystemInfoUtils.ApplicationOperation applicationOperation) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  MediaSetting  *****************");
            Log.d(TAG, "* type:" + hMediaSetting.getType());
            Log.d(TAG, "* opt:" + hMediaSetting.getOpt());
            int count = hMediaSetting.getMediaCount();
            for (int i = 0; i < count; i++) {
                Log.d(TAG, "MediaType:" + hMediaSetting.getMedia(i).getMediaType() +
                        ", name:" + hMediaSetting.getMedia(i).getName() +
                        ", size:" + hMediaSetting.getMedia(i).getSize());
            }
            String mediaServerPrefix = hMediaSetting.getMediaServerPrefix();
            if (null != mediaServerPrefix) {
                Log.d(TAG, "* mediaServerPrefix:" + hMediaSetting.getMediaServerPrefix());
            }
            Log.d(TAG, "***********************************************");
        }


        int devType = hMediaSetting.getType();
        if (0 != devType && devInfo.getDevType() != devType) {
            Log.d(TAG, "wrong device type! our device type:" + devInfo.getDevType());
            return;
        }

        NetMgrDefine.MediaOptEnum mediaOptEnum = hMediaSetting.getOpt();
        switch (mediaOptEnum) {
            case Inquire_Media_All:
                //请求反馈设备中的媒体文件
                inquireMediaAll(hMediaSetting, session, devInfo);
                break;
            case Remove_Media:
                //删除指定的媒体文件
                removeMedia(hMediaSetting, session, devInfo, heartBeatInfoUpdate, applicationOperation);
                break;
            case Empty_Media_ByType:
                //清空指定类型的媒体文件
                emptyMediaByType(hMediaSetting, session, devInfo, heartBeatInfoUpdate, applicationOperation);
                break;
            case Download_Media_ByName:
                //下载指定的媒体文件
                downloadMediaByName(hMediaSetting, session, devInfo, heartBeatInfoUpdate, applicationOperation);
                break;
            case Upload_Media_ByAddr:
                //上传指定的媒体文件
                uploadMediaByAddr(hMediaSetting, session, devInfo);
                break;
            default:
                break;
        }
    }

    //DispSetting 消息的处理函数
    public static void msgProcessHDispSetting(NetMgrMsg.HDispSetting hDispSetting, IoSession session,
                                              DeviceInfo devInfo, SystemInfoUtils.ApplicationOperation applicationOperation) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  DispSetting  *****************");
            Log.d(TAG, "* type:" + hDispSetting.getType());
            Log.d(TAG, "* textType:" + hDispSetting.getTextType());
            Log.d(TAG, "* textColor:" + hDispSetting.getTextColor());
            Log.d(TAG, "* textSize:" + hDispSetting.getTextSize());
            Log.d(TAG, "* textFont:" + hDispSetting.getTextFont());
            Log.d(TAG, "* textSpeed:" + hDispSetting.getTextSpeed());
            Log.d(TAG, "* picType:" + hDispSetting.getPicType());
            Log.d(TAG, "* picDispTime:" + hDispSetting.getPicDispTime());
            Log.d(TAG, "* vgetVideoPlayTime:" + hDispSetting.getVideoPlayTime());
            Log.d(TAG, "* videoPlayStyle:" + hDispSetting.getVideoPlayStyle());
            Log.d(TAG, "***********************************************");
        }

        int devType = hDispSetting.getType();
        if (0 != devType && devInfo.getDevType() != devType) {
            Log.d(TAG, "wrong device type! our device type:" + devInfo.getDevType());
            return;
        }

        //配置文件里面暂时只保存了picDispTime和videoPlayTime，所以暂时只检测这两个字段
        int picDispTime = hDispSetting.getPicDispTime();
        int videoPlayTime = hDispSetting.getVideoPlayTime();
        int textSize = hDispSetting.getTextSize();
        String textColor = hDispSetting.getTextColor();
        String textFont = hDispSetting.getTextFont();
        int textSpeed = hDispSetting.getTextSpeed();
        //如果上位机没设置该字段时，这边收到的默认值为-2
        boolean saveConfig = false;
        //0: 小  1: 中  2: 大
        if (textSize == 0 || textSize == 1 || textSize == 2) {
            devInfo.setTextSize(textSize);
            saveConfig = true;
        }
        //例：红色 #FFFF0000 或 #FF0000
        if (null != textColor && !("".equals(textColor))) {
            int colorIntVal;
            try {
                colorIntVal = Color.parseColor(textColor);
                devInfo.setTextColor(colorIntVal);
                saveConfig = true;
            } catch (Exception e) {
                Log.d(TAG, "Parse color from string:" + textColor + " error!");
                e.printStackTrace();
            }
        }
        if (SystemInfoUtils.fontToName.containsKey(textFont)) {
            devInfo.setTextFont(textFont);
            saveConfig = true;
        }
        //0: 慢  1: 中  2: 快
        if (textSpeed == 0 || textSpeed == 1 || textSpeed == 2) {
            devInfo.setTextSpeed(textSpeed);
            saveConfig = true;
        }
        if (picDispTime > 0) {
            devInfo.setPicDispTime(picDispTime);
            saveConfig = true;
        }
        if (videoPlayTime > 0) {
            devInfo.setVideoPlayTime(videoPlayTime);
            saveConfig = true;
        }
        if (saveConfig) {
            DeviceInfoUtils.saveDeviceInfoToFile(devInfo);

            Message msg = Message.obtain();
            msg.what = MsgWhatMediaOpt;
            msg.arg1 = MsgArg1DispSetting;
            applicationOperation.getMainHandler().sendMessage(msg);
        }
    }

    //PlayListSetting 消息的处理函数
    public static void msgProcessHPlayListSetting(NetMgrMsg.HPlayListSetting hPlayListSetting, IoSession session, DeviceInfo devInfo) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  PlayListSetting  *****************");
            Log.d(TAG, "* type:" + hPlayListSetting.getType());
            Log.d(TAG, "* opt:" + hPlayListSetting.getOpt());
            Log.d(TAG, "* content(" + hPlayListSetting.getContentCount() + "):" + hPlayListSetting.getContentList());
            Log.d(TAG, "***********************************************");
        }




    }

    //PlayStatistic 消息的处理函数
    public static void msgProcessHPlayStatistic(NetMgrMsg.HPlayStatistic hPlayStatistic, IoSession session, DeviceInfo devInfo) {
        if (printDebugInfo) {
            Log.d(TAG, "****************  PlayStatistic  *****************");
            Log.d(TAG, "* type:" + hPlayStatistic.getType());
            Log.d(TAG, "* opt:" + hPlayStatistic.getOpt());
            Log.d(TAG, "***********************************************");
        }
    }



    private static void inquireMediaAll(NetMgrMsg.HMediaSetting hMediaSetting, IoSession session, DeviceInfo devInfo) {

        String parentPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DeviceInfoUtils.MediaPath;
        String videoPath = parentPath + "/" + DeviceInfoUtils.VideoPath;
        String picturePath = parentPath + "/" + DeviceInfoUtils.PicturePath;
        String textPath = parentPath + "/" + DeviceInfoUtils.TextPath;

        ArrayList<NetMgrMsg.MediaResouce> feedBackMedia = new ArrayList<NetMgrMsg.MediaResouce>();

        NetMgrMsg.MediaResouce.Builder mediaResouceBuilder = NetMgrMsg.MediaResouce.newBuilder();

        File videoPathFile = new File(videoPath);
        if (videoPathFile.exists()) {
            if (videoPathFile.isDirectory()) {
                File[] videoFiles = videoPathFile.listFiles();
                for (File tmp : videoFiles) {
                    if (tmp.isFile()) {
                        mediaResouceBuilder.setMediaType(NetMgrDefine.MediaTypeEnum.VIDEO);
                        mediaResouceBuilder.setName(tmp.getName());
                        mediaResouceBuilder.setSize(SystemInfoUtils.fileSizeToString(tmp.length()));
                        feedBackMedia.add(mediaResouceBuilder.build());
                    }
                }
            } else {
                videoPathFile.delete();
                videoPathFile.mkdirs();
            }
        } else {
            videoPathFile.mkdirs();
        }
        File picturePathFile = new File(picturePath);
        if (picturePathFile.exists()) {
            if (picturePathFile.isDirectory()) {
                File[] pictureFiles = picturePathFile.listFiles();
                for (File tmp : pictureFiles) {
                    if (tmp.isFile()) {
                        mediaResouceBuilder.setMediaType(NetMgrDefine.MediaTypeEnum.PIC);
                        mediaResouceBuilder.setName(tmp.getName());
                        mediaResouceBuilder.setSize(SystemInfoUtils.fileSizeToString(tmp.length()));
                        feedBackMedia.add(mediaResouceBuilder.build());
                    }
                }
            } else {
                picturePathFile.delete();
                picturePathFile.mkdirs();
            }
        } else {
            picturePathFile.mkdirs();
        }
        File textPathFile = new File(textPath);
        if (textPathFile.exists() && textPathFile.isDirectory()) {
            if (textPathFile.isDirectory()) {
                File[] textFiles = textPathFile.listFiles();
                for (File tmp : textFiles) {
                    if (tmp.isFile()) {
                        mediaResouceBuilder.setMediaType(NetMgrDefine.MediaTypeEnum.TEXT);
                        mediaResouceBuilder.setName(tmp.getName());
                        mediaResouceBuilder.setSize(SystemInfoUtils.fileSizeToString(tmp.length()));
                        feedBackMedia.add(mediaResouceBuilder.build());
                    }
                }
            } else {
                textPathFile.delete();
                textPathFile.mkdirs();
            }
        } else {
            textPathFile.mkdirs();
        }

        Log.d(TAG, "feedBackMedia num:" + feedBackMedia.size());
        byte[] respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), feedBackMedia, 100);
        session.write(IoBuffer.wrap(respone));
    }

    //删除媒体文件之前会通知主界面，然后主界面去处理，防止把正在使用的媒体文件删除
    private static void removeMedia(NetMgrMsg.HMediaSetting hMediaSetting, IoSession session, DeviceInfo devInfo,
                                    HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate,
                                    SystemInfoUtils.ApplicationOperation applicationOperation) {

        /*
         *  考虑到可能会对主界面有影响（比如请求删除正在播放的视屏，或者界面显示的图片），而这里不方便对界面进行操作，
         *  所以将需要删除的媒体文件通过handler传给主线程，让主线程先做相应的预处理（定制播放视频，关闭打开的媒体文件等）
         *
         */

        //删除之后是否需要更新相应的列表
        int updateFlag = 0x0;

        if (hMediaSetting.getMediaCount() > 0) {
            Message msgToActivity = Message.obtain();
            msgToActivity.what = MsgWhatMediaOpt;
            msgToActivity.arg1 = MsgArg1MediaRemoveByName;
            msgToActivity.obj = hMediaSetting;
            applicationOperation.getMainHandler().sendMessage(msgToActivity);

            try {
                synchronized (SystemInfoUtils.getMediaOptObject()) {
                    //等待主线程预处理完毕，再进行后续的删除操作
                    SystemInfoUtils.getMediaOptObject().wait();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread wait for preprocess error!");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Receive remove media request without any Media Resource");
            return;
        }
        String parentPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DeviceInfoUtils.MediaPath;
        String videoPath = parentPath + "/" + DeviceInfoUtils.VideoPath;
        String picturePath = parentPath + "/" + DeviceInfoUtils.PicturePath;
        String textPath = parentPath + "/" + DeviceInfoUtils.TextPath;

        boolean deleteSuccess = true;
        ArrayList<NetMgrMsg.MediaResouce> fileList = new ArrayList<>(hMediaSetting.getMediaList());
        File file = null;
        for (NetMgrMsg.MediaResouce tmp : fileList) {
            switch (tmp.getMediaType()) {
                case VIDEO:
                    updateFlag |= MsgArg2VideoBit;
                    file = new File(videoPath + "/" + tmp.getName());
                    if (file.exists() && file.isFile()) {
                        if(file.delete()) {
                            Log.d(TAG, "file:" + videoPath + "/" + tmp.getName() + "delete success!");
                        } else {
                            deleteSuccess = false;
                            Log.d(TAG, "file:" + videoPath + "/" + tmp.getName() + "delete failed!");
                        }
                    }
                    break;
                case PIC:
                    updateFlag |= MsgArg2PicBit;
                    file = new File(picturePath + "/" + tmp.getName());
                    if (file.exists() && file.isFile()) {
                        if(file.delete()) {
                            Log.d(TAG, "file:" + picturePath + "/" + tmp.getName() + "delete success!");
                        } else {
                            deleteSuccess = false;
                            Log.d(TAG, "file:" + picturePath + "/" + tmp.getName() + "delete failed!");
                        }
                    }
                    break;
                case TEXT:
                    updateFlag |= MsgArg2TextBit;
                    file = new File(textPath + "/" + tmp.getName());
                    if (file.exists() && file.isFile()) {
                        if(file.delete()) {
                            Log.d(TAG, "file:" + textPath + "/" + tmp.getName() + "delete success!");
                        } else {
                            deleteSuccess = false;
                            Log.d(TAG, "file:" + textPath + "/" + tmp.getName() + "delete failed!");
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        //不在更新心跳消息
//        //删除媒体文件后更新心跳信息（心跳信息中包含磁盘剩余空间信息）
//        heartBeatInfoUpdate.update();
        byte[] respone = null;
        if (deleteSuccess) {
            respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, 100);
        } else {
            respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, -1);
        }

        //通知主线程需要更新媒体文件列表
        Message msg = Message.obtain();
        msg.what = MsgWhatMediaOpt;
        msg.arg1 = MsgArg1ReloadMedia;
        msg.arg2 = updateFlag;
        applicationOperation.getMainHandler().sendMessage(msg);

        //发送反馈信息
        session.write(IoBuffer.wrap(respone));
    }

    //清空某种类型的媒体文件之前需要通知主界面
    private static void emptyMediaByType(NetMgrMsg.HMediaSetting hMediaSetting, IoSession session, DeviceInfo devInfo,
                                         HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate,
                                         SystemInfoUtils.ApplicationOperation applicationOperation) {

        /*
         *  考虑到可能会对主界面有影响（比如请求删除正在播放的视屏，或者界面显示的图片），而这里不方便对界面进行操作，
         *  所以将需要删除的媒体文件通过handler传给主线程，让主线程先做相应的预处理（定制播放视频，关闭打开的媒体文件等）
         *
         */

        if (hMediaSetting.getMediaCount() > 0) {
            Message msgToActivity = Message.obtain();
            msgToActivity.what =MsgWhatMediaOpt;
            msgToActivity.arg1 = MsgArg1MediaRemoveByType;
            msgToActivity.obj = hMediaSetting;
            applicationOperation.getMainHandler().sendMessage(msgToActivity);

            try {
                synchronized (SystemInfoUtils.getMediaOptObject()) {
                    //等待主线程预处理完毕，再进行后续的删除操作
                    SystemInfoUtils.getMediaOptObject().wait();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread wait for preprocess error!");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Receive remove media by type request without any Media Resource");
            return;
        }

        String parentPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DeviceInfoUtils.MediaPath;
        String videoPath = parentPath + "/" + DeviceInfoUtils.VideoPath;
        String picturePath = parentPath + "/" + DeviceInfoUtils.PicturePath;
        String textPath = parentPath + "/" + DeviceInfoUtils.TextPath;

        byte[] respone = null;
        NetMgrMsg.MediaResouce mediaResouce = null;
        mediaResouce = hMediaSetting.getMedia(0);
        if (null == mediaResouce) {
            Log.d(TAG, "Can't get MediaFile type to delete!");
            respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, -1);
            session.write(IoBuffer.wrap(respone));
            return;
        }

        File pathFile = null;
        File[] files = null;

        switch (mediaResouce.getMediaType()) {
            case VIDEO:
                pathFile = new File(videoPath);
                break;
            case PIC:
                pathFile = new File(picturePath);
                break;
            case TEXT:
                pathFile = new File(textPath);
                break;
            default:
                Log.d(TAG, "Can't delete Unknown MediaFile type!");
                respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, -1);
                session.write(IoBuffer.wrap(respone));
                return;
        }
        if (pathFile.exists()) {
            if (pathFile.isDirectory()) {
                files = pathFile.listFiles();
                for (File tmp : files) {
                    tmp.delete();
                }
            } else {
                pathFile.delete();
                pathFile.mkdirs();
            }
        } else {
            pathFile.mkdirs();
        }

        //不在更新心跳消息
//        //清空某类型媒体文件后需要更新心跳信息（心跳信息中包含剩余磁盘空间信息）
//        heartBeatInfoUpdate.update();
        respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, 100);

        //通知主线程需要更新媒体文件列表
        Message msg = Message.obtain();
        msg.what = MsgWhatMediaOpt;
        msg.arg1 = MsgArg1ReloadMedia;
        msg.arg2 = 0x0;
        switch (mediaResouce.getMediaType()) {
            case VIDEO:
                msg.arg2 |= MsgArg2VideoBit;
                break;
            case PIC:
                msg.arg2 |= MsgArg2PicBit;
                break;
            case TEXT:
                msg.arg2 |= MsgArg2TextBit;
                break;
            default:
                break;
        }
        applicationOperation.getMainHandler().sendMessage(msg);

        //发送反馈信息
        session.write(IoBuffer.wrap(respone));
    }

    //下载新的媒体文件时需要通知主界面：1、防止正在使用的媒体文件被覆盖，2、有新文件下载下来后可能会更新界面
    private static void downloadMediaByName(NetMgrMsg.HMediaSetting hMediaSetting, IoSession session, DeviceInfo devInfo,
                                            final HeartBeatThread.HeartBeatInfoUpdate heartBeatInfoUpdate,
                                            SystemInfoUtils.ApplicationOperation applicationOperation) {

        /*
         *  考虑到可能会对主界面有影响（比如请求下载的文件正在播放），而这里不方便对界面进行操作，
         *  所以将需要下载的媒体文件通过handler传给主线程，让主线程查看是否需要做相应的处理（暂时的做法是先切换到播放默认资源，然后执行下载命令），
         *  下载完毕后，发送msg通知主线程可以更新媒体列表了
         *
         */

        //更新媒体列表标志
        int updateFlag = 0x0;

        if (hMediaSetting.getMediaCount() > 0) {
            //获取更新标志
            for (NetMgrMsg.MediaResouce tmp : hMediaSetting.getMediaList()) {
                if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.VIDEO) {
                    updateFlag |= MsgArg2VideoBit;
                }
                if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.PIC) {
                    updateFlag |= MsgArg2PicBit;
                }
                if (tmp.getMediaType() == NetMgrDefine.MediaTypeEnum.TEXT) {
                    updateFlag |= MsgArg2TextBit;
                }
            }
            //发送消息通知主线程
            Message msgToActivity = Message.obtain();
            msgToActivity.what = MsgWhatMediaOpt;
            msgToActivity.arg1 = MsgArg1MediaDownload;
            msgToActivity.obj = hMediaSetting;
            applicationOperation.getMainHandler().sendMessage(msgToActivity);

            try {
                synchronized (SystemInfoUtils.getMediaOptObject()) {
                    //等待主线程预处理完毕，再进行后续的删除操作
                    SystemInfoUtils.getMediaOptObject().wait();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread wait for preprocess error!");
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Receive download media request without any Media Resource");
            return;
        }

        ArrayList<FileSetDownload.FileDownloadItem> downloadItem = null;

        if (0 == hMediaSetting.getMediaCount()) {
            byte[] respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, 100);
            session.write(IoBuffer.wrap(respone));
            return;
        }

        downloadItem = new ArrayList<FileSetDownload.FileDownloadItem>();
        Log.d(TAG, "MediaList:" + hMediaSetting.getMediaList());
        for (NetMgrMsg.MediaResouce tmp : hMediaSetting.getMediaList()) {
            Log.d(TAG, "media, name:" + tmp.getName());
            downloadItem.add(new FileSetDownload.FileDownloadItem(tmp.getMediaType(), tmp.getName(),tmp.getMd5Sum()));
        }

        final String identify = devInfo.getIdentify();
        final NetMgrDefine.MediaOptEnum mediaOpt = hMediaSetting.getOpt();
        final IoSession toCBsession = session;
        final SystemInfoUtils.ApplicationOperation appOpt = applicationOperation;
        final int arg2UpdateFlag = updateFlag;
        FileSetDownload.FileSetDownloadCallBack downloadCallBack = new FileSetDownload.FileSetDownloadCallBack() {
            @Override
            public void download(int result) {
                if (100 == result || -1 == result) {
                    //通知主线程需要进行媒体文件列表
                    Message msg = Message.obtain();
                    msg.what = MsgWhatMediaOpt;
                    msg.arg1 = MsgArg1ReloadMedia;
                    msg.arg2 = arg2UpdateFlag;
                    appOpt.getMainHandler().sendMessage(msg);
                }
                byte[] respone = makeMsgCMediaSetting(identify, mediaOpt, null, result);
                toCBsession.write(IoBuffer.wrap(respone));

                //不在更新心跳消息
//                //传输完成或者传输失败后需要更新心跳信息
//                if (result == 100 || result == -1) {
//                    heartBeatInfoUpdate.update();
//                }
            }
        };

        FileSetDownload fileSetDownload = new FileSetDownload(downloadCallBack);
        fileSetDownload.downloadInit(hMediaSetting.getMediaServerPrefix(), downloadItem);
        fileSetDownload.downloadStart();
    }

    private static void uploadMediaByAddr(NetMgrMsg.HMediaSetting hMediaSetting, IoSession session, DeviceInfo devInfo) {

        ArrayList<FileSetUpload.FileUploadItem> uploadItem = null;

        if (0 == hMediaSetting.getMediaCount()) {
            byte[] respone = makeMsgCMediaSetting(devInfo.getIdentify(), hMediaSetting.getOpt(), null, 100);
            session.write(IoBuffer.wrap(respone));
            return;
        }

        uploadItem = new ArrayList<FileSetUpload.FileUploadItem>();
        for (NetMgrMsg.MediaResouce tmp : hMediaSetting.getMediaList()) {
            uploadItem.add(new FileSetUpload.FileUploadItem(tmp.getMediaType(), tmp.getName()));
        }

        final String identify = devInfo.getIdentify();
        final NetMgrDefine.MediaOptEnum mediaOpt = hMediaSetting.getOpt();
        final IoSession toCBsession = session;
        FileSetUpload.FileSetUploadCallBack uploadCallBack = new FileSetUpload.FileSetUploadCallBack() {
            @Override
            public void upload(int result) {
                byte[] respone = makeMsgCMediaSetting(identify, mediaOpt, null, result);
                toCBsession.write(IoBuffer.wrap(respone));
            }
        };

        FileSetUpload fileSetUpload = new FileSetUpload(uploadCallBack);
        fileSetUpload.uploadInit(hMediaSetting.getMediaServerPrefix(), uploadItem);
        fileSetUpload.uploadStart();
    }

}



















