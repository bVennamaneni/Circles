package com.bhavanavennamaneni.circles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private View circlesView;
    private GestureDetectorCompat gestureDetectorCompat;
    private ArrayList<CircleData> circlesArrayList;
    private boolean isGrow = false, isGrowing, runSetMotion, isMoving = false;
    private float screenWidth, screenHeight, scale;
    private int bottomEdge, minRadius, touchedCircleId = -1, circleId = 0, newRadius, maxRadius, movingCircleId;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdateTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        circlesView = new DrawCircles(this);
        setContentView(circlesView);
        circlesArrayList = new ArrayList<>();
        scale = getBaseContext().getResources().getDisplayMetrics().density;
        minRadius = (int) (24 * scale + 0.5f); //24dp in pixels
        maxRadius = (int) (44 * scale + 0.5f); //44dp in pixels
        bottomEdge = (int) (80 * scale + 0.5f);
        Display display = getWindowManager().getDefaultDisplay();
        Point ScreenSize = new Point();
        display.getSize(ScreenSize);
        screenWidth = ScreenSize.x;
        screenHeight = ScreenSize.y;

        setMotion();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        gestureDetectorCompat = new GestureDetectorCompat(this, new MyGestureDetector());
        circlesView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    isGrow = false;
                    circlesView.invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    isCircleTouched(event.getX(), event.getY());
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isMoving = false;
                }

                return gestureDetectorCompat.onTouchEvent(event);
            }
        });
    }

    private boolean isCircleTouched(float pos_x, float pos_y) {

        touchedCircleId = -1;
        if (!isMoving) {
            for (CircleData circle : circlesArrayList) {
                if ((pos_x < screenWidth && 0 <= pos_x) && (pos_y < (screenHeight - bottomEdge) && pos_y >= 0)) {
                    if (((pos_x - (circle.c_centerX)) * (pos_x - (circle.c_centerX))) + ((pos_y - (circle.c_centerY)) * (pos_y - (circle.c_centerY))) <= circle.c_radius * circle.c_radius) {
                        touchedCircleId = circle.c_id;
                        circle.c_centerX = pos_x;
                        circle.c_centerY = pos_y;
                        circle.c_inMotion = false;
                        isMoving = true;
                        movingCircleId = touchedCircleId;
                        circlesView.invalidate();
                        break;
                    }
                }
            }
        } else {
            touchedCircleId = movingCircleId;
            if ((pos_x < screenWidth && 0 <= pos_x) && (pos_y < (screenHeight - bottomEdge) && pos_y >= 0)) {
                CircleData circle = circlesArrayList.get(movingCircleId - 1);
                circle.c_centerX = pos_x;
                circle.c_centerY = pos_y;
                circlesArrayList.set(movingCircleId - 1, circle);
                circlesView.invalidate();
            }
        }
        if (touchedCircleId != -1)
            return true;
        else
            return false;
    }


    private boolean isCircleOutOfBounds(float x, float y, float r) {
        if (((x + r) < screenWidth && 0 <= (x - r)) && ((y + r) < (screenHeight - bottomEdge) && (y - r) >= 0))
            return false;
        else
            return true;
    }


    private void growCircle() {
        if (isGrow) {
            if (newRadius < maxRadius) {
                circlesView.postDelayed(new GrowCircle(), 30);
                newRadius += (int) (scale + 0.5f);
                isGrowing = true;
                circlesView.invalidate();
            } else {
                isGrowing = false;
                newRadius = minRadius;
            }
        } else
            isGrowing = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (lastUpdateTime == 0) {
            lastUpdateTime = event.timestamp;
            return;
        }
        float xAccelerate = Math.round((event.values[0] * 100) / 100.0f);
        float yAccelerate = Math.round((event.values[1] * 100) / 100.0f);
        long timeDelta = event.timestamp - lastUpdateTime;
        lastUpdateTime = event.timestamp;
        for (CircleData circle : circlesArrayList) {
            if (circle.c_inMotion) {
                circle.c_velocityX -= 150 * xAccelerate * (timeDelta / 1000000000.0f);
                circle.c_velocityY += 150 * yAccelerate * (timeDelta / 1000000000.0f);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("rew", "Sensor accuracy changed");
    }


    private class GrowCircle implements Runnable {
        @Override
        public void run() {
            growCircle();
        }
    }


    private void setMotion() {
        for (CircleData circle : circlesArrayList) {
            if (circle.c_inMotion) {
                if (!isCircleOutOfBounds(circle.c_centerX, circle.c_centerY, circle.c_radius)) {
                    if (circle.c_velocityX != 0 && circle.c_velocityY != 0) {
                        circle.c_centerX += (float) (circle.c_velocityX / 700.0);
                        circle.c_centerY += (float) (circle.c_velocityY / 700.0);
                        circle.c_velocityX = (float) (circle.c_velocityX * 0.997);
                        if (Math.abs(circle.c_velocityX) < 0.01)
                            circle.c_velocityX = 0;
                        circle.c_velocityY = (float) (circle.c_velocityY * 0.997);
                        if (Math.abs(circle.c_velocityY) < 0.01)
                            circle.c_velocityY = 0;
                    } else
                        circle.c_inMotion = false;

                    if (isCircleOutOfBounds(circle.c_centerX, circle.c_centerY, circle.c_radius)) {
                        if ((circle.c_centerX - circle.c_radius) < 0 || (circle.c_centerX + circle.c_radius) > screenWidth) {
                            circle.c_velocityX = (float) (circle.c_velocityX * -0.997);
                            if (Math.abs(circle.c_velocityX) < 0.01)
                                circle.c_velocityX = 0;
                            circle.c_centerX += (float) (circle.c_velocityX / 700.0);
                        } else if ((circle.c_centerY - circle.c_radius) < 0 || (circle.c_centerY + circle.c_radius) > (screenHeight - bottomEdge)) {
                            circle.c_velocityY = (float) (circle.c_velocityY * -0.997);
                            if (Math.abs(circle.c_velocityY) < 0.01)
                                circle.c_velocityY = 0;
                            circle.c_centerY += (float) (circle.c_velocityY / 700.0);
                        }
                    }
                }
            }
        }
        circlesView.invalidate();
        circlesView.postDelayed(new SetCircleInMotion(), 15);
    }


    private class SetCircleInMotion implements Runnable {
        @Override
        public void run() {
            if (runSetMotion) {
                setMotion();
            }
        }
    }


    public class DrawCircles extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public DrawCircles(Context context) {
            super(context);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
        }

        public DrawCircles(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);

            if (circlesArrayList.size() > 0) {

                if (!isGrowing) {
                    for (CircleData circle : circlesArrayList) {
                        canvas.drawCircle(circle.c_centerX, circle.c_centerY, circle.c_radius, paint);
                    }
                } else {
                    for (CircleData circle : circlesArrayList) {
                        if (circle.c_id != touchedCircleId)
                            canvas.drawCircle(circle.c_centerX, circle.c_centerY, circle.c_radius, paint);
                        else {
                            canvas.drawCircle(circle.c_centerX, circle.c_centerY, newRadius, paint);
                            circle.c_radius = newRadius;
                        }
                    }
                }
            }
        }

    }


    public class CircleData {
        int c_id;
        float c_centerX, c_centerY, c_radius, c_velocityX, c_velocityY;
        boolean c_inMotion;
    }


    public class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {

            if (!isCircleTouched(e.getX(), e.getY())) {
                if (circlesArrayList.size() < 15) {
                    CircleData circle = new CircleData();
                    circleId = circleId + 1;
                    circle.c_id = circleId;
                    circle.c_centerX = e.getX();
                    circle.c_centerY = e.getY();
                    circle.c_radius = minRadius;
                    circle.c_inMotion = false;
                    circlesArrayList.add(circle);
                    newRadius = minRadius;
                    isGrow = true;
                    growCircle();
                }
            } else
                isMoving = true;

            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            isGrow = false;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            isGrow = false;
            if (isCircleTouched(e2.getX(), e2.getY())) {
                runSetMotion = true;
                CircleData circle = circlesArrayList.get(touchedCircleId - 1);
                circle.c_inMotion = true;
                circle.c_velocityX = velocityX;
                circle.c_velocityY = velocityY;
                circlesArrayList.set(touchedCircleId - 1, circle);
            }
            return true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        runSetMotion = false;
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        runSetMotion = true;
        setMotion();
        boolean isRunning = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (!isRunning) {
            Log.i("rew", "could not start accelerometer");
        }
    }

}
