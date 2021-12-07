package com.brandon.scraper;

public class AssignmentChange {
    public String assignmentChangeType;
    public String assignmentName;
    public Double assignmentPoints; // note that points will be null if type == "modified", and pointsBefore/After will be null if type == "created"
    public Double assignmentPointsBefore;
    public Double assignmentPointsNow;
    public Double assignmentTotal;
    public String courseName;
    public boolean deleted;
    public String id;
    public String overallGradeBefore;
    public String overallGradeNow;
    public Double time;
    public String timeReadable;

    public Course course;

    public boolean shouldDisplay(){
        return assignmentChangeType.equals("created") ? (assignmentPoints != null) : !deleted;
    }
}
