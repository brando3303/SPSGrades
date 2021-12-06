package com.brandon.scraper;

import com.codename1.ui.Form;
import com.codename1.ui.plaf.Border;


public class Utils {

    /**
    * NOTE: Setting the color of a toolbar will not carry over after a Live CSS recompile
    * */
    public static void setToolbarUIIDForSolidColor(Form form, String UIID){
        form.getToolbar().setUIID(UIID);
        form.getToolbar().getUnselectedStyle().setBorder(Border.createEmpty(),true);
        form.getToolbar().getUnselectedStyle().setBgTransparency(255);
    }

    public static String intify(Double num){
        if(num % 1 == 0){
            return Math.round(num) + "";
        }
        return num + "";
    }
    public static String intify(String num){
        if(Double.parseDouble(num) % 1 == 0){
            return Math.round(Double.parseDouble(num)) + "";
        }
        return num;
    }
}
