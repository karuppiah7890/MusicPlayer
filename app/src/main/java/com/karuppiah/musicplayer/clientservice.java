package com.karuppiah.musicplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.Socket;

public class clientservice extends Service {

    public static Socket socket;

    clientprocess c1, c2;
    InetAddress serverip = null;
    Messenger messenger;
    public static boolean over1 = false, over2 = false;
    int pktsize;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Bundle extras = intent.getExtras();

        messenger = (Messenger) extras.get("MESSENGER");

        serverip = (InetAddress) extras.get("SERVER_IP");

        pktsize = (int) extras.get("PACKET_SIZE");

        c1 = new clientprocess(this, serverip, messenger, 15890, 1, pktsize);

        //c2 = new clientprocess(this, serverip, messenger, 15895, 2, pktsize);

        c1.start();
        //c2.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if(socket!=null)
                socket.close();

            Log.i("ClientService","Socket Closed!");
        }catch (Exception e){
            e.printStackTrace();
        }

        super.onDestroy();
    }
}

class clientprocess extends Thread {

    InetAddress serverip = null;
    Context context;
    Socket socket = null;
    Messenger messenger;
    int pktsize;
    int port, i;


    public clientprocess(Context context, InetAddress serverip, Messenger messenger, int port, int i, int pktsize) {
        this.i = i;
        this.context = context;
        this.serverip = serverip;
        this.messenger = messenger;
        this.port = port;
        this.pktsize = pktsize;
    }

    @Override
    public void run() {

        int size = pktsize * 1024;
        int len = 0;
        int totlen = 0;
        Message msg;
        long subtot = 0;
        int j = 0;

        try {

            String result = null;

            socket = new Socket(serverip, port);

            clientservice.socket = socket;

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            //File f = new File(Environment.getExternalStorageDirectory() + "/video" + i + ".mp4");

            String path = "";

            File f = null;
            String ok = "";
            int flag;

            do {

                totlen = 0;
                flag = 0;

                String request = in.readUTF();

                if (request.equals("List")) {
                    f = new File(Environment.getExternalStorageDirectory() + "/audio.txt");
                }

                else if(request.equals("Song")){

                    path = in.readUTF();

                    f = new File(path);
                }

                else if(request.equals("Bye"))
                {
                    msg = Message.obtain();

                    msg.what = ShareMusic.PACKET_SENT;

                    msg.obj = "Friend Disconnected!!";

                    messenger.send(msg);

                    break;
                }

                else {
                    Log.i("CLIENT SERVICE","BAD REQUEST! : " + request);
                    continue; // CHANGE TO RETURN LATER.
                }

                if (f.exists()) {
                    DataInputStream din = new DataInputStream(new FileInputStream(f));

                    byte[] sendData = new byte[size];

                    long start = System.currentTimeMillis();

                    out.writeLong(f.length());

                    while ((len = din.read(sendData)) != -1) {

                        out.write(sendData, 0, len);

                        totlen += len;

                            /*subtot+=len;
                            j++;

                            if(j%170==0)
                            {
                                msg = Message.obtain();
                                msg.what = ShareMusic.CURRENT;
                                msg.obj = subtot;
                                messenger.send(msg);
                                subtot = 0;
                            }*/

                    }

                    if(flag==1)
                    {
                        msg = Message.obtain();

                        msg.what = ShareMusic.PACKET_SENT;

                        msg.obj = f.getName() + " File Data Sending Stopped!";

                        messenger.send(msg);

                        continue;
                    }

                    out.flush();

                    long stop = System.currentTimeMillis();

                    double time = (stop - start) / 1000.0;

                    double speed = (totlen / time) / 1048576.0;

                    msg = Message.obtain();

                    msg.what = ShareMusic.PACKET_SENT;

                    msg.obj = f.length() + "Bytes Sent to " + serverip.toString()
                            + " ! in " + time + " secs. Speed : " + speed + " MB/s ";

                    messenger.send(msg);

                    din.close();

                    if(request.equals("List"))
                        f.delete();

                    //socketclose();

                } else {
                    out.writeLong(0);
                    msg = Message.obtain();
                    msg.what = ShareMusic.PACKET_SENT;
                    msg.obj = path + "File Doesn't Exist!";
                    messenger.send(msg);
                    //socketclose();
                }


            }while(true);

        } catch (Exception e) {
            msg = Message.obtain();
            msg.what = ShareMusic.PACKET_SENT;
            msg.obj = "Error : " + e.toString();

            //socketclose();
            try {
                messenger.send(msg);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }

            e.printStackTrace();
        }
    }


    public void socketclose() {
        try {
            if (socket != null)
                socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void kill()
    {
        Message msg = Message.obtain();

        msg.what = ShareMusic.KILL;

        Log.i("Client " + i, "KILL");

        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
