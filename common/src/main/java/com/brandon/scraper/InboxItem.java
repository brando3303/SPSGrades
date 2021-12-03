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

    public boolean shouldShow(){
        if(deleted){
            return false;
        }
        int numNullPoints = 0;
        for(AssignmentChange ac : assignmentChanges){
            if(ac.points == null){
                numNullPoints++;
            }
        }
        if(numNullPoints == assignmentChanges.size()){
            return false;
        }
        return true;
    }
}
