package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;
import com.codename1.components.InfiniteProgress;
import com.codename1.components.SpanLabel;
import com.codename1.components.Switch;
import com.codename1.io.Storage;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.system.Lifecycle;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.Table;
import com.codename1.ui.table.TableLayout;
import org.littlemonkey.connectivity.Connectivity;

import java.util.Date;

import static com.codename1.ui.CN.log;

//needs to implement PushCallback
public class Main extends Lifecycle {
    private Form gradesForm;
    private Form inboxForm;
    private Form signInForm;
    private Student currentUser;
    private static Main mainInstance;

    boolean signInWarningDisplayed = false;

    @Override
    public void runApp() {

        try {
            log(new SimpleDateFormat("yyyy-MM-dd").parse("2021-11-22").getTime() + "");
        } catch (ParseException e) {
            e.printStackTrace();
        }


        Form loading = new Form();
        loading.show();

        Display.getInstance().registerPush();
        Display.getInstance().lockOrientation(true);
        //encrypts all data stored, and is just simple but effective layer of protect from who knows
        //EncryptedStorage.install("BrandonEvan");

        //allows this class to act as a singleton
        mainInstance = this;

        //create the sign in form, but dont necessarily show it
        createSignInForm();

        //check if the file "userpass" exists, and if so, load them as they checked remember me
        if (Storage.getInstance().exists("userpass") && Storage.getInstance().exists("settings")) {
            log("this person has signed in before");
            Object[] userpass = ((Object[]) Storage.getInstance().readObject("userpass"));
            Object[] settingsJson = (Object[]) Storage.getInstance().readObject("settings");
            Settings settings = Deserializer.deserializeSettings(new String[]{(String) settingsJson[0]});

            //if the person has logged in before, and they aren't connected to the internet, then they need to connect...
            if (!Connectivity.isConnected()) {

                //continually show this dialog until the person is actually connected to the internet
                Dialog d = new Dialog("You are disconnected from the internet.");
                Button retryButton = new Button("retry");
                retryButton.addActionListener(e -> {
                    if (!Connectivity.isConnected()) {
                        log("retry was clicked, so loader was shown");
                        loading.show();
                        d.show();
                        return;
                    }
                    //if they finally are connected, go to their courses page
                    Dialog ip = new InfiniteProgress().showInfiniteBlocking();
                    signInExistingUser((String) userpass[0], (String) userpass[1], settings);
                });

                d.add(retryButton);
                d.show();

                return;
            }
            signInExistingUser((String) userpass[0], (String) userpass[1], settings);
            return;
        }


        signInForm.show();


    }

    private void signInExistingUser(String username, String pwd, Settings settings) {
        double time = System.currentTimeMillis();
        Dialog ip = new InfiniteProgress().showInfiniteBlocking();
        if (!Connectivity.isConnected()) {
            Dialog.show("You are disconnected from the internet.", "Reconnect to the internet and try again", "Ok", null);
            ip.dispose();
            return;
        }
        loadUserSignIn(username, pwd, settings);
        log("the time to sign in was: " + (System.currentTimeMillis() - time) / 1000 + " seconds");
    }

    private void createSignInForm() {
        signInForm = new Form("Sign In", BoxLayout.y());
        signInForm.setScrollable(false);

        //the username field
        TextField usernameField = new TextField("", "  Username", 20, TextArea.USERNAME);
        usernameField.setUIID("RoundBorder");

        //the password field
        TextField passwordField = new TextField("", "  Password", 20, TextArea.PASSWORD);
        passwordField.setUIID("RoundBorder");

        Button signInButton = new Button("Sign In");
        signInButton.addActionListener(e ->
                newSignInEnterAction(usernameField.getText(), passwordField.getText()));

        //adding listeners to the username and password listeners, calling the method "signInSwitchForm()" for both
        usernameField.setDoneListener(e -> newSignInEnterAction(usernameField.getText(), passwordField.getText()));
        passwordField.setDoneListener(e -> newSignInEnterAction(usernameField.getText(), passwordField.getText()));


        signInForm.add(new Label("Sign in using your SPS Source login."));
        signInForm.add(usernameField);
        signInForm.add(passwordField);
        signInForm.add(signInButton);

        //doesn't work anymore, became unuseful after implementing inbox and assignments which required separate get requests
        //signInFormReturn.add(testButton);

        //add hint for user if they aren't connected to the internet
        if (!Connectivity.isConnected()) {
            signInForm.add(new SpanLabel("You are likely unconnected to the internet and will be unable to sign in."));
        }

        //add a toolbar command which gives the user information about password security
        signInForm.getToolbar().addMaterialCommandToLeftBar("Security", FontImage.MATERIAL_HELP, 4, e -> Dialog.show("Information Security",
                "All user data is encrypted and stored in a secure database. When a user is no longer active or signs out, all data is deleted.", "OK", null));
    }

    //the method which is called when a user signs in from the sign in page
    private void newSignInEnterAction(String user, String password) {
        double time = System.currentTimeMillis();
        Dialog ip = new InfiniteProgress().showInfiniteBlocking();
        if (!Connectivity.isConnected()) {
            Dialog.show("You are disconnected from the internet.", "Reconnect to the internet and try again", "Ok", null);
            ip.dispose();
            return;
        }
        createUserSignIn(user, password, new Settings());
        log("the time to sign in was: " + (System.currentTimeMillis() - time) / 1000 + " seconds");

    }

    //called when the username or password is submitted
    private void createUserSignIn(String user, String pass, Settings settings) {

        try {
            currentUser = ScraperServer.createNewUser(user, pass);
        } catch (InvalidLoginInfo e) {
            log("error caught while trying to sign in with a new userefsffdfdssfdsfdsfsfd");
            if (!signInWarningDisplayed) {
                signInForm.add(new Label("wrong username or password", "SignInWarning"));
                signInWarningDisplayed = true;
            }

            signInForm.show();
            return;
        }
        currentUser.setSettings(settings);

        createInboxForm();
        createGradesForm();
        gradesForm.show();


        Storage.getInstance().writeObject("userpass", new String[]{currentUser.getUsername(), currentUser.getPassword()});
        Storage.getInstance().writeObject("settings", new String[]{settings.getNotifsOn() ? "on" : "off"});

    }

    private void loadUserSignIn(String username, String pwd, Settings settings) {

        try {
            currentUser = ScraperServer.getStudentFromDataBase(username, pwd);
        } catch (InvalidLoginInfo e) {
            log("error caught while trying to sign in with a new user");
            if (!signInWarningDisplayed) {
                signInForm.add(new Label("wrong username or password", "SignInWarning"));
                signInWarningDisplayed = true;
            }
            Storage.getInstance().deleteStorageFile("userpass");
            Storage.getInstance().deleteStorageFile("settings");
            log("deleted storage things");
            signInForm.show();
            return;
        }
        currentUser.setSettings(settings);
        createInboxForm();
        createGradesForm();
        gradesForm.show();

    }

    //called when a user successfully signs in
    private void createGradesForm() {
        gradesForm = new Form("Grades", BoxLayout.y());


        Utils.setToolbarUIIDForSolidColor(gradesForm,"TitleArea");



        gradesForm.getToolbar().addMaterialCommandToSideMenu("SignOut",
                FontImage.MATERIAL_LOGOUT, 4, e -> signOut());
        gradesForm.getToolbar().addMaterialCommandToSideMenu("Settings",
                FontImage.MATERIAL_SETTINGS, 4, e -> settingsSwitchForm());
        gradesForm.getToolbar().addMaterialCommandToRightBar("Inbox", FontImage.MATERIAL_UPDATE, e -> inboxForm.show());
        gradesForm.getContentPane().addPullToRefresh(() -> {
            if(!Connectivity.isConnected()){
                return;
            }
            currentUser = ScraperServer.updateUser(currentUser);
            createGradesForm();
            gradesForm.show();
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

            //create a functional button which listens for a class tab being clicked
            Button classSelectButton = new Button();
            classSelectButton.addActionListener(e -> selectClass(sc));

            //set the color of the letter grade (unfortunately this is impossible in css alone)
            grade.getAllStyles().setFgColor(Grade.getGradeColor(Double.parseDouble(sc.gradePercent)));

            Container nameTeacherBox = BoxLayout.encloseY(name, teacher);
            nameTeacherBox.setUIID("NameTeacherBox");

            // Container gradeBox = BoxLayout.encloseY(period, grade, percent);
            Container gradeBox = BoxLayout.encloseY(grade, percent);
            gradeBox.setUIID("GradePercentV");

            //the actual container which holds the contents of each class tab
            Container grid = new Container(new TableLayout(1, 3));
            sc.tab = grid;
            grid.setUIID("GradeGrid");
            grid.add(nameTeacherBox).add(gradeBox);
            grid.setLeadComponent(classSelectButton);

            gradesForm.add(grid);
        }
    }

    //called from the class tab container listener when clicked on
    //Goes to the assignments page for that class if there are any assignments
    private void selectClass(Course course) {
        if (course.frn != null) {
            if (course.getCoursePage() == null) {
                course.createCoursePage();
            }
            gradesForm.setVisible(false);
            course.getCoursePage().show();
        }
    }

    //called when the user is trying to sign out (from the grades form)
    private void signOut() {
        gradesForm.removeAll();
        if(Connectivity.isConnected()) {
            ScraperServer.deactivateUser(currentUser);
        }
        currentUser = null;
        Storage.getInstance().deleteStorageFile("userpass");
        Storage.getInstance().deleteStorageFile("settings");
        signInForm.show();
    }

    private void settingsSwitchForm(){
        Form settings = new Form("Settings",BoxLayout.y());
        Utils.setToolbarUIIDForSolidColor(settings,"TitleArea");
        settings.getToolbar().addMaterialCommandToLeftBar("Back",FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> gradesForm.show());


        Switch notificationsSwitch = new Switch();
        Button applySettingsButton = new Button("Apply Settings");

        if(currentUser.getSettings().getNotifsOn()){
            notificationsSwitch.setOn();
        }
        applySettingsButton.addActionListener(e -> {
            if(!Connectivity.isConnected()){
                Dialog.show("Please Connect to the Internet", "You are unable to save settings while disconnected from the internet", "ok", null);
                return;
            }
            if(notificationsSwitch.isOn()){
                currentUser.getSettings().setNotifsOn(true);
                log("notifs are on");
            }
            if(notificationsSwitch.isOff()){
                currentUser.getSettings().setNotifsOn(false);
                log("notifs are off");
            }

            //TODO: send server notification preferences
            Storage.getInstance().writeObject("settings", Deserializer.serializeSettings(currentUser.getSettings()));
            log("stored settings and user info");
            log(currentUser.getSettings().getNotifsOn() ? "notifs are on" : "notifs are off");
            gradesForm.show();
        });


        settings.add(new Label("Allow Notifications?"));
        settings.add(notificationsSwitch);
        settings.add(applySettingsButton);
        settings.show();

    }

    private void createInboxForm() {
        inboxForm = new Form("Inbox", BoxLayout.y());
        Utils.setToolbarUIIDForSolidColor(inboxForm,"TitleArea");
        inboxForm.getToolbar().addMaterialCommandToLeftBar("Back", FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> {
                    inboxForm.setVisible(false);
                    gradesForm.show();

                });
        inboxForm.getToolbar().addMaterialCommandToRightBar("Clear", FontImage.MATERIAL_CLEAR_ALL, 4,
                e -> {
                    inboxForm.removeAll();
                    inboxForm.show();
                    for (InboxItem ii : currentUser.inbox.inboxItems) {
                        if (!ii.deleted) {
                            ScraperServer.deleteInboxItem(ii, currentUser);
                        }
                    }

                });
        for (InboxItem ii : currentUser.inbox.inboxItems) {
            if (!ii.deleted) {
                log(ii.courseName);
                Container inboxItemContainer = new Container(new TableLayout(1, 2));
                inboxItemContainer.setUIID("GradeGrid");

                //selectable name: the name of the class which has been changed
                Container selectableText = new Container();
                Button selectInboxItem = new Button();
                selectInboxItem.addActionListener(e -> {
                    log("you clicked on an inbox item!!!");
                    selectClass(ii.course);

                });
                selectableText.setLeadComponent(selectInboxItem);
                Label courseName = new Label(ii.courseName);
                selectableText.add(courseName);
                courseName.setUIID("InboxItemClassName");

                //timestamp text: how long ago the change was detected "2 hrs"
                Date dif = new Date(new Date().getTime() - ii.time.longValue()*1000);

                int days = Integer.parseInt(new SimpleDateFormat("DD").format(dif));
                int hours = Integer.parseInt(new SimpleDateFormat("h").format(dif));
                int minutes =  Integer.parseInt(new SimpleDateFormat("mm").format(dif));
                int seconds = Integer.parseInt(new SimpleDateFormat("ss").format(dif));
                String timeStampText;
                if(days >= 1){
                    timeStampText = days + " days ago";
                } else if(hours >= 2){
                    timeStampText = hours + " hrs ago";
                } else if(minutes >= 5){
                    timeStampText = minutes + " mins ago";
                } else if(minutes == 0){
                    timeStampText = seconds + " secs ago";
                } else{
                    timeStampText = minutes + " mins, " + seconds + " secs ago";
                }
                Label timeStamp = new Label(timeStampText);
                timeStamp.setUIID("TimeStampText");

                //list of changed assignments: the assignment name and the way in which it changed
                Container assignmentTable = new Container(new TableLayout(ii.assignmentChanges.size(),2));
                assignmentTable.setUIID("AssignmentChangeTable");

                for(AssignmentChange ac : ii.assignmentChanges){

                    if(ac.type.equals("created")){
                        SpanLabel assignmentNameLabel = new SpanLabel("+ " + ac.name);
                        assignmentNameLabel.setTextUIID("CreatedAssignmentChange");
                        assignmentNameLabel.getTextComponent().getAllStyles().setFgColor(Grade.A.getColor());
                        Label newAssignmentGradeLabel = new Label(ac.points + "/" + ac.total);
                        newAssignmentGradeLabel.setUIID("NewAssignmentGrade");
                        newAssignmentGradeLabel.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.points, ac.total), true);
                        assignmentTable.add(assignmentNameLabel).add(newAssignmentGradeLabel);
                    }
                    else if(ac.type.equals("modified")){
                        SpanLabel assignmentNameLabel = new SpanLabel(ac.name);
                        assignmentNameLabel.setTextUIID("ModifiedAssignmentChange");;

                        Container assignmentGradeChange = new Container(new TableLayout(1,3));
                        assignmentGradeChange.setUIID("AssignmentGradeChangeContainer");

                        Label oldGrade = new Label(ac.pointsBefore + "/" + ac.total);
                        oldGrade.setUIID("AssignmentGradeChange");
                        oldGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.pointsBefore, ac.total), true);

                        Label newGrade = new Label(ac.pointsNow + "/" + ac.total);
                        newGrade.setUIID("AssignmentGradeChange");
                        newGrade.getAllStyles().setFgColor(Grade.getGradeColorFromFraction(ac.pointsNow, ac.total), true);

                        Label arrowImage = new Label();
                        arrowImage.setMaterialIcon(FontImage.MATERIAL_ARROW_RIGHT);
                        arrowImage.setUIID("AssignmentGradeChangeArrow");
                        if(ac.pointsNow > ac.pointsBefore){
                            arrowImage.getAllStyles().setFgColor(Grade.A.getColor());
                        }
                        if(ac.pointsNow < ac.pointsBefore){
                            arrowImage.getAllStyles().setFgColor(Grade.E.getColor());
                        }

                        assignmentTable.add(oldGrade).add(arrowImage).add(newGrade);
                    }
                }

                //the overall grade change on the right
                Container overallGradeChanges = BoxLayout.encloseY();
                overallGradeChanges.setUIID("OverallGradeChangesGrid");

                Label newGrade = new Label(ii.gradeNow);
                newGrade.setUIID("OverallGradeChange");
                newGrade.getAllStyles().setFgColor(Grade.getGradeColor(Double.parseDouble(ii.gradeNow)), true);


                Label oldGrade = new Label(ii.gradeBefore);
                oldGrade.setUIID("OverallGradeChange");
                oldGrade.getAllStyles().setFgColor(Grade.getGradeColor(Double.parseDouble(ii.gradeBefore)), true);

                Label arrowImage = new Label();
                arrowImage.setMaterialIcon(FontImage.MATERIAL_ARROW_DROP_DOWN);
                arrowImage.setUIID("OverallGradeChangeArrow");
                if(Double.parseDouble(ii.gradeNow) > Double.parseDouble(ii.gradeBefore)){
                    arrowImage.getAllStyles().setFgColor(Grade.A.getColor(), true);
                }
                if(Double.parseDouble(ii.gradeNow) < Double.parseDouble(ii.gradeBefore)){
                    arrowImage.getAllStyles().setFgColor(Grade.E.getColor(), true);
                }

                overallGradeChanges.add(oldGrade).add(arrowImage).add(newGrade);

                Container nameAssignmentListTable = new Container(BoxLayout.y());
                nameAssignmentListTable.setUIID("NameAssignmentListTable");

                nameAssignmentListTable.add(new Container(new TableLayout(1,2)).add(selectableText).add(timeStamp)).add(assignmentTable);



                Button delete = new Button(FontImage.MATERIAL_CLEAR);
                delete.addActionListener(e -> {
                    inboxItemContainer.remove();
                    if(Connectivity.isConnected()) {
                        ScraperServer.deleteInboxItem(ii, currentUser);
                    }
                    inboxForm.show();
                });

                inboxItemContainer.add(((TableLayout)inboxItemContainer.getLayout()).createConstraint().horizontalAlign(Component.LEFT).widthPercentage(90),nameAssignmentListTable);
                inboxItemContainer.add(((TableLayout)inboxItemContainer.getLayout()).createConstraint().horizontalAlign(Component.RIGHT).widthPercentage(10),overallGradeChanges);
                //inboxItemContainer.add(delete);

                inboxForm.add(inboxItemContainer);

            }
        }
    }

    public static Main getInstance() {
        return mainInstance;
    }

    public Form getGradesForm() {
        return gradesForm;
    }


//    @Override
//    public void push(String value) {
//        log("push: " + value);
//
//        //TODO: find out if this actually works
//        if (value.startsWith("@69")) {
//            for (Course sc : currentUser.courses) {
//                if (value.contains(sc.courseName)) {
//                    sc.createCoursePage();
//                    sc.getCoursePage().show();
//                }
//            }
//        }
//    }
//
//    @Override
//    public void registeredForPush(String deviceID) {
//        log("this device was registered");
//        Dialog.show("congratulations!!", "you were registered for push notifications", "ok", null);
//        //USE Push.getPushKey()
//        //TODO: register new devices with the server
//        //ServerScraper.registerForPush(deviceID);
//    }
//
//    @Override
//    public void pushRegistrationError(String s, int i) {
//
//    }
}
