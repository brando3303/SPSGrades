package com.brandon.scraper.formController;

import com.brandon.scraper.*;
import com.codename1.charts.util.ColorUtil;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.Table;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.SwipeBackSupport;

public class CourseFC extends FormController {
    private Course course;
    private boolean isCreated = false;

    public CourseFC(Main appInstance, Course course){
        super(appInstance);
        this.course = course;

    }

    @Override
    public Form start() {
        return createCoursePage();
    }

    private Form createCoursePage() {
        this.form = new Form(course.courseName, BoxLayout.y());
        Utils.setToolbarUIIDForSolidColor(this.form, "TitleArea");

        SwipeBackSupport.bindBack(this.form, args -> this.getApp().getGradesFC().getForm());

        this.form.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 200));
        this.form.getToolbar().addMaterialCommandToLeftBar("Back", FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> {
                    this.form.setVisible(false);
                    this.getApp().getGradesFC().show();
                });

        for (Assignment a : course.assignments) {
            Label name = new Label(a.name);
            name.setUIID("AssignmentName");
            Label points = new Label(a.points != null ? "" + a.points : "NA");
            Label total = new Label("" + a.total);
            Label percent = new Label(Course.getPercentString(a.total, a.points));

            if (a.points != null || a.total == 0) {
                percent.getAllStyles().setFgColor(Grade.getGradeColor(a.points / a.total * 100));
            } else {
                percent.getAllStyles().setFgColor(ColorUtil.GRAY);
            }


            Container gradeBox = BoxLayout.encloseY(points, total);
            gradeBox.setUIID("GradePercentV");


            Container assignmentTab = new Container(new TableLayout(1, 2));
            assignmentTab.setUIID("GradeGrid");

            assignmentTab.add(((TableLayout)assignmentTab.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(80),name).add(percent);

            this.form.add(assignmentTab);
        }
        isCreated = true;
        return this.form;
    }
    public boolean getIsCreated(){
        return isCreated;
    }
}
