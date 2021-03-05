package com.example.buttonapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    final byte delimiter = 35;
    int readBufferPosition = 0;
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mmBluetoothAdapter;
    private Button FORWARD;
    private Button REVERSE;
    private Button LEFT;
    private Button RIGHT;
    private TextView status;
    private TextView connectedStatus;
    private Set<BluetoothDevice> pairedDevices;
    List<BluetoothDevice> PairedDevices;

    public void sendMessage(String send_message){
        UUID uuid = UUID.fromString("ae14f5e2-9eb6-4015-8457-824d76384ba0");
        try{
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if(!mmSocket.isConnected()){
                mmSocket.connect();
            }
            String message = send_message;
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(message.getBytes());
        } catch(IOException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("mybutton");
        setContentView(R.layout.activity_main);
        FORWARD = (Button) findViewById(R.id.forward);
        REVERSE = (Button) findViewById(R.id.reverse);
        LEFT = (Button) findViewById(R.id.left);
        RIGHT = (Button) findViewById(R.id.right);
        status = (TextView) findViewById(R.id.status);
        connectedStatus = (TextView) findViewById(R.id.connectedStatus);
        final Handler handler = new Handler();
        final BluetoothManager mmBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mmBluetoothAdapter = mmBluetoothManager.getAdapter();
        pairedDevices = mmBluetoothAdapter.getBondedDevices();
        PairedDevices = new ArrayList<>(pairedDevices);
        for(int i = 0; i < PairedDevices.size(); i++){
            String name = PairedDevices.get(i).getName();
            if(name.equals("raspberrypi")){
                mmDevice = PairedDevices.get(i);
                break;
            }
        }
        if(mmBluetoothAdapter == null || !mmBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        final class workerThread implements Runnable{
            private String message;
            public workerThread(String msg){
                message = msg;
            }
            public void run(){
                sendMessage(message);
                while(!Thread.currentThread().isInterrupted()){
                    int bytesAvailable;
                    boolean workDone = false;
                    try{
                        final InputStream mmInputstream;
                        mmInputstream = mmSocket.getInputStream();
                        bytesAvailable = mmInputstream.available();
                        if(bytesAvailable > 0){
                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e("Bytes recieved from", "Raspberry Pi");
                            byte[] readBuffer = new byte[1024];
                            mmInputstream.read(packetBytes);
                            for(int i = 0; i < bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == delimiter){
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            status.setText(data);
                                        }
                                    });
                                    workDone = true;
                                    break;
                                }else{
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                            if(workDone == true){
                                mmSocket.close();
                                break;
                            }
                        }
                    }catch(IOException e){
                        //TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        FORWARD.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                (new Thread(new workerThread("forward"))).start();
            }
        });
    }
}