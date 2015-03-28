import edu.olivet.myoApi.BLEManager;




public class Tester {
	
	public static void main(String[] args) {
		BLEManager manager = new BLEManager();
		manager.init();
		manager. connectBLED("COM3");
		manager.setDiscovery(true);
	}

}
