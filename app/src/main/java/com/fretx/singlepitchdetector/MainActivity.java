package com.fretx.singlepitchdetector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public AudioInputHandler audioInputHandler;
    protected Thread audioThread;
    protected PitchDetectorYin yin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TODO: Dynamic handling of parameters
        audioInputHandler = new AudioInputHandler(44100,7200);
        yin = new PitchDetectorYin(44100,1764,882,0.10);
        audioInputHandler.addAudioAnalyzer(yin);
        audioThread = new Thread(audioInputHandler,"Audio Thread");
        audioThread.start();

//        Log.d("MainActivity", Float.toString(yin.result.getPitch()));

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(25);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView pitchText = (TextView) findViewById(R.id.pitchText );
                                float pitch = yin.result.getPitch();
                                if(pitch == -1){
                                    pitchText.setText("");
                                } else{
                                    pitchText.setText( Integer.toString((int) Math.round(pitch)) + " Hz");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();
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


