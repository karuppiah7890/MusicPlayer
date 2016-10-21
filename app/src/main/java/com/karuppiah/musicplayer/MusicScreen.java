package com.karuppiah.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicScreen extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private MusicController controller;

    private ArrayList<Song> songList;
    private ListView songView;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    private boolean paused = false, playbackPaused = false;
    private boolean mDebug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musicscreen);

        songView = (ListView) findViewById(R.id.song_list);

        songList = new ArrayList<Song>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        songList.add(new Song(0, "", "", ""));

        songList.add(new Song(0, "", "", ""));

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        setController();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);

            if(mDebug)
            Log.i("MAIN ACTIVITY", "Service Started");
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);

            musicSrv.setSockets();
            musicSrv.setContext(MusicScreen.this);
            musicSrv.setController(controller);

            musicBound = true;

            if(mDebug)
            Log.i("MAIN ACTIVITY", "Service bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;

            if(mDebug)
            Log.i("MAIN ACTIVITY", "Service Unbound / disconnected");
        }
    };

    public void songPicked(View view) {

        if(musicSrv.isDown())
        {
            Toast.makeText(this, "Already Downloading a song, Please Wait", Toast.LENGTH_LONG).show();
            return;
        }

        int i = Integer.parseInt(view.getTag().toString());

        if (i < songList.size() - 2) {

            //setController();

            musicSrv.setSong(i);

            musicSrv.playSong();

            /*if (playbackPaused) {
                setController();
                playbackPaused = false;
            }*/
        }

    }


    private void setController() {

        if (controller == null)
            controller = new MusicController(this);

        controller.setPrevNextListeners(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playNext();
                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPrev();
                    }
                });

        controller.setMediaPlayer(this);

        controller.setAnchorView(findViewById(R.id.song_list));

        controller.setEnabled(true);
    }

    private void playNext() {

        if(musicSrv.isWait()&&musicSrv.isDown())
        {
            Toast.makeText(this, "Already Downloading a song, Please Wait", Toast.LENGTH_LONG).show();
            return;
        }

        //setController();

        musicSrv.playNext();

        /*if (playbackPaused) {
            setController();
            playbackPaused = false;
        }*/

    }

    private void playPrev() {

        if(musicSrv.isWait()&&musicSrv.isDown())
        {
            Toast.makeText(this, "Already Downloading a song, Please Wait", Toast.LENGTH_LONG).show();
            return;
        }

        //setController();

        musicSrv.playPrev();

        /*if (playbackPaused) {
            setController();
            playbackPaused = false;
        }*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_musicscreen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_shuffle:
                break;

            case R.id.action_end:

                if(playIntent!=null)
                stopService(playIntent);

                musicSrv = null;
                finish();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getSongList() {

        File f = new File(Environment.getExternalStorageDirectory() + "/audio.txt");

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));

            String s;

            while ((s = br.readLine()) != null) {
                long id = Long.parseLong(s);

                s = br.readLine();
                String title = s;

                s = br.readLine();
                String artist = s;

                s = br.readLine();
                String path = s;

                songList.add(new Song(id, title, artist, path));
            }

            br.close();

        } catch (Exception e) {
            if(mDebug)
            Log.i("MusicScreen","Error in reading audio file!");
            e.printStackTrace();
        }

        f.delete();
    }

    @Override
    public void start() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "start");

        if(!musicSrv.isWait())
        {
            if(mDebug)
            Log.i("MAIN ACTIVITY", "start : playing");

            musicSrv.go();
        }

    }

    @Override
    public void pause() {
        playbackPaused = true;

        if(mDebug)
        Log.i("MAIN ACTIVITY", "pause");

        if(musicSrv.isPng()){
            if(mDebug)
            Log.i("MAIN ACTIVITY", "pause : pausing");

            musicSrv.pausePlayer();
        }

    }

    @Override
    public int getDuration() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "getDuration");

        if (musicSrv != null && musicBound && !musicSrv.isWait()) {

            if(mDebug)
            Log.i("MAIN ACTIVITY", "getDuration : " + musicSrv.getDur());

            return musicSrv.getDur();

        } else return 0;
    }

    @Override
    public int getCurrentPosition() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "getCurrentPosition");

        if (musicSrv != null && musicBound && !musicSrv.isWait()) {

            if(mDebug)
            Log.i("MAIN ACTIVITY", "getCurrentPosition : " + musicSrv.getPosn());

            return musicSrv.getPosn();

        } else return 0;
    }

    @Override
    public void seekTo(int pos) {

        if(musicSrv!=null&&!musicSrv.isWait()){
            if(mDebug)
            Log.i("MAIN ACTIVITY", "seekTo " + pos);
            musicSrv.seek(pos);
        }

    }

    @Override
    public boolean isPlaying() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "isPlaying");

        if (musicSrv != null && musicBound)
        {
            if(mDebug)
                Log.i("MAIN ACTIVITY", "isPlaying : " + musicSrv.isPng());

            return musicSrv.isPng();
        }

        else return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "canPause");

        if(musicSrv!=null){

            if(mDebug)
            Log.i("MAIN ACTIVITY", "canPause : " + musicSrv.isPng());

            return musicSrv.isPng();
        }


        return true;
    }

    @Override
    public boolean canSeekBackward() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "canSeekBackward");

        if(musicSrv!=null)
        {
            if(mDebug)
            Log.i("MAIN ACTIVITY", "canSeekBackward : " + musicSrv.isPng());

            return musicSrv.isPng();
        }

        return true;
    }

    @Override
    public boolean canSeekForward() {

        if(mDebug)
        Log.i("MAIN ACTIVITY", "canSeekForward");

        if(musicSrv!=null)
        {
            if(mDebug)
            Log.i("MAIN ACTIVITY", "canSeekForward : " + musicSrv.isPng());

            return musicSrv.isPng();
        }

        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }


    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    /*
    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }
    */

    @Override
    protected void onDestroy() {

        if(playIntent!=null)
        stopService(playIntent);

        try {

            if(Listen.clientsocket!=null) {
                DataOutputStream out = new DataOutputStream(Listen.clientsocket.getOutputStream());
                out.writeUTF("Bye");
                out.flush();
                if(mDebug)
                Log.i("MUSIC SERVICE", "SENT BYE!");

                Listen.clientsocket.close();

                if (Listen.serversocket != null)
                    Listen.serversocket.close();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        musicSrv = null;

        super.onDestroy();
    }
}