package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;

public enum Grade {
    // https://flatuicolors.com/palette/us
    A (ColorUtil.rgb(0, 184, 148), 93.0, 1000.0), // Dark green
    Am (ColorUtil.rgb(65, 219, 176), 90.0, 92.99), // Light green -20 darker
    Bp (ColorUtil.rgb(0, 206, 201), 87.0, 89.99), // Turquoise
    B (ColorUtil.rgb(116, 185, 255), 83.0, 86.99), // Light blue
    Bm (ColorUtil.rgb(9, 132, 227), 80.0, 82.99), // Dark blue
    Cp (ColorUtil.rgb(197,158,6),77.0,79.99), // Dark yellow -50 darker
    C (ColorUtil.rgb(197,158,6), 73.0,76.99), // Dark yellow -50 darker
    Cm (ColorUtil.rgb(197,158,6), 70.0,72.99), // Dark yellow -50 darker
    Dp (ColorUtil.rgb(214, 48, 49),67.0, 69.99), // Red
    D (ColorUtil.rgb(214, 48, 49), 60.0,66.99), // Red
    E (ColorUtil.rgb(214, 48, 49),0.0,59.99); // Red

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
