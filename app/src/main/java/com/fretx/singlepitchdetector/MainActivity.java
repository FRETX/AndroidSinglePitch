package com.fretx.singlepitchdetector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public AudioInputHandler audioInputHandler;
    protected Thread audioThread;
    protected PitchDetectorYin yin;

    private TextView pitchText;
    private TextView medianText;
    private PitchView pitchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI references
        pitchText = (TextView) findViewById(R.id.pitchText );
        medianText = (TextView) findViewById(R.id.medianPitch);
        pitchView = (PitchView) findViewById(R.id.pitchView);
        initGui();

        //Audio Parameters
        int maxFs = AudioInputHandler.getMaxSamplingFrequency();
        int minBufferSize = AudioInputHandler.getMinBufferSize(maxFs);
        audioInputHandler = new AudioInputHandler(maxFs,minBufferSize);
        int minF0 = 60;
        int frameLength = (int)(2*(float)maxFs/(float)minF0);
        float frameOverlap = 0.5f;
        float yinThreshold = 0.10f;
        //We set the lower bound of pitch detection (minF0) to 60Hz considering the guitar strings
        //The minimum buffer size for YIN must be minT0 * 2, where minT0 is the wavelength corresponding to minF0
        //So the frame length for YIN in samples is: (1/minF0) * 2 * maxFs

        //Create new pitch detector
        yin = new PitchDetectorYin(maxFs,frameLength,Math.round((float)frameLength*frameOverlap),yinThreshold);
        //Patch it to audii handler
        audioInputHandler.addAudioAnalyzer(yin);
        //Start the audio thread
        audioThread = new Thread(audioInputHandler,"Audio Thread");
        audioThread.start();



        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        //Even though YIN is producing a pitch estimate every 16ms, that's too fast for the UI on some devices
                        //So we set it to 25ms, which is good enough
                        Thread.sleep(25);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                float pitch = yin.result.getPitch();
                                //pitchView.setCurrentPitch(pitch);
                                pitchView.setCurrentPitch(yin.medianPitch);
                                if(pitch == -1){
                                    pitchText.setText("Pitch: ");
                                    medianText.setText("Median: ");
                                } else{
                                    pitchText.setText( "Pitch: " + Integer.toString((int) Math.round(pitch)) + " Hz");
                                    if(yin.medianPitch > 0){
                                        medianText.setText( "Median: " + Integer.toString((int) Math.round(yin.medianPitch)) + " Hz");
                                    }
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
        //TODO: pause/destroy? audio thread
    }

    protected void onResume(){
        super.onResume();
        //TODO: resume/restart? audio thread
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


    private void initGui(){
        Button eButton = (Button) findViewById(R.id.e_button);
        eButton.setOnClickListener(tunerListener);
        Button aButton = (Button) findViewById(R.id.a_button);
        aButton.setOnClickListener(tunerListener);
        Button dButton = (Button) findViewById(R.id.d_button);
        dButton.setOnClickListener(tunerListener);
        Button gButton = (Button) findViewById(R.id.g_button);
        gButton.setOnClickListener(tunerListener);
        Button bButton = (Button) findViewById(R.id.b_button);
        bButton.setOnClickListener(tunerListener);
        Button eeButton = (Button) findViewById(R.id.ee_button);
        eeButton.setOnClickListener(tunerListener);
    }

    private View.OnClickListener tunerListener = new View.OnClickListener() {
        public void onClick(View v) {
            TextView targetPitch = (TextView) findViewById(R.id.targetPitchText);
            float note = 0;
            switch (v.getId()) {
                case R.id.e_button:
                    note = 82.41f;
                    break;
                case R.id.a_button:
                    note = 110.00f;
                    break;
                case R.id.d_button:
                    note = 146.83f;
                    break;
                case R.id.g_button:
                    note = 196.00f;
                    break;
                case R.id.b_button:
                    note = 246.94f;
                    break;
                case R.id.ee_button:
                    note = 329.63f;
                    break;
            }
            pitchView.setCenterPitch(note);
            targetPitch.setText("Target pitch: " + Float.toString(note));
        }
    };

}


