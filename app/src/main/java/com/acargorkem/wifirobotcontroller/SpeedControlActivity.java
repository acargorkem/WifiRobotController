package com.acargorkem.wifirobotcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class SpeedControlActivity extends AppCompatActivity {

    private String url = "http://192.168.1.150"; //esp32 local ip
    private String temperature , pressure , altitude , humidity;

    private TextView mTextViewTemperature;
    private TextView mTextViewPressure;
    private TextView mTextViewAltitude;
    private TextView mTextViewHumidity;

    private RequestQueue requestQueue;


    private String[] result;
    private String[] speedArray;
    double leftSpeed, rightSpeed;
    int angleTemp = 0, strengthTemp = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_control);


        mTextViewTemperature = findViewById(R.id.txtTemperature);
        mTextViewPressure = findViewById(R.id.txtPressure);
        mTextViewAltitude = findViewById(R.id.txtAltitude);
        mTextViewHumidity = findViewById(R.id.txtHumidity);
        Button btnGetSensor = findViewById(R.id.getSensorData);

        //get sensor data listener
        btnGetSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestQueue == null) {
                    requestQueue = Volley.newRequestQueue(SpeedControlActivity.this);
                }
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url + "/bme280", null, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {

                                try {
                                    temperature = response.getString("temperature");
                                    pressure = response.getString("pressure");
                                    altitude = response.getString("altitude");
                                    humidity = response.getString("humidity");
                                    mTextViewTemperature.setText(getString(R.string.temperatureValue,temperature));
                                    mTextViewPressure.setText(getString(R.string.pressureValue,pressure));
                                    mTextViewAltitude.setText(getString(R.string.altitudeValue,altitude));
                                    mTextViewHumidity.setText(getString(R.string.humidityValue,humidity));
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
        });

        // joystick view listener
        final JoystickView joystickRight = findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onMove(int angle, int strength) {

                if (angle != angleTemp || strength != strengthTemp) {
                    angleTemp = angle;
                    strengthTemp = strength;
                    speedArray = getSpeed(angle, strength);
                    postRequest("/drive",
                            "angle=" + angleTemp,
                            "leftSpeed=" + speedArray[0],
                            "rightSpeed=" + speedArray[1]);
                }

            }
        },500);

    }

    private void postRequest(String path, final String... myParameters) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(SpeedControlActivity.this);
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url + path, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("postRequest", response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(SpeedControlActivity.this, error.toString(),
                        Toast.LENGTH_SHORT).show();
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


    //convert angle,strength to leftSpeed,rightSpeed
    private String[] getSpeed(double angle, double strength) {

        if (175 <= angle && angle <= 185) {
            leftSpeed = strength;
            rightSpeed = strength;
        }
        //forward
        else if (85 <= angle && angle <= 95) {
            leftSpeed = strength;
            rightSpeed = strength;
        }
        //right
        else if ((0 <= angle && angle <= 5) || (355 <= angle && angle < 360)) {
            leftSpeed = strength;
            rightSpeed = strength;
        }
        //back
        else if (265 <= angle && angle <= 275) {
            leftSpeed = strength;
            rightSpeed = strength;
        }
        //forward diagonal
        else if ((5 < angle && angle < 85) || (95 < angle && angle < 175)) {
            leftSpeed = ((180 - angle) * strength) / 180;
            rightSpeed = (angle / 180) * strength;
        }
        //backward diagonal
        else if ((185 < angle && angle < 265) || (275 < angle && angle < 355)) {
            leftSpeed = ((angle - 180) * strength) / 180;
            rightSpeed = ((360 - angle) * strength) / 180;
        } else {
            leftSpeed = 0;
            rightSpeed = 0;
        }
        leftSpeed = leftSpeed * 255 / 100;
        rightSpeed = rightSpeed * 255 / 100;
        return new String[]{Double.toString(leftSpeed), Double.toString(rightSpeed)};
    }

}