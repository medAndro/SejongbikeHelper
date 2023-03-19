package com.meda.sejongbikehelper;

import java.io.Serializable;

public class GPS implements Serializable {
    String time;
    double distance;

    public GPS(String time, Double distance) {
        this.time = time;
        this.distance = distance;
    }
    public String getTime(){
        return time;
    }
    public void setTime(String time){
        this.time = time;
    }
    public double getDistance(){
        return distance;
    }
    public void setDistance(String time){
        this.distance = distance;
    }
}
