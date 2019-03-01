package com.williamdemirci.starship;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.pes.androidmaterialcolorpickerdialog.ColorPickerCallback;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // Theme
    private Switch themeSwitch;
    private ConstraintLayout mainLayout;
    // Layout control
    private ImageView colorPickerIcon;
    private SeekBar speedBar;
    // Utils
    private int defaultColorR = 0;
    private int defaultColorG = 0;
    private int defaultColorB = 0;
    private ColorPicker colorPicker;
    // Bluetooth
    private ImageView disconnectBluetoothDeviceButton;
    private String selectedBluetoothDeviceAdress = null;
    private ProgressDialog progressDialogConnection;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket bluetoothSocket = null;
    private boolean bluetoothConnected = false;
    static UUID bluetoothDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Control
    private int move = 0;
    private int speed = 50;
    // Logs
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById();
        setTheme();
        setBluetooth();
        setDisconnectBluetoothDevice();
        //sendData();
        setOnClickListener();
        initColorPicker();
    }

    private void sendData() {
        @SuppressLint("DefaultLocale") String data = "s" + String.format("%02d", getMove()) + String.format("%03d", getSpeed());
        if (bluetoothSocket!=null) {
            try {
                bluetoothSocket.getOutputStream().write(data.getBytes());
            }
            catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Couldn't send data to the device", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int getSpeed() {
        getSpeedBarValue();

        return speed;
    }

    private void getSpeedBarValue() {
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                i++;
                Log.d("Speed", "Seekbar : " + String.valueOf(progress) + " | i = " + String.valueOf(i));
                speed = progress;
                sendData();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private int getMove() {
        move = 50;
        //TODO get all SetOnClicListener

        return move;
    }

    private void setOnClickListener() {
        colorPickerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayColorPicker();
            }
        });
    }

    private void findViewById() {
        mainLayout = (ConstraintLayout) findViewById(R.id.mainLayout);
        themeSwitch = (Switch) findViewById(R.id.themeSwitch);
        speedBar = (SeekBar) findViewById(R.id.speedBar);
        disconnectBluetoothDeviceButton = (ImageView) findViewById(R.id.disconnectButton);
        colorPickerIcon = (ImageView) findViewById(R.id.colorPickerIcon);
    }

    private void initColorPicker() {
        colorPicker = new ColorPicker(MainActivity.this, defaultColorR, defaultColorG, defaultColorB);
    }

    public void displayColorPicker() {
        colorPicker.show();
        colorPicker.enableAutoClose();
        colorPicker.setCallback(new ColorPickerCallback() {
            @Override
            public void onColorChosen(@ColorInt int color) {
                //Log.d("ColorPicker : Alpha", Integer.toString(Color.alpha(color)));
                Log.d("ColorPicker: Red", Integer.toString(Color.red(color)));
                Log.d("ColorPicker: Green", Integer.toString(Color.green(color)));
                Log.d("ColorPicker: Blue", Integer.toString(Color.blue(color)));

                Log.d("ColorPicker: Pure Hex", Integer.toHexString(color));
                Log.d("ColorPicker: #Hex no alpha", String.format("#%06X", (0xFFFFFF & color)));
                //Log.d("ColorPicker : #Hex with alpha", String.format("#%08X", (0xFFFFFFFF & color)));

                updateColorIcon(color);
            }
        });
    }

    private void updateColorIcon(int color) {
        colorPickerIcon.setColorFilter(color);
    }

    private void getBluetoothAddress() { // get bluetooth address of selected device from SelectBluetoothDeviceActivity
        Intent selectBluetoothDeviceIntent = getIntent();
        selectedBluetoothDeviceAdress = selectBluetoothDeviceIntent.getStringExtra(SelectBluetoothDeviceActivity.EXTRA_SELECTED_BLUETOOTH_DEVICE_ADDRESS);
    }

    private void setBluetooth() {
        getBluetoothAddress();
        new ConnectBluetooth().execute();
    }

    private void setDisconnectBluetoothDevice() {
        disconnectBluetoothDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectBluetoothDevice();
            }
        });
    }

    private void disconnectBluetoothDevice() {
        if (bluetoothSocket!=null) {
            try {
                bluetoothSocket.close();
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG).show();
            }
            catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Could not disconnect to the device", Toast.LENGTH_LONG).show();
            }
        }
        finish(); // go back to the SelectBluetoothDeviceActivity
    }

    private class ConnectBluetooth extends AsyncTask<Void, Void, Void> {
        private boolean connected = true;

        @Override
        protected void onPreExecute() {
            progressDialogConnection = ProgressDialog.show(MainActivity.this, "Connection", "Please wait during the connection.");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) { // while onPreExecute()
            try {
                if (bluetoothSocket == null || !bluetoothConnected) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // create a bluetooth adapter
                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(selectedBluetoothDeviceAdress); // connect to the bluetooth device
                    bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(bluetoothDeviceUUID); // type of bluetooth protocol
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery(); // stop bluetooth discovery
                    bluetoothSocket.connect();
                }
            }
            catch (IOException e) {
                connected = false;
//                Toast.makeText(getApplicationContext(), "Error : + " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!connected) {
                Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_LONG).show();
                finish(); // go back to the SelectBluetoothDeviceActivity
            }
            else {
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                bluetoothConnected = true;
            }
            progressDialogConnection.dismiss();
        }
    }

    private void setTheme() { // change the theme if Switch is checked
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mainLayout.setBackgroundResource(R.color.colorBackgroundDark);
                }
                else {
                    mainLayout.setBackgroundResource(R.color.colorBackgroundLight);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        disconnectBluetoothDevice();
    }
}
