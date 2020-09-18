package com.moghis.yolov4;

import android.app.Application;
import android.graphics.Typeface;

public class MyApplication extends Application {

    static Typeface typeface;
    @Override
    public void onCreate() {
        super.onCreate();
        typeface=Typeface.createFromAsset(getAssets(),"fonts/iranian_sans.ttf");
    }

    public Typeface getTypeface()
    {
        return typeface;
    }

}
