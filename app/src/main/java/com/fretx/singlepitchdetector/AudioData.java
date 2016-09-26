package com.fretx.singlepitchdetector;

/**
 * Created by Onur Babacan on 9/24/16.
 */
import android.support.annotation.Nullable;

import java.util.Arrays;

public class AudioData {

    protected short[] audioBuffer;

    protected final int samplingFrequency;
//    protected final int frameShift;
//    protected final int frameLength;
//    protected int atFrame;
//    protected final int maxFrames;
//    private int head;

//    private final int DEFAULT_SAMPLING_FREQUENCY = 44100;
//    private final float DEFAULT_FRAME_SHIFT_IN_SECONDS = 0.03; //30 ms
//    private final float DEFAULT_FRAME_LENGTH_IN_SECONDS = 0.005; //5ms

    public AudioData(short[] aBuf , int fs){
        audioBuffer = aBuf.clone();
        samplingFrequency = fs;
    }

    public int length(){
        return audioBuffer.length;
    }

//    public boolean getNextFrame(short [] outputBuffer){
//        if(atFrame < maxFrames){
//            atFrame ++;
//            if(head + frameLength > audioBuffer.length){
//                //zero pad the end
//                head = audioBuffer.length-1;
//                outputBuffer = (Arrays.copyOf(Arrays.copyOfRange(audioBuffer,head,audioBuffer.length-1), frameLength)).clone();
//                return true;
//            } else {
//                //get regular frame
//                head = head+frameShift-1;
//                outputBuffer = Arrays.copyOfRange(audioBuffer,head,head+frameLength-1);
//                return true;
//            }
//        } else {
//            //return null to signal that the end is reached
//            return false;
//        }
//    }

//
//    public AudioData(short[] aBuf){
//        this(aBuf,DEFAULT_SAMPLING_FREQUENCY,
//                Math.round((float)DEFAULT_SAMPLING_FREQUENCY*DEFAULT_FRAME_LENGTH_IN_SECONDS),
//                Math.round((float)DEFAULT_SAMPLING_FREQUENCY*DEFAULT_FRAME_SHIFT_IN_SECONDS));
//    }
//
//    public AudioData(short[] aBuf, int fs){
//        this(aBuf,DEFAULT_SAMPLING_FREQUENCY,DEFAULT_FRAME_SHIFT,DEFAULT_FRAME_LENGTH);
//    }



}
