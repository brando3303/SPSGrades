package com.brandon.scraper.formController;

import com.brandon.scraper.*;
import com.codename1.components.SpanLabel;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.*;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BoxLayout;
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
                    inboxForm.removeAll();
                    inboxForm.show();
                    for (InboxItem ii : currentUser.inbox.getInboxItems()) {
                        if (!ii.deleted) {
                            ScraperServer.deleteInboxItem(ii, currentUser);
                            ii.deleted = true;
                        }
                    }
                    app.getGradesFC().updateInboxButtonBadge();
                });
        for (InboxItem ii : currentUser.inbox.getInboxItems()) {
            if (!ii.deleted) {
                //main container
                log(ii.courseName);
                Container inboxItemContainer = new Container(new TableLayout(1, 2));
                inboxItemContainer.setUIID("GradeGrid");

                //selectable name: the name of the class which has been changed
                Container selectableText = new Container();
                Button selectInboxItem = new Button();
                selectInboxItem.addActionListener(e -> {
                    log("you clicked on an inbox item!!!");
                    app.getGradesFC().selectClass(ii.course);

                });
                selectableText.setLeadComponent(selectInboxItem);
                Label courseName = new Label(ii.courseName);
                selectableText.add(courseName);
                courseName.setUIID("InboxItemClassName");

                //timestamp text: how long ago the change was detected "2 hrs"
                //the number at the end is the convertion from pst to gmt
                Date dif = new Date(Math.abs(new Date().getTime() - ii.time.longValue()*1000) + 8*60*60*1000);

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
                Label timeStamp = new Label(timeStampText);
                timeStamp.setUIID("TimeStampText");

                //table of changed assignments: the assignment name and the way in which it changed
                Container assignmentTable = new Container(new TableLayout(ii.assignmentChanges.size(),2));
                assignmentTable.setUIID("AssignmentChangeTable");

                for(AssignmentChange ac : ii.assignmentChanges){

                    if(ac.type.equals("created")){
                        SpanLabel assignmentNameLabel = new SpanLabel("+ " + ac.name + ":");
                        assignmentNameLabel.setTextUIID("CreatedAssignmentChange");
                        assignmentNameLabel.getTextComponent().getAllStyles().setFgColor(Grade.A.getColor());
                        Label newAssignmentGradeLabel = new Label(ac.points + "/" + ac.total);
                        newAssignmentGradeLabel.setUIID("NewAssignmentGrade");
                        newAssignmentGradeLabel.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.points, ac.total), true);
                        assignmentTable.add(((TableLayout)inboxItemContainer.getLayout()).createConstraint().horizontalAlign(Component.LEFT).widthPercentage(80),assignmentNameLabel).add(newAssignmentGradeLabel);
                    }
                    else if(ac.type.equals("modified")){
                        SpanLabel assignmentNameLabel = new SpanLabel(ac.name + ":");
                        assignmentNameLabel.setTextUIID("ModifiedAssignmentChange");;

                        Container assignmentGradeChange = new Container(new TableLayout(1,3));
                        assignmentGradeChange.setUIID("AssignmentGradeChangeContainer");

                        Label oldGrade = new Label(Math.round(ac.pointsBefore / ac.total * 100) + "%");
                        oldGrade.setUIID("AssignmentGradeChange");
                        log(ac.pointsBefore + "");
                        log(ac.pointsNow + "");
                        oldGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.pointsBefore, ac.total), true);

                        // Label newGrade = new Label(ac.pointsNow + "/" + ac.total);
                        Label newGrade = new Label(Math.round(ac.pointsNow / ac.total * 100) + "%");
                        newGrade.setUIID("AssignmentGradeChange");
                        newGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.pointsNow, ac.total), true);

                        Label arrowImage = new Label();
                        arrowImage.setUIID("AssignmentGradeChangeArrow");
                        if(ac.pointsNow > ac.pointsBefore){
                            arrowImage.getAllStyles().setFgColor(Grade.A.getColor());
                        }
                        if(ac.pointsNow < ac.pointsBefore){
                            arrowImage.getAllStyles().setFgColor(Grade.E.getColor());
                        }
                        arrowImage.setMaterialIcon(FontImage.MATERIAL_ARROW_RIGHT);
                        assignmentGradeChange.add(oldGrade).add(arrowImage).add(newGrade);

                        assignmentTable.add(assignmentNameLabel).add(assignmentGradeChange);
                    }
                }

                //the overall grade change on the right
                Container overallGradeChanges = BoxLayout.encloseY();
                overallGradeChanges.setUIID("OverallGradeChangesGrid");

                Label newGrade = new Label(ii.gradeNow + "%");
                newGrade.setUIID("OverallGradeChange");
                newGrade.getAllStyles().setFgColor(Grade.getGradeColor(ii.gradeNow), true);


                Label oldGrade = new Label(ii.gradeBefore + "%");
                oldGrade.setUIID("OverallGradeChange");
                oldGrade.getAllStyles().setFgColor(Grade.getGradeColor(ii.gradeBefore), true);

                Label arrowImage = new Label();
                arrowImage.setUIID("OverallGradeChangeArrow");
                try {
                    if (Double.parseDouble(ii.gradeNow) > Double.parseDouble(ii.gradeBefore)) {
                        arrowImage.getAllStyles().setFgColor(Grade.A.getColor());
                    }
                    if (Double.parseDouble(ii.gradeNow) < Double.parseDouble(ii.gradeBefore)) {
                        arrowImage.getAllStyles().setFgColor(Grade.E.getColor());
                    }
                } catch(Exception e){
                    arrowImage.getAllStyles().setFgColor(Grade.NA.getColor());
                }
                arrowImage.setMaterialIcon(FontImage.MATERIAL_ARROW_DROP_DOWN);

                overallGradeChanges.add(oldGrade).add(arrowImage).add(newGrade);

                Container nameAssignmentListTable = new Container(BoxLayout.y());
                nameAssignmentListTable.setUIID("NameAssignmentListTable");

                nameAssignmentListTable.add(new Container(new TableLayout(1,2)).add(selectableText).add(timeStamp)).add(assignmentTable);



                Button delete = new Button(FontImage.MATERIAL_CLEAR);
                delete.addActionListener(e -> {
                    inboxItemContainer.remove();
                    if(Connectivity.isConnected()) {
                        ScraperServer.deleteInboxItem(ii, currentUser);
                        app.getGradesFC().updateInboxButtonBadge();
                    }

                    inboxForm.show();
                });

                //adding all of the components to the inbox item container
                inboxItemContainer.add(((TableLayout)inboxItemContainer.getLayout()).createConstraint().horizontalAlign(Component.LEFT).widthPercentage(87),nameAssignmentListTable);
                inboxItemContainer.add(((TableLayout)inboxItemContainer.getLayout()).createConstraint().horizontalAlign(Component.RIGHT).widthPercentage(13),overallGradeChanges);
                //inboxItemContainer.add(delete);

                //add inbox item container to list
                inboxForm.add(inboxItemContainer);

            }
        }
        return this.form;
    }
}
