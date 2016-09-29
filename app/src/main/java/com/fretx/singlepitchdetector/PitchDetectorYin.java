package com.fretx.singlepitchdetector;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Onur Babacan on 9/23/16.
 */


public class PitchDetectorYin implements AudioAnalyzer {

    protected final int samplingFrequency;
    protected final int frameShift;
    protected final int frameLength;
    private final double threshold;

    protected AudioData audioData;
    private short[] tempBuffer;
    protected int head;
    protected int atFrame;
    protected int maxFrames;
    private final float[] yinBuffer;

    protected PitchDetectionResult result;
    private final int nLastValues = 10;
    protected float[] lastValues = new float[nLastValues];
    protected float medianPitch = -1;

    //TODO: parameter-less constructor with default values
    private static final double DEFAULT_THRESHOLD = 0.20;
    private static final int DEFAULT_SAMPLING_FREQUENCY = 44100;
    //We arbitrarily set the lower bound of detection to 50Hz. At least two pitch periods are needed
    //So the default frame length is: (1/50) * 44100 * 2 = 1764
    public static final int DEFAULT_FRAME_LENGTH = 1764;
    //An overlap of 50% is good enough for real-time applications
    public static final int DEFAULT_FRAME_SHIFT = 882;

    @Override
    public void process(AudioData inputAudioData) {
        audioData = inputAudioData;
        if(audioData.length() < frameLength){
            maxFrames = (int) Math.ceil( (double)(audioData.length() - frameLength) / (double) frameShift);
        } else {
            maxFrames = 1;
        }
        atFrame = 1;
        head = 0;
        while((tempBuffer = getNextFrame()) != null ){
            result = getPitch(tempBuffer);
//            Log.d("YIN", Float.toString(result.getPitch()));
        }
        processingFinished();
    }

    @Override
    public void processingFinished() {

    }

    private static float median(float[] m) {
        int middle = m.length/2;
        if (m.length%2 == 1) {
            return m[middle];
        } else {
            return (m[middle-1] + m[middle]) / 2;
        }
    }

    @Nullable
    private short[] getNextFrame(){
        short[] outputBuffer;
        if(atFrame <= maxFrames){
            atFrame ++;
            if(head + frameLength > audioData.length()){
                //zero pad the end
                outputBuffer = (Arrays.copyOf(Arrays.copyOfRange(audioData.audioBuffer,head,audioData.length()-1), frameLength)).clone();
                head = audioData.length()-1;
                return outputBuffer;
            } else {
                //get regular frame
                outputBuffer = Arrays.copyOfRange(audioData.audioBuffer,head,head+frameLength);
                head = head+frameShift-1;
                return outputBuffer;
            }

        } else {
            //return null to signal that the end is reached
            return null;
        }
    }

    public PitchDetectorYin(final int samplingFrequency, final int frameLength, final int frameShift, final double threshold) {
        this.samplingFrequency = samplingFrequency;
        this.frameLength = frameLength;
        this.frameShift = frameShift;
        this.threshold = threshold;
        this.head = -1;
        this.atFrame = -1;
        this.maxFrames = -1;
        this.yinBuffer = new float[frameLength / 2];
        this.tempBuffer = new short[frameLength];
        this.result = new PitchDetectionResult();
        Arrays.fill(lastValues,-1);
    }

    public PitchDetectionResult getPitch(final short[] audioBufferShort) {

        int tauEstimate;
        float pitchInHertz;
        float[] audioBuffer = shortToFloat(audioBufferShort);

        difference(audioBuffer);
        cumulativeMeanNormalizedDifference();
        tauEstimate = absoluteThreshold();
        if (tauEstimate != -1) {
            final float betterTau = parabolicInterpolation(tauEstimate);
            pitchInHertz = (float) samplingFrequency / betterTau;
            if(pitchInHertz > samplingFrequency/4){ //This is mentioned in the YIN paper
                pitchInHertz = -1;
            }
        } else{
            // no pitch found
            pitchInHertz = -1;
        }
        result.setPitch(pitchInHertz);

        for (int i = lastValues.length -1 ; i > 0; i--) {
            lastValues[i] = lastValues[i-1];
        }
        lastValues[0] = pitchInHertz;
        updateMedianPitch();

        return result;
    }

    private void updateMedianPitch(){
        int pitchedValuesCount = 0;
        for (int i = 0; i < lastValues.length ; i++) {
            if(lastValues[i] > 0){
                pitchedValuesCount++;
            }
        }
        if(pitchedValuesCount > 3){
            float[] sortedPitchValues = new float[pitchedValuesCount];
            int y = 0;
            for (int i = 0; i < lastValues.length ; i++) {
                if(lastValues[i] > 0){
                    sortedPitchValues[y] = lastValues[i];
                    y++;
                }
            }
            Arrays.sort(sortedPitchValues);
            medianPitch = PitchDetectorYin.median(sortedPitchValues);
        } else medianPitch = -1;
    }

    private void difference(final float[] audioBuffer) {
        Arrays.fill(yinBuffer,0);
        for  (int tau = 1; tau < yinBuffer.length; tau++) {
            for (int t = 0; t < yinBuffer.length; t++) {
                yinBuffer[tau] += (float) Math.pow(audioBuffer[t] - audioBuffer[t + tau],2);
            }
        }
    }

    private void cumulativeMeanNormalizedDifference() {
        int tau;
        yinBuffer[0] = 1;
        float runningSum = 0;
        for (tau = 1; tau < yinBuffer.length; tau++) {
            runningSum += yinBuffer[tau];
            yinBuffer[tau] = yinBuffer[tau] / runningSum * tau;
        }
    }

    private int absoluteThreshold() {
        int tau = 0;
        int i = 0;
        boolean tauFound = false;

        while(!tauFound && i+1 < yinBuffer.length){
            if(yinBuffer[i] < threshold){
                while(yinBuffer[i+1] < yinBuffer[i]){
                    tau = i++;
                }
                tauFound = true;
                result.setProbability(1 - yinBuffer[tau]);
            } else i++;
        }

        if(!tauFound){
            tau = -1;
            result.setProbability(0);
            result.setPitched(false);
        } else result.setPitched(true);

        return tau;
    }

    private float parabolicInterpolation(final int tauEstimate) {
        final float betterTau;
        if(tauEstimate > 0 && tauEstimate < yinBuffer.length-1){
            float y1,y2,y3;
            y1 = yinBuffer[tauEstimate-1];
            y2 = yinBuffer[tauEstimate];
            y3 = yinBuffer[tauEstimate+1];
            betterTau = tauEstimate + (y3 - y1) / (2 * (2 * y2 - y3 - y1));
        } else {
            //TODO: Implement proper boundary conditions
            betterTau = tauEstimate;
        }
        return betterTau;
    }

    private float[] shortToFloat(short[] audio) {
        float[] output = new float[audio.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = audio[i] / 32768f;
        }
        return output;
    }


}
