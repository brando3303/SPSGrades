package com.brandon.scraper.formController;

import com.brandon.scraper.*;
import com.codename1.charts.util.ColorUtil;
import com.codename1.components.SpanLabel;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.*;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.Table;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.SwipeBackSupport;
import org.littlemonkey.connectivity.Connectivity;

import java.util.Date;

import static com.codename1.ui.CN.log;

public class InboxFC extends FormController {

    public InboxFC(Main app) {
        super(app);
    }

    @Override
    public Form start() {
        return createInboxForm();
    }

    private Form createInboxForm() {
        //setting up title bar
        Student currentUser = this.getApp().getCurrentUser();
        Main app = this.getApp();
        Form inboxForm = new Form("Inbox", BoxLayout.y());
        this.form = inboxForm;
        Utils.setToolbarUIIDForSolidColor(inboxForm,"TitleArea");
        SwipeBackSupport.bindBack(inboxForm, args -> this.getApp().getGradesFC().getForm());
        inboxForm.getToolbar().addMaterialCommandToLeftBar("Back", FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> {
                    inboxForm.setVisible(false);
                    this.getApp().getGradesFC().show();

                });
        inboxForm.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL,true, 200));

        inboxForm.getToolbar().addMaterialCommandToRightBar("Clear", FontImage.MATERIAL_CLEAR_ALL, 4,
                e -> {
                    Command ok = new Command("Ok"){
                        @Override
                        public void actionPerformed(ActionEvent evt){
                            inboxForm.removeAll();
                            Label message = new Label("Grade changes will appear here");
                            message.setUIID("EmptyInboxMessage");
                            inboxForm.add(message);
                            inboxForm.show();
                            for (Course c : currentUser.courses){
                                c.deleteInbox();
                            }
                            app.getGradesFC().updateInboxButtonBadge();
                        }
                    };

                    Dialog.show("Clear Inbox?","You can't undo this", ok, new Command("Cancel"));

                });

        //if there are no inbox items
        if(currentUser.getAllAssignmentChanges(false).size() == 0){
            Label message = new Label("Grade changes will appear here");
            message.setUIID("EmptyInboxMessage");
            inboxForm.add(message);
            return this.form;
        }
        //create each course's inbox container
        for(Course c : currentUser.courses){
            if(c.getInboxSize() != 0){
                this.form.add(createCourseInboxItemComponent(c));
            }
        }
        return this.form;
    }

    private Component createCourseInboxItemComponent(Course course){
        //main container
        log(course.courseName);
        Container inboxItemContainer = BoxLayout.encloseY();
        inboxItemContainer.setUIID("GradeGrid");

        //the title container
        Container courseInboxTitle = TableLayout.encloseIn(3);


        //selectable name: the name of the class which has been changed
        Container selectableTitle = new Container();
        Button selectInboxItem = new Button();
        selectInboxItem.addActionListener(e -> {
            log("you clicked on an inbox item!!!");
            this.getApp().getGradesFC().selectClass(course);

        });
        selectableTitle.setLeadComponent(selectInboxItem);
        Label courseName = new Label(course.courseName);
        selectableTitle.add(courseName);
        courseName.setUIID("CourseName");

        //the overall grade change


        Label overallGradeChange = createOverallGradeChangeLabel(course);

        //the delete inbox items button
        Button delete = new Button("✕");
        delete.getAllStyles().setFgColor(ColorUtil.GRAY,true);

        delete.addActionListener(e -> {
            inboxItemContainer.remove();
            if(Connectivity.isConnected()) {
                course.deleteInbox();
                this.getApp().getGradesFC().updateInboxButtonBadge();
                this.form.setTransitionOutAnimator(null);
                createInboxForm().show();
            }

            this.form.show();
        });

        courseInboxTitle.add(((TableLayout)courseInboxTitle.getLayout())
                        .createConstraint()
                        .widthPercentage(55),
                        selectableTitle)
                .add(((TableLayout)courseInboxTitle.getLayout())
                .createConstraint()
                .widthPercentage(30),overallGradeChange)
                .add(((TableLayout)courseInboxTitle.getLayout())
                        .createConstraint()
                        .widthPercentage(15),delete);

        //table of changed assignments: the assignment name and the way in which it changed
        Container assignmentTable = new Container(new TableLayout(course.assignmentChanges.size(),4));
        assignmentTable.setUIID("AssignmentChangeTable");

        int createdPlusWidth = 6;
        int assignmentNameWidth = 42;
        int fractionGradeWidth = 38;
        for(AssignmentChange ac : course.assignmentChanges){
            if(!ac.shouldDisplay()){
                continue;
            }


            Label greenPlus = new Label();
            greenPlus.setUIID("AssignmentChangePointsLabel");
            greenPlus.getAllStyles().setFgColor(Grade.A.getColor(), true);
            if(ac.assignmentChangeType.equals("created")){
                greenPlus.setText("+");
            } else{
                greenPlus.setText(" ");
            }
            assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(createdPlusWidth),greenPlus);


            SpanLabel assignmentNameLabel = new SpanLabel(ac.assignmentName);
            assignmentNameLabel.setTextUIID("AssignmentChangeLabel");
            assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(assignmentNameWidth-createdPlusWidth),assignmentNameLabel);

            if(ac.assignmentChangeType.equals("created") && ac.assignmentPoints != null){

                Label newAssignmentGradeLabel = new Label(Utils.intify(ac.assignmentPoints) + "/" + Utils.intify(ac.assignmentTotal));
                newAssignmentGradeLabel.setUIID("AssignmentChangePointsLabel");
                newAssignmentGradeLabel.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.assignmentPoints, ac.assignmentTotal), true);
                assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(fractionGradeWidth), newAssignmentGradeLabel);
            }
            else if(ac.assignmentChangeType.equals("modified")){

                Container assignmentGradeChange = new Container(new TableLayout(1,3));
                assignmentGradeChange.setUIID("AssignmentGradeChangeContainer");

                Label oldGrade = new Label(Utils.intify(ac.assignmentPointsBefore) + "/" + Utils.intify(ac.assignmentTotal));
                oldGrade.setUIID("AssignmentChangePointsLabel");
                oldGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.assignmentPointsBefore, ac.assignmentTotal), true);

                // Label newGrade = new Label(ac.pointsNow + "/" + ac.total);
                Label newGrade = new Label(Utils.intify(ac.assignmentPointsNow) + "/" + Utils.intify(ac.assignmentTotal));
                newGrade.setUIID("AssignmentChangePointsLabel");
                newGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.assignmentPointsNow, ac.assignmentTotal), true);

                Label arrowImage = new Label("▶");
                arrowImage.setUIID("AssignmentChangeArrow");
                if(ac.assignmentPointsNow > ac.assignmentPointsBefore){
                    arrowImage.getAllStyles().setFgColor(Grade.A.getColor());
                }
                if(ac.assignmentPointsNow < ac.assignmentPointsBefore){
                    arrowImage.getAllStyles().setFgColor(Grade.E.getColor());
                }
                assignmentGradeChange.add(oldGrade).add(arrowImage).add(newGrade);
                assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(fractionGradeWidth),assignmentGradeChange);
            }
            Label timeStamp = new Label(createTimeStamp(ac.time));
            timeStamp.setUIID("TimeStampText");
            assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(100-fractionGradeWidth-assignmentNameWidth),timeStamp);
        }




        //adding all of the components to the inbox item container
        inboxItemContainer.add(courseInboxTitle);
        inboxItemContainer.add(assignmentTable);
        //inboxItemContainer.add(((TableLayout)inboxItemContainer.getLayout()).createConstraint().horizontalAlign(Component.RIGHT).widthPercentage(13),overallGradeChanges);
        //inboxItemContainer.add(delete);

        return inboxItemContainer;
    }

    public static String createTimeStamp(Double pstEpochTime){
        Date dif = new Date(Math.abs(new Date().getTime() - pstEpochTime.longValue()*1000) + 8*60*60*1000);

        //minus one because "DD" is days in the year starting with 1
        int days = Integer.parseInt(new SimpleDateFormat("DD").format(dif)) - 1;
        int hours = Integer.parseInt(new SimpleDateFormat("H").format(dif));
        int minutes =  Integer.parseInt(new SimpleDateFormat("mm").format(dif));
        String timeStampText;
        if(days > 1){
            timeStampText = days + " days ago";
        } else if(days == 1){
            timeStampText = days + " day ago";
        } else if(hours > 1){
            timeStampText = hours + " hrs ago";
        }else if(hours == 1){
            timeStampText = hours + " hour ago";
        } else{
            timeStampText = minutes + " mins ago";
        }
        return timeStampText;
    }

    public static Label createOverallGradeChangeLabel(Course course) {
        Label returnLabel = new Label();
        returnLabel.setUIID("OverallGradeChange");

        if (Double.parseDouble(course.gradePercent) == course.getLowestOverallGradeInbox()) {
            returnLabel.setText(course.gradePercent + "%");
        }
        if (Double.parseDouble(course.gradePercent) > course.getLowestOverallGradeInbox()){
            returnLabel.setText(course.gradePercent + "%(▲" + Utils.intify(Math.abs(Double.parseDouble(course.gradePercent) - course.getLowestOverallGradeInbox())) + "%)");
            returnLabel.getAllStyles().setFgColor(Grade.A.getColor(), true);
        }
        else if (Double.parseDouble(course.gradePercent) < course.getLowestOverallGradeInbox()){
            returnLabel.setText(course.gradePercent + "%(▼" + Utils.intify(Math.abs(Double.parseDouble(course.gradePercent) - course.getLowestOverallGradeInbox())) + "%)");
            returnLabel.getAllStyles().setFgColor(Grade.Dp.getColor(), true);
        }
        return returnLabel;
    }

}
