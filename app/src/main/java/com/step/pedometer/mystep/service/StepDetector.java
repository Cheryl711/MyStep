package com.step.pedometer.mystep.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import com.step.pedometer.mystep.utils.CountDownTimer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Acceleration sensor to monitor the number of steps

 */

public class StepDetector implements SensorEventListener {
    private final String TAG="TAG_StepDetector";     //"StepDetector";
    //The number of three axis data (x,y,z)
    private final int valueNum=5;
    //Peak difference for storing calculation thresholds
    private float[] tempValue =new float[valueNum];
    private int tempCount=0;
    //Whether the rising flag
    private boolean isDirectionUp=false;
    //Rising times
    private int continueUpCount=0;
    //The number of consecutive rises in the previous point, in order to record the number of rises in the peaks
    private int continueUpFormerCount=0;
    //The state of the previous point, rising or falling
    private boolean lastStatus =false;
    //Wave peak
    private float peakOfWave=0;
    //Trough
    private float valleyOfWave=0;
    //The peak time
    private long timeOfThisPeak=0;
    //Last peak time
    private long timeOfLastPeak=0;
    //Current time
    private long timeOfNow=0;
    //Last sensor value
    private float gravityOld=0;
    //Dynamic threshold requires dynamic data, this value is used for the threshold of these dynamic data
    private final float initialValue=(float)1.7;
    //Initial threshold
    private float ThreadValue=(float)2.0;

    //Initial range
    private float minValue=11f;
    private float maxValue=19.6f;

    /**
     * 0-Preparation, 1-Timer, 2-Normal Step Counter
     */
    private int CountTimeState=0;
    //Record the current number of steps
    public static int CURRENT_STEP=0;
    //Record the number of temporary steps
    public static int TEMP_STEP=0;
    //Record the number of temporary steps last time
    private int lastStep=-1;
    //Calculate the average using the three dimensions of the x, y, and z axes
    public static float average=0;
    private Timer timer;
    //Countdown 3.5 seconds, pedometer not displayed within 3.5 seconds, used to shield minor fluctuations
    private long duration=3500;
    private TimeCount time;
    OnSensorChangeListener onSensorChangeListener;

    // Define the callback function
    public interface OnSensorChangeListener{
        void onChange();
    }
    //Constructor
    public StepDetector(Context context){
        super();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1){

    }

    //Listener set method
    public void setOnSensorChangeListener(OnSensorChangeListener onSensorChangeListener){
        this.onSensorChangeListener=onSensorChangeListener;
    }
    //The function that is called when the sensor changes
    @Override
    public void onSensorChanged(SensorEvent event){
        Sensor sensor=event.sensor;
        //Sync block
        synchronized (this){
            //Acceleration sensor
            if(sensor.getType()==sensor.TYPE_ACCELEROMETER){
                calc_step(event);
            }
        }
    }

    synchronized private void calc_step(SensorEvent event){
        //Calculate the average value of the x, y, and z axes of the accelerometer (to compensate for data errors caused by excessive values in one direction)
        average=(float)Math.sqrt(Math.pow(event.values[0],2)
                +Math.pow(event.values[1],2)+Math.pow(event.values[2],2));
        detectorNewStep(average);
    }

    /**
     * Monitor new steps
     */
    private void detectorNewStep(float values) {
        if(gravityOld==0){
            gravityOld=values;
        }else{
            if(DetectorPeak(values,gravityOld)){
                timeOfLastPeak=timeOfThisPeak;
                timeOfNow=System.currentTimeMillis();

                if(timeOfNow-timeOfLastPeak>=200&&(peakOfWave-valleyOfWave>=ThreadValue)
                        &&(timeOfNow-timeOfLastPeak)<=2000){
                    timeOfThisPeak=timeOfNow;
                    //Update interface processing does not involve algorithms
                    preStep();
                }
                if(timeOfNow-timeOfLastPeak>=200
                        &&(peakOfWave-valleyOfWave>=initialValue)){
                    timeOfThisPeak=timeOfNow;
                    ThreadValue=Peak_Valley_Thread(peakOfWave-valleyOfWave);
                }
            }
        }
        gravityOld=values;
    }

    /**
     * Determine the status and step count
     */
    private void preStep(){
        if(CountTimeState==0){
            //Turn on the timer (3.5 seconds countdown, 0.7 second countdown interval) to monitor every 0.7 side within 3.5 seconds.
            time=new TimeCount(duration,700);
            time.start();
            CountTimeState=1;  //Timing
            Log.v(TAG,"Turn on the timer");
        }else if(CountTimeState==1){
            TEMP_STEP++;          //If the data measured by the sensor satisfies the step-by-step condition, the number of steps increases by one.
            Log.v(TAG,"Timing TEMP_STEP:"+TEMP_STEP);
        }else if(CountTimeState==2){
            CURRENT_STEP++;
            if(onSensorChangeListener!=null){
                //Call onChange() here so that the number of steps in the status bar is constantly updated in the StepService
                onSensorChangeListener.onChange();
            }
        }
    }

    /**
     * Monitoring crest
     * The following four conditions are judged as peaks
     * 1. The current trend is to decline: isDirectionUp is false
     * 2. The previous point is a rising trend: lastStatus is true
     * 3. Until the crest, continue to rise more than or equal to 2 times
     * 4. Peak value greater than 1.2g, less than 2g
     * Record troughs
     * 1. Observe the waveform diagram, you can find where the step appears, the next wave trough is the peak, there are more obvious features and differences
     * 2. So record each trough value, in order to compare with the next crest
     * @param newValue
     * @param oldValue
     * @return
     */
    public boolean DetectorPeak(float newValue,float oldValue){
        lastStatus=isDirectionUp;
        if(newValue>=oldValue){
            isDirectionUp=true;
            continueUpCount++;
        }else{
            continueUpFormerCount=continueUpCount;
            continueUpCount=0;
            isDirectionUp=false;
        }
        if(!isDirectionUp&&lastStatus&&(continueUpFormerCount>=2&&(oldValue>=minValue&&oldValue<maxValue))){
            //Satisfy the four conditions above the peak, this time the peak state
            peakOfWave=oldValue;
            return true;
        }else if(!lastStatus&&isDirectionUp){
            //Satisfy trough conditions, which are trough states
            valleyOfWave=oldValue;
            return false;
        }else{
            return false;
        }
    }

    /**
     * Threshold calculation
     * 1. Calculate the threshold value by the difference between the wave troughs
     * 2. Record 4 values and store them in the tempValue[] array
     * 3. The threshold is calculated by passing the array into the function averageValue
     * @param value
     * @return
     */
    public float Peak_Valley_Thread(float value){
        float tempThread=ThreadValue;
        if(tempCount<valueNum){
            tempValue[tempCount]=value;
            tempCount++;
        }else{
            //此时tempCount=valueNum=5
            tempThread=averageValue(tempValue,valueNum);
            for(int i=1;i<valueNum;i++){
                tempValue[i-1]=tempValue[i];
            }
            tempValue[valueNum-1]=value;
        }
        return tempThread;
    }

    /**
     * Gradient threshold
     * 1. Calculate the mean of the array
     *2. Gradualize the threshold in a range by averaging
     * @param value
     * @param n
     * @return
     */
    public float averageValue(float value[],int n){
        float ave=0;
        for(int i=0;i<n;i++){
            ave+=value[i];
        }
        ave=ave/valueNum;  //Calculate the array mean
        if(ave>=8){
            Log.v(TAG,"Over 8");
            ave=(float)4.3;
        }else if(ave>=7&&ave<8){
            Log.v(TAG,"7-8");
            ave=(float)3.3;
        }else if(ave>=4&&ave<7){
            Log.v(TAG,"4-7");
            ave=(float)2.3;
        }else if(ave>=3&&ave<4){
            Log.v(TAG,"3-4");
            ave=(float)2.0;
        }else{
            Log.v(TAG,"else (ave<3)");
            ave=(float)1.7;
        }
        return ave;
    }




    class TimeCount extends CountDownTimer {
        /**
         * Constructor
         * @param millisInFuture countdown time
         * @param countDownInterval countdown interval
         */
        public TimeCount(long millisInFuture,long countDownInterval){
            super(millisInFuture,countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if(lastStep==TEMP_STEP){
                //For a period of time, if TEMP_STEP does not increase the number of steps, the timer stops and the pedometer stops
                Log.v(TAG,"onTick Stop timing");
                time.cancel();
                CountTimeState=0;
                lastStep=-1;
                TEMP_STEP=0;
            }else{
                lastStep=TEMP_STEP;
            }

        }

        @Override
        public void onFinish() {
            //If the timer ends normally, start step counting
            time.cancel();
            CURRENT_STEP+=TEMP_STEP;
            lastStep=-1;
            Log.v(TAG,"The timer ends normally");

            timer=new  Timer(true);
            TimerTask task=new TimerTask(){
                public void run(){
                    //Stop pedometer when the number of steps is not increasing
                    if(lastStep==CURRENT_STEP){
                        timer.cancel();
                        CountTimeState=0;
                        lastStep=-1;
                        TEMP_STEP=0;
                        Log.v(TAG,"Stop pedometer："+CURRENT_STEP);
                    }else{
                        lastStep=CURRENT_STEP;
                    }
                }
            };
            timer.schedule(task,0,2000);   //Perform every two seconds and constantly monitor if you have stopped moving.
            CountTimeState=2;
        }
    }
}
