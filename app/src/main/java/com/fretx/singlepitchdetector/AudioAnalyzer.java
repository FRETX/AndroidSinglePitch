package com.fretx.singlepitchdetector;

/**
 * Created by Onur Babacan on 9/23/16.
 */

public interface AudioAnalyzer {

    void process(AudioData audioData);
    void processingFinished();

//    int samplingFrequency = 44100;
//    int frameShift;
//    int frameLength;
//    int head;

}
