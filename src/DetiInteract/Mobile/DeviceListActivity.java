package DetiInteract.Mobile;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity
{
	private BluetoothAdapter btAdapter;
	private ArrayAdapter<String> btPairedDevices;
	private ArrayAdapter<String> btNewDevices;
	
	 // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		//setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		
		//set result as canceled if user hits Back
		setResult(Activity.RESULT_CANCELED);
		
		btPairedDevices = new ArrayAdapter<String>(this, R.layout.device_name);
		btNewDevices = new ArrayAdapter<String>(this, R.layout.device_name);
		
        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(btPairedDevices);
        pairedListView.setOnItemClickListener(deviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(btNewDevices);
        newDevicesListView.setOnItemClickListener(deviceClickListener);
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
        
        // get the bluetooth adapter 
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        //if (pairedDevices.size() > 0) 
        //{
            findViewById(R.id.text_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) 
            {
            	btPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        //} 
        //else 
        //{
        //    String noDevices = getResources().getText(R.string.no_paired).toString();
        //    btPairedDevices.add(noDevices);
        //}
        
        // start searching for devices
        //discoverDevices();
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		if (btAdapter != null)
			btAdapter.cancelDiscovery();
		
		this.unregisterReceiver(receiver);
	}
	
	private void discoverDevices()
	{
		setProgressBarIndeterminateVisibility(true);
		setTheme(R.string.searching);
		
		findViewById(R.id.text_new_devices).setVisibility(View.VISIBLE);
		
		if (btAdapter.isDiscovering())
			btAdapter.cancelDiscovery();
		
		btAdapter.startDiscovery();
	}
	
	private OnItemClickListener deviceClickListener = new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			btAdapter.cancelDiscovery();
			
			String deviceInfo = ((TextView)arg1).getText().toString();
			String address = deviceInfo.substring(deviceInfo.length() - 17);
			
			// Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
		}
		
	};
	
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    btNewDevices.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (btNewDevices.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.no_found).toString();
                    btNewDevices.add(noDevices);
                }
            }
        }
    };

}
