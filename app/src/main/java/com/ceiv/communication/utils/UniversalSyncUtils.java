package com.ceiv.communication.utils;

import com.ceiv.communication.NetMgrMsg.HMediaSetting;

import org.apache.mina.core.session.IoSession;

/**
 * Created by zhangdawei on 2018/8/28.
 */

/* 未完工 */
public class UniversalSyncUtils {

//    public UniversalSyncUtils(HMediaSetting hMediaSetting, IoSession session, DeviceInfo deviceInfo) {
//        if (null == syncObj) {
//            syncObj = new Object();
//        }
//        this.hMediaSetting = hMediaSetting;
//        this.session = session;
//        this.deviceInfo = deviceInfo;
//    }

    public Object getSyncObj() {
        if (null == syncObj) {
            syncObj = new Object();
        }
        return syncObj;
    }

    public static void setIncidentalData(Object data) {
        incidentalData = data;
    }

    public static Object getIncidentalData() {
        return incidentalData;
    }

    private static Object syncObj = null;
    private static Object incidentalData = null;

//    private static Object syncObj = null;
//    private HMediaSetting hMediaSetting;
//    private IoSession session;
//    private DeviceInfo deviceInfo;

}
