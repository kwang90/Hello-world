package ble.detection.bledetection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.appcompat.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

@SuppressLint({ "SetJavaScriptEnabled", "ShowToast" })
public class BLE_Activity extends ActionBarActivity implements BeaconListener {

	private static int ACCESS_RSSI_THRESHOLD = -55;
	private final static long SCAN_PERIOD = 30000;
    private final static long SCAN_SLEEP_PERIOD = 3000;
	private static final int REQUEST_ENABLE_BT = 123;
	//private BLE_Activity c;
	private Thread mThread;
    private BLE_Scan mBLEScan;
	private BluetoothAdapter mBluetoothAdapter;
	private HashMap<String, String> beaconsWebpage = new HashMap<String, String>();	// MAC -> URL
	private WebView myWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ble__detection);
		
		//Check if BLE supported on the device 
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		    Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
		}
		else
		{
			//Set up layout
			myWebView = (WebView) findViewById(R.id.webView);
			WebSettings webSettings = myWebView.getSettings();
			webSettings.setJavaScriptEnabled(true);
			myWebView.loadUrl("file:///android_asset/Empty.html");
			
			//Read configurations from assets
			try {
				final AssetManager mAsset = getAssets();
				BufferedReader reader = new BufferedReader(new InputStreamReader(mAsset.open("MAC_List.txt")));
				String line = "";
				while ((line = reader.readLine()) != null)
				{
					String[] str = line.split(";");
					beaconsWebpage.put(str[0], str[1]);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Create BLE_Scan object
			final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = bluetoothManager.getAdapter();
			mBLEScan = new BLE_Scan((BeaconListener)this, mBluetoothAdapter, beaconsWebpage.keySet());
			
			//Keep turning on BLE scanning
			mThread = new Thread(){
				@Override
				public void run(){
					 
					try {
						while(true)
						{
							//Start Scanning BLE devices 
							if(!BLE_Scan.SCANNING_RUN)
								mBLEScan.bleScan(true, SCAN_PERIOD);
							//Stop updating for pre-defined period
							sleep(SCAN_SLEEP_PERIOD);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			 };
		}
	}
	
	@Override
	protected void onResume()
	{
		// Requests user permission to enable Bluetooth.
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		Toast.makeText(this, "On Resume", Toast.LENGTH_SHORT).show();
		
		//Start the beacon updating thread
		if(!mThread.isAlive())
			mThread.start();
		Toast.makeText(this, "Initial ACCESS_RSSI_THRESHOLD\n" + ACCESS_RSSI_THRESHOLD, Toast.LENGTH_SHORT).show();
		super.onResume();
	}
	
	@Override
	protected void onDestroy()
	{
		mBLEScan.bleScan(false);
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		//Stops scanning
		mBLEScan.bleScan(false);
		super.onPause();
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ble__detection, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_calibration:
                rssiCalibration();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void rssiCalibration() {
    	
    	BLE_Scan.CALIBRATION_ON = true;
    	Toast.makeText(this, "Calibration On", Toast.LENGTH_SHORT).show();
	}
    

	@Override
    /**
     * Callback method for loudest beacon changed
     */
	public void onBeaconChanged(final String newMac, final Integer rssi) { 
    	
    	String url;
    	if(	rssi < ACCESS_RSSI_THRESHOLD )
    		url = "Empty.html";
    	else
    		url = beaconsWebpage.get(newMac);
    	
    	final String strRun = url;
    	runOnUiThread(new Runnable(){
			@Override
			public void run() {
		    	myWebView.loadUrl("file:///android_asset/"+strRun);	
		    	//Toast.makeText(c, newMac + "," +rssi, Toast.LENGTH_LONG).show();			
			}} );
	}

	@Override
	public void onBeaconCalibration(final String mac, Integer avgRssi) {
		
		ACCESS_RSSI_THRESHOLD = avgRssi;	
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				//Toast.makeText(c, "ACCESS_RSSI_THRESHOLD Set to : "+ ACCESS_RSSI_THRESHOLD + "\nBy :" + mac, Toast.LENGTH_LONG).show();		
			}} );
	}

}
