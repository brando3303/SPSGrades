package com.brandon.scraper.formController;

import com.brandon.scraper.Main;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.TableLayout;

import static com.codename1.ui.CN.log;

public class LoadingFC extends FormController{

    Label displayText;

    Container barContainer;
    Container loadingBar;


    Image displayImage;
    private final int STARTINGPERCENT = 1;
    private int percent;

    public LoadingFC(Main app) {
        super(app);
    }

    @Override
    public Form start() {
        percent = STARTINGPERCENT;
        BorderLayout border = new BorderLayout();
        Form loadingForm = new Form(border);
        loadingForm.setUIID("LoadingScreenForm");
        this.form = loadingForm;

        displayText = new Label();
        displayText.setUIID("LoadingScreenText");

        loadingForm.add(BorderLayout.CENTER, displayText);
        loadingBar = new Container();
        loadingBar.add(new Label(" "));
        loadingBar.setUIID("LoadingBar");

        TableLayout barLayout = new TableLayout(1,2);
        barContainer = new Container(barLayout);
        barContainer.setUIID("LoadingBarContainer");
        barContainer.add(barLayout.createConstraint().widthPercentage(STARTINGPERCENT),loadingBar);
        barContainer.add(new Label(" "));
        loadingForm.add(BorderLayout.SOUTH, barContainer);

        return this.form;
    }

    public void setProgress(int percent){
        this.percent = percent;
        barContainer.removeAll();
        barContainer.add(((TableLayout)barContainer.getLayout()).createConstraint().widthPercentage(this.percent), loadingBar);
        barContainer.add(new Label(" "));
        this.form.show();
    }

    public void addProgress(int progress){
        setProgress(percent+progress);
    }

    public int getPercent(){
        return this.percent;
    }

    public void setMessage(String message){
        displayText.setText(message);
        this.form.show();
    }

    public void setImage(Image image){
        displayImage = image;

    }

    public void runTimed(long totalMs, int intervals){
        Thread execute = new Thread(() -> {
            while(Display.getInstance().getCurrent().equals(this.form) && percent+100/intervals < 100){
                addProgress(100/intervals);
                log("updated bar");
                this.show();
                try {
                    Thread.sleep(totalMs/intervals);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        execute.start();
    }
}
