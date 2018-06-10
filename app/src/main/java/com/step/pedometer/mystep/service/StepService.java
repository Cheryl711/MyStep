package com.step.pedometer.mystep.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.step.pedometer.mystep.MainActivity;
import com.step.pedometer.mystep.R;
import com.step.pedometer.mystep.config.Constant;
import com.step.pedometer.mystep.pojo.StepData;
import com.step.pedometer.mystep.utils.CountDownTimer;
import com.step.pedometer.mystep.utils.DbUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class StepService extends Service implements SensorEventListener {
    private final String TAG="TAG_StepService";   //"StepService";
    //Default is 30 seconds for storage
    private static int duration=30000;
    private static String CURRENTDATE="";   //Current date
    private SensorManager sensorManager;    //Sensor Manager
    private StepDetector stepDetector;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private Messenger messenger=new Messenger(new MessengerHandler());
    //broadcast
    private BroadcastReceiver mBatInfoReceiver;
    private PowerManager.WakeLock mWakeLock;
    private TimeCount time;

    //Step sensor type 0-counter 1-detector 2-accelerometer
    private static int stepSensor = -1;
    private List<StepData> mStepData;

    //For step sensors
    private int previousStep;    //Used to record the number of steps
    private boolean isNewDay=false;    //Used to determine if it is a new day. If it is a new day, assign the previous step to the previousStep.

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case Constant.MSG_FROM_CLIENT:
                    try{
                        Messenger messenger=msg.replyTo;
                        Message replyMsg=Message.obtain(null,Constant.MSG_FROM_SERVER);
                        Bundle bundle=new Bundle();
                        //Send the current number of steps as a message
                        bundle.putInt("step",StepDetector.CURRENT_STEP);
                        replyMsg.setData(bundle);
                        messenger.send(replyMsg);  //Send message to return
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();
        //Initialize the broadcast
        initBroadcastReceiver();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Start Step Monitor
                startStepDetector();
            }
        }).start();
        startTimeCount();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        initTodayData();
        updateNotification("Steps today:"+StepDetector.CURRENT_STEP+" steps");
        return START_STICKY;
    }
    /**
     * Get today's date
     */
    private String getTodayDate(){
        Date date=new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * Date of the day of initialization
     */
    private void initTodayData(){
        CURRENTDATE=getTodayDate();
        //There is a judgment in the creation method, if the database has been created will not be created twice
        DbUtils.createDb(this,Constant.DB_NAME);

        //Get the date of the day
        List<StepData> list=DbUtils.getQueryByWhere(StepData.class,"today",new String[]{CURRENTDATE});
        if(list.size()==0||list.isEmpty()){
            //If you get the data of the day is empty, the number of steps is 0
            StepDetector.CURRENT_STEP=0;
            isNewDay=true;  //Used to determine whether to store previous data, later used
        }else if(list.size()==1){
            isNewDay=false;
            //Get the number of steps in the database if there is data for the current day in the database
            StepDetector.CURRENT_STEP=Integer.parseInt(list.get(0).getStep());
        }else{
            Log.e(TAG, "Error！");
        }
    }

    /**
     * Initialize the broadcast
     */
    private void initBroadcastReceiver(){
        //Defining intent filters
        final IntentFilter filter=new IntentFilter();
        //Screen off screen broadcast
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //Date modification
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        //Turn off the broadcast
        filter.addAction(Intent.ACTION_SHUTDOWN);
        //Screen highlight broadcast
        filter.addAction(Intent.ACTION_SCREEN_ON);
        //Screen unlock broadcast
        filter.addAction(Intent.ACTION_USER_PRESENT);
        //When the long press the power button to pop up the "shutdown" dialog or lock screen, the system will send this broadcast
        //example：Occasionally, system dialogs are used. Permissions can be very high and can be overridden on the lock screen or the "shutdown" dialog box.
        //So listen to this broadcast and hide your own dialogue when you receive it. For example, click on the pop-up dialog in the bottom right corner of the pad.
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        mBatInfoReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action=intent.getAction();

                if(Intent.ACTION_SCREEN_ON.equals(action)){
                    Log.v(TAG,"screen on");
                }else if(Intent.ACTION_SCREEN_OFF.equals(action)){
                    Log.v(TAG,"screen off");
                    save();
                    //Changed to 60 seconds for storage
                    duration=60000;
                }else if(Intent.ACTION_USER_PRESENT.equals(action)){
                    Log.v(TAG,"screen unlock");
                    save();
                    //Changed to 30 seconds for storage
                    duration=30000;
                }else if(Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())){
                    Log.v(TAG,"receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS  System dialog box appears");
                    //Save once
                    save();
                }else if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())){
                    Log.v(TAG,"receive ACTION_SHUTDOWN");
                    save();
                }else if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction())){
                    Log.v(TAG,"receive ACTION_TIME_CHANGED");
                    initTodayData();
                }
            }
        };
        registerReceiver(mBatInfoReceiver,filter);
    }

    private void startTimeCount(){
        time=new TimeCount(duration,1000);
        time.start();
    }

    /**
     * @param content
     */
    private void updateNotification(String content){
        builder=new NotificationCompat.Builder(this);
        builder.setPriority(Notification.PRIORITY_MIN);
        PendingIntent contentIntent=PendingIntent.getActivity(this,0,
                new Intent(this, MainActivity.class),0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.mipmap.ic_notification);
        builder.setTicker("M Fit");
        builder.setContentTitle("M Fit");
        //Setting is not cleared
        builder.setOngoing(true);
        builder.setContentText(content);
        Notification notification=builder.build(); //The above are the properties set in the constructor of the Notification constructor

        startForeground(0,notification);
        nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(R.string.app_name,notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private void startStepDetector(){
        if(sensorManager!=null&& stepDetector !=null){
            sensorManager.unregisterListener(stepDetector);
            sensorManager=null;
            stepDetector =null;
        }
        //Get sleep lock, the purpose is to keep the CPU running after the black screen, so that the service can continue to run
        getLock(this);
        sensorManager=(SensorManager)this.getSystemService(SENSOR_SERVICE);
        //Pedometer sensor can be used after android4.4
        int VERSION_CODES = Build.VERSION.SDK_INT;
        if(VERSION_CODES>=19){
            addCountStepListener();
        }else{
            addBasePedoListener();
        }
    }


    private void addCountStepListener(){
        Sensor detectorSensor=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(countSensor!=null){
            stepSensor = 0;
            Log.v(TAG, "countSensor Step sensor");
            sensorManager.registerListener(StepService.this,countSensor,SensorManager.SENSOR_DELAY_UI);
        }else if(detectorSensor!=null){
            stepSensor = 1;
            Log.v("base", "detector");
            sensorManager.registerListener(StepService.this,detectorSensor,SensorManager.SENSOR_DELAY_UI);
        }else{
            stepSensor = 2;
            Log.e(TAG,"Count sensor not available! No sensor available, only acceleration sensor");
            addBasePedoListener();
        }
    }


    /**
     * Use acceleration sensor
     */
    private void addBasePedoListener(){
        //The StepDetector class is only called when an acceleration sensor is used
        stepDetector =new StepDetector(this);
        //Get the sensor type, the type obtained here is the accelerometer
        //This method is used to register, only registered will take effect, parameters: SensorEventListener instance, Sensor instance, update rate
        Sensor sensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(stepDetector,sensor,SensorManager.SENSOR_DELAY_UI);
        stepDetector.setOnSensorChangeListener(new StepDetector.OnSensorChangeListener() {
            @Override
            public void onChange() {
                updateNotification("Steps today:"+StepDetector.CURRENT_STEP+" steps");
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(stepSensor == 0){   //Use pedometer sensor
            if(isNewDay) {
                //Used to determine if it is a new day. If it is then record the data in the step count of the pedometer sensor
                // The number of steps taken today = the number of steps the sensor is currently counting - the number of steps before the statistics
                previousStep = (int) event.values[0];    //Get the number of steps counted by the sensor
                isNewDay = false;
                save();
                //To prevent the database from being saved before the previousStep assignment, we update the information in the database.
                List<StepData> list=DbUtils.getQueryByWhere(StepData.class,"today",new String[]{CURRENTDATE});
                //change the data
                StepData data=list.get(0);
                data.setPreviousStep(previousStep+"");
                DbUtils.update(data);
            }else {
                //Remove the previous data
                List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "today", new String[]{CURRENTDATE});
                StepData data=list.get(0);
                this.previousStep = Integer.valueOf(data.getPreviousStep());
            }
            StepDetector.CURRENT_STEP=(int)event.values[0]-previousStep;

            //Or just use the following sentence, but the program may not be able to step after it is closed. According to demand can choose.
            //If you record the number of steps taken when the program is started, you can use this method - StepDetector.CURRENT_STEP++;

        }else if(stepSensor == 1){
            StepDetector.CURRENT_STEP++;
        }
        //Update status bar information
        updateNotification("Steps today：" + StepDetector.CURRENT_STEP + " steps");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * save data
     */
    private void save(){
        int tempStep=StepDetector.CURRENT_STEP;

        List<StepData> list=DbUtils.getQueryByWhere(StepData.class,"today",new String[]{CURRENTDATE});
        if(list.size()==0||list.isEmpty()){
            StepData data=new StepData();
            data.setToday(CURRENTDATE);
            data.setStep(tempStep+"");
            data.setPreviousStep(previousStep+"");
            DbUtils.insert(data);
        }else if(list.size()==1){
            //change the data
            StepData data=list.get(0);
            data.setStep(tempStep+"");
            DbUtils.update(data);
        }
    }

    @Override
    public void onDestroy(){
        //Cancel the foreground process
        stopForeground(true);
        DbUtils.closeDb();
        unregisterReceiver(mBatInfoReceiver);
        Intent intent=new Intent(this,StepService.class);
        startService(intent);
        super.onDestroy();
    }

    //  Synchronization method    Get sleep lock
    synchronized private PowerManager.WakeLock getLock(Context context){
        if(mWakeLock!=null){
            if(mWakeLock.isHeld()) {
                mWakeLock.release();
                Log.v(TAG,"Release lock");
            }

            mWakeLock=null;
        }

        if(mWakeLock==null){
            PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            mWakeLock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,StepService.class.getName());
            mWakeLock.setReferenceCounted(true);
            Calendar c=Calendar.getInstance();
            c.setTimeInMillis((System.currentTimeMillis()));
            int hour =c.get(Calendar.HOUR_OF_DAY);
            if(hour>=23||hour<=6){
                mWakeLock.acquire(5000);
            }else{
                mWakeLock.acquire(300000);
            }
        }
        Log.v(TAG,"Got the lock");
        return (mWakeLock);
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture,long countDownInterval){
            super(millisInFuture,countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            //If the timer ends normally, start step counting
            time.cancel();
            save();
            startTimeCount();
        }
    }


}
