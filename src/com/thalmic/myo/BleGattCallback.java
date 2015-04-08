package com.thalmic.myo;

import java.util.UUID;

import org.thingml.bglib.BDAddr;

public abstract class BleGattCallback
{
  public void onDeviceConnectionFailed(BDAddr address) {}
  
  public void onDeviceConnected(BDAddr address) {}
  
  public void onDeviceDisconnected(BDAddr address) {}
  
  public void onServicesDiscovered(BDAddr address, boolean success) {}
  
  public void onCharacteristicRead(BDAddr address, UUID uuid, byte[] value, boolean success) {}
  
  public void onCharacteristicWrite(BDAddr address, UUID uuid, boolean success) {}
  
  public void onCharacteristicChanged(BDAddr address, UUID uuid, byte[] value) {}
  
  public void onReadRemoteRssi(BDAddr address, int rssi, boolean success) {}
}
