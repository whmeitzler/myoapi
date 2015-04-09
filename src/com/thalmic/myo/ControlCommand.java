package com.thalmic.myo;

import edu.olivet.myoApi.MyoDevice;

public abstract class ControlCommand { private static final byte COMMAND_SET_MODE = 1;
  private static final byte COMMAND_VIBRATION = 3;
  private static final byte COMMAND_UNLOCK = 10;
  private static final byte COMMAND_USER_ACTION = 11;
  private static final byte VIBRATION_NONE = 0;
  private static final byte VIBRATION_SHORT = 1;
  private static final byte VIBRATION_MEDIUM = 2;
  private static final byte VIBRATION_LONG = 3;
  private static final byte EMG_MODE_DISABLED = 0;
  
  public static enum EmgMode { DISABLED, FV, EMG};
  
  private static final byte EMG_MODE_RAW_FV = 1;
  

  private static final byte EMG_MODE_RAW_EMG = 2;
  

  private static final byte IMU_MODE_DISABLED = 0;
  

  private static final byte IMU_MODE_ENABLED = 1;
  

  private static final byte CLASSIFIER_MODE_DISABLED = 0;
  

  private static final byte CLASSIFIER_MODE_ENABLED = 1;
  

  private static final byte UNLOCK_LOCK = 0;
  

  private static final byte UNLOCK_TIMEOUT = 1;
  

  private static final byte UNLOCK_HOLD = 2;
  

  private static final byte USER_ACTION_GENERIC = 0;
  
  private static enum SetMode
  {
    COMMAND_TYPE, 
    PAYLOAD_SIZE, 
    EMG_MODE, 
    IMU_MODE, 
    CLASSIFIER_MODE;
    
    private SetMode() {}
  }
  
  private static enum Vibration
  {
    COMMAND_TYPE, 
    PAYLOAD_SIZE, 
    VIBRATION_TYPE;
    
    private Vibration() {}
  }
  
  private static enum Unlock
  {
    COMMAND_TYPE, 
    PAYLOAD_SIZE, 
    UNLOCK_TYPE;
    
    private Unlock() {}
  }
  
  private static enum UserAction
  {
    COMMAND_TYPE, 
    PAYLOAD_SIZE, 
    USER_ACTION;
    
    private UserAction() {}
  }
  
  static byte[] createForSetMode(EmgMode streamEmg, boolean streamImu, boolean enableClassifier) { byte emgMode = 0;
    switch (streamEmg) {
    case FV: 
      emgMode = 1;
      break;
    case EMG: 
      emgMode = 2;
    }
    
    byte imuMode = (byte) (streamImu ? 1 : 0);
    byte classifierMode = (byte) (enableClassifier ? 1 : 0);
    
    return createForSetMode(emgMode, imuMode, classifierMode);
  }
  
  private static byte[] createForSetMode(byte emgMode, byte imuMode, byte classifierMode) {
    byte[] controlCommand = new byte[SetMode.values().length];
    
    controlCommand[SetMode.COMMAND_TYPE.ordinal()] = 1;
    controlCommand[SetMode.PAYLOAD_SIZE.ordinal()] = ((byte)(controlCommand.length - 2));
    
    controlCommand[SetMode.EMG_MODE.ordinal()] = emgMode;
    controlCommand[SetMode.IMU_MODE.ordinal()] = imuMode;
    controlCommand[SetMode.CLASSIFIER_MODE.ordinal()] = classifierMode;
    
    return controlCommand;
  }
  
  public static byte[] createForVibrate(MyoDevice.VibrationType vibrationType) {
    byte[] command = new byte[Vibration.values().length];
    
    command[Vibration.COMMAND_TYPE.ordinal()] = 3;
    command[Vibration.PAYLOAD_SIZE.ordinal()] = 1;
    command[Vibration.VIBRATION_TYPE.ordinal()] = getVibrationType(vibrationType);
    
    return command;
  }
  
  private static byte getVibrationType(MyoDevice.VibrationType vibrationType) {
    switch (vibrationType) {
    case SHORT: 
      return 1;
    case MEDIUM: 
      return 2;
    case LONG: 
      return 3;
    }
    return 0;
  }
  
  public static byte[] createForUnlock(MyoDevice.UnlockType unlockType)
  {
    byte[] command = new byte[Unlock.values().length];
    
    command[Unlock.COMMAND_TYPE.ordinal()] = 10;
    command[Unlock.PAYLOAD_SIZE.ordinal()] = 1;
    command[Unlock.UNLOCK_TYPE.ordinal()] = getUnlockTypeType(unlockType);
    
    return command;
  }
  
  private static byte getUnlockTypeType(MyoDevice.UnlockType unlockType) {
    if (unlockType == null)
    {
      return 0;
    }
    
    switch (unlockType) {
    case TIMED: 
      return 1;
    case HOLD: 
      return 2;
    }
    
    throw new IllegalArgumentException("Unknown UnlockType: " + unlockType);
  }

  public static byte[] createForUserAction()
  {
    byte[] command = new byte[Unlock.values().length];
    
    command[UserAction.COMMAND_TYPE.ordinal()] = 11;
    command[UserAction.PAYLOAD_SIZE.ordinal()] = 1;
    command[UserAction.USER_ACTION.ordinal()] = 0;
    
    return command;
  }
}
