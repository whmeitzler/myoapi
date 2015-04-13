package edu.olivet.myo;

import org.thingml.bglib.*;
import org.thingml.bglib.gui.*;

public class Main {
	public static void main(String[] args) throws Throwable {
		//BLED112.getAvailableSerialPorts().stream().map(e -> e.getName()).forEach(System.out::println);
		BGAPITransport trans = BLED112.connectBLED112("COM5");//BLED112.selectSerialPort());//("/dev/tty.usbmodem1");
		BGAPI api = new BGAPI(trans);
		//BGAPIPacketLogger logger = new BGAPIPacketLogger();
		api.addListener(new LoggerListener(api));
		//api.getLowLevelDriver().addListener(logger);
		api.send_system_hello();
	}
}
