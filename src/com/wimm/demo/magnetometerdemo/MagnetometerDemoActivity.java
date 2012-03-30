/* 
 * Copyright (C) 2012 WIMM Labs Incorporated
 */


package com.wimm.demo.magnetometerdemo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.wimm.framework.app.LauncherActivity;

/*
 *  This is a demo of the Magnetometer sensors capabilities on the WIMM through 
 *  a simple compass in the form of a line and a circle.
 *  
 *  It will also detect Magnetic interference and prompt users with instructions
 *  to either move away from the source or re-calibrate the sensors. 
 *  
 */

public class MagnetometerDemoActivity extends LauncherActivity implements SensorEventListener {
	public static final String TAG = MagnetometerDemoActivity.class.getSimpleName();

    /**
     * We could use Math.toDegrees(), but we can take away division from 
     * calculations by just multiplying by this static value instead.
     */
    public static float TO_DEGREES = (1 / (float) Math.PI) * 180;    

    /* 
     * If the magnetometer reports values over a certain value the user is
     * either near a strong magnet, we need to prompt the user to move away
     * or re-calibrate the device.  
     */
    private static final float MAGNETIC_INTERFERENCE_THRESHOLD = SensorManager.MAGNETIC_FIELD_EARTH_MAX * 2.5f;
    private static final float RECALIBRATION_THRESHOLD = MAGNETIC_INTERFERENCE_THRESHOLD * 4;    

    /* 
     * We will track the last time we displayed the Interference view and only 
     * show it again after this time interval.
     */    
    private static final int MS_BETWEEN_INTERFERENCE = 1000;
    
	private CompassView mCompassView;
    private TextView mTextView;

	private static SensorManager mSensorManager;	
	private Sensor mMagneticFieldSensor;
	private Sensor mAccelerometerSensor;
	private PowerManager.WakeLock mWakeLock;
	
	// Rotation matrix required for our Orientation calculations
    private float mRotationMatrix[] = new float[9];
    // Used to store device orientation about 3 axis.
    private float mOrientation[] = new float[3];
    
    private float[] mMagneticFieldVals;
    private float[] mAccelerationVals;
    private long mLastInterferenceTime;
    private boolean mRecalibrationNeeded;
    private boolean mShowCalibrationText = false;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.main);

		/* 
		 * Obtain a wakelock so that we can prevent the watchface from obscuring 
		 * our activity while the user is reading the compass. 
		 */
	    final PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE); 
	    mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MagnetometerDemoWakeLock");

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    mMagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    
        mTextView = (TextView)findViewById(R.id.textView);
        mCompassView = (CompassView)findViewById(R.id.compassView);
	    
    }

	@Override
	protected void onPause() {
		super.onPause();

        // If we don't unregister our listeners for the sensors they will
        // remain on and have a profound impact on device battery life.
        mSensorManager.unregisterListener(this, mMagneticFieldSensor);
        mSensorManager.unregisterListener(this, mAccelerometerSensor);
        
        if (null != mWakeLock) {
            mWakeLock.release();
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
		
        // Register to receive accelerometer and magnetometer events.
        mSensorManager.registerListener(this, 
                mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, 
                mMagneticFieldSensor, SensorManager.SENSOR_DELAY_UI);

        if (null != mWakeLock) {
            mWakeLock.acquire();
        }
	}	

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
        // We aren't concerned with sensor accuracy in this demo.
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
        // For details about the information contained within SensorEvents see:
        // http://developer.android.com/guide/topics/sensors/sensors_motion.html
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
                // We need both the acceleration and magnetometer data to determine
                // compass rotation. Save these values.
				mAccelerationVals = event.values.clone();
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
                // We need both the acceleration and magnetometer data to determine
                // compass rotation. Save these values.
				mMagneticFieldVals = event.values.clone();

				//Calculating the resultant magnitude of the magneticField registered 
				final float magneticField =
                    (float)Math.sqrt(mMagneticFieldVals[0] * mMagneticFieldVals[0] +
				        mMagneticFieldVals[1] * mMagneticFieldVals[1] +
				        mMagneticFieldVals[2] * mMagneticFieldVals[2]);

				/* 
				 * Set the flag for re-calibration when we detected 
				 * mangeticField that is more than the 
				 * RECALIBRATION_THRESHOLD  
				 */
				if (magneticField > RECALIBRATION_THRESHOLD) {
						mRecalibrationNeeded = true;
				}				
				
				/*
				 * If we need a re-calibration and the density is low enough
				 * (i.e user have moved away from source of interference), we 
				 * will re-calibrate the sensors by unregistering and than
				 * registering for them again.
				 * 
				 */
				if (mRecalibrationNeeded && (magneticField < RECALIBRATION_THRESHOLD)) {
					
					mSensorManager.unregisterListener(this, mMagneticFieldSensor);
					mSensorManager.unregisterListener(this, mAccelerometerSensor);
					
					mRecalibrationNeeded = false;
					
					mSensorManager.registerListener(this, 
					        mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
					mSensorManager.registerListener(this, 
					        mMagneticFieldSensor, SensorManager.SENSOR_DELAY_UI);
				}
				
				final long now = SystemClock.elapsedRealtime();
				/* 
				 * To avoid constantly flipping between the interference view and compass
				 * face when the readings are on the magnetic threshold boundary, we will 
				 * only update the view every few seconds.
				 * 
				 */
				if (now - mLastInterferenceTime > MS_BETWEEN_INTERFERENCE) {
				    mLastInterferenceTime = now;
				    
				    /* 
				     * We will set the flag to display "interference_text" when 
				     * the magneticField detected is larger than the 
				     * MAGNETIC_INTERFERENCE_THRESHOLD. 
				     */
                    mShowCalibrationText = magneticField > MAGNETIC_INTERFERENCE_THRESHOLD;
				}
				break;
        }
        
		setCompassRotation();
	}

	private void setCompassRotation(){
        if (null !=mAccelerationVals && null !=mMagneticFieldVals) {
            // For details on getRotationMatrix and getOrientation please see
            // http://developer.android.com/reference/android/hardware/SensorManager.html
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerationVals, mMagneticFieldVals)) {
                SensorManager.getOrientation(mRotationMatrix, mOrientation);

                // mOrientation[0] now holds the rotation around the z-axis
                // in radians from -pi to pi, clockwise being positive from zero degrees.
                float degrees = -mOrientation[0] * TO_DEGREES;
                
                mCompassView.setRotation(degrees);
                
                if (mShowCalibrationText) {
			        mTextView.setText(R.string.interference_text);
                } else {
                		mTextView.setText(Float.toString(degrees));
                }
                
            }
	    }
		
	}
	
	
}
