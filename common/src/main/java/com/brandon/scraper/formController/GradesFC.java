package com.brandon.scraper.formController;

import com.brandon.scraper.*;
import com.codename1.components.SpanLabel;
import com.codename1.io.Storage;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.TableLayout;
import org.littlemonkey.connectivity.Connectivity;

public class GradesFC extends FormController{
    private Button inboxButton;

    public GradesFC(Main app) {
        super(app);
    }

    @Override
    public Form start() {
        return createGradesForm();
    }

    private Form createGradesForm() {
        Student currentUser = this.getApp().getCurrentUser();

        Form gradesForm = new Form("Grades", BoxLayout.y());
        this.form = gradesForm;
        Utils.setToolbarUIIDForSolidColor(gradesForm,"TitleArea");

        gradesForm.getToolbar().addMaterialCommandToSideMenu("SignOut",
                FontImage.MATERIAL_LOGOUT, 4, e -> signOut());

        //*Advertisment*
        SpanLabel sl = new SpanLabel("Sponsored by @roosevelt_cookie_mafia");
        sl.setUIID("GradePercentV");
        sl.setTextUIID("Percent");
        gradesForm.getToolbar().addComponentToLeftSideMenu(sl);

        //here lies settings, RIP
        //gradesForm.getToolbar().addMaterialCommandToSideMenu("Settings",
        //        FontImage.MATERIAL_SETTINGS, 4, e -> this.getApp().getSettingsFC().start().show());

        inboxButton = new Button("inbox");
        inboxButton.setUIID("InboxIconBadge");
        inboxButton.addActionListener(e -> this.getApp().getInboxFC().show());
        inboxButton.setMaterialIcon(FontImage.MATERIAL_UPDATE);
        gradesForm.getToolbar().add(BorderLayout.EAST, inboxButton);
        int numItems = currentUser.getAllAssignmentChanges(false).size();
        if (numItems > 0) {
            inboxButton.setBadgeText(Integer.toString(numItems));
            inboxButton.setBadgeUIID("InboxButtonBadge");
        }

        gradesForm.getContentPane().addPullToRefresh(() -> {
            if(!Connectivity.isConnected()){
                return;
            }
            gradesForm.setTransitionOutAnimator(null);
            this.getApp().setCurrentUser(ScraperServer.updateUser(this.getApp().getCurrentUser()));
            this.start();
            this.getApp().getInboxFC().start();
            this.show();

        });

        //create a class tab for every class in the current student
        for (Course sc : currentUser.courses) {
            Label name = new Label(sc.courseName);
            Label teacher = new Label(sc.teacher);
            Label period = new Label(sc.period);
            Label grade = new Label(sc.gradeLetter);
            Label percent = new Label((sc.gradePercent.equals("NA")) ? "NA" : sc.gradePercent + "%");

            name.setUIID("CourseName");
            teacher.setUIID("Teacher");
            period.setUIID("Period");
            grade.setUIID("LetterGrade");
            percent.setUIID("Percent");

            //the button that will take you to the class's assignments
            Button classSelectButton = new Button(FontImage.MATERIAL_ARROW_FORWARD_IOS);
            classSelectButton.addActionListener(e -> selectClass(sc));
            classSelectButton.setUIID("CourseArrow");

            //set the color of the letter grade (unfortunately this is impossible in css alone)
            grade.getAllStyles().setFgColor(Grade.getGradeColor(sc.gradePercent));

            Container nameTeacherBox = BoxLayout.encloseY(name, teacher);
            nameTeacherBox.setUIID("NameTeacherBox");

            // Container gradeBox = BoxLayout.encloseY(period, grade, percent);
            Container gradeBox = BoxLayout.encloseY(grade, percent);
            gradeBox.setUIID("GradePercentV");

            //the actual container which holds the contents of each class tab
            Container classTable = new Container(new TableLayout(1, 3));
            sc.tab = classTable;
            classTable.setUIID("GradeGrid");
            classTable.add(((TableLayout)classTable.getLayout()).createConstraint().horizontalAlign(Component.LEFT).widthPercentage(78),nameTeacherBox);
            classTable.add(((TableLayout)classTable.getLayout()).createConstraint().horizontalAlign(Component.RIGHT),gradeBox);
            if(sc.assignments.size() != 0) {
                classTable.add(((TableLayout)classTable.getLayout()).createConstraint().horizontalAlign(Component.RIGHT), classSelectButton);
            }
            gradesForm.add(classTable);
        }
        return gradesForm;
    }

    public void selectClass(Course course) {
        if (course.assignments.size() != 0) {
            if (!course.getCourseFormC().getIsCreated()) {
                course.getCourseFormC().start();
            }
            this.form.setVisible(false);
            course.getCourseFormC().show();
        }
    }

    //called when the user is trying to sign out (from the grades form)
    private void signOut() {
        this.form.removeAll();
        if(Connectivity.isConnected()) {
            ScraperServer.deactivateUser(getApp().getCurrentUser());
        }
        this.getApp().setCurrentUser(null);
        this.getApp().setSignedIn(false);
        Storage.getInstance().deleteStorageFile("userpass");
        Storage.getInstance().deleteStorageFile("settings");
        this.getApp().getSignInFC().show();
    }

    public void updateInboxButtonBadge(){
        //update the badge of the inbox button on the grades form
        if(this.getApp().getCurrentUser().getAllAssignmentChanges(false).size() == 0){
            inboxButton.setBadgeText("");
            return;
        }
        inboxButton.setBadgeText(Integer.toString(this.getApp().getCurrentUser().getAllAssignmentChanges(false).size()));
    }
}
