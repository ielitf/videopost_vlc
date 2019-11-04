package com.ceiv.BrtUtils;

import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

public class RouteInfo implements Parcelable, Serializable {
    private static final long serialVersionUID = 3L;

    //必须有的字段：
    //路线ID
    private String RouteID;
    //路线名称
    private String RouteName;
    //具体路线的数据
    //上行
    private SegmentInfo upline;
    //下行
    private SegmentInfo downline;

    //非必须字段：
    //是否快车
    private boolean IsBRT;

    public String getRouteID() {
        return RouteID;
    }

    public void setRouteID(String routeID) {
        RouteID = routeID;
    }

    public String getRouteName() {
        return RouteName;
    }

    public void setRouteName(String routeName) {
        RouteName = routeName;
    }

    public SegmentInfo getUpline() {
        return upline;
    }

    public void setUpline(SegmentInfo upline) {
        this.upline = upline;
    }

    public SegmentInfo getDownline() {
        return downline;
    }

    public void setDownline(SegmentInfo downline) {
        this.downline = downline;
    }

    public boolean isBRT() {
        return IsBRT;
    }

    public void setBRT(boolean BRT) {
        IsBRT = BRT;
    }

    public boolean isValid() {
        if (null != RouteID && !("".equals(RouteID)) &&
                null != RouteName && !("".equals(RouteName)) &&
                null != upline && null != downline) {
            return true;
        }
        Log.d("RouteInfoTest", "RoutID:" + RouteID +
                " RouteName:" + RouteName +
                " upline null ? " + (null == upline ? "yes" : "no") +
                " downline null ? " + (null == downline ? "yes" : "no"));
        return false;
    }

    public RouteInfo() {

    }

    public RouteInfo(String ID, String name, SegmentInfo upline, SegmentInfo downline, boolean isBRT) {
        this.RouteID = ID;
        this.RouteName = name;
        this.upline = upline;
        this.downline = downline;
        this.IsBRT = isBRT;
    }

    public RouteInfo(Parcel in) {
        RouteID = in.readString();
        RouteName = in.readString();
        Parcelable[] parcelables = in.readParcelableArray(SegmentInfo.class.getClassLoader());
        //两种方法，必须和下面的对应
//        if (parcelables != null) {
//            segmentInfos = Arrays.copyOf(parcelables, parcelables.length, SegmentInfo[].class);
//        }
//        in.readTypedArray(segmentInfos, SegmentInfo.CREATOR);
        upline = in.readParcelable(SegmentInfo.class.getClassLoader());
        downline = in.readParcelable(SegmentInfo.class.getClassLoader());
        IsBRT = in.readByte() == 1;
    }

    public final static Creator<RouteInfo> CREATOR = new Creator<RouteInfo>() {
        @Override
        public RouteInfo createFromParcel(Parcel source) {
            return new RouteInfo(source);
        }

        @Override
        public RouteInfo[] newArray(int size) {
            return new RouteInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(RouteID);
        dest.writeString(RouteName);
        //两种方法，必须和上面的对应
        //dest.writeParcelableArray(segmentInfos, flags);
        //dest.writeTypedArray(segmentInfos, flags);
        dest.writeParcelable(upline, flags);
        dest.writeParcelable(downline, flags);
        dest.writeByte((byte) (IsBRT ? 1 : 0));
    }

    public boolean equals(RouteInfo routeInfo) {

        if (null == routeInfo) {
            return false;
        }
        ByteArrayOutputStream bo1 = null;
        ObjectOutputStream oo1 = null;
        ByteArrayOutputStream bo2 = null;
        ObjectOutputStream oo2 = null;
        byte[] arr1 = null;
        byte[] arr2 = null;
        try {
            bo1 = new ByteArrayOutputStream();
            oo1 = new ObjectOutputStream(bo1);
            oo1.writeObject(this);
            arr1 = bo1.toByteArray();
            bo2 = new ByteArrayOutputStream();
            oo2 = new ObjectOutputStream(bo2);
            oo2.writeObject(routeInfo);
            arr2 = bo2.toByteArray();
            if (Arrays.equals(arr1, arr2)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}













