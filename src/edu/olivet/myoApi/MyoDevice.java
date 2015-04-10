package edu.olivet.myoApi;
import com.thalmic.myo.GattConstants;

import java.util.ArrayList;
import java.util.UUID;

import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIPacket;

import com.thalmic.myo.Arm;
import com.thalmic.myo.BleGattCallback;
import com.thalmic.myo.ControlCommand;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Pose;
import com.thalmic.myo.XDirection;

public class MyoDevice extends BleGattCallback{

	public static enum VibrationType{SHORT, MEDIUM, LONG};
	public static enum UnlockType{TIMED, HOLD};
	public static enum ConnectionState{CONNECTED, CONNECTING, DISCONNECTED};
	//BlueTooth connections
	private BGAPI bgapi;
	int connection;
	//Listeners
	private ArrayList<DeviceListener> listeners;
	//Myo status
	private String mName;
	private final BDAddr mAddress;
	private boolean mAttached;
	private ConnectionState mConnState = ConnectionState.DISCONNECTED;
	private Pose mCurrentPose = Pose.UNKNOWN;
	private Arm mCurrentArm = Arm.UNKNOWN;
	private XDirection mCurrentXDirection = XDirection.UNKNOWN;
	private boolean mUnlocked;
	private Pose mUnlockPose = Pose.UNKNOWN;

	public MyoDevice(BGAPI bgapi, BDAddr address, int BGAPIConnection){
		if(bgapi != null)
			this.bgapi = bgapi;
		this.listeners = new ArrayList<DeviceListener>();
		this.mAddress = address;
		this.connection = BGAPIConnection;
	}

	public void addListner(DeviceListener listener){
		this.listeners.add(listener);
	}
	public void removeListener(DeviceListener listener){
		this.listeners.remove(listener);
	}
	public String getName(){
		return mName;
	}
	public Pose getPose(){
		return mCurrentPose;
	}
	public Pose getUnlockPose(){
		return this.mUnlockPose;
	}
	public boolean isUnlocked(){
		return mUnlocked;
	}
	public XDirection getXDirection()
	{
		return this.mCurrentXDirection;
	}
	public Arm getCurrentArm(){
		return this.mCurrentArm;
	}
	public BDAddr getAddress(){
		return this.mAddress;
	}
	public ConnectionState getConnectionState(){ 
		return this.mConnState;
	}
	public boolean isAttached(){
		return this.mAttached;
	}
	//Setters
	public void setUnlockPose(Pose newPose){
		this.mUnlockPose = newPose;
	}
	public void setCurrentArm(Arm newArm){
		this.mCurrentArm = newArm;
	}
	void setName(String name) {
		this.mName = name;
	}

	void setConnectionState(ConnectionState state) {
		this.mConnState = state;
	}

	void setCurrentPose(Pose pose) {
		this.mCurrentPose = pose;
	}
	  public void configureDataAcquisition(String address, ControlCommand.EmgMode streamEmg, boolean streamImu, boolean enableClassifier)
	  {
	    byte[] enableCommand = ControlCommand.createForSetMode(streamEmg, streamImu, enableClassifier);
	    
	    writeControlCommand(enableCommand);
	  }
	  
	  public void requestRssi(String address) {
	    this.mBleManager.getBleGatt().readRemoteRssi(address);
	  }
	  
	  public void vibrate(VibrationType vibrationType) {
	    byte[] vibrateCommand = ControlCommand.createForVibrate(vibrationType);
	    writeControlCommand(vibrateCommand);
	  }
	  
	  public void unlock(UnlockType unlockType) {
	    byte[] unlockCommand = ControlCommand.createForUnlock(unlockType);
	    writeControlCommand(unlockCommand);
	  }
	  
	  public void notifyUserAction() {
	    byte[] command = ControlCommand.createForUserAction();
	    writeControlCommand(command);
	  }
	  
	  private void writeControlCommand(byte[] controlCommand) {
	    UUID serviceUuid = GattConstants.CONTROL_SERVICE_UUID;
	    UUID charUuid = GattConstants.COMMAND_CHAR_UUID;
	    BGAPIPacket packet = new BGAPIPacket(controlCommand);
	    //this.mBleManager.getBleGatt().writeCharacteristic(address, serviceUuid, charUuid, controlCommand);
	    bgapi.send_attclient_prepare_write(connection, serviceUuid., charUuid, controlCommand);
	  }
}
