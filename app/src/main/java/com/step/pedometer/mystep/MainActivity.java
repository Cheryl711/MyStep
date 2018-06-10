package com.step.pedometer.mystep;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.step.pedometer.mystep.activity.activityActivity;
import com.step.pedometer.mystep.config.Constant;
import com.step.pedometer.mystep.login.LoginActivity;
import com.step.pedometer.mystep.service.StepService;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity  implements Handler.Callback {


    private static final String TAG = "MainActivity";
    private Context mContext = MainActivity.this;
    private String c = "0.000000001";
    private EditText editText;
    private TextView showTextView;
    private String fileName = "chenqu_java.txt";



    //Cycle the interval between the number of steps at the current time
    private long TIME_INTERVAL = 500;
    //Controls
    private TextView text_step;    //Shows the number of steps taken
    private TextView text_calories;

    private Messenger messenger;
    private Messenger mGetReplyMessenger = new Messenger( new Handler( this ) );
    private Handler delayHandler;

    //Open service as bind, so ServiceConnection receives callbacks
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                messenger = new Messenger( service );
                Message msg = Message.obtain( null, Constant.MSG_FROM_CLIENT );
                msg.replyTo = mGetReplyMessenger;
                messenger.send( msg );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    //Receive the number of steps from the server callback
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Constant.MSG_FROM_SERVER:
                //Update steps
                DecimalFormat df = new DecimalFormat( ".00" );
                text_step.setText( msg.getData().getInt( "step" ) + "" );
                text_calories.setText( df.format( msg.getData().getInt( "step" ) * Double.parseDouble( c ) * 0.000357781754 ) + "" );
                delayHandler.sendEmptyMessageDelayed( Constant.REQUEST_SERVER, TIME_INTERVAL );
                break;
            case Constant.REQUEST_SERVER:
                try {
                    Message msgl = Message.obtain( null, Constant.MSG_FROM_CLIENT );
                    msgl.replyTo = mGetReplyMessenger;
                    messenger.send( msgl );
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        text_step = (TextView) findViewById( R.id.main_text_step );
        text_calories = (TextView) findViewById( R.id.main_text_calories );
        delayHandler = new Handler( this );

        editText = (EditText) findViewById( R.id.addText );
        showTextView = (TextView) findViewById( R.id.showText );
        Button addButton = (Button) this.findViewById( R.id.addButton );

        addButton.setOnClickListener( listener );


        ImageView view1 = (ImageView) findViewById( R.id.activitybutton );

        view1.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d( TAG, "onClick: button1" );

                Intent intent = new Intent( MainActivity.this, activityActivity.class );
                startActivity( intent );
            }
        } );

        ImageView view2 = (ImageView) findViewById( R.id.profileMenu );

        view2.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d( TAG, "onClick: navigating to login screen" );

                Intent intent = new Intent( MainActivity.this, LoginActivity.class );
                startActivity( intent );
            }
        } );

    }

    // Statement listener
    private View.OnClickListener listener = new View.OnClickListener() {
        public void onClick(View v) {
            Button view = (Button) v;
            switch (view.getId()) {
                case R.id.addButton:
                    save();
                    break;


            }

        }

    };


    private void save() {
        String content = editText.getText().toString();
        c = content;
        try {

            FileOutputStream outputStream = openFileOutput( fileName,
                    Activity.MODE_PRIVATE );
            outputStream.write( content.getBytes() );
            outputStream.flush();
            outputStream.close();
            Toast.makeText( MainActivity.this, "Calories updated", Toast.LENGTH_LONG ).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void read() {
        try {
            FileInputStream inputStream = this.openFileInput( fileName );
            byte[] bytes = new byte[1024];
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            while (inputStream.read( bytes ) != -1) {
                arrayOutputStream.write( bytes, 0, bytes.length );
            }
            inputStream.close();
            arrayOutputStream.close();
            String content = new String( arrayOutputStream.toByteArray() );
            showTextView.setText( content );

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        setupService();
    }

    /**
     * Open service
     */
    private void setupService() {
        Intent intent = new Intent( this, StepService.class );
        bindService( intent, conn, Context.BIND_AUTO_CREATE );
        startService( intent );
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack( true );
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        //Cancel service binding
        unbindService( conn );
        super.onDestroy();
    }

}

