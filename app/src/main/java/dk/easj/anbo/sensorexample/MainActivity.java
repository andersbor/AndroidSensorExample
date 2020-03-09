package dk.easj.anbo.sensorexample;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int DEFAULT_UDP_PORT = 14593;
    private int port = DEFAULT_UDP_PORT;
    private SensorManager mSensorManager;
    //private Sensor mPressure, mLight, mGravity, mAcceleration, mProximity, mMagneticField, mStepCounter;
    private List<Sensor> sensorList;
    private final List<CheckBox> checkBoxList = new ArrayList<>();
    private EditText udpPortView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        udpPortView = findViewById(R.id.mainEditTextUdpPort);
        udpPortView.setText(Integer.toString(port));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //TextView messageView = (TextView) findViewById(R.id.messageView);
        // http://stackoverflow.com/questions/2858161/display-an-android-sensors-list
        sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        /*
        StringBuffer sb = new StringBuffer();
        for (Sensor sensor : sensorList) {
            //sb.append(sensor.getStringType()); // API 20++
            sb.append(sensor.getName());
            sb.append("\n");
        }
        messageView.setText(sb);
        */

        ViewGroup layout = findViewById(R.id.mainLinearLayout);
        ViewGroup.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        for (final Sensor sensor : sensorList) {
            CheckBox checkBox = new CheckBox(this);
            checkBoxList.add(checkBox);
            checkBox.setText(sensor.getName());
            layout.addView(checkBox, params);
        }

        final ToggleButton toggle = findViewById(R.id.mainToggleButtonOnOff);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    registerListeners();
                    String udpPortStr = udpPortView.getText().toString();
                    port = Integer.parseInt(udpPortStr);
                } else {
                    unregisterListeners();
                }
            }
        });
    }

    /*
    private void getCensors() {
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    } */

    @Override
    public void onSensorChanged(SensorEvent event) {
        sendUdpBroadcast(event.sensor.getName(), event.values, port);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_PRESSURE:
                float millibars_of_pressure = event.values[0];
                //sendBroadcast(event.sensor.getName(), event.values);
                Log.d("MINE", "Pressure " + millibars_of_pressure);
                break;
            case Sensor.TYPE_LIGHT:
                float light = event.values[0];
                Log.d("MINE", "Light " + light);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                Log.d("MINE", "Acceleration: " + x + " " + y + " " + z);
                break;
            case Sensor.TYPE_PROXIMITY:
                float distance_in_cm = event.values[0];
                Log.d("MINE", "Proximity " + distance_in_cm + " cm");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                Log.d("MINE", "Magnetic field: " + x + " " + y + " " + z);
                break;
            case Sensor.TYPE_STEP_COUNTER:
                Log.d("MINE", "Step counter: " + event.values[0]);
                break;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* empty */ }

    /*
    @Override
    protected void onResume() {
        Log.d("MINE", "onResume called");

        super.onResume();
        registerListeners();
    }
    */

    private void registerListeners() {
        /*
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        */
        for (int i = 0; i < sensorList.size(); i++) {
            CheckBox cb = checkBoxList.get(i);
            if (cb.isChecked()) {
                mSensorManager.registerListener(this, sensorList.get(i), SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    /*
    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        unregisterListeners();
    }*/

    private void unregisterListeners() {
        mSensorManager.unregisterListener(this);
    }

    private void sendUdpBroadcast(String name, float[] values, int port) {
        String message = name + "\n" + Arrays.toString(values);
        Log.d("MINE", message);
        sendUdpBroadcastAsync(message, port);
    }

    private void sendUdpBroadcastAsync(String message, int port) {
        UdpBroadCaster broadCaster = new UdpBroadCaster();
        // https://stackoverflow.com/questions/10135910/is-there-a-constructor-associated-with-nested-classes
        UdpBroadCaster.MessagePort mp = broadCaster.new MessagePort(message, port);
        broadCaster.execute(mp);
    }

    // Networking must be done in a background thread/task
    static class UdpBroadCaster extends AsyncTask<UdpBroadCaster.MessagePort, Void, Void> {

        class MessagePort {
            MessagePort(String message, int port) {
                this.message = message;
                this.port = port;
            }

            final String message;
            final int port;
        }

        @Override
        protected Void doInBackground(MessagePort... messagePorts) {
            try {
                sendUdpBroadcast(messagePorts[0].message, messagePorts[0].port);
            } catch (IOException e) {
                Log.e("MINE", e.getMessage(), e);
            }
            return null;
        }

        private void sendUdpBroadcast(String messageStr, int port) throws IOException {
            String broadcastIP = "255.255.255.255";
            InetAddress inetAddress = InetAddress.getByName(broadcastIP);
            DatagramSocket socket = new DatagramSocket();
            // todo try with resource? requires higher api level 19
            // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
            socket.send(sendPacket);
            Log.d("MINE", "Broadcast sent: " + messageStr);
            socket.close();
        }
    }
}
