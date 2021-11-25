package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;

public enum Grade {
    // https://flatuicolors.com/palette/us
    A (ColorUtil.rgb(0, 184, 148), 93.0, 1000.0), // Dark green
    Am (ColorUtil.rgb(85, 239, 196), 90.0, 92.99), // Light green
    Bp (ColorUtil.rgb(0, 206, 201), 87.0, 89.99), // Turquoise
    B (ColorUtil.rgb(116, 185, 255), 83.0, 86.99), // Light blue
    Bm (ColorUtil.rgb(9, 132, 227), 80.0, 82.99), // Dark blue
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
