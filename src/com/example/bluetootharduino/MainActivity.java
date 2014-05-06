package com.example.bluetootharduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.EditText;  
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements OnClickListener
{
    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);
        
        //Open Button
        openButton.setOnClickListener(this);
        
        //Send Button
        sendButton.setOnClickListener(this);
        
        //Close button
        closeButton.setOnClickListener(this);
    }
    
    boolean findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
        	System.out.println("no bluetooth available");
            myLabel.setText("No bluetooth adapter available");
        return false;
        }
        else
        {
        	if(!mBluetoothAdapter.isEnabled())
        	{
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
            System.out.println("started activity");
        	}
        
        	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        	if(pairedDevices.size() > 0)
        	{
        		for(BluetoothDevice device : pairedDevices)
        		{
        			if(device.getName().startsWith("HC-05")) 
        			{
        				mmDevice = device;
        				Log.d("ArduinoBT", "findBT found device named "+ mmDevice.getName());
        				Log.d("ArduinoBT", "device address is "+ mmDevice.getAddress());
        				break;
        			}
        		}
        	}
        myLabel.setText("Bluetooth Device Found");
        return true;
        }
    }
    
    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
        mmSocket.connect();
        System.out.println("connct is succseful");
        mmOutputStream = mmSocket.getOutputStream();
        System.out.println("gt output stream");
        mmInputStream = mmSocket.getInputStream();
        System.out.println("gt input stream");
        myLabel.setText("Bluetooth Opened");
        recieve();
        
        
    }
    void recieve() throws IOException{
    	final Handler handler = new Handler(); 
        final byte delimiter = 10; //This is the ASCII code for a newline character
        
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
               while(!Thread.currentThread().isInterrupted() && !stopWorker)
               {
                    try 
                    {
                        int bytesAvailable = mmInputStream.available();                        
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            myLabel.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
    	}
    
    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        System.out.println("m");
        mmOutputStream.write(msg.getBytes());
        System.out.println("m1");
        myLabel.setText("Data Sent");
        System.out.println("m2");
    }
    
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		switch(view.getId()){
		case R.id.open:
			
			//try 
           // {
                if(findBT()){
                try {
                	System.out.println("opening connection");
					openBT();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}}
           // }
           // catch (IOException ex) { ex.printStackTrace();}
			
			break;
		case R.id.close:
			try 
            {
                closeBT();
            }
            catch (IOException ex) { ex.printStackTrace();}
			
			break;
		case R.id.send:
			
			try 
            {
                sendData();
            }
            catch (IOException ex) {
            	ex.printStackTrace();
            }
			
			break;
		}
	}
}