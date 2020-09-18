package com.moghis.yolov4;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class CustomDialog extends Dialog{

    public static final int INFO = 1;
    public static final int EXIT = 2;
    private int content;
    private Context context;

    public CustomDialog(@NonNull Context context , int content) {
        super(context);
        this.context = context;
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyApplication app = (MyApplication)context.getApplicationContext();

        switch (content) {

            case INFO:
                setContentView(R.layout.dialog_info);

                Button okBtn = findViewById(R.id.okBtn);
                okBtn.setTypeface(app.getTypeface());

                TextView infoText = findViewById(R.id.infoText);
                infoText.setTypeface(app.getTypeface());

                okBtn.setOnClickListener(view -> dismiss());
                break;
            case EXIT:
                setContentView(R.layout.dialog_exit);

                Button stayBtn = findViewById(R.id.stayBtn);
                stayBtn.setTypeface(app.getTypeface());

                Button exitBtn = findViewById(R.id.exitBtn);
                exitBtn.setTypeface(app.getTypeface());

                TextView exitText = findViewById(R.id.exitText);
                exitText.setTypeface(app.getTypeface());

                stayBtn.setOnClickListener(view -> dismiss());
                exitBtn.setOnClickListener(view -> ((Activity) context ).finish());
                break;
        }


        setCanceledOnTouchOutside(false);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onBackPressed() {

    }
}
