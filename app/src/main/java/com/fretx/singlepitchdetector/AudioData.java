package com.fretx.singlepitchdetector;

/**
 * Created by Onur Babacan on 9/24/16.
 */

public class AudioData {

    protected short[] audioBuffer;
    protected final int samplingFrequency;

    public AudioData(short[] aBuf , int fs){
        audioBuffer = aBuf.clone();
        samplingFrequency = fs;
    }

    public int length(){
        return audioBuffer.length;
    }
}
