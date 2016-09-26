package com.fretx.singlepitchdetector;

import android.media.AudioRecord;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

//TODO: The UI and device-specific settings are placeholders for now

public class MainActivity extends AppCompatActivity {

    public AudioInputHandler audioInputHandler;
    protected Thread audioThread;
    protected PitchDetectorYin yin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioInputHandler = new AudioInputHandler(44100,7200);
        yin = new PitchDetectorYin(44100,7200,882,441,0.2);
        audioInputHandler.addAudioAnalyzer(yin);
        audioThread = new Thread(audioInputHandler,"Audio Thread");
        audioThread.start();
        //TODO: show the result on the TextView
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onResume(){
        super.onResume();
    }

    protected void onDestroy(){
        audioInputHandler.onDestroy();
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

}


