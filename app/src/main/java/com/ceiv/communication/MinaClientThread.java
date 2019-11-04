package com.ceiv.communication;

import android.util.Log;

import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.SystemInfoUtils;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhangdawei on 2018/8/9.
 */

public class MinaClientThread extends Thread {

    private final static String TAG = "MinaClientThread";

    private String serverIp;
    private int serverPort;
    private NioSocketConnector connector = null;
    private IoSession session = null;
    private DeviceInfo deviceInfo;
    private SystemInfoUtils.ApplicationOperation applicationOperation;

    private boolean requestQuit = false;

    //查看当前组播线程有没有对我们发起停止线程的请求
    public boolean getRequestQuitStatus() {
        return requestQuit;
    }

    //提供给外部的方法，目的是停止当前线程并做相关清理工作
    public void connectStop() {
        requestQuit = true;
        //如果已经连接到服务器，则关闭session，触发sessionClosed
        if (session != null) {
            if (session.isConnected()) {
                session.closeNow();
            }
        }
    }

    //默认采用DeviceInfo里面的服务器信息
    public MinaClientThread(DeviceInfo deviceInfo, SystemInfoUtils.ApplicationOperation applicationOperation) {
        serverIp = deviceInfo.getServerIp();
        serverPort = deviceInfo.getServerPort();
        this.deviceInfo = deviceInfo;
        this.applicationOperation = applicationOperation;
    }

    //手动设置连接的服务器信息
    public MinaClientThread(String ip, int port, DeviceInfo deviceInfo, SystemInfoUtils.ApplicationOperation applicationOperation) {
        serverIp = ip;
        serverPort = port;
        this.deviceInfo = deviceInfo;
        this.applicationOperation = applicationOperation;
    }

    @Override
    public void run() {
        Log.d(TAG, "MinaClientThread[" + getId() + "] runing ... ");

        connector = new NioSocketConnector();

        //设置连接超时
        connector.setConnectTimeoutMillis(10000);

        //添加过滤器，目的是为了断线重连
        connector.getFilterChain().addFirst("reconnection", new IoFilterAdapter() {
            @Override
            public void sessionClosed(NextFilter nextFilter, IoSession ioSession) throws Exception {
                while (true){
                    try {
                        if (requestQuit) {
                            Log.d(TAG, "Thread[" + getId() + "] RequestQuit:" + requestQuit + "in sessionClosed!");
                            if (connector != null) {
                                connector.dispose();
                            }
                            break;
                        }
                        Log.d(TAG, "Thread[" + getId() + "] sessionClosed, sleep 3 s");
                        Thread.sleep(3000);
                        Log.d(TAG, "Thread[" + getId() + "] Try to reconnect server");
                        ConnectFuture future = connector.connect();
                        //等待连接创建成功
                        future.awaitUninterruptibly();
                        //获取会话
                        session = future.getSession();
                        if (session.isConnected()) {
                            Log.d(TAG, "Thread[" + getId() + "] Reconnect Server[" + connector.getDefaultRemoteAddress().getHostName() +
                                    ":" + connector.getDefaultRemoteAddress().getPort() + "] Success");
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Thread[" + getId() + "] Reconnect Server failed, going to try again after 3 seconds");
                        e.printStackTrace();
                    }
                }
            }
        });
        //加一个处理粘包、断包的过滤器
        connector.getFilterChain().addLast("DMScodec", new ProtocolCodecFilter(new DMSCodecFactory()));

        connector.getSessionConfig().setTcpNoDelay(true);
        //设置读写都空闲时间：30秒
        connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30000);
        //设置读(接收通道)空闲时间：40秒
        //connector.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, 40000);
        //设置写(发送通道)空闲时间：50秒
        //connector.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, 50000);

        //设置处理器
        connector.setHandler(new MinaClientHandler(this, deviceInfo, applicationOperation));

        //设置默认访问地址
        connector.setDefaultRemoteAddress(new InetSocketAddress(serverIp, serverPort));

        while (true){
            if (requestQuit) {
                Log.d(TAG, "Thread[" + getId() + "] RequestQuit:" + requestQuit + " in main minaclientthread!");
                if (null != session) {
                    if (session.isConnected()) {
                        session.closeNow();
                    }
                }
                if (connector != null) {
                    connector.dispose();
                }
                break;
            }
            try {
                Log.d(TAG, "Thread[" + getId() + "] Try to connect server");
                ConnectFuture future = connector.connect();
                future.awaitUninterruptibly();
                session = future.getSession();
                Log.d(TAG, "Thread[" + getId() + "] Connect Server[" + connector.getDefaultRemoteAddress().getHostName() +
                        ":" + connector.getDefaultRemoteAddress().getPort() + "] Success, Time:" +
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                break;

            } catch (Exception e) {
                Log.e(TAG, "Thread[" + getId() + "] Connect Server[" + connector.getDefaultRemoteAddress().getHostName() +
                        ":" + connector.getDefaultRemoteAddress().getPort() + "] Failed, Time:" +
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                e.printStackTrace();
                if (!requestQuit) {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception ex) {
                        Log.e(TAG, "MinaClientThread[" + getId() + "] sleep error!");
                        e.printStackTrace();
                    }
                }
            }
        }
        Log.d(TAG, "MinaClientThread[" + getId() + "] end");
    }
}
