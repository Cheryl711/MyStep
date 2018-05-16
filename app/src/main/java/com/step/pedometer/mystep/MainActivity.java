package com.step.pedometer.mystep;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.step.pedometer.mystep.activity.activityActivity;
import com.step.pedometer.mystep.config.Constant;
import com.step.pedometer.mystep.login.LoginActivity;
import com.step.pedometer.mystep.service.StepService;

public class MainActivity extends AppCompatActivity  implements Handler.Callback {


    private static final String TAG = "MainActivity";
    private Context mContext = MainActivity.this;

   // firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;


    //循环取当前时刻的步数中间的时间间隔
    private long TIME_INTERVAL = 500;
    //控件
    private TextView text_step;    //显示走的步数
    private TextView text_calories;

    private Messenger messenger;
    private Messenger mGetReplyMessenger = new Messenger(new Handler(this));
    private Handler delayHandler;

    //以bind形式开启service，故有ServiceConnection接收回调
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                messenger = new Messenger(service);
                Message msg = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                msg.replyTo = mGetReplyMessenger;
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    //接收从服务端回调的步数
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Constant.MSG_FROM_SERVER:
                //更新步数
                text_step.setText(msg.getData().getInt("step") + "");
                text_calories.setText(msg.getData().getInt("step")/43 + "");
                delayHandler.sendEmptyMessageDelayed(Constant.REQUEST_SERVER, TIME_INTERVAL);
                break;
            case Constant.REQUEST_SERVER:
                try {
                    Message msgl = Message.obtain(null, Constant.MSG_FROM_CLIENT);
                    msgl.replyTo = mGetReplyMessenger;
                    messenger.send(msgl);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_step = (TextView) findViewById(R.id.main_text_step);
        text_calories = (TextView) findViewById( R.id.main_text_calories );
        delayHandler = new Handler(this);



        ImageView view1=(ImageView) findViewById(R.id.activitybutton);

        view1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: button1");

                Intent intent =new Intent(MainActivity.this, activityActivity.class);
                startActivity(intent);
            }
        });

        ImageView view2 = (ImageView) findViewById(R.id.profileMenu);

        view2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to login screen");

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

    }
    @Override
    public void onStart() {
        super.onStart();
        setupService();
    }
    /**
     * 开启服务
     */
    private void setupService() {
        Intent intent = new Intent(this, StepService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        //取消服务绑定
        unbindService(conn);
        super.onDestroy();
    }

//     /*
//    -----------Firebase-----------------
//     */
//
//    //checks to see if the @param 'user' is logged in
//    private void checkCurrentUser (FirebaseUser user){
//        Log.d(TAG, "checkCurrentUser: checking if user is logged in.");
//
//        if (user == null){
//            Intent intent = new Intent (mContext, LoginActivity.class);
//            startActivity(intent);
//        }
//    }
//
//    //set up the firebase auth object
//    private void setupFirebaseAuth(){
//        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth");
//
//        mAuth = FirebaseAuth.getInstance();
//
//        mAuthListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                FirebaseUser user = firebaseAuth.getCurrentUser();
//
//                // check if the user is logged in
//                checkCurrentUser(user);
//
//                if (user != null){
//                    // user is signed in
//                    Log.d(TAG, "onAuthStateChanged: sined_in: " + user.getUid());
//                } else {
//                    // user is signed out
//                    Log.d(TAG, "onAuthStateChanged: signed_out");
//                }
//            }
//        };
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        // Check if user is signed in (non-null) and update UI accordingly.
//        mAuth.addAuthStateListener(mAuthListener);
//        checkCurrentUser(mAuth.getCurrentUser());
//        setupService();
//    }

//    public void onStop(){
//        super.onStop();
//        if(mAuthListener != null){
//            mAuth.removeAuthStateListener(mAuthListener);
//        }
//    }

    /*
    -----------Firebase-----------------
     */


}

