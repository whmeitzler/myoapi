package edu.olivet.myo;

import javax.swing.JOptionPane;

import org.thingml.bglib.*;
import org.thingml.bglib.gui.*;

public class Main {
	public static void main(String[] args){
		BLEExplorerFrame f = new BLEExplorerFrame();
		f.main(args);
	}
	public static void amain(String[] args) throws Throwable {
		//BLED112.getAvailableSerialPorts().stream().map(e -> e.getName()).forEach(System.out::println);
		BGAPITransport trans = BLED112.connectBLED112(BLED112.selectSerialPort());//("/dev/tty.usbmodem1");
		try {
			BGAPI api = new BGAPI(trans);
			BGAPIPacketLogger logger = new BGAPIPacketLogger();
			api.addListener(new BGAPIDefaultListener() {
			    @SuppressWarnings("unused")
				public void receive_system_get_info(Integer major, Integer minor, Integer patch, Integer build, Integer ll_version, Integer protocol_version, Integer hw) {
			        System.out.println("get_info_rsp :" + major + "." + minor + "." + patch + " (" + build + ") " + "ll=" + ll_version + " hw=" + hw);
			    }

			    @SuppressWarnings("unused")
				public void receive_gap_scan_response(Integer rssi, Integer packet_type, BDAddr sender, Integer address_type, Integer bond, byte[] data) {
			        JOptionPane.showMessageDialog(null,null,  "FOUND: " + sender.toString() + "["+ new String(data).trim() + "] (rssi = " + rssi + ", packet type= " + packet_type + ")", JOptionPane.INFORMATION_MESSAGE);
			    	System.err.println("FOUND: " + sender.toString() + "["+ new String(data).trim() + "] (rssi = " + rssi + ", packet type= " + packet_type + ")");
			    }

			    @Override
			    public void receive_system_hello() {
			        System.out.println("GOT HELLO!");
			    }

			    @Override
				public void receive_attributes_write(int result) {
			    	System.out.println("receive_attributes_write");
			    }

			    @Override
				public void receive_attributes_read(int handle, int offset, int result, byte[] value) {
			    	System.out.println("receive_attributes_read");
			    }

			    @Override
				public void receive_attributes_read_type(int handle, int result, byte[] value) {
			    	System.out.println("receive_attributes_read_type");
			    }

			    @Override
				public void receive_attributes_user_response() {
			    	System.out.println("receive_attributes_user_response");
			    }

			    @Override
				public void receive_attributes_value(int connection, int reason, int handle, int offset, byte[] value) {
			    	System.out.println("receive_attributes_value");
			    }

			    @Override
				public void receive_attributes_user_request(int connection, int handle, int offset) {
			    	System.out.println("receive_attributes_user_request");
			    }
			});
	        try {
	            Thread.sleep(500);
	        } catch (InterruptedException ex) { }
	        System.out.println("Requesting Version Number...");
			api.send_system_get_info();
			Thread.sleep(2000);
			api.send_system_hello();
			Thread.sleep(3000);
			api.getLowLevelDriver().addListener(logger);
			api.send_gap_set_scan_parameters(10, 250, 1);
			Thread.sleep(100);
			System.out.println("Scanning");
	        	api.send_gap_discover(1);
	            Thread.sleep(5000);
		} finally {}
	}
}
