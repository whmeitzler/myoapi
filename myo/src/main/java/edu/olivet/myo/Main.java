package edu.olivet.myo;

import org.thingml.bglib.*;
import org.thingml.bglib.gui.*;

public class Main {
	public static void main(String[] args) throws Throwable {
		BGAPITransport trans = BLED112.connectBLED112("COM5");
		try {
			BGAPI api = new BGAPI(trans);
			api.addListener(new BGAPIDefaultListener() {
			    @SuppressWarnings("unused")
				public void receive_system_get_info(Integer major, Integer minor, Integer patch, Integer build, Integer ll_version, Integer protocol_version, Integer hw) {
			        System.out.println("get_info_rsp :" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version + " hw=" + hw);
			    }

			    @SuppressWarnings("unused")
				public void receive_gap_scan_response(Integer rssi, Integer packet_type, BDAddr sender, Integer address_type, Integer bond, byte[] data) {
			        System.out.println("FOUND: " + sender.toString() + "["+ new String(data).trim() + "] (rssi = " + rssi + ", packet type= " + packet_type + ")");
			    }

			    @Override
			    public void receive_system_hello() {
			        System.out.println("GOT HELLO!");
			    }
			});
	        try {
	            Thread.sleep(500);
	        } catch (InterruptedException ex) { }
	        System.out.println( "Requesting Version Number..." );
			api.send_system_get_info();
			api.send_system_hello();
			api.send_gap_set_scan_parameters(10, 250, 1);
			api.send_gap_discover(1);
	        try {
	            Thread.sleep(5000);
	        } catch (InterruptedException ex) { }
		} finally {
			trans.stop();
		}
	}
}
