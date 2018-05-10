/*
 * The app is built based on the example code provided by the RedBear Team:
 * https://github.com/RedBearLab/Android
 */
package com.example.lianghe.InClass_EXE2_REGLed;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.example.lianghe.InClass_EXE2_REGLed.BLE.RBLGattAttributes;
import com.example.lianghe.InClass_EXE2_REGLed.BLE.RBLService;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Define the device name and the length of the name
    // Note the device name and the length should be consistent with the ones defined in the Duo sketch
    private String mTargetDeviceName = "Arduino";
    private int mNameLen = 0x08;
    private static int SMOOTHING_WINDOW_SIZE = 20;
    private static int MAX_ACCEL_VALUE = 30;
    private static final int SPEECH_REQUEST_CODE = 0;

    private final static String TAG = MainActivity.class.getSimpleName();

    // Declare all variables associated with the UI components
    private Button mConnectBtn = null;
    private TextView mDeviceName = null;
    private TextView mRssiValue = null;
    private TextView mUUID = null;
    private SeekBar mRedSeekbar;
    private LinearLayout bgElement;
    private SeekBar mGreenSeekbar;
    private SeekBar mBlueSeekbar;
    private ToggleButton mAccelBtn;
    private ToggleButton mSpeechBtn;

    private int RedValue;
    private int GreenValue;
    private int BlueValue;
    private String mBluetoothDeviceName = "";
    private String mBluetoothDeviceUUID = "";

    // Declare all Bluetooth stuff
    private BluetoothGattCharacteristic mCharacteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean mConnState = false;
    private boolean mScanFlag = false;
    private boolean mUseAccel = false;

    // accelerometer stuff
    private SensorManager _sensorManager;
    private Sensor _accelSensor;
    private float _rawAccelValues[] = new float[3];

    // smoothing accelerometer signal stuff
    private float _accelValueHistory[][] = new float[3][SMOOTHING_WINDOW_SIZE];
    private float _runningAccelTotal[] = new float[3];
    private float _curAccelAvg[] = new float[3];
    private int _curReadIndex = 0;

    private byte[] mData = new byte[3];
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000;   // millis

    final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    // Process service connection. Created by the RedBear Team
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void setButtonDisable() {
        flag = false;
        mConnState = false;
        mBlueSeekbar.setEnabled(flag);
        mGreenSeekbar.setEnabled(flag);
        mRedSeekbar.setEnabled(flag);
        mAccelBtn.setEnabled(flag);
        mSpeechBtn.setEnabled(flag);
        mConnectBtn.setText("Connect");
        mRssiValue.setText("");
        mDeviceName.setText("");
        mUUID.setText("");
    }

    private void setButtonEnable() {
        flag = true;
        mConnState = true;
        mBlueSeekbar.setEnabled(flag);
        mGreenSeekbar.setEnabled(flag);
        mRedSeekbar.setEnabled(flag);
        mAccelBtn.setEnabled(flag);
        mSpeechBtn.setEnabled(flag);
        mConnectBtn.setText("Disconnect");
    }

    // Process the Gatt and get data if there is data coming from Duo board. Created by the RedBear Team
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                setButtonDisable();
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());

                byte[] buf = new byte[] { (byte) 0x04, (byte) 0x00, (byte) 0x00 };
                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
            }
        }
    };

    // Display the received RSSI on the interface
    private void displayData(String data) {
        if (data != null) {
            mRssiValue.setText(data);
            mDeviceName.setText(mBluetoothDeviceName);
            mUUID.setText(mBluetoothDeviceUUID);
        }
    }

    // Get Gatt service information for setting up the communication
    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        setButtonEnable();
        startReadRssi();

        mCharacteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
    }

    // Start a thread to read RSSI from the board
    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    // Scan all available BLE-enabled devices
    private void scanLedDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    // Callback function to search for the target Duo board which has matched UUID
    // If the Duo board cannot be found, debug if the received UUID matches the predefined UUID on the board
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
				@Override
				public void run() {
					byte[] serviceUuidBytes = new byte[16];
					String serviceUuid = "";
                    for (int i = (21+mNameLen), j = 0; i >= (6+mNameLen); i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    /*
                     * This is where you can test if the received UUID matches the defined UUID in the Arduino
                     * Sketch and uploaded to the Duo board: 0x713d0000503e4c75ba943148f18d941e.
                     */
					serviceUuid = bytesToHex(serviceUuidBytes);
					if (stringToUuidString(serviceUuid).equals(
							RBLGattAttributes.BLE_SHIELD_SERVICE
									.toUpperCase(Locale.ENGLISH)) && device.getName().equals(mTargetDeviceName)) {
						mDevice = device;
						mBluetoothDeviceName = mDevice.getName();
						mBluetoothDeviceUUID = serviceUuid;
					}
				}
			});
        }
    };

    // Convert an array of bytes into Hex format string
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Convert a string to a UUID format
    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch(sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                _rawAccelValues[0] = sensorEvent.values[0];
                _rawAccelValues[1] = sensorEvent.values[1];
                _rawAccelValues[2] = sensorEvent.values[2];

                // Smoothing algorithm adapted from: https://www.arduino.cc/en/Tutorial/Smoothing
                for (int i = 0; i < 3; i++) {
                    _runningAccelTotal[i] = _runningAccelTotal[i] - _accelValueHistory[i][_curReadIndex];
                    _accelValueHistory[i][_curReadIndex] = _rawAccelValues[i];
                    _runningAccelTotal[i] = _runningAccelTotal[i] + _accelValueHistory[i][_curReadIndex];
                    _curAccelAvg[i] = _runningAccelTotal[i] / SMOOTHING_WINDOW_SIZE;
                }

                _curReadIndex++;
                if(_curReadIndex >= SMOOTHING_WINDOW_SIZE){
                    _curReadIndex = 0;
                }

                if(mUseAccel){
                    int mappedValX = map(Math.abs(_curAccelAvg[0]), 0, MAX_ACCEL_VALUE, 0, 255 );
                    int mappedValY = map(Math.abs(_curAccelAvg[1]), 0, MAX_ACCEL_VALUE, 0, 255 );
                    int mappedValZ = map(Math.abs(_curAccelAvg[2]), 0, MAX_ACCEL_VALUE, 0, 255 );

                    byte[] bufX = new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00 };
                    byte[] bufY = new byte[] { (byte) 0x06, (byte) 0x00, (byte) 0x00 };
                    byte[] bufZ = new byte[] { (byte) 0x07, (byte) 0x00, (byte) 0x00 };

                    bufX[1] = (byte) mappedValX;
                    bufY[1] = (byte) mappedValY;
                    bufZ[1] = (byte) mappedValZ;

                    RedValue = mappedValX;
                    GreenValue = mappedValY;
                    BlueValue = mappedValZ;

                    mCharacteristicTx.setValue(bufX);
                    mBluetoothLeService.writeCharacteristic(mCharacteristicTx);
                    mCharacteristicTx.setValue(bufY);
                    mBluetoothLeService.writeCharacteristic(mCharacteristicTx);
                    mCharacteristicTx.setValue(bufZ);
                    mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                    bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));
                }
        }
    }

    /**
     * Like the Arduino map function that re-maps a number from one range to another.
     * https://www.arduino.cc/reference/en/language/functions/math/map/
     * @param x
     * @param in_min
     * @param in_max
     * @param out_min
     * @param out_max
     * @return
     */
    public static int map(float x, int in_min, int in_max, int out_min, int out_max)
    {
        return (int)((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //accel stuff
        // See https://developer.android.com/guide/topics/sensors/sensors_motion.html
        _sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _accelSensor = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        _sensorManager.registerListener(this, _accelSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Associate all UI components with variables
        mConnectBtn = (Button) findViewById(R.id.connectBtn);
        mDeviceName = (TextView) findViewById(R.id.deviceName);
        mRssiValue = (TextView) findViewById(R.id.rssiValue);
        mRedSeekbar = (SeekBar) findViewById(R.id.LEDRedSeekBar);
        mGreenSeekbar = (SeekBar) findViewById(R.id.LEDGreenSeekBar);
        mBlueSeekbar = (SeekBar) findViewById(R.id.LEDBlueSeekBar);
        mAccelBtn = (ToggleButton) findViewById(R.id.DOutBtn);
        mSpeechBtn = (ToggleButton) findViewById(R.id.SpeechBtn);
        bgElement = (LinearLayout) findViewById(R.id.container);

        RedValue = 0;
        GreenValue = 0;
        BlueValue = 0;

        mUUID = (TextView) findViewById(R.id.uuidValue);

        mAccelBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                mRedSeekbar.setEnabled(!isChecked);
                mGreenSeekbar.setEnabled(!isChecked);
                mBlueSeekbar.setEnabled(!isChecked);
                mSpeechBtn.setEnabled(!isChecked);
                mUseAccel = isChecked;
             }
        });

        mSpeechBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                mRedSeekbar.setEnabled(!isChecked);
                mGreenSeekbar.setEnabled(!isChecked);
                mBlueSeekbar.setEnabled(!isChecked);
                mAccelBtn.setEnabled(!isChecked);
                mUseAccel = false;
                if(isChecked){
                    displaySpeechRecognizer();
                }
            }
        });

        // Connection button click event
        mConnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mScanFlag == false) {
                    // Scan all available devices through BLE
                    scanLedDevice();

                    Timer mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {

                        @Override
                        public void run() {
                            if (mDevice != null) {
                                mDeviceAddress = mDevice.getAddress();
                                mBluetoothLeService.connect(mDeviceAddress);
                                mScanFlag = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast toast = Toast
                                                .makeText(
                                                        MainActivity.this,
                                                        "Couldn't search Ble Shiled device!",
                                                        Toast.LENGTH_SHORT);
                                        toast.setGravity(0, 0, Gravity.CENTER);
                                        toast.show();
                                    }
                                });
                            }
                        }
                    }, SCAN_PERIOD);
                }

                System.out.println(mConnState);
                if (mConnState == false) {
                    mBluetoothLeService.connect(mDeviceAddress);
                } else {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                    setButtonDisable();
                }
            }
        });

        // Send data to Duo board
        // Configure the servo Seekbars
        mRedSeekbar.setEnabled(false);
        mGreenSeekbar.setEnabled(false);
        mBlueSeekbar.setEnabled(false);

        mRedSeekbar.setMax(255);
        mGreenSeekbar.setMax(255);
        mBlueSeekbar.setMax(255);

        mRedSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                byte[] buf = new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) mRedSeekbar.getProgress();
                RedValue = mRedSeekbar.getProgress();

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));
            }
        });

        mGreenSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                byte[] buf = new byte[] { (byte) 0x06, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) mGreenSeekbar.getProgress();
                GreenValue = mGreenSeekbar.getProgress();

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));
            }
        });

        mBlueSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                byte[] buf = new byte[] { (byte) 0x07, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) mBlueSeekbar.getProgress();
                BlueValue = mBlueSeekbar.getProgress();

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue));
            }
        });


        // Bluetooth setup. Created by the RedBear team.
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(MainActivity.this,
                RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if BLE is enabled on the device. Created by the RedBear team.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();

        flag = false;

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }

    // Create a list of intent filters for Gatt updates. Created by the RedBear team.
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say a color to change the light!");
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            //reset colors
            //byte[] bufreset = new byte[] { (byte) 0x04, (byte) 0x00, (byte) 0x00 };
            //mCharacteristicTx.setValue(bufreset);
            //mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

            if(spokenText.equals("red")){

                byte[] buf = new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) 255;
                RedValue = 255;
                GreenValue = 0;
                BlueValue = 0;

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));

            }else if(spokenText.equals("green")){
                byte[] buf = new byte[] { (byte) 0x06, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) 255;
                RedValue = 0;
                GreenValue = 255;
                BlueValue = 0;

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));

            } else if (spokenText.equals("blue")) {
                byte[] buf = new byte[] { (byte) 0x07, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) 255;
                RedValue = 0;
                GreenValue = 0;
                BlueValue = 255;

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));

            }else if (spokenText.equals("yellow")) {
                byte[] buf = new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) 255;
                RedValue = 255;
                GreenValue = 255;
                BlueValue = 0;

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                buf = new byte[] { (byte) 0x06, (byte) 0x00, (byte) 0x00 };

                buf[1] = (byte) 255;

                mCharacteristicTx.setValue(buf);
                mBluetoothLeService.writeCharacteristic(mCharacteristicTx);

                bgElement.setBackgroundColor(Color.argb(255, RedValue, GreenValue, BlueValue ));

            }

            Toast toast = Toast
                    .makeText(
                            MainActivity.this,
                            spokenText,
                            Toast.LENGTH_LONG);
            toast.setGravity(0, 0, Gravity.CENTER);
            toast.show();
        }
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}