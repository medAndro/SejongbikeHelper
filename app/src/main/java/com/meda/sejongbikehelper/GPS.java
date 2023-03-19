package com.meda.sejongbikehelper;

import java.io.Serializable;

public class GPS implements Serializable {
    String time;

    public GPS(String time) {
        this.time = time;
    }
    public String getTime(){
        return time;
    }
    public void setTime(String time){
        this.time = time;
    }

}
