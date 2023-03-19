package com.meda.sejongbikehelper;

import java.io.Serializable;

public class Station implements Serializable {
    String id;
    String name;
    String bikeNum;
    String originBikeNum;
    Boolean notiAllow = false;

    public Station(String id, String name, String bikeNum) {
        this.id = id;
        this.name = name;
        this.bikeNum = bikeNum;
        this.notiAllow = false;
    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public String getId(){
        return id;
    }
    public void setId(String id){  this.id = id; }
    public String getBikeNum(){
        return bikeNum;
    }
    public void setBikeNum(String bikeNum){  this.bikeNum = bikeNum; }
    public String getOriginBikeNum(){
        return originBikeNum;
    }
    public void setOriginBikeNum(String originBikeNum){  this.originBikeNum = originBikeNum; }
    public Boolean getNotiAllow(){ return notiAllow; }
    public void setNotiAllow(Boolean notiAllow){ this.notiAllow = notiAllow; }

}
