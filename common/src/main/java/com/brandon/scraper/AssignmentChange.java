package com.brandon.scraper;

public class AssignmentChange {
    public String name;
    public Double points; // note that points will be null if type == "modified", and pointsBefore/After will be null if type == "created"
    public Double pointsBefore;
    public Double pointsNow;
    public Double total;
    public String type;
}
