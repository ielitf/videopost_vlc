package com.ceiv.BrtUtils;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.SearchRecentSuggestions;

import java.io.Serializable;
import java.util.ArrayList;

public class SegmentInfo implements Parcelable, Serializable {

    private final static long serialVersionUID = 2L;

    public final static int UPLINE = 1;
    public final static int DOWNLINE = 2;

    //必须有的字段：
    //票价
    private float RoutePrice;
    //运行方向  1：上行  2：下行  3：环形
    private int RunDirection;
    //单程下站点列表
    private ArrayList<StationItem> StationList;
    //首班车时间
    private String firstTime;
    //末班车时间
    private String lastTime;

    //非必须字段：
    //首末班描述
    private String FirtLastShiftInfo;
    //首末班描述2
    private String FirtLastShiftInfo2;
    //单程ID
    private String SegmentID;
    //单程名称
    private String SegmentName;

    public float getRoutePrice() {
        return RoutePrice;
    }

    public void setRoutePrice(float routePrice) {
        RoutePrice = routePrice;
    }

    public int getRunDirection() {
        return RunDirection;
    }

    public void setRunDirection(int runDirection) {
        RunDirection = runDirection;
    }

    public ArrayList<StationItem> getStationList() {
        return StationList;
    }

    public void setStationList(ArrayList<StationItem> stationList) {
        StationList = stationList;
    }

    public String getFirstTime() {
        return firstTime;
    }

    public void setFirstTime(String firstTime) {
        this.firstTime = firstTime;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public String getFirtLastShiftInfo() {
        return FirtLastShiftInfo;
    }

    public void setFirtLastShiftInfo(String firtLastShiftInfo) {
        FirtLastShiftInfo = firtLastShiftInfo;
    }

    public String getFirtLastShiftInfo2() {
        return FirtLastShiftInfo2;
    }

    public void setFirtLastShiftInfo2(String firtLastShiftInfo2) {
        FirtLastShiftInfo2 = firtLastShiftInfo2;
    }

    public String getSegmentID() {
        return SegmentID;
    }

    public void setSegmentID(String segmentID) {
        SegmentID = segmentID;
    }

    public String getSegmentName() {
        return SegmentName;
    }

    public void setSegmentName(String segmentName) {
        SegmentName = segmentName;
    }

    public boolean isValid() {
        if (RoutePrice > 0 && (RunDirection == 1 || RunDirection == 2) &&
                null != StationList && StationList.size() > 0 &&
                null != firstTime && !("".equals(firstTime)) &&
                null != lastTime && !("".equals(lastTime))) {
            return true;
        }
        return false;
    }

    public SegmentInfo() {
        RoutePrice = -1;
        RunDirection = -1;
    }

    public SegmentInfo(float price, int dir, ArrayList<StationItem> list, String firstTime,
                       String lastTime, String info, String info2, String ID, String name) {
        this.RoutePrice = price;
        this.RunDirection = dir;
        this.StationList = list;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.FirtLastShiftInfo = info;
        this.FirtLastShiftInfo2 = info2;
        this.SegmentID = ID;
        this.SegmentName = name;
    }

    public SegmentInfo(Parcel source) {

        RoutePrice = source.readFloat();
        RunDirection = source.readInt();
        //两种方法，注意要和下面的writeTypeList对应
        //source.readList(StationList, ArrayList.class.getClassLoader());
        source.readTypedList(StationList, StationItem.CREATOR);
        firstTime = source.readString();
        lastTime = source.readString();
        FirtLastShiftInfo = source.readString();
        FirtLastShiftInfo2 = source.readString();
        SegmentID = source.readString();
        SegmentName = source.readString();
    }

    public static final Creator<SegmentInfo> CREATOR = new Creator<SegmentInfo>() {
        @Override
        public SegmentInfo createFromParcel(Parcel source) {
            return new SegmentInfo(source);
        }

        @Override
        public SegmentInfo[] newArray(int size) {
            return new SegmentInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(RoutePrice);
        dest.writeInt(RunDirection);
        //这里必须和上面的对应
        dest.writeTypedList(StationList);
        dest.writeString(firstTime);
        dest.writeString(lastTime);
        dest.writeString(FirtLastShiftInfo);
        dest.writeString(FirtLastShiftInfo2);
        dest.writeString(SegmentID);
        dest.writeString(SegmentName);
    }
}
