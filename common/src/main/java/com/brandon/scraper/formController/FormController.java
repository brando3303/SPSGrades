package com.brandon.scraper.formController;

import com.brandon.scraper.Main;
import com.codename1.ui.Form;

public abstract class FormController {
    private Main app;
    protected Form form;

    public FormController(Main app){
        this.app = app;
    }

    public abstract Form start();

    public Form show(){
        form.show();
        return form;
    }

    public Form getForm(){
        return form;
    }

    public Main getApp(){
        return app;
    }

}
