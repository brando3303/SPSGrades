package com.brandon.scraper;

public class Deserializer {
    public static Settings deserializeSettings(String[] oArray){
        Settings s =  new Settings();
        s.setNotifsOn(oArray[0].equals("on"));
        return s;
    }
    public static String[] serializeSettings(Settings settings){
        return new String[]{settings.notifsOn ? "on" : "off"};
    }
}
