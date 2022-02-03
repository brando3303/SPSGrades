package com.brandon.scraper;

public class AssignmentChange {
    public String assignmentChangeType;
    public String assignmentName = "assignment";
    public Double assignmentPoints; // note that points will be null if type == "modified", and pointsBefore/After will be null if type == "created"
    public Double assignmentPointsBefore;
    public Double assignmentPointsNow;
    public Double assignmentTotal;
    public String courseName = "Course";
    public boolean deleted;
    public String id;
    public String overallGradeBefore = "NA";
    public String overallGradeNow = "NA";
    public Double time = 1643865717.;
    public String timeReadable = "time";

    public Course course;

    public boolean shouldDisplay(){
        if(assignmentChangeType.equals("created") && assignmentPoints == null){
            return false;
        }
        if(deleted){
            return false;
        }
        return true;
    }
}
