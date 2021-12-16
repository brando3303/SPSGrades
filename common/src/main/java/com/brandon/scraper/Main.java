package com.brandon.scraper;

import com.brandon.scraper.formController.*;
import com.codename1.components.InfiniteProgress;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Storage;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.push.Push;
import com.codename1.push.PushCallback;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import org.littlemonkey.connectivity.Connectivity;

import java.util.Date;

import static com.codename1.ui.CN.log;

//needs to implement PushCallback
public class Main extends Lifecycle implements PushCallback {

    private final double minsBeforeReload = 1; //mins

    //the Form Controllers for this app
    private GradesFC gradesFC = new GradesFC(this);
    private InboxFC inboxFC = new InboxFC(this);
    private SignInFC signInFC = new SignInFC(this);
    private SettingsFC settingsFC = new SettingsFC(this);
    private LoadingFC loadingFC = new LoadingFC(this);

    private static Main mainInstance;
    private Student currentUser;

    private boolean started = false;
    private boolean signedIn = false;
    private boolean redirectToInbox = false;
    private String deviceId;
    private double lastTimeInFocus;



    @Override
    public void start(){
        ConnectionRequest.setHandleErrorCodesInGlobalErrorHandler(false);

        if(!started){
            runApp();
            return;
        } else if(started && !signedIn){
            signInFC.show();
        } else if(started && signedIn && (new Date().getTime()-lastTimeInFocus) >= minsBeforeReload * 60 * 1000){
            loadingFC.start();
            loadingFC.setMessage("Loading User...");
            loadingFC.show();
            signInExistingUser(currentUser.getUsername(),currentUser.getPassword(),currentUser.getSettings());
        }
    }

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
        signInFC.start();
        started = true;
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
        signInFC.show();
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
        signedIn = true;
    }

    private void loadUserSignIn(String username, String pwd, Settings settings) {

        try {
            currentUser = ScraperServer.getStudentFromDataBase(username, pwd);
        } catch (InvalidLoginInfo e) {
            log("error caught while trying to sign in with a new user");
            Storage.getInstance().deleteStorageFile("userpass");
            Storage.getInstance().deleteStorageFile("settings");
            log("deleted storage things");
            signInFC.show();
            return;
        }
        currentUser.setSettings(settings);
        inboxFC.start();
        gradesFC.start();

        if(redirectToInbox){
            inboxFC.show();
        }else {
            gradesFC.show();
        }
    }

    public SettingsFC getSettingsFC() {
        return settingsFC;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId){
        this.deviceId = deviceId;
    }

    public SignInFC getSignInFC() {
        return signInFC;
    }

    public InboxFC getInboxFC() {
        return inboxFC;
    }

    public static Main getInstance() {
        return mainInstance;
    }

    public GradesFC getGradesFC() {
        return gradesFC;
    }

    public Student getCurrentUser() {
        return currentUser;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    public void setSignedIn(boolean signedIn) {
        this.signedIn = signedIn;
    }

    public void setCurrentUser(Student s){
        currentUser = s;
    }

    public LoadingFC getLoadingFC() {
        return loadingFC;
    }

    @Override
    public void stop(){
        lastTimeInFocus = new Date().getTime();
    }

    @Override
    public void push(String value) {
        log("push: " + value);

        //TODO: find out if this actually works
        if (value.startsWith("@69")) {

            if(started && signedIn) {
                inboxFC.start().show();
            }
            else {
                redirectToInbox = true;
            }
        }
    }

    @Override
    public void registeredForPush(String s) {

        if(started && signedIn){
            ScraperServer.sendDeviceID(currentUser.getUsername(), currentUser.getPassword(), Push.getPushKey());
        }
        deviceId = Push.getPushKey();
        log("this device was registered. Device ID: " + deviceId );
    }

    @Override
    public void pushRegistrationError(String s, int i) {

    }
}
