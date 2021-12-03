package com.brandon.scraper;

import com.codename1.charts.util.ColorUtil;

public enum Grade {
    // https://flatuicolors.com/palette/us
    A (ColorUtil.rgb(0, 184, 148), 93.0, 1000.0), // Dark green
    Am (ColorUtil.rgb(65, 219, 176), 90.0, 92.99), // Light green -20 darker
    Bp (ColorUtil.rgb(0, 206, 201), 87.0, 89.99), // Turquoise
    B (ColorUtil.rgb(116, 185, 255), 83.0, 86.99), // Light blue
    Bm (ColorUtil.rgb(9, 132, 227), 80.0, 82.99), // Dark blue
    Cp (ColorUtil.rgb(225, 112, 85),77.0,79.99), // Orange
    C (ColorUtil.rgb(225, 112, 85), 73.0,76.99), // Orange
    Cm (ColorUtil.rgb(225, 112, 85), 70.0,72.99), // Orange
    Dp (ColorUtil.rgb(214, 48, 49),67.0, 69.99), // Red
    D (ColorUtil.rgb(214, 48, 49), 60.0,66.99), // Red
    E (ColorUtil.rgb(214, 48, 49),0.0,59.99), // Red
    NA (ColorUtil.rgb(99, 110, 114),-2.0,0.0); //Grey

    private int color;
    private Double max;
    private Double min;
    Grade(int color, Double min, Double max){
        this.color = color;
        this.min = min;
        this.max = max;
    }

    public int getColor(){
        return this.color;
    }

    public static int getGradeColor(Double percent){
        for(Grade g : Grade.values()){
            if(percent >= g.min && percent < g.max){
                return g.color;
            }
        }
        return 0;
    }

    public static int getGradeColor(String percentS){

        if(percentS.equals("NA")){
            return NA.color;
        }
        Double percent = Double.parseDouble(percentS);
        for(Grade g : Grade.values()){
            if(percent >= g.min && percent < g.max){
                return g.color;
            }
        }
        return 0;
    }
    
    public static int getGradeColorFromFraction(Double points, Double total){
        if(total == 0){
            return A.color;
        }
        return getGradeColor(points/total*100);
    }

}
