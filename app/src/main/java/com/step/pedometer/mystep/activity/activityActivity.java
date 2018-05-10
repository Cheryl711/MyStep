package com.step.pedometer.mystep.activity;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.step.pedometer.mystep.R;

public class activityActivity extends AppCompatActivity {
    private static final String TAG = "activityActivity";

    private Context mContext = activityActivity.this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity);
        Log.d(TAG, "onCreate: 1view");




    }
}