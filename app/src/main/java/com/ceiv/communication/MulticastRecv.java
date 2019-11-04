package com.ceiv.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by zhangdawei on 2018/5/18.
 */

public class MulticastRecv {

    private final static String TAG = "MulticastRecv";

    private static int multicastPort;
    private static String multicastIp;

    private MulticastSocket socket;

    private byte[] recvBuffer;
    private DatagramPacket packet;

    private NetworkInterface eth0;
    private InetAddress address;

    public MulticastRecv(String ip, int port) throws Exception {

        multicastIp = ip;
        multicastPort = port;

        address = InetAddress.getByName(multicastIp);
        socket = new MulticastSocket(multicastPort);

        recvBuffer = new byte[1024];
        packet = new DatagramPacket(recvBuffer, recvBuffer.length, address, multicastPort);

        Enumeration enumeration = NetworkInterface.getNetworkInterfaces();

        boolean findEth0 = false;
        while (enumeration.hasMoreElements()) {
            eth0 = (NetworkInterface) enumeration.nextElement();
            if (eth0.getName().equals("eth0")) {
                findEth0 = true;
                break;
            }
        }

        if (findEth0) {
            try {
                socket.joinGroup(new InetSocketAddress(address, multicastPort), eth0);
            } catch (IOException e) {
                throw new Exception("join Multicast group failed!");
            }
        } else {
            throw new Exception("can't find eth0 Network Interface!");
        }

    }

    public byte[] receiveMulticast() throws Exception {

        socket.receive(packet);
        byte[] recv = new byte[packet.getLength()];

        System.arraycopy(recvBuffer, 0, recv, 0, packet.getLength());

        return recv;
    }

    public void leaveMulticastGroup() throws Exception {

        socket.leaveGroup(new InetSocketAddress(address, multicastPort), eth0);

    }

}
