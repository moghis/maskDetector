package com.moghis.yolov4;

import android.graphics.Color;
import android.graphics.RectF;

public class Box {
    public float x0,y0,x1,y1;
    private int label;
    private float score;
    private static String[] labels={"with mask incorrectly", "with mask", "no mask"};
    public Box(float x0,float y0, float x1, float y1, int label, float score){
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.label = label;
        this.score = score;
    }

    public RectF getRect(){
        return new RectF(x0,y0,x1,y1);
    }

    public String getLabel(){
        return labels[label];
    }

    public float getScore(){
        return score;
    }

    public int getColor(){

        int blue;
        int green;
        int red;

        if(label == 0){
            blue = 52;
            green = 186;
            red = 235;
        } else if (label == 1){
            blue = 0;
            green = 255;
            red = 0;
        } else {
            blue = 0;
            green = 0;
            red = 255;
        }
        return Color.argb(255,red,green,blue);
    }
}
