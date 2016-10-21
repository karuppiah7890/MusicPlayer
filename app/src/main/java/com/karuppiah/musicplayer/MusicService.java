package com.karuppiah.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final int PLAYER = 2;
    public static final int ERROR = 3;

    private MediaPlayer player;
    private MediaController controller;
    private ArrayList<Song> songs;
    private int songPosn;

    getFile g = null;
    private boolean down = false;

    private boolean wait = true;

    private String songTitle = "";
    private long songId = -1;
    private static final int NOTIFY_ID = 1;

    ServerSocket serversocket = null;
    Socket clientsocket = null;

    private final IBinder musicBind = new MusicBinder();
    private MusicScreen mainactivity;

    private final Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case PLAYER :

                    down = false;

                    if(mDebug)
                    Log.i("MUSIC SERVICE","download set to false!");

                    File localf = new File( (String)msg.obj );

                    try {
                        FileInputStream fin = new FileInputStream(localf);
                        //Uri u = Uri.parse(localf.getAbsolutePath());
                        //player.setDataSource(MusicService.this,u);

                        player.setDataSource(fin.getFD());

                        if(mDebug)
                        Log.i("MUSIC SERVICE", "PLAYER DATA SOURCE SET [Downloaded File]");

                        player.prepareAsync();

                        if(mDebug)
                        Log.i("MUSIC SERVICE", "PREPARING");

                    } catch (Exception e) {

                        if(mDebug)
                        Log.i("MUSIC SERVICE","ERROR Toast Has to be created ! Did you see toast ? :P");

                        Toast.makeText(mainactivity, "An error Occured while playing the song!", Toast.LENGTH_LONG).show();

                        e.printStackTrace();
                    }

                    break;

                case ERROR :

                    down = false;

                    if(mDebug)
                    Log.i("MUSIC SERVICE","Toast Has to be created ! Did you see toast ? :P");

                    Toast.makeText(mainactivity, (String) msg.obj, Toast.LENGTH_LONG).show();

                    break;
            }
        }
    };

    private boolean mDebug = false;

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        player.stop();
        player.release();

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        songPosn = 0;

        player = new MediaPlayer();

        initMusicPlayer();
    }

    public boolean isDown()
    {
        return down;
    }

    public boolean isWait()
    {
        return wait;
    }

    public void initMusicPlayer() {

        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

    }

    public void setSockets() {
        serversocket = Listen.serversocket;
        clientsocket = Listen.clientsocket;

        if(mDebug)
        Log.i("MUSIC SERVICE", "CLIENT SOCKET : " + clientsocket.getRemoteSocketAddress().toString());

        if(mDebug)
        Log.i("MUSIC SERVICE", "MY SERVER SOCKET : " + serversocket.getLocalSocketAddress().toString());
    }

    public void setContext(Context context) {
        mainactivity = (MusicScreen) context;
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public void setController(MediaController controller) {
        this.controller = controller;
    }


    public void setSong(int songPosn) {

        if(mDebug)
        Log.i("MUSIC SERVICE","setSong Called");

        if(isDown())
            return;

        this.songPosn = songPosn;

        if(mDebug)
        Log.i("MUSIC SERVICE","songPosn Set!");
    }

    public void playSong() {

        wait = true;

        if(mDebug)
        Log.i("MUSIC SERVICE", "Wait Set to true!");

        player.reset();

        if(mDebug)
        Log.i("MUSIC SERVICE", "PLAYER RESET");

        Song playSong = songs.get(songPosn);

        songTitle = playSong.getTitle();
        songId = playSong.getID();

        String songPath = playSong.getPath();

        File localf = new File(Environment.getExternalStorageDirectory() + "/SharedMusic/" + songId + " " + songTitle + ".mp3");

        if(!localf.exists())
        {
            if(mDebug)
            Log.i("MUSIC SERVICE","Have to download!");

            if(isDown())
                return;

            down = true;

            if(mDebug)
            Log.i("MUSIC SERVICE","download set to true!");

            /*
            if(g!=null && g.isAlive()){

                g.setStop();

                try{

                    g.join();

                }catch (Exception e){

                    Log.i("MUSIC SERVICE","EXCEPTION IN THREAD JOIN");
                    e.printStackTrace();
                }

            } */

            g = new getFile(songId,songTitle,songPath,clientsocket,mHandler);

            g.start();

            if(mDebug)
            Log.i("MUSIC SERVICE", "download starts!");

            return;
        }

        Uri u = Uri.parse(localf.getAbsolutePath());

        try {

            player.setDataSource(this, u);

            if(mDebug)
            Log.i("MUSIC SERVICE", "PLAYER DATA SOURCE SET [Local File]");

        } catch (Exception e) {
            if(mDebug)
            Log.e("MUSIC SERVICE", "Error while setting data source", e);
        }

        if(mDebug)
        Log.i("MUSIC SERVICE", "PLAYER PREPARING ASYNC");

        try {

            player.prepareAsync();

        }catch (Exception e){

            if(mDebug)
            Log.i("MUSIC SERVICE","Error Toast Has to be created ! Did you see toast ? :P");
            Toast.makeText(mainactivity,"An error Occured!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        Log.i("MUSIC SERVICE", "PLAYER PREPARED ASYNC");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        if(mDebug)
        Log.i("MUSIC SERVICE ERROR","AN ERROR OCCURRED ! LOOK AT THIS LOG CAREFULLY");

        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        mp.start();
        wait = false;

        if(mDebug)
        Log.i("MUSIC SERVICE","wait set to false!");

        controller.show(0);

        if(mDebug)
        Log.i("MUSIC SERVICE", "PLAYER STARTED");

        Intent notIntent = new Intent(this, MusicScreen.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);

    }


    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
    }

    public void playPrev() {
        songPosn--;

        if (songPosn < 0)
            songPosn = songs.size() - 3;

        playSong();
    }

    public void playNext() {
        songPosn++;

        if (songPosn == songs.size() - 2)
            songPosn = 0;

        playSong();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }
}