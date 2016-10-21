package com.karuppiah.musicplayer;

import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;


class getFile extends Thread{

    long songId;
    String songTitle;
    String songPath;
    Socket clientsocket;
    Handler mHandler;
    int stop = 0;

    private static Object obj;
    private boolean mDebug = false;

    public getFile(long songId, String songTitle, String songPath, Socket clientsocket, Handler mHandler) {

        this.songId = songId;
        this.songTitle = songTitle;
        this.songPath = songPath;
        this.clientsocket = clientsocket;
        this.mHandler = mHandler;
    }

    public void setStop() {
        stop = 1;
    }

    @Override
    public void run() {

        long starttime, stoptime;
        int len;
        boolean once = true,send=true;
        int totlen = 0;
        int size = 60*1024;

        File localf = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/" + songId + " " + songTitle + ".mp3");

        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());

        long available = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

        Log.i("GET FILE","AVAILABLE BYTES : " + available);

        try {

            long fileSize;

            byte[] receiveData = null;

            receiveData = new byte[size];

            DataInputStream in;
            DataOutputStream out;

            try{

                out = new DataOutputStream(clientsocket.getOutputStream());

                out.writeUTF("Song");

                out.writeUTF(songPath);

                if(mDebug)
                Log.i("MUSIC SERVICE", "REQUEST PATH : " + songPath + " SENT!");

                in = new DataInputStream(clientsocket.getInputStream());

                fileSize = in.readLong();

            }catch (Exception e){

                if(mDebug)
                Log.i("MUSIC SERVICE", e.toString());

                e.printStackTrace();
                mHandler.obtainMessage(MusicService.ERROR,"Friend Disconnected!").sendToTarget();
                return;
            }

            if(mDebug)
            Log.i("MUSIC SERVICE","RESPONSE RECEIVED!");

            if(mDebug)
            Log.i("MUSIC SERVICE", "Server : filesize is : " + fileSize);

            if(fileSize==0)
            {
                mHandler.obtainMessage(MusicService.ERROR,songTitle + " file removed from friend's device!").sendToTarget();
                return;
            }

            if(fileSize>available)
            {
                mHandler.obtainMessage(MusicService.ERROR,"Not enough space to store the song").sendToTarget();
                return;
            }


            FileOutputStream fout = new FileOutputStream(localf, true);

            DataOutputStream dout = new DataOutputStream(fout);

            while (fileSize > 0 && ((len = in.read(receiveData, 0, (int) Math.min(receiveData.length, fileSize))) != -1)) {

                if (once) {
                    starttime = System.currentTimeMillis();
                    once = false;
                }

                dout.write(receiveData, 0, len);

                //Log.i("Server", "Length of buffer : " + receiveData.length + " and len is : " + len);

                totlen += len;


                /*if(send&&totlen>=2024) {
                    send = false;
                    mHandler.obtainMessage(MusicService.PLAYER, localf.getAbsolutePath()).sendToTarget();
                }*/


                fileSize -= len;
            }

            dout.flush();
            dout.close();

            stoptime = System.currentTimeMillis();

            mHandler.obtainMessage(MusicService.PLAYER,localf.getAbsolutePath()).sendToTarget();

        }catch (Exception e){

            if(mDebug)
            Log.i("MUSIC SERVICE", e.toString());

            e.printStackTrace();
            mHandler.obtainMessage(MusicService.ERROR,"Some error occured while transferring the file!").sendToTarget();
        }
    }

}