package com.acargorkem.wifirobotcontroller;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MJpegStreamView extends androidx.appcompat.widget.AppCompatImageView {

    private Bitmap bitmap = null;
    private boolean run = false;
    private String url = null;
    private int interval = 1000;


    // constructors
    public MJpegStreamView(@NonNull Context context) {
        super(context);
    }

    public MJpegStreamView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        myAttributes(attrs, 0);
    }

    public MJpegStreamView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        myAttributes(attrs, defStyleAttr);
    }

    public void stop() {
        run = false;
    }

    public void start() {
        if (url == null || run)
            return;

        run = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (run) {
                    try {
                        bitmap = getBitmap(url);
                        post(new Runnable() {
                            public void run() {
                                setImageBitmap(bitmap);
                            }
                        });

                        Thread.sleep(interval);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }).start();
    }


    // set attributes for constructors
    private void myAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs,
                R.styleable.MJpegStreamView, defStyleAttr, 0);
        try {
            url = typedArray.getString(R.styleable.MJpegStreamView_url);
            interval = typedArray.getInteger(R.styleable.MJpegStreamView_interval, 1000);
        } catch (Exception e) {
            typedArray.recycle();
        }
    }

    // getter for bitmap
    private Bitmap getBitmap(String myUrl) {
        try {
            URL url = new URL(myUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e("stream_error","err",e);
            return null;
        }
    }

    // getters and setters for field variables
    public boolean isRun() {
        return run;
    }

    public void setRun(boolean run) {
        this.run = run;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
