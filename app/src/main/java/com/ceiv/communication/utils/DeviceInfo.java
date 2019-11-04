package com.ceiv.communication.utils;

import android.util.Log;

import java.util.HashMap;

/**
 * Created by zhangdawei on 2018/8/16.
 */

public class DeviceInfo {

    public DeviceInfo() {
        identify = null;
        serverIp = null;
        serverPort = -1;
        infoPublishServer = null;
        devType = -1;
        currentStationId = -1;
        nextStationId = -1;
        themeStyle = -1;
        textType = -1;
        textColor = 0x0;
        textSize = -1;
        textFont = null;
        textSpeed = -1;
        picDispTime = -1;
        videoPlayTime = -1;
        flag = 0x0;
    }

    // bit0
    private String identify;
    // bit1
    private String serverIp;
    // bit2
    private int serverPort;
    // bit3
    private String infoPublishServer;
    // bit4
    private int devType;
    // bit5
    private int currentStationId;
    // bit6
    private int nextStationId;
    // bit7
    private int themeStyle;
    // bit8
    private int textType;
    // bit9
    private int textColor;      //ARGB: #FFFFFFFF 为完全不透明的白色
    // bit10
    private int textSize;       //0: 小  1: 中  2: 大
    // bit11
    private String textFont;    //heiti: 黑体 songti: 宋体 kaiti: 楷体 lishu: 隶书
    // bit12
    private int textSpeed;      //0: 慢速  1: 正常  2: 快速
    // bit13
    private int picDispTime;
    // bit14
    private int videoPlayTime;


    // long 为64位，可表示64个字段的状态， 0表示未设置， 1表示已设置
    private long flag;

    public synchronized void setIdentify(String identify) {
        if (null != identify && !("".equals(identify))) {
            this.identify = identify;
            flag |= 0x01;
        }
    }

    public boolean isIdentifySet() {
        return (flag & 0x01) != 0;
    }

    public String getIdentify() {
        return identify;
    }

    public synchronized void setServerIp(String serverIp) {
        if (null != serverIp && !("".equals(serverIp))) {
            this.serverIp = serverIp;
            flag |= 0x02;
        }
    }

    public boolean isServerIpSet() {
        return (flag & 0x02) != 0;
    }

    public String getServerIp() {
        return serverIp;
    }

    public synchronized void setServerPort(int serverPort) {
        if (serverPort > 0) {
            this.serverPort = serverPort;
            flag |= 0x04;
        }
    }

    public boolean isServerPortSet() {
        return (flag & 0x04) != 0;
    }

    public int getServerPort() {
        return serverPort;
    }

    public synchronized void setInfoPublishServer(String infoPublishServer) {
        if (null != infoPublishServer && !("".equals(infoPublishServer))) {
            this.infoPublishServer = infoPublishServer;
            flag |= 0x08;
        }
    }

    public boolean isInfoPublishServerSet() {
        return (flag & 0x08) != 0;
    }

    public String getInfoPublishServer() {
        return infoPublishServer;
    }

    public synchronized void setDevType(int devType) {
        if (devType > 0) {
            this.devType = devType;
            flag |= 0x10;
        }
    }

    public boolean isDevTypeSet() {
        return (flag & 0x10) != 0;
    }

    public int getDevType() {
        return devType;
    }

    public synchronized void setCurrentStationId(int currentStationId) {
        if (currentStationId >= 0) {
            this.currentStationId = currentStationId;
            flag |= 0x20;
        }
    }

    public boolean isCurrentStationIdSet() {
        return (flag & 0x20) != 0;
    }

    public int getCurrentStationId() {
        return currentStationId;
    }

    public synchronized void setNextStationId(int nextStationId) {
        if (nextStationId >= 0) {
            this.nextStationId = nextStationId;
            flag |= 0x40;
        }
    }

    public boolean isNextStationIdSet() {
        return (flag & 0x40) != 0;
    }

    public int getNextStationId() {
        return nextStationId;
    }

    public synchronized void setThemeStyle(int themeStyle) {
        if (1 == themeStyle || 2 == themeStyle || 3 == themeStyle || 4 == themeStyle) {
            this.themeStyle = themeStyle;
            flag |= 0x80;
        }
    }

    public boolean isThemeStyleSet() {
        return (flag & 0x80) != 0;
    }

    public int getThemeStyle() {
        return themeStyle;
    }

    public synchronized void setTextType(int textType) {
        if (textType >= 0) {
            this.textType = textType;
            flag |= 0x100;
        }
    }

    public boolean isTextTypeSet() {
        return (flag & 0x100) != 0;
    }

    public int getTextType() {
        return textType;
    }

    public synchronized void setTextColor(int textColor) {
        this.textColor = textColor;
        flag |= 0x200;
    }

    public boolean isTextColorSet() {
        return (flag & 0x200) != 0;
    }

    public int getTextColor() {
        return textColor;
    }

    public synchronized void setTextSize(int textSize) {
        if (textSize == 0 || textSize == 1 || textSize == 2) {
            this.textSize = textSize;
            flag |= 0x400;
        }
    }

    public boolean isTextSizeSet() {
        return (flag & 0x400) != 0;
    }

    public int getTextSize() {
        return textSize;
    }

    public synchronized void setTextFont(String textFont) {
        if (SystemInfoUtils.fontToName.containsKey(textFont)) {
            this.textFont = textFont;
            flag |= 0x800;
        }
    }

    public boolean isTextFontSet() {
        return (flag & 0x800) != 0;
    }

    public String getTextFont() {
        return textFont;
    }

    public synchronized void setTextSpeed(int textSpeed) {
        if (0 == textSpeed || 1 == textSpeed || 2 == textSpeed) {
            this.textSpeed = textSpeed;
            flag |= 0x1000;
        }
    }

    public boolean isTextSpeedSet() {
        return (flag & 0x1000) != 0;
    }

    public int getTextSpeed() {
        return textSpeed;
    }

    public synchronized void setPicDispTime(int picDispTime) {
        if (picDispTime > 0) {
            this.picDispTime = picDispTime;
            flag |= 0x2000;
        }
    }

    public boolean isPicDispTimeSet() {
        return (flag & 0x2000) != 0;
    }

    public int getPicDispTime() {
        return picDispTime;
    }

    public synchronized void setVideoPlayTime(int videoPlayTime) {
        if (videoPlayTime > 0) {
            this.videoPlayTime = videoPlayTime;
            flag |= 0x4000;
        }
    }

    public boolean isVideoPlayTimeSet() {
        return (flag & 0x4000) != 0;
    }

    public int getVideoPlayTime() {
        return videoPlayTime;
    }

    public long getInfoFlag() {
        return flag;
    }

    public boolean isInfoComplete() {
        Log.e("==========", "flag："+ flag);
//        return (0x7FFF == flag);
        return true;
    }

}
