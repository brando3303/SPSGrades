package com.brandon.scraper;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.validation.Constraint;

import static com.codename1.ui.CN.log;

public class ExpandableText extends Button {
    Container expanded;

    public ExpandableText(String text){
        super(text);
        this.addActionListener(e ->{
            log("expandable text opened");
            Container parent = this.getParent();
            Constraint constraint = (Constraint)parent.getLayout().getComponentConstraint(this);
            log(parent.getComponentIndex(this) + "");
            parent.replace(this,expanded, null);
            parent.getComponentForm().show();

        });
        expanded = new Container();
        Button close = new Button();
        close.addActionListener(e ->{
            log("expandable text closed");
            Container parent = expanded.getParent();

            parent.replace(expanded,this, null);
            parent.getComponentForm().show();
        });
        expanded.setLeadComponent(close);
        expanded.add(new SpanLabel(text));

    }
}
