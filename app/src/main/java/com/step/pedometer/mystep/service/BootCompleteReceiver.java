package com.step.pedometer.mystep.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Boot up to complete the broadcast

 */

public class BootCompleteReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent){
        Intent i=new Intent(context,StepService.class);
        context.startService(i);
    }
}
