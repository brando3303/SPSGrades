package com.brandon.scraper;

import java.util.ArrayList;
import java.util.List;

public class Student {

    public List<Course> courses;
    public Inbox inbox;
    private Settings settings;

    private String username;
    private String password;

    public Settings getSettings() {
        return settings;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Student(String username, String password){
        courses = new ArrayList<>();
        this.settings = new Settings();
        this.username = username;
        this.password = password;
    }

    public void setSettings(Settings settings){
        this.settings = settings;
    }
}
