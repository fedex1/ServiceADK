/*
* @author RobotGrrl.com (original source: https://github.com/RobotGrrl/ServiceADK)
* @author Yozzo, Ralph (ralph@brooklynmarathon.com)
*/
// ServiceADKActivity.java
// ---------------------------
// RobotGrrl.com
// November 29, 2011

package com.brooklynmarathon.serviceadk;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
//import com.robotgrrl.serviceadk.R;

public class ServiceADKActivity extends Activity implements Runnable, OnClickListener  {
    /** Called when the activity is first created. */
    
	private static final String TAG = "ServiceADKActivity";
	
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	int CURRENT_TAB = 0;
	
	Thread mThread;
	
	TextView fileDescText, inputStreamText, outputStreamText, accessoryText;
	Button readButton;
	
	protected void handleJoyMessage(JoyMsg j) {
	}

	protected void handleLightMessage(LightMsg l) {
	}

	protected void handleTemperatureMessage(TemperatureMsg t) {
		setTemperature(t.getTemperature());
	}

	protected void handleSwitchMessage(SwitchMsg o) {
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}
	
	private final DecimalFormat mTemperatureFormatter = new DecimalFormat(
			"###" + (char)0x00B0);


	public void setTemperature(int temperatureFromArduino) {
		/*
		 * Arduino board contains a 6 channel (8 channels on the Mini and Nano,
		 * 16 on the Mega), 10-bit analog to digital converter. This means that
		 * it will map input voltages between 0 and 5 volts into integer values
		 * between 0 and 1023. This yields a resolution between readings of: 5
		 * volts / 1024 units or, .0049 volts (4.9 mV) per unit.
		 */
		double voltagemv = temperatureFromArduino * 4.9;
		/*
		 * The change in voltage is scaled to a temperature coefficient of 10.0
		 * mV/degC (typical) for the MCP9700/9700A and 19.5 mV/degC (typical)
         * for the MCP9701/9701A. The out- put voltage at 0 degC is also scaled
         * to 500 mV (typical) and 400 mV (typical) for the MCP9700/9700A and
		 * MCP9701/9701A, respectively. VOUT = TC�TA+V0degC
		 */
		double kVoltageAtZeroCmv = 400;
		double kTemperatureCoefficientmvperC = 19.5;
		double ambientTemperatureC = ((double) voltagemv - kVoltageAtZeroCmv)
				/ kTemperatureCoefficientmvperC;
		double temperatureF = (9.0 / 5.0) * ambientTemperatureC + 32.0;
		
		//mTemperature.setText(mTemperatureFormatter.format(temperatureF));
		// show temperature in this accessory text, temporarily 
		accessoryText.setText(mTemperatureFormatter.format(temperatureF));
		
		
		Log.d(TAG, "QQQ: temperature: " + temperatureF);
		long now = System.currentTimeMillis();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// then you use
		long past = prefs.getLong("temperature.saved", 0);
		
		Log.d(TAG, "QQQ: log now: " + now + " past: " + past + " diff: " + (now-past));
		if (now - past > 1000*60*10){  // every 10 minutes
		//if (now - past > 1000*60*1){  // every 1 minutes
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong("temperature.saved", now); // value to store
			editor.commit();
			String locationProvider = LocationManager.NETWORK_PROVIDER;
			// Or use LocationManager.GPS_PROVIDER
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
			String location = "";
			if (lastKnownLocation != null){
				location="&l="+ lastKnownLocation.getLatitude()+","+			lastKnownLocation.getLongitude();
			}
            // THIS IS WHERE YOU CAN SEND SENSOR READINGS TO A SERVER IN THE CLOUD or whereever you wish.
            // THIS IS WHERE YOU CAN SEND SENSOR READINGS TO A SERVER IN THE CLOUD or whereever you wish.
            // THIS IS WHERE YOU CAN SEND SENSOR READINGS TO A SERVER IN THE CLOUD or whereever you wish.
			//new AsyncHTTP().execute("http://PLACE-MY-SERVER-HERE/temperature?v=" +temperatureF+location);

			// no api to thermostatmonitor log seems to exist, you have to upload a csv file
			//new AsyncHTTP().execute("http://api.thermostatmonitor.com/json/?apiKey=GeVJQWTWzwg&action=setTemperature&thermostatIP=192.168.1.106&degrees=" + temperatureF +"&hold=1");
		}
	
	}
	
	//private Handler mHandler = new Handler();
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SWITCH:
				SwitchMsg o = (SwitchMsg) msg.obj;
				handleSwitchMessage(o);
				break;

			case MESSAGE_TEMPERATURE:
				TemperatureMsg t = (TemperatureMsg) msg.obj;
				handleTemperatureMessage(t);
				break;

			case MESSAGE_LIGHT:
				LightMsg l = (LightMsg) msg.obj;
				handleLightMessage(l);
				break;

			case MESSAGE_JOY:
				JoyMsg j = (JoyMsg) msg.obj;
				handleJoyMessage(j);
				break;

			}
		}
	};
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
	
	// ---------
	// Lifecycle
	// ---------
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
        super.onCreate(savedInstanceState);
        
        mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
        
		
		Log.e(TAG, "Hellohello!");
		
		startService(new Intent(this, ADKService.class));
		
        setContentView(R.layout.main);
        
        fileDescText = (TextView)findViewById(R.id.textView1);
		inputStreamText = (TextView)findViewById(R.id.textView2);
		outputStreamText = (TextView)findViewById(R.id.textView3);
		accessoryText = (TextView)findViewById(R.id.textView4);
		
		readButton = (Button)findViewById(R.id.button1);
		
		readButton.setOnClickListener(this);
        
    }

    @Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}
    
    @Override
	public void onResume() {
    	
    	Log.v(TAG, "onResume");
    	
		super.onResume();
		
		try {
			ADKService.self.stopUpdater();
		} catch(Exception e) {
			Log.d(TAG, "Stopping the updater failed");
		}
		
		Intent intent = getIntent();
		
		if (mInputStream != null && mOutputStream != null) {
			Log.v(TAG, "input and output stream weren't null!");
			enableControls(true);
			return;
		}
		
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		
		Log.v(TAG, "all the accessories: " + accessories);
		
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				Log.v(TAG, "mUsbManager does have permission");
				openAccessory(accessory);
			} else {
				Log.v(TAG, "mUsbManager did not have permission");
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
		
		// Let's update the textviews for easy debugging here...
		updateTextViews();
		
	}
    
    @Override
	public void onPause() {
    	Log.v(TAG, "onPause");
    	//closeAccessory();
    	
    	try {
    		ADKService.self.startUpdater();
		} catch(Exception e) {
			
		}
    	
        Log.v(TAG, "done, now pause");
    	
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
	
	 @Override
	 protected void onStop() {
	      super.onStop();
	}
	 
		private static final int MESSAGE_SWITCH = 1;
		private static final int MESSAGE_TEMPERATURE = 2;
		private static final int MESSAGE_LIGHT = 3;
		private static final int MESSAGE_JOY = 4;

		public static final byte LED_SERVO_COMMAND = 2;
		public static final byte RELAY_COMMAND = 3;
		
		
		protected class SwitchMsg {
			private byte sw;
			private byte state;

			public SwitchMsg(byte sw, byte state) {
				this.sw = sw;
				this.state = state;
			}

			public byte getSw() {
				return sw;
			}

			public byte getState() {
				return state;
			}
		}
		
		protected class TemperatureMsg {
			private int temperature;

			public TemperatureMsg(int temperature) {
				this.temperature = temperature;
			}

			public int getTemperature() {
				return temperature;
			}
		}

		protected class LightMsg {
			private int light;

			public LightMsg(int light) {
				this.light = light;
			}

			public int getLight() {
				return light;
			}
		}

		protected class JoyMsg {
			private int x;
			private int y;

			public JoyMsg(int x, int y) {
				this.x = x;
				this.y = y;
			}

			public int getX() {
				return x;
			}

			public int getY() {
				return y;
			}
		}

		private int composeInt(byte hi, byte lo) {
			int val = (int) hi & 0xff;
			val *= 256;
			val += (int) lo & 0xff;
			return val;
		}

	@Override
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {
				case 0x1:
					Log.d(TAG, "MESSAGE_SWITCH: len: " + len);

					if (len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_SWITCH);
						m.obj = new SwitchMsg(buffer[i + 1], buffer[i + 2]);
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				case 0x4:
					Log.d(TAG, "MESSAGE_TEMPERATURE: len: " + len);

					if (len >= 3) {
						Message m = Message.obtain(mHandler,
								MESSAGE_TEMPERATURE);
						m.obj = new TemperatureMsg(composeInt(buffer[i + 1],
								buffer[i + 2]));
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				case 0x5:
					Log.d(TAG, "MESSAGE_LIGHT: len: " + len);

					if (len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_LIGHT);
						m.obj = new LightMsg(composeInt(buffer[i + 1],
								buffer[i + 2]));
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				case 0x6:
					Log.d(TAG, "MESSAGE_JOY: len: " + len);
					if (len >= 3) {
						Message m = Message.obtain(mHandler, MESSAGE_JOY);
						m.obj = new JoyMsg(buffer[i + 1], buffer[i + 2]);
						mHandler.sendMessage(m);
					}
					i += 3;
					break;

				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
	}
/*
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				Log.v(TAG, "Read: " + buffer[i]);
					
				final int val = (int)buffer[i];
					
				mHandler.post(new Runnable() {
		            @Override
		            public void run() {
		            	// This gets executed on the UI thread so it can safely modify Views
		            	inputStreamText.setText("Read: " + val);
		            }
				});
					
				switch (buffer[i]) {
					default:
						Log.d(TAG, "unknown msg: " + buffer[i]);
						i = len;
						break;
					}
			}
				
		}
	}
	*/
    
    // ------------
    // ADK Handling
	// ------------
	
	private void openAccessory(UsbAccessory accessory) {
		
		Log.e(TAG, "openAccessory: " + accessory);
		
		Log.d(TAG, "this is mUsbManager: " + mUsbManager);
		
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		
		Log.d(TAG, "Tried to open");
		
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			mThread = new Thread(null, this, "DemoKit"); // meep
			mThread.start(); // meep
			Log.d(TAG, "accessory opened");
			enableControls(true);
		} else {
			Log.d(TAG, "accessory open fail");
			enableControls(false);
		}
	}
	
	private void closeAccessory() {

		Log.e(TAG, "closing accessory");
		
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
		
		enableControls(false);
		
	}

	public void sendCommand(byte command, byte target, int value) {
		byte[] buffer = new byte[3];
		if (value > 255)
			value = 255;

		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}
	

    public void sendPress(char c) {
		
    	byte[] buffer = new byte[2];
		buffer[0] = (byte)'!';
		buffer[1] = (byte)c;
			
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
		
	}
    
	public boolean adkConnected() {
    	//if(mInputStream != null && mOutputStream != null) return true;
    	if(mFileDescriptor != null) return true;
    	return false;
    }
	
	
	// --------------
	// User interface
	// --------------
	
	private void enableControls(boolean b) {
		((ServiceADKApplication) getApplication()).setInputStream(mInputStream);
		((ServiceADKApplication) getApplication()).setOutputStream(mOutputStream);
		((ServiceADKApplication) getApplication()).setFileDescriptor(mFileDescriptor);
		((ServiceADKApplication) getApplication()).setUsbAccessory(mAccessory);
		updateTextViews();
		
		if(!b) {
			try {
	    		ADKService.self.stopUpdater();
			} catch(Exception e) {
				
			}
		}
		
	}
    
    @Override
	public void onClick(View v) {
		Log.v(TAG, "click!");
		
		if(v.getId() == R.id.button1) {
			Log.v(TAG, "Pressed Read");
			sendPress('a');
		}
		
	}
    
    private void updateTextViews() {

    	Log.v(TAG, "updated text views");
    	
		if(mInputStream == null) {
			inputStreamText.setText("Input stream is NULL");
			Log.d(TAG, "Input stream is NULL");
		} else {
			inputStreamText.setText("Input stream is not null");
			Log.d(TAG, "Input stream is not null");
		}

		if(mOutputStream == null) {
			outputStreamText.setText("Output stream is NULL");
			Log.d(TAG, "Output stream is NULL");
		} else {
			outputStreamText.setText("Output stream is not null");
			Log.d(TAG, "Output stream is not null");
		}

		if(mAccessory == null) {
			accessoryText.setText("USB Accessory is NULL");
			Log.d(TAG, "USB Accessory is NULL");
		} else {
			accessoryText.setText("USB Accessory is not null");
			Log.d(TAG, "USB Accessory is not null");
		}

		if(mFileDescriptor == null) {
			fileDescText.setText("File Descriptor is NULL");
			Log.d(TAG, "File Descriptor is NULL");
		} else {
			fileDescText.setText("File Descriptor is not null");
			Log.d(TAG, "File Descriptor is not null");
		}
    	
    }
    
}
