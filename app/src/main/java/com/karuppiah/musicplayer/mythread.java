package com.karuppiah.musicplayer;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class mythread extends Thread {

    Handler mHandler;
    private boolean mDebug = false;

    public mythread(Handler mHandler) {

        this.mHandler = mHandler;
    }

    @Override
    public void run() {

        long starttime, stoptime;
        int len;
        boolean once = true;
        int totlen = 0;
        int size = 60*1024;

        try {
            File f = new File(Environment.getExternalStorageDirectory() + "/audio.txt");

            FileOutputStream fout = new FileOutputStream(f);

            DataOutputStream dout = new DataOutputStream(fout);

            ServerSocket serversocket = new ServerSocket(15890);

            Socket clientsocket = serversocket.accept();

            mHandler.obtainMessage(Listen.CONNECTED,"").sendToTarget();

            byte[] receiveData = null;

            receiveData = new byte[size];



            DataOutputStream out = new DataOutputStream(clientsocket.getOutputStream());

            out.writeUTF("List");



            DataInputStream in = new DataInputStream(clientsocket.getInputStream());

            long fileSize = in.readLong();

            if(mDebug)
            Log.i("MY THREAD", "filesize is : " + fileSize);

            while (fileSize > 0 && ((len = in.read(receiveData, 0, (int) Math.min(receiveData.length, fileSize))) != -1)) {

                if (once) {
                    starttime = System.currentTimeMillis();
                    once = false;
                }

                dout.write(receiveData,0,len);

                //Log.i("MY THREAD", "Length of data : " + receiveData.length + " and len is : " + len);

                totlen += len;

                fileSize -= len;
            }

            dout.flush();
            dout.close();

            Listen.serversocket = serversocket;
            Listen.clientsocket = clientsocket;

            mHandler.obtainMessage(Listen.DONE,"").sendToTarget();

            stoptime = System.currentTimeMillis();

            if(mDebug)
            Log.i("MY THREAD", "Thread over!");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

};