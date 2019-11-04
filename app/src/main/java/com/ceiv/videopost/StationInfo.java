package com.ceiv.videopost;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by zhangdawei on 2018/9/13.
 */

public class StationInfo {

    private final static String TAG = "StationInfo";

    public ArrayList<StationInfoItem> downline;
    public ArrayList<StationInfoItem> upline;

    public static class StationInfoItem {
        public String name;
        public String ename;
        public boolean upline;
        public int dualSerial;

        public StationInfoItem() {
            name = null;
            ename = null;
            upline = false;
            dualSerial = -1;
        }

        public StationInfoItem(String name, boolean upline, int dualSerial) throws Exception {
            if (null == name || "".equals(name)) {
                throw new Exception("Invalid Station name!");
            }
            this.name = name;
            this.upline = upline;
            this.dualSerial = dualSerial;
        }

        public StationInfoItem(String name, String ename, boolean upline, int dualSerial) throws Exception {
            if (null == name || "".equals(name)) {
                throw new Exception("Invalid Station name!");
            }
            this.name = name;
            this.ename = ename;
            this.upline = upline;
            this.dualSerial = dualSerial;
        }

        public boolean isValid() {
            if (dualSerial >= 0 && name != null && !("".equals(name)) && ename != null) {
                return true;
            }
            return false;
        }

    }

    public static class StationInfoComparator implements Comparator {
        //按照双程号升序排列
        @Override
        public int compare(Object o1, Object o2) {
            StationInfoItem item1 = (StationInfoItem) o1;
            StationInfoItem item2 = (StationInfoItem) o2;
            return item1.dualSerial - item2.dualSerial;
        }
    }

    //比较两个站点信息是否相同，暂时只比较双程号和中文名称
    public boolean equals(StationInfo target) {

        if (target == null) {
            return false;
        }
        ArrayList<StationInfoItem> tarDownline = target.downline;
        ArrayList<StationInfoItem> tarUpline = target.upline;
        ArrayList<StationInfoItem> srcDownline = this.downline;
        ArrayList<StationInfoItem> srcUpline = this.upline;
        StationInfoItem srcItem = null;
        StationInfoItem tarItem = null;

        if (srcDownline.size() != tarDownline.size() || srcUpline.size() != tarUpline.size()) {
            return false;
        }
        for (int i = 0; i < srcDownline.size(); i++) {
            srcItem = srcDownline.get(i);
            tarItem = tarDownline.get(i);
            //这里只比较双程号和站点名称
            if (srcItem.dualSerial != tarItem.dualSerial || !srcItem.name.equals(tarItem.name)) {
                return false;
            }
        }
        for (int i = 0; i < srcUpline.size(); i++) {
            srcItem = srcUpline.get(i);
            tarItem = tarUpline.get(i);
            //这里只比较双程号和站点名称
            if (srcItem.dualSerial != tarItem.dualSerial || !srcItem.name.equals(tarItem.name)) {
                return false;
            }
        }
        return true;
    }
}
