package com.karuppiah.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


public class DashBoard extends AppCompatActivity {

    private Button bListen;
    private Button bShare;

    Intent i;
    String name = "Karuppiah";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        bListen = (Button) findViewById(R.id.bListen);
        bShare = (Button) findViewById(R.id.bShare);

        bListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                i = new Intent(DashBoard.this,Listen.class);

                startActivity(i);

                finish();
            }
        });

        bShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                i = new Intent(DashBoard.this,ShareMusic.class);

                startActivity(i);

                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
