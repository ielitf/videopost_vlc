package com.ceiv.BrtUtils;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Comparator;

public class StationItem implements Parcelable, Serializable {

    private final static long serialVersionUID = 1L;
    //必须有的字段：
    //从StationID字段中提取出来的该站点在规划站点列表中的序号（即我们平常称呼的几号站）
    private int StationNum;
    //双程号
    private int DualSerial;
    //站点名称
    private String StationName;
    //站点英文名称
    private String StationEName;

    //非必须字段：
    //目前和海信商议结果是在Stationmemo字段描述（中文名|英文名）
    private String Stationmemo;
    //站点唯一编码
    private String StationID;
    //该站点在此线路中的序号
    private String StationNO;
    //站点位置信息
    //经度
    private float Longitude;
    //纬度
    private float Latitude;

    public boolean isValid() {
        if (StationNum > 0 && DualSerial > 0
                && StationEName != null && !("".equals(StationEName))
                && StationName != null && !("".equals(StationName))) {
            return true;
        }
        return false;
    }


    public StationItem() {
        StationNum = -1;
        DualSerial = -1;
    }

    public StationItem(int num, int dualSerial, String memo, String ename, String ID, String name, String NO, float longitude, float latitude) {
        this.StationNum = num;
        this.DualSerial = dualSerial;
        this.Stationmemo = memo;
        this.StationEName = ename;
        this.StationID = ID;
        this.StationName = name;
        this.StationNO = NO;
        this.Longitude = longitude;
        this.Latitude = latitude;
    }

    public StationItem(Parcel in) {
        StationNum = in.readInt();
        DualSerial = in.readInt();
        Stationmemo = in.readString();
        StationEName = in.readString();
        StationID = in.readString();
        StationName = in.readString();
        StationNO = in.readString();
        Longitude = in.readFloat();
        Latitude = in.readFloat();
    }

    public int getStationNum() {
        return StationNum;
    }

    public void setStationNum(int stationNum) {
        StationNum = stationNum;
    }

    public int getDualSerial() {
        return DualSerial;
    }

    public void setDualSerial(int dualSerial) {
        DualSerial = dualSerial;
    }

    public String getStationmemo() {
        return Stationmemo;
    }

    public void setStationmemo(String stationmemo) {
        Stationmemo = stationmemo;
    }

    public String getStationEName() {
        return StationEName;
    }

    public void setStationEName(String stationEName) {
        this.StationEName = stationEName;
    }

    public String getStationID() {
        return StationID;
    }

    public void setStationID(String stationID) {
        StationID = stationID;
    }

    public String getStationName() {
        return StationName;
    }

    public void setStationName(String stationName) {
        StationName = stationName;
    }

    public String getStationNO() {
        return StationNO;
    }

    public void setStationNO(String stationNO) {
        StationNO = stationNO;
    }

    public float getLongitude() {
        return Longitude;
    }

    public void setLongitude(float longitude) {
        Longitude = longitude;
    }

    public float getLatitude() {
        return Latitude;
    }

    public void setLatitude(float latitude) {
        Latitude = latitude;
    }

    public static final Creator<StationItem> CREATOR = new Creator<StationItem>() {
        @Override
        public StationItem createFromParcel(Parcel source) {
            return new StationItem(source);
        }

        @Override
        public StationItem[] newArray(int size) {
            return new StationItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(StationNum);
        dest.writeInt(DualSerial);
        dest.writeString(Stationmemo);
        dest.writeString(StationEName);
        dest.writeString(StationID);
        dest.writeString(StationName);
        dest.writeString(StationNO);
        dest.writeFloat(Longitude);
        dest.writeFloat(Latitude);
    }


    public static Comparator<StationItem> StationItemComparator = new Comparator<StationItem>() {
        @Override
        public int compare(StationItem o1, StationItem o2) {
            //按照双程号升序排列
            StationItem item1 = (StationItem) o1;
            StationItem item2 = (StationItem) o2;
            return item1.getDualSerial() - item2.getDualSerial();
        }
    };


}
