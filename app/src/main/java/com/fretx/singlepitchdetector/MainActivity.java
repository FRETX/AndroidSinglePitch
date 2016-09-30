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

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public AudioInputHandler audioInputHandler;
    protected Thread audioThread;
    protected PitchDetectorYin yin;

    private TextView pitchText;
    private TextView medianText;
    private PitchView pitchView;
    private TextView targetPitch;
    private boolean autoDetectEnabled;
    float [] tuning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI references
        initGui();
        autoDetectEnabled = true;

        final float [] tuning = {82.41f,110.00f,146.83f,196.00f,246.94f,329.63f};

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

                                        if(autoDetectEnabled){
                                            float [] differences = tuning.clone();
                                            for (int i = 0; i < differences.length; i++) {
                                                differences[i] -= yin.medianPitch;
                                                differences[i] = Math.abs(differences[i]);
                                            }
                                            int minIndex = getMinIndex(differences);
                                            pitchView.setCenterPitch(tuning[minIndex]);
                                            targetPitch.setText("Target pitch: " + Float.toString(tuning[minIndex]));
                                        }

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
        pitchText = (TextView) findViewById(R.id.pitchText );
        medianText = (TextView) findViewById(R.id.medianPitch);
        pitchView = (PitchView) findViewById(R.id.pitchView);
        targetPitch = (TextView) findViewById(R.id.targetPitchText);
        Button autoDetectButton = (Button) findViewById(R.id.autoDetectButton);
        autoDetectButton.setOnClickListener(tunerListener);
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
            float note = 0;
            switch (v.getId()) {
                case R.id.autoDetectButton:
                    MainActivity.this.autoDetectEnabled = true;
                    break;
                case R.id.e_button:
                    note = MainActivity.this.tuning[0];
                    MainActivity.this.autoDetectEnabled = false;
                    break;
                case R.id.a_button:
                    note = MainActivity.this.tuning[1];
                    MainActivity.this.autoDetectEnabled = false;
                    break;
                case R.id.d_button:
                    note = MainActivity.this.tuning[2];
                    MainActivity.this.autoDetectEnabled = false;
                    break;
                case R.id.g_button:
                    note = MainActivity.this.tuning[3];
                    MainActivity.this.autoDetectEnabled = false;
                    break;
                case R.id.b_button:
                    note = MainActivity.this.tuning[4];
                    MainActivity.this.autoDetectEnabled = false;
                    break;
                case R.id.ee_button:
                    note = MainActivity.this.tuning[5];
                    MainActivity.this.autoDetectEnabled = false;
                    break;
            }
            pitchView.setCenterPitch(note);
            targetPitch.setText("Target pitch: " + Float.toString(note));
        }
    };

    private static int getMinIndex(float[] array) {
        float minValue = Float.MAX_VALUE;
        int minIndex = -1;
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

}


