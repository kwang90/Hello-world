package ble.detection.bledetection;

public interface BeaconListener {
	
	/**
	 * 
	 * @param newMac
	 * @param avgRssi 
	 */
	public void onBeaconChanged(String newMac, Integer avgRssi);
	
	/**
	 * 
	 * @param mac
	 * @param avgRssi
	 */
	public void onBeaconCalibration(String mac, Integer avgRssi);
}
