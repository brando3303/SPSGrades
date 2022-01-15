package com.brandon.scraper.formController;

import com.brandon.scraper.Assignment;
import com.brandon.scraper.Course;
import com.brandon.scraper.Main;
import com.brandon.scraper.Utils;
import com.codename1.components.Accordion;
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
        this.form = new Form(course.courseName + " - " + course.gradePercent + "%", BoxLayout.y());
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
            Label percent = Utils.createFrationLabel(a.points,a.total,true,null);

            Accordion dropDown = new Accordion();
            dropDown.setScrollableY(false);
            dropDown.setBackgroundItemUIID("GradeGrid");
            dropDown.setHeaderUIID("AssignmentDropDown");

            Container gradeBox = BoxLayout.encloseY(points, total);
            gradeBox.setUIID("GradePercentV");


            Container assignmentTab = new Container(new TableLayout(1, 2));

            assignmentTab.add(((TableLayout)assignmentTab.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(75),name)
                    .add(((TableLayout)assignmentTab.getLayout()).createConstraint().horizontalAlign(Table.RIGHT),percent);

            dropDown.addContent(assignmentTab,new SpanLabel(a.name));
            this.form.add(dropDown);
        }
        isCreated = true;
        return this.form;
    }
    public boolean getIsCreated(){
        return isCreated;
    }
}
