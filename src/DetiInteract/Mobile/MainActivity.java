package DetiInteract.Mobile;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import DetiInteract.Mobile.Bluetooth.*;

public class MainActivity extends Activity implements OnGestureListener, SensorEventListener
{
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private static final int REQUEST_HELP = 3;

	public static final int MESSAGE_TOAST = 1;

	public static final String TOAST = "toast";
	public static final String DEVICE_NAME = "device_name";
	
	private float initialYaw;
	private boolean gotInitialValue = false;

	private SensorManager sensorManager;
	private GestureDetector gesture;

	// Remember some things for zooming
	private PointF start = new PointF();
	private PointF mid = new PointF();
	private float oldDist = 1f;
	private boolean isZooming = false;

	private TextView view;
	private TextView Xview;
	private TextView Yview;
	private TextView Zview;

	private BluetoothService btService;
	private BluetoothAdapter btAdapter = null;
	private BluetoothDevice btDevice = null;

	// Handler that gets information from the BluetoothService.
	private final Handler commHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what) {
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		// Exit if no bluetooth available
		if (btAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// get the reference for the textboxes
		view = (TextView) findViewById(R.id.gesture);
		Xview = (TextView) findViewById(R.id.xValue);
		Yview = (TextView) findViewById(R.id.yValue);
		Zview = (TextView) findViewById(R.id.zValue);

		gesture = new GestureDetector(this, this);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		//btService = new BluetoothService(this, commHandler);
	}

	/**
	 * Called when activity starts/restarts
	 */
	@Override
	public void onStart()
	{
		super.onStart();

		// Ask user to enable bluetooth if needed
		if (!btAdapter.isEnabled()) {
			Toast.makeText(this, "Ligue o Bluetooth primeiro.",
					Toast.LENGTH_LONG).show();
			// finish();
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}

		// start the bluetooth service
		btService = new BluetoothService(this, commHandler);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_GAME);
	}

	/**
	 * Called when activite resumes (after onStart() or returning from onPause())
	 */
	@Override
	public void onResume()
	{
		super.onResume();

		// call the DeviceListActivity
		if (btDevice == null && btAdapter.isEnabled()) {
			Intent connectIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
		}

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_GAME);
	}

	/**
	 * Called when activity is paused (another activity is launched, focus is
	 * lost)
	 */
	@Override
	public void onPause()
	{
		sensorManager.unregisterListener(this);

		sendStop();
		btDevice = null;
		btService.stop();

		super.onPause();
	}

	/**
	 * Called when activity is no longer visible (after pause)
	 */
	@Override
	public void onStop()
	{
		sensorManager.unregisterListener(this);

		sendStop();
		btDevice = null;
		btService.stop();

		finish();
		System.exit(0);

		super.onStop();
	}

	/**
	 * Called before activity is shut down (after stop)
	 */
	@Override
	public void onDestroy()
	{
		sendStop();
		btDevice = null;

		// Stop the bluetooth service
		if (btService != null)
			btService.stop();
		finish();
		System.exit(0);

		super.onDestroy();
	}

	/**
	 * Sends a text message to a bluetooth device.
	 * 
	 * @param message string to be sent.
	 */
	private void sendMessage(String message)
	{
		if (!btAdapter.isEnabled()) {
			return;
		}
		if (btDevice == null) {
			return;
		}

		if (message.length() > 0) {
			byte[] data = message.getBytes();
			btService.write(data);
		}
	}

	/**
	 * Handles the result of a startActivityForResult() call.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				// Get device address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);

				// get bluetooth device object
				BluetoothDevice device = btAdapter.getRemoteDevice(address);

				// attempt connection to the device
				boolean result = btService.connect(device);
				if (result) {
					btDevice = device;
				} else {
					btDevice = null;
				}
			}
			break;
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this,
						"Bluetooth ligado, lance a aplicação novamente.",
						Toast.LENGTH_SHORT).show();
				finish();
				// Bluetooth is enabled, start the communication service
				// btService = new BluetoothService(this, commHandler);
			} else {
				// User didn't enable bluetooth or error occured, leave
				// application
				Toast.makeText(this, "Bluetooth não ligado", Toast.LENGTH_SHORT)
						.show();
				finish();
			}
			break;
		case REQUEST_HELP:
			break;
		}
	}

	/**
	 * Populates the Options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);

		return true;
	}

	/**
	 * Performs the appropriate action after selecting a menu item.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
		case R.id.connect:
			// Launch the DeviceListActivity
			Intent connectIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.help:
			Intent helpIntent = new Intent(this, HelpActivity.class);
			startActivityForResult(helpIntent, REQUEST_HELP);
			return true;
		}

		return false;
	}

	/**
	 * Sends a STOP message.
	 */
	public void sendStop()
	{
		view.setText("- STOP -");
		sendMessage("[00:000000000:000000000:000000000]");
		sendMessage("[00:000000000:000000000:000000000]");
		sendMessage("[00:000000000:000000000:000000000]");
		sendMessage("[00:000000000:000000000:000000000]");
		sendMessage("[00:000000000:000000000:000000000]");
		sendMessage("[00:000000000:000000000:000000000]");
		sendMessage("[00:000000000:000000000:000000000]");
	}

	/**
	 * Processes a touch event.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent me)
	{
		switch (me.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(me);
			if (oldDist > 20f) {
				midPoint(mid, me);
				isZooming = true;
			}
			break;
			
		case MotionEvent.ACTION_POINTER_UP:
				isZooming = false;
				oldDist = -1;
				return gesture.onTouchEvent(me);

		case MotionEvent.ACTION_MOVE:
			if (isZooming) {
				float newDist = spacing(me);
				if (newDist > 20f) {
						if (oldDist < 20f) { return gesture.onTouchEvent(me); };
						float scale = newDist / oldDist;
						onZoom(scale);
				}
			}
			break;
		}

		if (!isZooming)
			return gesture.onTouchEvent(me);
		
		return false;
	}

	/**
	 * Calculates the spacing between touche points in a zoom gesture. 
	 */
	private float spacing(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * Calculates the midpoint of a zoom gesture.
	 * @param point
	 * @param event
	 */
	private void midPoint(PointF point, MotionEvent event)
	{
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/**
	 * Processes a finger Down touch. Displays on device screen. No Bluetooth
	 * message sent.
	 */
	public boolean onDown(MotionEvent e)
	{
		view.setText("- DOWN -");
		return false;
	}

	/**
	 * Processes a Tap touch. Displays on device screen. Sends a bluetooth
	 * message.
	 */
	public boolean onSingleTapUp(MotionEvent e)
	{
		view.setText("- SINGLE TAP UP -");
		sendMessage("[01:000000000:000000000:000000000]");
		return false;
	}

	/**
	 * Processes a Show Press touch (medium length press). Displays on device
	 * screen. Sends a bluetooth message.
	 */
	public void onShowPress(MotionEvent e)
	{
		view.setText("- SHOW PRESS -");
		sendMessage("[02:000000000:000000000:000000000]");
	}

	/**
	 * Processes a Long Press touch. Displays on device screen. Sends a
	 * bluetooth message.
	 */
	public void onLongPress(MotionEvent e)
	{
		view.setText("- LONG PRESS -");
		sendMessage("[03:000000000:000000000:000000000]");
	}

	/**
	 * Processes a Fling gesture. Displays on device screen. Sends a bluetooth
	 * message with velocity values.
	 */
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY)
	{
		String x = Float.toString(velocityX);
		if (x.length() > 9) {
			x = x.substring(0, 8);
		}
		if (x.length() < 9) {
			for (int i = x.length(); i < 9; i++)
				x += "0";
		}
		String y = Float.toString(velocityY);
		if (y.length() > 9) {
			y = y.substring(0, 8);
		}
		if (y.length() < 9) {
			for (int i = y.length(); i < 9; i++)
				y += "0";

		}

		view.setText("- FLING - X:" + x + " Y:" + y);
		sendMessage("[04:" + x + ":" + y + ":000000000]");
		return false;
	}

	/**
	 * Processes a Zoom gesture. Displays on device screen. Sends a bluetooth
	 * message with velocity values.
	 */
	public void onZoom(float value)
	{
		String x = Float.toString(value);
		if (x.length() > 9) {
			x = x.substring(0, 8);
		}
		if (x.length() < 9) {
			for (int i = x.length(); i < 9; i++)
				x += "0";
		}
		view.setText("- ZOOM - amnt:" + x);
		sendMessage("[07:" + x + ":000000000:000000000]");
	}

	/**
	 * Processes a Scroll gesture. Displays on device screen. Sends a bluetooth
	 * message with distance values.
	 */
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		if (isZooming) return false;
		
		String x = Float.toString(distanceX);
		if (x.length() > 9) {
			x = x.substring(0, 8);
		}
		if (x.length() < 9) {
			for (int i = x.length(); i < 9; i++)
				x += "0";

		}
		String y = Float.toString(distanceY);
		if (y.length() > 9) {
			y = y.substring(0, 8);
		}
		if (y.length() < 9) {
			for (int i = y.length(); i < 9; i++)
				y += "0";

		}
		
		view.setText("- SCROLL - X:" + x + " Y:" + y);
		sendMessage("[05:" + x + ":" + y + ":000000000]");
		
		return false;
	}

	/**
	 * Processas a change of accuracy for the sensor
	 */
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{
	}

	/**
	 * Processes changes on a sensor's data.
	 */
	public void onSensorChanged(SensorEvent event)
	{	
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			
			if (!gotInitialValue) {
				initialYaw = event.values[0];
				gotInitialValue = true;
			}
			
			String x = Float.toString(initialYaw - event.values[0]);
			if (x.length() > 9) {
				x = x.substring(0, 8);
			}
			if (x.length() < 9) {
				for (int i = x.length(); i < 9; i++)
					x += "0";
			}

			String y = Float.toString(event.values[1]);
			if (y.length() > 9) {
				y = y.substring(0, 8);
			}
			if (y.length() < 9) {
				for (int i = y.length(); i < 9; i++)
					y += "0";
			}

			String z = Float.toString(event.values[2]);
			if (z.length() > 9) {
				z = z.substring(0, 8);
			}
			if (z.length() < 9) {
				for (int i = z.length(); i < 9; i++)
					z += "0";
			}

			Xview.setText("X: " + x);
			Yview.setText("Y: " + y);
			Zview.setText("Z: " + z);

			sendMessage("[06:" + x + ":" + y + ":" + z + "]");
		}
	}
}