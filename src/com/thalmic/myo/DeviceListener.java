package com.thalmic.myo;

import edu.olivet.myoApi.MyoDevice;

public abstract interface DeviceListener
{
  public abstract void onAttach(MyoDevice paramMyo, long paramLong);
  
  public abstract void onDetach(MyoDevice paramMyo, long paramLong);
  
  public abstract void onConnect(MyoDevice paramMyo, long paramLong);
  
  public abstract void onDisconnect(MyoDevice paramMyo, long paramLong);
  
  public abstract void onArmSync(MyoDevice paramMyo, long paramLong, Arm paramArm, XDirection paramXDirection);
  
  public abstract void onArmUnsync(MyoDevice paramMyo, long paramLong);
  
  public abstract void onUnlock(MyoDevice paramMyo, long paramLong);
  
  public abstract void onLock(MyoDevice paramMyo, long paramLong);
  
  public abstract void onPose(MyoDevice paramMyo, long paramLong, Pose paramPose);
  
  public abstract void onOrientationData(MyoDevice paramMyo, long paramLong, Quaternion paramQuaternion);
  
  public abstract void onAccelerometerData(MyoDevice paramMyo, long paramLong, Vector3 paramVector3);
  
  public abstract void onGyroscopeData(MyoDevice paramMyo, long paramLong, Vector3 paramVector3);
  
  public abstract void onRssi(MyoDevice paramMyo, long paramLong, int paramInt);
}
