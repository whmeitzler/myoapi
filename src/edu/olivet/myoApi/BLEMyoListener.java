package edu.olivet.myoApi;

import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPIDefaultListener;

public class BLEMyoListener extends BGAPIDefaultListener {
	MyoDevice myoDevice;
	
	public void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value) {}
	
	public void receive_connection_disconnected(int connection, int reason) {}
	
	public void receive_attclient_attribute_value(int connection, int atthandle, int type, byte[] value) {}
	
	/*Receives BGAPI calls and forwards them to associated MyoDevice
	 * Provides the following functionality to the MyoDevice
	 *   public void onDeviceConnectionFailed(Address address) {}
  
  public void onDeviceConnected(Address address) {}
  
  public void onDeviceDisconnected(Address address) {}
  
  public void onServicesDiscovered(Address address, boolean success) {}
  
  public void onCharacteristicRead(Address address, UUID uuid, byte[] value, boolean success) {}
  
  public void onCharacteristicWrite(Address address, UUID uuid, boolean success) {}
  
  public void onCharacteristicChanged(Address address, UUID uuid, byte[] value) {}
  
  public void onReadRemoteRssi(Address address, int rssi, boolean success) {}
	 * */
}
