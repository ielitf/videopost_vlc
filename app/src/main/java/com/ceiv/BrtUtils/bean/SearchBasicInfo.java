package com.ceiv.BrtUtils.bean;

import java.util.List;

public class SearchBasicInfo {
    private String basicId;
    private String type;
    private String version;
    private String seqNo;
    private String currentTime;
    private String weather;
    private List <StationInfo> stationInfo;

    public String getBasicId() {
        return basicId;
    }

    public void setBasicId(String basicId) {
        this.basicId = basicId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(String seqNo) {
        this.seqNo = seqNo;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public List<StationInfo> getStationInfo() {
        return stationInfo;
    }

    public void setStationInfo(List<StationInfo> stationInfo) {
        this.stationInfo = stationInfo;
    }

    @Override
    public String toString() {
        return "SearchBasicInfo{" +
                "basicId='" + basicId + '\'' +
                ", type='" + type + '\'' +
                ", version='" + version + '\'' +
                ", seqNo='" + seqNo + '\'' +
                ", currentTime='" + currentTime + '\'' +
                ", weather='" + weather + '\'' +
                ", stationInfo=" + stationInfo +
                '}';
    }
}
