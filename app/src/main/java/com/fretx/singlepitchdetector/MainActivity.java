package com.fretx.singlepitchdetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public AudioInputHandler audioInputHandler;
    private Thread audioThread;
    private Thread guiThread;
    private boolean processingIsRunning = false;
    protected PitchDetectorYin yin;

    private TextView pitchText;
    private TextView medianText;
    private PitchView pitchView;
    private TextView targetPitch;
    private boolean autoDetectEnabled = true;
    private float [] tuning = {82.41f,110.00f,146.83f,196.00f,246.94f,329.63f};

    //This is arbitrary, so why not The Answer to Life, Universe, and Everything?
    private final int PERMISSION_CODE_RECORD_AUDIO = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate","method called");
        setContentView(R.layout.activity_main);
        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //UI references
        initGui();
    }

    protected void onStart(){
        super.onStart();
        Log.d("onStart","method called");
    }


    protected void onStop(){
        super.onStop();
        Log.d("onStop","method called");
//        stopProcessing();
        //TODO: pause/destroy? audio thread
    }

    protected void onPause(){
        super.onPause();
        Log.d("onPause","method called");
//        stopProcessing();
    }

    protected void onResume(){
        //TODO: For some reason this doesn't get called when you switch apps
        //Only gets called when the app is created for the first time
        super.onResume();
        Log.d("onResume","method called");
        //Ask for runtime permissions
        boolean permissionsGranted = askForPermissions();
        Log.d("onResume","permissionsGranted: " + permissionsGranted);
        if(permissionsGranted) {
            Log.d("onResume","resuming");
            startProcessing();
        }

        //TODO: resume/restart? audio thread
    }

    protected void onDestroy(){
        super.onDestroy();
        Log.d("onDestroy","method called");
        stopProcessing();

    }


    //Processing handlers
    private void startAudioThread(){
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
        //Patch it to audio handler
        audioInputHandler.addAudioAnalyzer(yin);
        //Start the audio thread
        audioThread = new Thread(audioInputHandler,"Audio Thread");
        audioThread.start();
    }

    private void startGuiThread(){
        guiThread = new Thread() {
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
        guiThread.start();
    }

    private void stopProcessing(){
        Log.d("stopProcessing","method called");
        if(processingIsRunning){
            if(audioInputHandler != null){
                audioInputHandler.onDestroy();
                audioInputHandler = null;
            }
            if(yin != null){
                yin = null;
            }
            if(audioThread != null){
                try {
                    audioThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                audioThread = null;
            }
            if(guiThread != null){
                try {
                    guiThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                guiThread = null;
            }
            processingIsRunning = false;
            Log.d("stopProcessing","processes stopped");
        }

    }

    private void startProcessing(){
        Log.d("startProcessing","method called");
        if(!processingIsRunning){
            startAudioThread();
            startGuiThread();
            processingIsRunning = true;
            Log.d("startProcessing","processes started");
        }

    }

    //GUI Bindings
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


    //Permissions
    private boolean askForPermissions(){

        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (result == PackageManager.PERMISSION_GRANTED){
//            Toast.makeText(MainActivity.this,"You already have the permission",Toast.LENGTH_LONG).show();
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECORD_AUDIO)){
                //If the user has denied the permission previously your code will come to this block
                //Here you can explain why you need this permission
                //Explain here why you need this permission
            }
            //And finally ask for the permission
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE_RECORD_AUDIO);
            return false;
        }
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Checking the request code of our request
        if(requestCode == PERMISSION_CODE_RECORD_AUDIO){
            //If permission is granted
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //Displaying a toast
//                Toast.makeText(this,"Permission granted now you can record audio",Toast.LENGTH_LONG).show();
                startProcessing();
            }else{
                //Displaying another toast if permission is not granted
                Toast.makeText(this,"FretX Tuner cannot work without this permission. Restart the app to ask for it again.", Toast.LENGTH_LONG).show();
            }
        }
    }


    //Utility
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


