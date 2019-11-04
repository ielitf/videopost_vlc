package com.ceiv.BrtUtils.bean;

public class Stations {
    private String id;
    private String name;
    private String nameEn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @Override
    public String toString() {
        return "Stations{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", nameEn='" + nameEn + '\'' +
                '}';
    }
}
