package com.thalmic.myo;

import edu.olivet.myoApi.MyoDevice;

public abstract class AbstractDeviceListener
  implements DeviceListener
{
  public void onAttach(MyoDevice MyoDevice, long timestamp) {}
  
  public void onDetach(MyoDevice MyoDevice, long timestamp) {}
  
  public void onConnect(MyoDevice MyoDevice, long timestamp) {}
  
  public void onDisconnect(MyoDevice MyoDevice, long timestamp) {}
  
  public void onArmSync(MyoDevice MyoDevice, long timestamp, Arm arm, XDirection xDirection) {}
  
  public void onArmUnsync(MyoDevice MyoDevice, long timestamp) {}
  
  public void onUnlock(MyoDevice MyoDevice, long timestamp) {}
  
  public void onLock(MyoDevice MyoDevice, long timestamp) {}
  
  public void onPose(MyoDevice MyoDevice, long timestamp, Pose pose) {}
  
  public void onOrientationData(MyoDevice MyoDevice, long timestamp, Quaternion rotation) {}
  
  public void onAccelerometerData(MyoDevice MyoDevice, long timestamp, Vector3 accel) {}
  
  public void onGyroscopeData(MyoDevice MyoDevice, long timestamp, Vector3 gyro) {}
  
  public void onRssi(MyoDevice MyoDevice, long timestamp, int rssi) {}
}
