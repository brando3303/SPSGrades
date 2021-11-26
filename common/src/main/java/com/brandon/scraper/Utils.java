package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;


public class Utils {

    /**
    * NOTE: Setting the color of a toolbar will not carry over after a Live CSS recompile
    * */
    public static void setToolbarUIIDForSolidColor(Form form, String UIID){
        form.getToolbar().setSelectedStyle(UIManager.getInstance().getComponentStyle(UIID));
        form.getToolbar().getUnselectedStyle().setBorder(Border.createEmpty(),true);
        form.getToolbar().getUnselectedStyle().setBgTransparency(255);
    }
}
