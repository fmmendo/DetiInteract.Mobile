package DetiInteract.Mobile.Bluetooth;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import android.R.string;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import DetiInteract.Mobile.*;

public class BluetoothService
{
	private final BluetoothAdapter btAdapter;

	private final Handler commHandler;

	private BluetoothSocket btSocket;
	private BluetoothDevice btDevice;

	private InputStream btInputStream;
	private BufferedOutputStream btOutputStream;
	
	private ReentrantLock lock = new ReentrantLock();
	
	private int accepted = 0;
	private int rejected = 0;

	/**
	 * Constructor. Configures communication between devices.
	 * 
	 * @param _context The UI activity Context
	 */
	public BluetoothService(Context _context, Handler handler)
	{
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		commHandler = handler;

		btSocket = null;
		btDevice = null;

		btInputStream = null;
		btOutputStream = null;
	}

	/**
	 * Starts the BluetoothService.
	 */
	public void start()
	{

	}

	/**
	 * Stops the BluetoothService
	 */
	public void stop()
	{
		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (IOException e) {
			}
			btSocket = null;
		}
		if (btDevice != null) {
			btDevice = null;
		}
        
		btInputStream = null;
		btOutputStream = null;
	}
	
	/**
	 * Sends a message for the main activity to print as Toast text.
	 * @param toastText text to be written.
	 */
	private void sendMessageToMainActivity(String toastText)
	{
		Message msg = commHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, toastText);
		msg.setData(bundle);
		commHandler.sendMessage(msg);
	}
	
	/**
	 * Initiates connection to the given device. Finds a socket and tries to
	 * connect to it, if successful calls the connected() method, if not, sends
	 * a Toast message back to the UI activity via the Handler.
	 * 
	 * @param device BluetoothDevice to connect to.
	 */
	public boolean connect(BluetoothDevice device)
	{
		btDevice = device;
		BluetoothSocket tmp = null;

		// Get the socket for the connection to the given device
		try {
			Method m = btDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			tmp = (BluetoothSocket) m.invoke(btDevice, 1);
		} catch (Exception e) {
			sendMessageToMainActivity("Unable to get Socket!");
			return false;
		}

		btSocket = tmp;

		// cancel device discovery for better performance
		btAdapter.cancelDiscovery();

		// connect to the socket
		try {
			btSocket.connect();
		} catch (IOException e) {
			sendMessageToMainActivity("Unable to connect to socket");

			try {
				btSocket.close();
			} catch (IOException e2) {
				sendMessageToMainActivity("Unable to close socket");
				return false;
			}
		}

		// Send the name of the connected device back to the UI Activity
		sendMessageToMainActivity("Ligado a: " + device.getName());

		boolean result = connected(btSocket);
		
		return result;
	}

	/**
	 * Gets the Input and Output streams for the given socket. Sends a Toast
	 * message back to the UI activity via the Handler if unsuccessful.
	 * 
	 * @param socket The BluetoothSocket on which the connection was made.
	 */
	public boolean connected(BluetoothSocket socket)
	{
		InputStream in = null;
		OutputStream out = null;

		try {
			in = btSocket.getInputStream();
			out = btSocket.getOutputStream();
		} catch (Exception e) {
			sendMessageToMainActivity("Unable to get Streams");
			return false;
		}

		btInputStream = in;
		btOutputStream = new BufferedOutputStream(out);
		
		return true;
	}

	/**
	 * Write to the open Ouput stream. Sends a Toast message back to the UI
	 * activity via the Handler if unsuccessful.
	 * 
	 * @param data Bytes to be written.
	 */
	public synchronized boolean write(byte[] data)
	{
		if (btDevice != null && btOutputStream != null) {
			if (lock.tryLock() == true)
			{
				try {
					btOutputStream.flush();
					btOutputStream.write(data);
				} catch (IOException e) {
					return false;
				}
				lock.unlock();
				return true;
			}
			else {
				rejected++;
				return false;
			}
		}
		return false;
	}

}
