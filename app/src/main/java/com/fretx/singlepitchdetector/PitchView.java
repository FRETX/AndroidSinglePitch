package com.fretx.singlepitchdetector;

/**
 * Sample code for "Making Musical Apps" by Peter Brinkmann
 * http://shop.oreilly.com/product/0636920022503.do
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


public class PitchView extends View {

    private float centerPitch, currentPitch;
    private int width, height;
    private final Paint paint = new Paint();
    protected double pitchRangeInCents = 200;

    public PitchView(Context context) {
        super(context);
    }

    public PitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PitchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCenterPitch(float centerPitch) {
        this.centerPitch = centerPitch;
        invalidate();
    }

    public void setCurrentPitch(float currentPitch) {
        this.currentPitch = currentPitch;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float halfWidth = width / 2;
        paint.setStrokeWidth(6.0f);
        paint.setColor(Color.BLUE);
        canvas.drawLine(halfWidth, 0, halfWidth, height, paint);

        double currentPitchInCents = (1200 * Math.log(currentPitch) / Math.log(2));
        double centerPitchInCents = (1200 * Math.log(centerPitch) / Math.log(2));
        double difference = centerPitchInCents - currentPitchInCents;

        //10 cents is the "just noticeable difference" for a lot of humans
        if(Math.abs(difference) < 10 ){
            paint.setStrokeWidth(6.0f);
            paint.setColor(Color.GREEN);
        } else {
            paint.setStrokeWidth(8.0f);
            paint.setColor(Color.RED);
        }

        double angleOfIndicator = Double.NaN;
        //Draw the line between an interval of one semitone lower and one semitone higher than center pitch
        if(currentPitchInCents > centerPitchInCents + pitchRangeInCents){
            //Draw a straight line to the right
            angleOfIndicator = 90;
        } else if (currentPitchInCents < centerPitchInCents - pitchRangeInCents){
            //Draw a straight line to the left
            angleOfIndicator = -90;
        } else {
            angleOfIndicator = (difference / pitchRangeInCents) * 90;
        }


        //arbitrary mapping for better display
        angleOfIndicator = (Math.exp((90-Math.abs(angleOfIndicator)) / -30)-0.0498) * 90 / 85.52 * 90 * Math.signum(angleOfIndicator);

        //convert to radians from degrees
        angleOfIndicator = Math.toRadians(angleOfIndicator);
        //reverse direction to match the left-to-right increasing frequency
        angleOfIndicator *= -1;

        canvas.drawLine(halfWidth, height,
                halfWidth + (float)Math.sin(angleOfIndicator) * height * 0.9f,
                height - (float)Math.cos(Math.abs(angleOfIndicator)) * height * 0.9f, paint);



    }

}