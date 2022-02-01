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
    public ArrayList<AssignmentChange> assignmentChanges;

    private CourseFC courseFC;
    public Container tab;

    public Course(Student courseOwner){

        assignments = new ArrayList<>();
        assignmentChanges = new ArrayList<>();
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
        return Utils.intify(Math.round((points/total)*1000)/10.0) + "%";
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

    public void sortAssignmentChanges(){
        Double[] epochData = new Double[assignmentChanges.size()];

        for(int i = 0; i < assignmentChanges.size(); i++){
            epochData[i] = assignmentChanges.get(i).time;
        }
        Arrays.sort(epochData);
        ArrayList<AssignmentChange> sortedAssignmentChanges = new ArrayList<>();
        for(Double e : epochData){
            for(Iterator<AssignmentChange> iterator = assignmentChanges.listIterator(); iterator.hasNext();){
                AssignmentChange a = iterator.next();
                if(e == a.time){
                    sortedAssignmentChanges.add(a);
                    assignmentChanges.remove(a);
                    break;
                }
            }
        }
        Collections.reverse(sortedAssignmentChanges);
        assignmentChanges = sortedAssignmentChanges;

    }

    public Double getLowestOverallGradeInbox(){
        Double lowest = 10000.;
        for(AssignmentChange ac : assignmentChanges){
            //TODO handle a null lowest value
            if(ac.overallGradeBefore != null && Double.parseDouble(ac.overallGradeBefore) < lowest && ac.shouldDisplay()){
                lowest = Double.parseDouble(ac.overallGradeBefore);
            }
        }
        if(lowest == 10000){
            return -1.;
        }
        return lowest;
    }

    public void deleteInbox(){
        for(AssignmentChange ac : assignmentChanges){
            if(!ac.deleted){
                ScraperServer.deleteAssignmentChange(ac,courseOwner);
                ac.deleted = true;
            }
        }
    }

    public int getInboxSize(){
        int size = 0;
        for(AssignmentChange ac : assignmentChanges){
            if(ac.shouldDisplay()){
                size++;
            }
        }
        return size;
    }
}
