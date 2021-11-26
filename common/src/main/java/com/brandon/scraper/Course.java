package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;
import com.codename1.components.SpanLabel;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.TableLayout;
//import com.sun.tools.javac.util.ArrayUtils; // This was giving me an error -Evan

import static com.codename1.ui.CN.log;

import java.util.*;

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
        if (total == 0 || points == null) {
            return "NA";
        }
        return Math.round((points/total)*10000)/100.0 + "%";
    }

    //creates new form which contains the assignments for this class
    public Form createCoursePage(){
        classPage = new Form(courseName, BoxLayout.y());
        Utils.setToolbarUIIDForSolidColor(classPage,"TitleArea");
        classPage.getToolbar().addMaterialCommandToLeftBar("Back",FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> {
            classPage.setVisible(false);
            Main.getInstance().getGradesForm().show();
        });

        for(Assignment a : assignments){
            SpanLabel name = new SpanLabel(a.name);
            Label points = new Label(a.points != null ? "" + a.points : "NA");
            Label total = new Label("" + a.total);
            Label percent = new Label(getPercentString(a.total, a.points));

            if(a.points != null || a.total == 0) {
                percent.getAllStyles().setFgColor(Grade.getGradeColor(a.points/a.total*100));
            }
            else{percent.getAllStyles().setFgColor(ColorUtil.GRAY);};


            Container gradeBox = BoxLayout.encloseY(points, total);
            gradeBox.setUIID("GradePercentV");


            Container assignmentTab = new Container( new TableLayout(1,2));
            assignmentTab.setUIID("GradeGrid");

            assignmentTab.add(percent).add(name);

            classPage.add(assignmentTab);
        }
        return classPage;
    }

    //this is convoluted and frankly I dont care
    public void sortAssignments(){
        long[] epochData = new long[assignments.size()];

        for(int i = 0; i < assignments.size(); i++){
            epochData[i] = assignments.get(i).epochDate;
        }
        Arrays.sort(epochData);
        for(long b : epochData){
            log(b  + "");
        }
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
