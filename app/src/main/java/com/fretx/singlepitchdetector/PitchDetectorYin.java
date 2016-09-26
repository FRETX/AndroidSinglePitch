package com.fretx.singlepitchdetector;

import android.support.annotation.Nullable;
import android.util.FloatProperty;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Onur Babacan on 9/23/16.
 */

//TODO: Rewrite the code used from TarsosDSP to avoid GPL

public class PitchDetectorYin implements AudioAnalyzer {

    private short[] tempBuffer;
    protected final int samplingFrequency;
    private final int bufferSize;
    protected final int frameShift;
    protected final int frameLength;
    protected int head;
    protected int atFrame;
    protected int maxFrames;
    protected AudioData audioData;

    /**
     * The default YIN threshold value. Should be around 0.10~0.15. See YIN
     * paper for more information.
     */
    private static final double DEFAULT_THRESHOLD = 0.20;

    /**
     * The default size of an audio buffer (in samples).
     */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    /**
     * The default overlap of two consecutive audio buffers (in samples).
     */
    public static final int DEFAULT_OVERLAP = 1536;

    /**
     * The actual YIN threshold.
     */
    private final double threshold;

    /**
     * The audio sample rate. Most audio has a sample rate of 44.1kHz.
     */
    private final float sampleRate;

    /**
     * The buffer that stores the calculated values. It is exactly half the size
     * of the input buffer.
     */
    private final float[] yinBuffer;

    /**
     * The result of the pitch detection iteration.
     */
    protected PitchDetectionResult result;


//    @Override
//    public void process(short[] audioBuffer) {
//        tempBuffer = audioBuffer.clone();
//        result = getPitch(tempBuffer);
//        Log.d("YIN", Float.toString(result.getPitch()));
//    }

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
            Log.d("YIN", Float.toString(result.getPitch()));
        }
    }

    @Override
    public void processingFinished() {

    }


    @Nullable
    private short[] getNextFrame(){
        short[] outputBuffer = new short[frameLength];
        if(atFrame <= maxFrames){
            atFrame ++;

            if(head + frameLength > audioData.length()){
                //zero pad the end
                outputBuffer = (Arrays.copyOf(Arrays.copyOfRange(audioData.audioBuffer,head,audioData.length()-1), frameLength)).clone();
                head = audioData.length()-1;
                return outputBuffer;
//                return true;
            } else {
                //get regular frame
                outputBuffer = Arrays.copyOfRange(audioData.audioBuffer,head,head+frameLength);
                head = head+frameShift-1;
//                return true;
                return outputBuffer;
            }

        } else {
            //return false to signal that the end is reached
            return null;
        }
    }


    public PitchDetectorYin(final int samplingFrequency, final int bufferSize, final int frameLength, final int frameShift, final double threshold) {
        this.samplingFrequency = samplingFrequency;
        this.bufferSize = bufferSize;
        this.frameLength = frameLength;
        this.frameShift = frameShift;
        this.threshold = threshold;
        this.sampleRate = (float) samplingFrequency;
        this.head = -1;
        this.atFrame = -1;
        this.maxFrames = -1;
        this.yinBuffer = new float[frameLength / 2];
        this.tempBuffer = new short[frameLength];
        this.result = new PitchDetectionResult();
    }

    /**
     * The main flow of the YIN algorithm. Returns a pitch value in Hz or -1 if
     * no pitch is detected.
     *
     * @return a pitch value in Hz or -1 if no pitch is detected.
     */
    public PitchDetectionResult getPitch(final short[] audioBufferShort) {

//        Log.d("YIN","starting pitch detection");
        final int tauEstimate;
        final float pitchInHertz;

        final float[] audioBuffer = shortToFloat(audioBufferShort);

        // step 2
        difference(audioBuffer);

        // step 3
        cumulativeMeanNormalizedDifference();

        // step 4
        tauEstimate = absoluteThreshold();

        // step 5
        if (tauEstimate != -1) {
            final float betterTau = parabolicInterpolation(tauEstimate);

            // step 6
            // TODO Implement optimization for the AUBIO_YIN algorithm.
            // 0.77% => 0.5% error rate,
            // using the data of the YIN paper
            // bestLocalEstimate()

            // conversion to Hz
            pitchInHertz = sampleRate / betterTau;
        } else{
            // no pitch found
            pitchInHertz = -1;
        }

        result.setPitch(pitchInHertz);

        return result;
    }

    /**
     * Implements the difference function as described in step 2 of the YIN
     * paper.
     */
    private void difference(final float[] audioBuffer) {
        int index, tau;
        float delta;
        for (tau = 0; tau < yinBuffer.length; tau++) {
            yinBuffer[tau] = 0;
        }
        for (tau = 1; tau < yinBuffer.length; tau++) {
            for (index = 0; index < yinBuffer.length; index++) {
                delta = audioBuffer[index] - audioBuffer[index + tau];
                yinBuffer[tau] += delta * delta;
            }
        }
    }

    /**
     * The cumulative mean normalized difference function as described in step 3
     * of the YIN paper. <br>
     * <code>
     * yinBuffer[0] == yinBuffer[1] = 1
     * </code>
     */
    private void cumulativeMeanNormalizedDifference() {
        int tau;
        yinBuffer[0] = 1;
        float runningSum = 0;
        for (tau = 1; tau < yinBuffer.length; tau++) {
            runningSum += yinBuffer[tau];
            yinBuffer[tau] *= tau / runningSum;
        }
    }

    /**
     * Implements step 4 of the AUBIO_YIN paper.
     */
    private int absoluteThreshold() {
        // Uses another loop construct
        // than the AUBIO implementation
        int tau;
        // first two positions in yinBuffer are always 1
        // So start at the third (index 2)
        for (tau = 2; tau < yinBuffer.length; tau++) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < yinBuffer.length && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++;
                }
                // found tau, exit loop and return
                // store the probability
                // From the YIN paper: The threshold determines the list of
                // candidates admitted to the set, and can be interpreted as the
                // proportion of aperiodic power tolerated
                // within a periodic signal.
                //
                // Since we want the periodicity and and not aperiodicity:
                // periodicity = 1 - aperiodicity
                result.setProbability(1 - yinBuffer[tau]);
                break;
            }
        }


        // if no pitch found, tau => -1
        if (tau == yinBuffer.length || yinBuffer[tau] >= threshold) {
            tau = -1;
            result.setProbability(0);
            result.setPitched(false);
        } else {
            result.setPitched(true);
        }

        return tau;
    }

    /**
     * Implements step 5 of the AUBIO_YIN paper. It refines the estimated tau
     * value using parabolic interpolation. This is needed to detect higher
     * frequencies more precisely. See http://fizyka.umk.pl/nrbook/c10-2.pdf and
     * for more background
     * http://fedc.wiwi.hu-berlin.de/xplore/tutorials/xegbohtmlnode62.html
     *
     * @param tauEstimate
     *            The estimated tau value.
     * @return A better, more precise tau value.
     */
    private float parabolicInterpolation(final int tauEstimate) {
        final float betterTau;
        final int x0;
        final int x2;

        if (tauEstimate < 1) {
            x0 = tauEstimate;
        } else {
            x0 = tauEstimate - 1;
        }
        if (tauEstimate + 1 < yinBuffer.length) {
            x2 = tauEstimate + 1;
        } else {
            x2 = tauEstimate;
        }
        if (x0 == tauEstimate) {
            if (yinBuffer[tauEstimate] <= yinBuffer[x2]) {
                betterTau = tauEstimate;
            } else {
                betterTau = x2;
            }
        } else if (x2 == tauEstimate) {
            if (yinBuffer[tauEstimate] <= yinBuffer[x0]) {
                betterTau = tauEstimate;
            } else {
                betterTau = x0;
            }
        } else {
            float s0, s1, s2;
            s0 = yinBuffer[x0];
            s1 = yinBuffer[tauEstimate];
            s2 = yinBuffer[x2];
            // fixed AUBIO implementation, thanks to Karl Helgason:
            // (2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
            betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
        }
        return betterTau;
    }

    //===========
    private float[] shortToFloat(short[] audio) {

        float[] output = new float[audio.length];

        for (int i = 0; i < output.length; i++) {
            //mapping the [-32768,32768] range to [-1,1]
            output[i] = audio[i] / 32768f;
        }
        return output;
    }


}
