package com.brandon.scraper.formController;

import com.brandon.scraper.*;
import com.codename1.components.SpanLabel;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.*;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.Table;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.SwipeBackSupport;

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
                    inboxForm.removeAll();
                    Label message = new Label("Grade changes will appear here.");
                    message.setUIID("EmptyInboxMessage");
                    inboxForm.add(message);
                    inboxForm.show();
                    for (AssignmentChange ac : currentUser.getAllAssignmentChanges(false)) {
                        if (!ac.deleted) {
                            //TODO: fix delete inbox item
                            //ScraperServer.deleteInboxItem(ii, currentUser);
                            ac.deleted = true;
                        }
                    }

                    app.getGradesFC().updateInboxButtonBadge();
                });

        //if there are no inbox items
        if(currentUser.getAllAssignmentChanges(false).size() == 0){
            Label message = new Label("Grade changes will appear here.");
            message.setUIID("EmptyInboxMessage");
            inboxForm.add(message);
            return this.form;
        }

        //for each inbox item
//        for (InboxItem ii : currentUser.inbox.getInboxItems()) {
//            if (!ii.shouldShow()) {
//                continue;
//            }
//            inboxForm.add(createInboxItemComponent(ii));
//        }
        for(Course c : currentUser.courses){
            if(c.assignmentChanges.size() != 0){
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

        //selectable name: the name of the class which has been changed
        Container selectableText = new Container();
        Button selectInboxItem = new Button();
        selectInboxItem.addActionListener(e -> {
            log("you clicked on an inbox item!!!");
            this.getApp().getGradesFC().selectClass(course);

        });
        selectableText.setLeadComponent(selectInboxItem);
        Label courseName = new Label(course.courseName);
        selectableText.add(courseName);
        courseName.setUIID("CourseName");

        //table of changed assignments: the assignment name and the way in which it changed
        Container assignmentTable = new Container(new TableLayout(course.assignmentChanges.size(),3));
        assignmentTable.setUIID("AssignmentChangeTable");

        for(AssignmentChange ac : course.assignmentChanges){
            if(!ac.showDisplay()){
                continue;
            }

            SpanLabel assignmentNameLabel = new SpanLabel(ac.assignmentName + ":");
            assignmentNameLabel.setTextUIID("AssignmentChangeLabel");
            assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(46),assignmentNameLabel);



            if(ac.assignmentChangeType.equals("created") && ac.assignmentPoints != null){

                Label newAssignmentGradeLabel = new Label(Utils.intify(ac.assignmentPoints) + "/" + Utils.intify(ac.assignmentTotal));
                newAssignmentGradeLabel.setUIID("AssignmentChangeLabel");
                newAssignmentGradeLabel.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.assignmentPoints, ac.assignmentTotal), true);
                assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(36), newAssignmentGradeLabel);
            }
            else if(ac.assignmentChangeType.equals("modified")){

                Container assignmentGradeChange = new Container(new TableLayout(1,3));
                assignmentGradeChange.setUIID("AssignmentGradeChangeContainer");

                Label oldGrade = new Label(Utils.intify(ac.assignmentPointsBefore) + "/" + Utils.intify(ac.assignmentTotal));
                oldGrade.setUIID("AssignmentChangeLabel");
                oldGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.assignmentPointsBefore, ac.assignmentTotal), true);

                // Label newGrade = new Label(ac.pointsNow + "/" + ac.total);
                Label newGrade = new Label(Utils.intify(ac.assignmentPointsNow) + "/" + Utils.intify(ac.assignmentTotal));
                newGrade.setUIID("AssignmentChangeLabel");
                newGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.assignmentPointsNow, ac.assignmentTotal), true);

                Label arrowImage = new Label();
                arrowImage.setUIID("AssignmentChangeArrow");
                if(ac.assignmentPointsNow > ac.assignmentPointsBefore){
                    arrowImage.getAllStyles().setFgColor(Grade.A.getColor());
                }
                if(ac.assignmentPointsNow < ac.assignmentPointsBefore){
                    arrowImage.getAllStyles().setFgColor(Grade.E.getColor());
                }
                arrowImage.setMaterialIcon(FontImage.MATERIAL_ARROW_RIGHT);
                assignmentGradeChange.add(oldGrade).add(arrowImage).add(newGrade);

                assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(36),assignmentGradeChange);
            }
            Label timeStamp = new Label(createTimeStamp(ac.time));
            timeStamp.setUIID("TimeStampText");
            assignmentTable.add(((TableLayout)assignmentTable.getLayout()).createConstraint().horizontalAlign(Table.LEFT).widthPercentage(18),timeStamp);
        }

//        Button delete = new Button(FontImage.MATERIAL_CLEAR);
//        delete.addActionListener(e -> {
//            inboxItemContainer.remove();
//            if(Connectivity.isConnected()) {
//                ScraperServer.deleteInboxItem(, this.getApp().getCurrentUser());
//                this.getApp().getGradesFC().updateInboxButtonBadge();
//            }
//
//            this.form.show();
//        });

        //adding all of the components to the inbox item container
        inboxItemContainer.add(selectableText);
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
}
