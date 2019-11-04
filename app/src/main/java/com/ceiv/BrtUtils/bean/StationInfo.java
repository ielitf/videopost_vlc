package com.ceiv.BrtUtils.bean;

import java.util.List;

public class StationInfo {

    private String routeId;
    private String direction;
    private List<Stations> stations;

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public List<Stations> getStations() {
        return stations;
    }

    public void setStations(List<Stations> stations) {
        this.stations = stations;
    }

    @Override
    public String toString() {
        return "StationInfo{" +
                "routeId='" + routeId + '\'' +
                ", direction='" + direction + '\'' +
                ", stations=" + stations +
                '}';
    }
}
