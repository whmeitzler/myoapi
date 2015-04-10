package edu.olivet.myoApi;

import gnu.io.SerialPort;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.thingml.bglib.BDAddr;
import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIDefaultListener;
import org.thingml.bglib.BGAPIListener;
import org.thingml.bglib.BGAPITransport;
import org.thingml.bglib.gui.BLED112;
import org.thingml.bglib.gui.BLEDevice;
import org.thingml.bglib.gui.BLEDeviceList;
import org.thingml.bglib.gui.BLEService;

public class BLEManager{
	public BLEDeviceList devList;
	static SerialPort port;
	static BGAPI bgapi;
	BLEConnectionListener listener;
	boolean discovery_state;

	public BLEManager(){
		bgapi = null;
		listener = new BLEConnectionListener(this);
	}

	public void init(){
		Executors.newSingleThreadExecutor().execute(new Runnable(){
			public void run(){
				BLED112.initRXTX();
			}
		});
	}
	public void connectBLED(){
		connectBLED(BLED112.selectSerialPort());
	}
	public void disconnectBLED(){
		if (bgapi != null) {
			bgapi.removeListener(listener);
			bgapi.send_system_reset(0);
			bgapi.disconnect();
		}
		if (port != null) {
			port.close();
		}
		bgapi = null;
		port = null;
	}
	public void connectBLED(String comPort){
		port  = BLED112.connectSerial(comPort);
		if (port != null) {
			try {
				bgapi = new BGAPI(new BGAPITransport(port.getInputStream(), port.getOutputStream()));
				bgapi.addListener(listener);
				Thread.sleep(250);
				bgapi.send_system_get_info();
			} catch (Exception ex) {
				Logger.getLogger(BLEManager.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}  

	public void connect(BDAddr address){
		bgapi.send_gap_connect_direct(address, 1, 0x3C, 0x3C, 0x64,0);
	}
	public void scan(boolean set){
		if(set){
			bgapi.send_gap_set_scan_parameters(10, 250, 1);
			bgapi.send_gap_discover(1);
		}else{
			bgapi.send_gap_end_procedure();
		}
	}

}
