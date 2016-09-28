package com.fretx.singlepitchdetector;

/**
 * Created by Onur Babacan on 9/23/16.
 */

public class PitchDetectionResult {
    private float probability;
    private float pitch;
    private boolean pitched;

    public PitchDetectionResult(){
        pitch = -1;
        probability = -1;
        pitched = false;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float p) {
        pitch = p;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float p) {
        probability = p;
    }

    public boolean isPitched() {
        return pitched;
    }

    public void setPitched(boolean p) {
        pitched = p;
    }
}