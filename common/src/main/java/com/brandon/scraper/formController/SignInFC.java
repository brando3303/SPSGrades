package com.brandon.scraper.formController;

import com.brandon.scraper.*;
import com.codename1.components.SpanLabel;
import com.codename1.io.NetworkManager;
import com.codename1.io.Storage;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import org.littlemonkey.connectivity.Connectivity;

import static com.codename1.ui.CN.log;

public class SignInFC extends FormController{
    private boolean signInWarningDisplayed = false;

    public SignInFC(Main app) {
        super(app);
    }

    @Override
    public Form start() {
        return createSignInForm();
    }

    private Form createSignInForm() {
        Form signInForm = new Form("Sign In", BoxLayout.y());
        this.form = signInForm;
        Utils.setToolbarUIIDForSolidColor(signInForm,"TitleArea");
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

        return this.form;
    }

    //the method which is called when a user signs in from the sign in page
    private void newSignInEnterAction(String user, String password) {
        double time = System.currentTimeMillis();
        if (!Connectivity.isConnected()) {
            Dialog.show("You are disconnected from the internet.", "Reconnect to the internet and try again", "Ok", null);
            return;
        }

        createUserSignIn(user, password, new Settings());
        log("the time to sign in was: " + (System.currentTimeMillis() - time) / 1000 + " seconds");
        this.getApp().setSignedIn(true);
    }

    //called when the username or password is submitted
    private void createUserSignIn(String user, String pass, Settings settings) {
        Main app = this.getApp();

        LoadingFC lfc = app.getLoadingFC();
        lfc.start();
        lfc.setMessage("Loading New User");
        lfc.show();

        try {
            app.setCurrentUser(ScraperServer.createNewUser(user, pass, loadingStudent -> {
                log("ran the progress update");
                int period = 0;
                while(!NetworkManager.getInstance().isQueueIdle()){
                    lfc.setProgress(Math.min(lfc.getPercent() + 20, 100));
                    lfc.setMessage("Loading " + loadingStudent.courses.get(Math.min(period,loadingStudent.courses.size()-1)).courseName);
                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    period++;
                }
            }));
        } catch (InvalidLoginInfo e) {
            log("error caught while trying to sign in with a new userefsffdfdssfdsfdsfsfdw");
            if (!signInWarningDisplayed) {
                this.form.add(new Label("wrong username or password", "SignInWarning"));
                signInWarningDisplayed = true;
            }

            this.form.show();
            return;
        }
        app.getCurrentUser().setSettings(settings);

        app.getInboxFC().start();
        app.getGradesFC().start().show();

        if(app.getDeviceId() != null) {
            ScraperServer.sendDeviceID(app.getCurrentUser().getUsername(),app.getCurrentUser().getPassword(), app.getDeviceId());
        }

        Storage.getInstance().writeObject("userpass", new String[]{app.getCurrentUser().getUsername(), app.getCurrentUser().getPassword()});
        Storage.getInstance().writeObject("settings", new String[]{settings.getNotifsOn() ? "on" : "off"});

    }
}
