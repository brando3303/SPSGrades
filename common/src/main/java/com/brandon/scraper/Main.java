package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;
import com.codename1.components.InfiniteProgress;
import com.codename1.components.SpanLabel;
import com.codename1.components.Switch;
import com.codename1.io.Storage;
import com.codename1.system.Lifecycle;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.TableLayout;
import org.littlemonkey.connectivity.Connectivity;

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



        Form loading = new Form();
        loading.show();

        Display.getInstance().registerPush();
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
                "We are unable to actually view your password as communication with The Source occurs entirely on your device.", "OK", null));
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
        gradesForm.getToolbar().setSelectedStyle(UIManager.getInstance().getComponentStyle("TitleArea"));

        //gradesForm.getToolbar().getUnselectedStyle().setBgColor(ColorUtil.GREEN);

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
            Label period = new Label("" + sc.period.intValue());
            Label grade = new Label(sc.gradeLetter);
            Label percent = new Label((sc.gradePercent.equals("NA")) ? "NA" : sc.gradePercent + "%");

            name.setUIID("CourseName");
            period.setUIID("Period");
            grade.setUIID("LetterGrade");
            percent.setUIID("Percent");

            //create a functional button which listens for a class tab being clicked
            Button classSelectButton = new Button();
            classSelectButton.addActionListener(e -> selectClass(sc));

            //set the color of the letter grade (unfortunately this is impossible in css alone)
            switch (grade.getText()) {
                case "A":
                    grade.getAllStyles().setFgColor(ColorUtil.GREEN);
                    break;
                case "B":
                    grade.getAllStyles().setFgColor(ColorUtil.BLUE);
                    break;
                case "C":
                    grade.getAllStyles().setFgColor(ColorUtil.YELLOW);
                    break;
                case "D":
                    grade.getAllStyles().setFgColor(ColorUtil.red(0));
                    break;
                case "NA":
                    grade.getAllStyles().setFgColor(ColorUtil.GRAY);
                    break;
            }

            Container gradeBox = BoxLayout.encloseY(period, grade, percent);
            gradeBox.setUIID("GradePercentV");

            //the actual container which holds the contents of each class tab
            Container grid = new Container(new TableLayout(1, 3));
            sc.tab = grid;
            grid.setUIID("GradeGrid");
            grid.add(gradeBox).add(name);
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
                Container c = new Container(new TableLayout(1, 2));
                c.setUIID("GradeGrid");

                Container selectableText = new Container();
                Button selectInboxItem = new Button();
                selectInboxItem.addActionListener(e -> {
                    log("you clicked on an inbox item!!!");
                    selectClass(ii.course);

                });
                selectableText.setLeadComponent(selectInboxItem);

                Button delete = new Button(FontImage.MATERIAL_CLEAR);
                delete.addActionListener(e -> {
                    c.remove();
                    if(Connectivity.isConnected()) {
                        ScraperServer.deleteInboxItem(ii, currentUser);
                    }
                    inboxForm.show();
                });

                c.add(selectableText.add(new Label(ii.courseName)));
                c.add(delete);

                inboxForm.add(c);

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


