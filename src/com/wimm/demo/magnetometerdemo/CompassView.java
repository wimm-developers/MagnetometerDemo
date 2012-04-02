/* 
 * Copyright (C) 2012 WIMM Labs Incorporated
 */

package com.wimm.demo.magnetometerdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

// A simple compass view consisting of a circle and a line.
public class CompassView extends View {

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.RED);
    }

    private Paint mPaint;
    private float mRotationDegrees = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float xCenter = (float) (getWidth() / 2.0);
        float yCenter = (float) (getHeight() / 2.0);

        float radius = Math.max(xCenter, yCenter);
        canvas.drawCircle(xCenter, yCenter, radius, mPaint);

        // Rotate the canvas to draw our compass pointer.
        canvas.save();
        canvas.rotate(mRotationDegrees,xCenter, yCenter);
        canvas.drawLine(xCenter, yCenter, xCenter, yCenter-radius, mPaint);
        canvas.restore();
    }

    // Update our compass rotation and request a redraw.
    public void setRotation(float degrees) {
        mRotationDegrees = degrees;
        invalidate();
    }
}
