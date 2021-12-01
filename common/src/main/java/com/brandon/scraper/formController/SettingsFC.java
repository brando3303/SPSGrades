package com.brandon.scraper.formController;

import com.brandon.scraper.Deserializer;
import com.brandon.scraper.Main;
import com.brandon.scraper.Student;
import com.brandon.scraper.Utils;
import com.codename1.components.Switch;
import com.codename1.io.Storage;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import org.littlemonkey.connectivity.Connectivity;

import static com.codename1.ui.CN.log;

public class SettingsFC extends FormController{

    public SettingsFC(Main app) {
        super(app);
    }

    @Override
    public Form start() {
        return settingsSwitchForm();
    }

    private Form settingsSwitchForm(){
        Student currentUser = this.getApp().getCurrentUser();

        Form settings = new Form("Settings", BoxLayout.y());
        this.form = settings;
        Utils.setToolbarUIIDForSolidColor(settings,"TitleArea");
        settings.getToolbar().addMaterialCommandToLeftBar("Back", FontImage.MATERIAL_ARROW_BACK_IOS_NEW, 4,
                e -> this.getApp().getGradesFC().show());


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
            this.getApp().getGradesFC().show();
        });
        settings.add(notificationsSwitch).add(new Label("recieve notifications?")).add(applySettingsButton);

        return this.form;
    }
}
