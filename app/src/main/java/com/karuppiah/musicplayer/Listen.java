package com.karuppiah.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class Listen extends AppCompatActivity {

    public static ServerSocket serversocket = null;
    public static Socket clientsocket = null;

    public static final int CONNECTED = 1;
    public static final int DONE = 2;

    public TextView tvListenStatus;

    private int flag = 0;

    Intent i;

    private final Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case CONNECTED :

                    tvListenStatus.setText("Connecting...");
                    break;

                case DONE :

                    tvListenStatus.setText("Done!");

                    startActivity(i);

                    finish();

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listen);

        tvListenStatus = (TextView) findViewById(R.id.tvListenStatus);

        i = new Intent(Listen.this,MusicScreen.class);

        tvListenStatus.setText("Ask your friend to click \"Share\" ");

        mythread t = new mythread(mHandler);

        t.start();

        Log.i("DASHBOARD", "Waiting for thread to finish");

        File dir = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/");

        dir.mkdirs();

        Toast.makeText(Listen.this, "All songs are stored to\n" + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
