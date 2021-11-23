package com.brandon.scraper;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.TableLayout;

import java.util.ArrayList;
import java.util.List;

public class Course {

    public String courseName;
    public String frn;
    public String gradeLetter;
    public String gradePercent;
    public Double period;
    public String teacher;

    public List<Assignment> assignments;


    private Form classPage;
    public Container tab;


    public Course(){

        assignments = new ArrayList<>();


    }

    public Form getCoursePage(){
        return classPage;

    }

    public static String getPercentString(Double total, Double points){
        return (points == null) ? "NA" : Math.round((points/total)*10000)/100.0 + "%";
    }

    //creates new form which contains the assignments for this class
    public Form createCoursePage(){
        classPage = new Form(courseName, BoxLayout.y());
        classPage.getToolbar().addMaterialCommandToLeftBar("Back",FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> {
            classPage.setVisible(false);
            Main.getInstance().getGradesForm().show();
        });

        for(Assignment a : assignments){
            Label name = new Label(a.name);
            Label points = new Label(a.points != null ? "" + a.points : "NA");
            Label total = new Label("" + a.total);
            Label percent = new Label(getPercentString(a.total, a.points));

            Container gradeBox = BoxLayout.encloseY(points, total);
            gradeBox.setUIID("GradePercentV");


            Container assignmentTab = new Container( new TableLayout(1,2));
            assignmentTab.setUIID("GradeGrid");

            assignmentTab.add(percent).add(name);

            classPage.add(assignmentTab);
        }
        return classPage;
    }
}
