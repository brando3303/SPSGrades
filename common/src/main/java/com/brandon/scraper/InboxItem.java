package com.brandon.scraper;

import java.util.ArrayList;

public class InboxItem {
    public String index;
    public ArrayList<AssignmentChange> assignmentChanges;
    public String courseName;
    public Boolean deleted;
    public String gradeBefore;
    public String gradeNow;
    public Double time;
    public String timeReadable;

    public Course course;

    public InboxItem(){
        assignmentChanges = new ArrayList<>();
    }
}
