package ble.detection.bledetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

public class BLE_Scan {
	
	public static boolean CALIBRATION_ON = false;
	public static boolean SCANNING_RUN= false;
	private static int COUNTER_THRESHOLD = 9;	//Continuous appearance times of a beacon
	private static int SAMPLE_NUMBER = 10;
	private static long SCAN_PERIOD = 30000;	//Default scan period
	private static int counter = 0;
	private BeaconListener mListener;
	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler = new Handler();
	private HashMap<BluetoothDevice, ArrayList<Integer>> deviceRssiMap = new HashMap<BluetoothDevice, ArrayList<Integer>>();
    private HashMap<BluetoothDevice, Integer> deviceAvgRssi = new HashMap<BluetoothDevice, Integer>();
    private BluetoothDevice mLoudestDevice;
    private BluetoothDevice preLoudestDevice;
	private Set<String> macList;
	private boolean pageLoaded = false;
	
	/**
	 * Constructor
	 * @param adapter
	 * @param set
	 */
	BLE_Scan(BeaconListener listener, BluetoothAdapter adapter, Set<String> set)
	{
		mListener = listener;
		mBluetoothAdapter = adapter;
		macList = set;
	}
	
	/**
	 * To start/stop Scanning
	 * @param bool
	 * @param period
	 */
	public void bleScan(boolean bool, final long period)
	{
		SCAN_PERIOD = period;
		scanLeDevice(bool);
	}
	
	/**
	 * To start/stop Scanning
	 * @param bool
	 */
	public void bleScan(boolean bool)
	{
		scanLeDevice(bool);
	}
	
	/**
	 * Starts or stops scanning BLE devices
	 * @param enable
	 */
    private void scanLeDevice(final boolean enable) {
        
        if (enable) {
            // Stops scanning after the scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	SCANNING_RUN = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            SCANNING_RUN = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else{  
        	SCANNING_RUN = false;
        	mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * Device scan callback method
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    	
        public void onLeScan( final BluetoothDevice device, int rssi, byte[] scanRecord ) 
        {          	
        	//Only add devices in file "MAC_List.txt"
        	if (!macList.contains(device.getAddress()))
        		return;        	
			if(!deviceRssiMap.containsKey(device))	
				deviceRssiMap.put(device, new ArrayList<Integer>());
			deviceRssiMap.get(device).add(rssi);

		   //Calculate Average RSSI values for the device as soon as certain number of samples are collected
		   if(deviceRssiMap.get(device).size() > SAMPLE_NUMBER)
		   {
			   int rssiSum = 0;
			   for (Integer i : deviceRssiMap.get(device))
			   {
				   rssiSum += i;
			   }
			   deviceAvgRssi.put(device, rssiSum / deviceRssiMap.get(device).size());
			   
			   //Comparing all the devices by the average of samples for each to obtain the loudest device
			   mLoudestDevice = device;
			   for (BluetoothDevice dev : deviceAvgRssi.keySet())
			   {
				   if( deviceAvgRssi.get(dev) > deviceAvgRssi.get(mLoudestDevice))
				   {
					   mLoudestDevice = dev;
				   }   
			   }
			   
			   //fire the onBeaconChanged event if a beacon becomes loudest continuously for pre-defined times threshold
			   if (preLoudestDevice == null || !preLoudestDevice.equals(mLoudestDevice))
			   {
				   preLoudestDevice = mLoudestDevice;
				   counter = 1;
				   pageLoaded = false;
			   }
			   else
			   {
				   counter++;
				   if(counter > COUNTER_THRESHOLD && !pageLoaded)
				   {
					   mListener.onBeaconChanged(mLoudestDevice.getAddress(), deviceAvgRssi.get(mLoudestDevice));
					   pageLoaded = true;
				   }
			   }
			   
			   //Calibration Mode
			   if(CALIBRATION_ON)
			   {
				   CALIBRATION_ON = false;
				   mListener.onBeaconCalibration(mLoudestDevice.getAddress(), deviceAvgRssi.get(mLoudestDevice));
			   }
			   
			   //Keep the number of samples, so the average RSSI value is always calculated by the latest samples
			   deviceRssiMap.get(device).remove(0);
		   }
        }
    };
}
