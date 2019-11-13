package com.ceiv.communication;

import android.util.Log;

import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.SystemInfoUtils;


/**
 * Created by zhangdawei on 2018/8/10.
 */

public class MulticastThread extends Thread {

    private final static String TAG = "MulticastThread";

    private String multicastIp;
    private int multicastPort;
    private MinaClientThread minaClientThread;
    private DeviceInfo devInfo;
    private SystemInfoUtils.ApplicationOperation applicationOperation;
    private String oldIp;
    private int oldPort;
    private MulticastRecv multicastRecv;

    public MulticastThread(String ip, int port, DeviceInfo deviceInfo,
                           SystemInfoUtils.ApplicationOperation applicationOperation) {
        multicastIp = ip;
        multicastPort = port;
        this.applicationOperation = applicationOperation;
        this.devInfo = deviceInfo;

        oldIp = devInfo.getServerIp();
        oldPort = devInfo.getServerPort();

        //起始的时候先按照配置文件中的默认地址去连接服务器
        Log.d(TAG, "Start MinaClientThread with default config ...");
        minaClientThread = new MinaClientThread(devInfo, applicationOperation);
        minaClientThread.start();
    }


    @Override
    public void run() {

        byte[] msg_total = null;
        byte[] msg_body = null;
        int msg_length;
        int msg_ver;
            int msg_type;

            Log.d(TAG, "MulticastThread running ... ");

            try {
            multicastRecv = new MulticastRecv(multicastIp, multicastPort);
        } catch (Exception e) {
            Log.e(TAG, "Create MulticastRecv error!");
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                Log.d(TAG, "Thread[" + getId() + "] Prepare to receive multicast message");
                msg_total = multicastRecv.receiveMulticast();
                Log.d(TAG, "Thread[" + getId() + "] Receive something");

                //接受的数据太短，丢弃
                if (msg_total.length <= 6) {
                    Log.d(TAG, "Thread[" + getId() + "] Receive Multicast message too short!");
                    continue;
                }
                Log.d(TAG, "Thread[" + getId() + "] Received Multicast msg total length:" + msg_total.length);

                msg_length = ((msg_total[0] << 8) & 0xff00) | (msg_total[1] & 0xff);
                msg_ver = ((msg_total[2] << 8) & 0xff00) | (msg_total[3] & 0xff);
                msg_type = ((msg_total[4] << 8) & 0xff00) | (msg_total[5] & 0xff);

                msg_body = new byte[msg_length - 4];
                Log.d(TAG, "Thread[" + getId() + "] Received Multicast protobuf msg length:" + msg_body.length);
                System.arraycopy(msg_total, 6, msg_body, 0, msg_body.length);

                //消息的协议版本判断
                if (ProtocolMessageProcess.PROTOCOL_VER != msg_ver) {
                    Log.d(TAG, "Thread[" + getId() + "] Protocol version wrong!");
                    continue;
                }

                //消息类型判断
                if (ProtocolMessageProcess.TYPE_Multicast != msg_type) {
                    Log.d(TAG, "Thread[" + getId() + "] Multicast msg type wrong!");
                    continue;
                }
            } catch (Exception e) {
                Log.e(TAG, "Thread[" + getId() + "] Receive Multicast message error!");
                e.printStackTrace();
                continue;
            }

            try {
                NetMgrMsg.HMulticast hMulticast = NetMgrMsg.HMulticast.parseFrom(msg_body);
                Log.d(TAG, "****************  HMulticast[" + getId() + "]  *****************");
                Log.d(TAG, "* serverIp:" + hMulticast.getServerIp());
                Log.d(TAG, "* serverPort:" + hMulticast.getServerPort());
                Log.d(TAG, "* opt:" + hMulticast.getOpt());
                Log.d(TAG, "* content:" + hMulticast.getContent());
                Log.d(TAG, "*********************************************************");

                //查看是否有调试模式的请求
                if (hMulticast.getOpt() == NetMgrDefine.MulticastOptEnum.DBG_MODE) {
                    if (hMulticast.getContent() == ProtocolMessageProcess.DEBUG_MODE_ON) {
                        SystemInfoUtils.debugModeControl(applicationOperation.getMainHandler(), true);
                    } else if (hMulticast.getContent() == ProtocolMessageProcess.DEBUG_MODE_OFF) {
                        SystemInfoUtils.debugModeControl(applicationOperation.getMainHandler(), false);
                    } else {
                        Log.d(TAG, "Thread[" + getId() + "] Unsupport debug_mode content!");
                    }
                }

                //处理接收到的数据
                String tmpIp = hMulticast.getServerIp();
                int tmpPort = hMulticast.getServerPort();
                if (!SystemInfoUtils.isIpAddr(tmpIp) || tmpPort <= 0) {
                    Log.d(TAG, "Thread[" + getId() + "] Receive Invalid Server Info!");
                    continue;
                }
                if (!tmpIp.equals(oldIp) || tmpPort != oldPort) {
                    //收到了新的连接信息
                    oldIp = tmpIp;
                    oldPort = tmpPort;

                    //关闭并清理之前的连接
                    Log.d(TAG, "Thread[" + getId() + "] Stop old MinaClientThread ... ");
                    if (minaClientThread != null) {
                        minaClientThread.connectStop();
                        if (minaClientThread.isAlive()) {
                            minaClientThread.interrupt();
                            //minaClientThread.join();
                            Log.d(TAG, "Thread[" + getId() + "] Old MinaClientThread has Quit!");
                            minaClientThread = null;
                        }
                    }
                    //重新连接新的服务器地址
                    Log.d(TAG, "Thread[" + getId() + "] Restart MinaClientThread with new config ... ");
                    minaClientThread = new MinaClientThread(tmpIp, tmpPort, devInfo, applicationOperation);
                    minaClientThread.start();
                }

            } catch (Exception e) {
                Log.e(TAG, "Thread[" + getId() + "] Parse Multicast information from protobuf msg failed!");
                e.printStackTrace();
                continue;
            }
        }
    }
}
