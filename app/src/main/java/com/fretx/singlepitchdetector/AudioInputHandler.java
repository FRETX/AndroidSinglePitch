package com.fretx.singlepitchdetector;

/**
 * Created by Onur Babacan on 9/23/16.
 */


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AudioInputHandler implements Runnable {

    public boolean isPaused = false;
    public boolean isFinished = false;
    private Object pauseLock = new Object();

    private final String TAG = "AudioInputHandler";
    private AudioRecord audioInputStream;
    private short [] audioBufferTemp;
    protected short [] audioBuffer;

    protected final int samplingFrequency;
    protected final int audioBufferSize;

    private List<AudioAnalyzer> audioAnalyzers;

    private static final int DEFAULT_SAMPLING_FREQUENCY = 44100;
    private static final int DEFAULT_AUDIO_BUFFER_SIZE = 7200;

    public AudioInputHandler(){
        this(DEFAULT_SAMPLING_FREQUENCY,DEFAULT_AUDIO_BUFFER_SIZE);
    }

    public AudioInputHandler(int fs){
        this(fs,DEFAULT_AUDIO_BUFFER_SIZE);
    }

    public AudioInputHandler(int fs, int bufSize){
        samplingFrequency = fs;
        audioBufferSize = bufSize;

        audioBuffer = new short[audioBufferSize];
        audioBufferTemp = new short[audioBufferSize];

        audioAnalyzers = new CopyOnWriteArrayList<AudioAnalyzer>();

        int maxSamplingFrequency = getMaxSamplingFrequency();

        if(samplingFrequency <= maxSamplingFrequency){
            int minBufferSize = getMinBufferSize(samplingFrequency);
            int minBufferSizeInSamples =  minBufferSize/2;
            if(minBufferSizeInSamples <= audioBufferSize ){
                audioInputStream = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, samplingFrequency,
                        android.media.AudioFormat.CHANNEL_IN_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT,
                        audioBufferSize * 2);
                audioInputStream.startRecording();
            }else{
                throw new IllegalArgumentException("Buffer size must be at least " + (minBufferSize *2));
            }
        } else {
            throw new IllegalArgumentException("Sampling frequency must be at least " + maxSamplingFrequency);
        }
    }

    public void run(){
        while(!isFinished){
            int samplesRead = audioInputStream.read(audioBufferTemp,0,audioBufferSize);
            if(samplesRead != audioBufferSize){
                Log.e(TAG,"Could not read audio data");
            } else {
                audioBuffer = audioBufferTemp.clone();
                AudioData audioData = new AudioData(audioBuffer,samplingFrequency);

                for(AudioAnalyzer analyzer : audioAnalyzers){
                    analyzer.process(audioData);
                }
            }
        }
    }

    public void onDestroy(){
        isFinished = true;
        releaseInputStream();
    }

    public void releaseInputStream(){
        audioInputStream.stop();
        audioInputStream.release();
        Log.d(TAG,"Audio recording stopped and stream released");
    }

    public void addAudioAnalyzer (final AudioAnalyzer audioAnalyzer){
        audioAnalyzers.add(audioAnalyzer);
    }
    public void removeAudioAnalyzer (final AudioAnalyzer audioAnalyzer){
        audioAnalyzers.remove(audioAnalyzer);
    }

    public static int getMaxSamplingFrequency() {
        int maxSamplingFrequency = 0;
        for (int fs : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(fs, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                maxSamplingFrequency = fs;
            }
        }
        return maxSamplingFrequency;
    }

    public static int getMinBufferSize(int fs){
        return AudioRecord.getMinBufferSize(fs, android.media.AudioFormat.CHANNEL_IN_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT);
    }

}
