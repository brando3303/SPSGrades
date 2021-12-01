package com.brandon.scraper;

import com.brandon.scraper.formController.CourseFC;
import com.codename1.ui.Container;

import java.util.*;

public class Course {
    private Student courseOwner;

    public String courseName;
    public String frn;
    public String gradeLetter;
    public String gradePercent;
    public String period;
    public String teacher;

    public List<Assignment> assignments;

    private CourseFC courseFC;
    public Container tab;

    public Course(Student courseOwner){

        assignments = new ArrayList<>();
        this.courseOwner = courseOwner;
        courseFC = new CourseFC(Main.getInstance(),this);

    }

    public CourseFC getCourseFormC(){
        return courseFC;

    }

    public static String getPercentString(Double total, Double points){
        if (total == 0 || points == null) {
            return "NA";
        }
        return Math.round((points/total)*10000)/100.0 + "%";
    }

    //this is convoluted and frankly I dont care
    public void sortAssignments(){
        long[] epochData = new long[assignments.size()];

        for(int i = 0; i < assignments.size(); i++){
            epochData[i] = assignments.get(i).epochDate;
        }
        Arrays.sort(epochData);
        ArrayList<Assignment> sortedAssignments = new ArrayList<>();
        for(long e : epochData){
            for(Iterator<Assignment> iterator = assignments.listIterator(); iterator.hasNext();){
                Assignment a = iterator.next();
                if(e == a.epochDate){
                    sortedAssignments.add(a);
                    assignments.remove(a);
                    break;
                }
            }
        }
        Collections.reverse(sortedAssignments);
        assignments = sortedAssignments;

    }
}
