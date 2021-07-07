package com.acargorkem.wifirobotcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.longdo.mjpegviewer.MjpegView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class SpeedControlActivity extends AppCompatActivity {

    private String BASE_URL = "http://192.168.1.160:5000/"; //wifi device ip
    private String temperature, pressure, altitude, humidity;

    private TextView mTextViewTemperature;
    private TextView mTextViewPressure;
    private TextView mTextViewAltitude;
    private TextView mTextViewHumidity;

    private RequestQueue requestQueue;

    private MjpegView viewer;

    private String[] result;
    private String[] speedArray;
    double leftSpeed, rightSpeed;
    int angleTemp = 0, strengthTemp = 0;
    private Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_control);


        mTextViewTemperature = findViewById(R.id.txtTemperature);
        mTextViewPressure = findViewById(R.id.txtPressure);
        mTextViewAltitude = findViewById(R.id.txtAltitude);
        mTextViewHumidity = findViewById(R.id.txtHumidity);

        Button stopButton = findViewById(R.id.button_camera_stop);
        Button startButton = findViewById(R.id.button_camera_start);

        viewer = findViewById(R.id.mJpeg_Stream);
        viewer.setMode(MjpegView.MODE_FIT_WIDTH);
        viewer.setAdjustHeight(true);
        viewer.setUrl(BASE_URL + "video_feed");

        // sensor data listener
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sensorRequest();
            }
        }, 0, 60 * 1000);// 1 minute interval

        // joystick view listener
        final JoystickView joystickRight = findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onMove(int angle, int strength) {
                if (angle != angleTemp || strength != strengthTemp) {
                    if (strength > 10 || strength == 0) {
                        angleTemp = angle;
                        strengthTemp = strength;
                        speedArray = getSpeed(angle, strength);
                        postRequest("drive",
                                "angle=" + angleTemp,
                                "leftSpeed=" + speedArray[0],
                                "rightSpeed=" + speedArray[1]);
                    }

                }

            }
        }, 500); // 500 ms interval

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewer.stopStream();
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewer.startStream();
            }
        });
    }

    private void sensorRequest() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(SpeedControlActivity.this);
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, BASE_URL + "bme280", null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            temperature = response.getString("temperature");
                            pressure = response.getString("pressure");
                            altitude = response.getString("altitude");
                            humidity = response.getString("humidity");
                            mTextViewTemperature.setText(getString(R.string.temperatureValue, temperature));
                            mTextViewPressure.setText(getString(R.string.pressureValue, pressure));
                            mTextViewAltitude.setText(getString(R.string.altitudeValue, altitude));
                            mTextViewHumidity.setText(getString(R.string.humidityValue, humidity));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("getJSONObjectRequest", error.toString());
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void postRequest(String path, final String... myParameters) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(SpeedControlActivity.this);
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, BASE_URL + path, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("postRequest", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("postRequest", error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                for (String myParameter : myParameters) {
                    result = myParameter.split("=");
                    Log.i("postRequest", result[0] + result[1]);
                    params.put(result[0], result[1]);
                }

                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(1000, 0, 1));
        requestQueue.add(stringRequest);
    }


    //convert angle&strength to leftSpeed&rightSpeed
    private String[] getSpeed(double angle, double strength) {

        //forward diagonal
        if ((5 < angle && angle < 85) || (95 < angle && angle < 175)) {
            leftSpeed = ((180 - angle) * strength) / 180;
            rightSpeed = (angle / 180) * strength;
        }
        //backward diagonal
        else if ((185 < angle && angle < 265) || (275 < angle && angle < 355)) {
            leftSpeed = ((angle - 180) * strength) / 180;
            rightSpeed = ((360 - angle) * strength) / 180;
        } else {
            leftSpeed = strength;
            rightSpeed = strength;
        }

        return new String[]{Double.toString(leftSpeed), Double.toString(rightSpeed)};
    }

}