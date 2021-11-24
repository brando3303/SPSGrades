package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;

public enum Grade {
    A (ColorUtil.rgb(163,244,72), 93.0, 1000.0),
    Am (ColorUtil.rgb(73,218,154), 90.0, 92.99),
    Bp (ColorUtil.rgb(52,187,230), 87.0, 89.99),
    B (ColorUtil.rgb(52,187,230), 83.0, 86.99),
    Bm (ColorUtil.rgb(67,85,219), 80.0, 82.99),
    Cp (ColorUtil.rgb(247,208,56),77.0,79.99),
    C (ColorUtil.rgb(247,208,56), 73.0,76.99),
    Cm (ColorUtil.rgb(235,117,50), 70.0,72.99),
    Dp (ColorUtil.rgb(230,38,31),67.0, 69.99),
    D (ColorUtil.rgb(230,38,31), 60.0,66.99),
    E (ColorUtil.rgb(230,38,31),0.0,59.99);

    private int color;
    private Double max;
    private Double min;
    Grade(int color, Double min, Double max){
        this.color = color;
        this.min = min;
        this.max = max;
    }

    public static int getGradeColor(Double percent){
        for(Grade g : Grade.values()){
            if(percent >= g.min && percent < g.max){
                return g.color;
            }
        }
        return 0;
    }

}
