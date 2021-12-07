package com.brandon.scraper;

import java.util.ArrayList;
import java.util.List;

public class Student {

    public List<Course> courses;
    public Double GPA;
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

//    public Double calculateGPA(){
//        Double GPA = 0.0;
//        for(Course c : courses){
//            GPA += Double.parseDouble(c.gradePercent)/100;
//        }
//        GPA = GPA/courses.size()*4;
//        GPA = Math.round(GPA * 100.0) / 100.0;
//        this.GPA = GPA;
//        return GPA;
//    }

    public ArrayList<AssignmentChange> getAllAssignmentChanges(boolean deletedToo) {

        ArrayList<AssignmentChange> a = new ArrayList<>();
        if (deletedToo) {
            for (Course c : courses) {
                for (AssignmentChange ac : c.assignmentChanges) {
                    a.add(ac);
                }
            }
            return a;
        }
        for (Course c : courses) {
            for (AssignmentChange ac : c.assignmentChanges) {
                if(!ac.deleted && ac.shouldDisplay()) {
                    a.add(ac);
                }
            }
        }

        return a;
    }
}
