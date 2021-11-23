package com.brandon.scraper;

public class Settings {
    public Boolean notifsOn = true;

    public Settings(){}

    public Settings(Boolean notifsOn){
        this.notifsOn = notifsOn;
    }

    public void setNotifsOn(Boolean notifsOn){
        this.notifsOn = notifsOn;
    }

    public Boolean getNotifsOn(){
        return notifsOn;
    }

}
