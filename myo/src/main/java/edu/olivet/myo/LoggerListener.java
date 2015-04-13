package edu.olivet.myo;

import java.util.*;

import org.thingml.bglib.*;
import org.thingml.bglib.gui.*;

import edu.olivet.myo.ControlCommand.*;

public class LoggerListener implements BGAPIListener {
	public final BGAPI api;

	public LoggerListener(BGAPI api) {
		this.api = api;
	}

	@Override
	public void receive_system_hello() {
		System.out.println("receive_system_hello()");
		api.send_system_get_info();
	}

	@Override
	public void receive_system_address_get(BDAddr arg0) {
		System.out.println("receive_system_address_get(" + arg0 + ")");
	}

	@Override
	public void receive_system_reg_read(int arg0, int arg1) {
		System.out.println("receive_system_reg_read(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_system_get_connections(int arg0) {
		System.out.println("receive_system_get_connections(" + arg0 + ")");
	}

	@Override
	public void receive_system_reset() {
		System.out.println("receive_system_reset()");
	}

	@Override
	public void receive_system_get_counters(int arg0, int arg1, int arg2, int arg3) {
		System.out.println("receive_system_get_counters(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ")");
	}

	@Override
	public void receive_system_read_memory(int arg0, byte[] arg1) {
		System.out.println("receive_system_read_memory(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_system_reg_write(int arg0) {
		System.out.println("receive_system_reg_write(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_io_port_config_direction(int arg0) {
		System.out.println("receive_hardware_io_port_config_direction(" + arg0 + ")");
	}

	private int state;
	private Iterator<Group> groupItr;
	private Group group;

	@Override
	public void receive_attclient_procedure_completed(int connection, int result, int chrhandle) {
		System.out.println("receive_attclient_procedure_completed(" + connection + ", " + result + ", " + chrhandle + ")");
		switch (state) {
			case SERVICES:
				state = ATTRIBUTES;
				groupItr = groups.values().iterator();
			case ATTRIBUTES:
				if (groupItr != null && groupItr.hasNext()) {
					group = groupItr.next();
					System.out.println(group.uuid);
					api.send_attclient_find_information(connection, group.start, group.end);
					break;
				}
				groupItr = null;
				group = null;
				state = IDLE;
				System.out.println("hey");
				//int atthandle = group.atts.get(new MaybeUUID("42 48 12 4a 7f 2c 48 47 b9 de 4 a9 4 1 6 d5"));
				Group g = groups.get(new MaybeUUID(UUID.fromString("4248124a-7f2c-4847-b9de-04a9040006d5")));
				int atthandle = g.atts.get(new MaybeUUID(UUID.fromString("4248124a-7f2c-4847-b9de-04a9040106d5")));
				byte[] data = ControlCommand.createForVibrate(VibrationType.LONG);
				System.out.println(Arrays.toString(data));
				api.send_attclient_write_command(connection, atthandle, data);
				api.send_attclient_attribute_write(connection, atthandle, data);
				api.send_attclient_prepare_write(connection, atthandle, 0, data);
				api.send_attclient_execute_write(connection, 1);
				break;
		}
	}

	@Override
	public void receive_attclient_attribute_value(int arg0, int arg1, int arg2, byte[] arg3) {
		System.out.println("receive_attclient_attribute_value(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + ByteUtils.bytesToString(arg3) + ")");
	}

	@Override
	public void receive_hardware_io_port_config_irq(int arg0) {
		System.out.println("receive_hardware_io_port_config_irq(" + arg0 + ")");
	}

	@Override
	public void receive_connection_version_update(int arg0, int arg1) {
		System.out.println("receive_connection_version_update(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attclient_read_by_group_type(int arg0, int arg1) {
		System.out.println("receive_attclient_read_by_group_type(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attclient_find_by_type_value(int arg0, int arg1) {
		System.out.println("receive_attclient_find_by_type_value(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attclient_attribute_write(int arg0, int arg1) {
		System.out.println("receive_attclient_attribute_write(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_hardware_io_port_config_pull(int arg0) {
		System.out.println("receive_hardware_io_port_config_pull(" + arg0 + ")");
	}

	@Override
	public void receive_attclient_find_information(int arg0, int arg1) {
		System.out.println("receive_attclient_find_information(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_connection_channel_map_set(int arg0, int arg1) {
		System.out.println("receive_connection_channel_map_set(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_hardware_io_port_config_function(int arg0) {
		System.out.println("receive_hardware_io_port_config_function(" + arg0 + ")");
	}

	@Override
	public void receive_attclient_attribute_found(int arg0, int arg1, int arg2, int arg3, byte[] arg4) {
		System.out.println("receive_attclient_attribute_found(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ", " + ByteUtils.bytesToString(arg4) + ")");
	}

	@Override
	public void receive_connection_channel_map_get(int arg0, byte[] arg1) {
		System.out.println("receive_connection_channel_map_get(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_attclient_find_information_found(int connection, int chrhandle, byte[] uuid) {
		System.out.println("receive_attclient_find_information_found(" + connection + ", " + chrhandle + ", " + new MaybeUUID(uuid) + ")");
		group.atts.put(new MaybeUUID(uuid), chrhandle);
	}

	@Override
	public void receive_attclient_read_multiple_response(int arg0, byte[] arg1) {
		System.out.println("receive_attclient_read_multiple_response(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_gap_set_directed_connectable_mode(int arg0) {
		System.out.println("receive_gap_set_directed_connectable_mode(" + arg0 + ")");
	}

	@Override
	public void receive_attclient_indicated(int arg0, int arg1) {
		System.out.println("receive_attclient_indicated(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_flash_write_words() {
		System.out.println("receive_flash_write_words()");
	}

	@Override
	public void receive_gap_set_mode(int arg0) {
		System.out.println("receive_gap_set_mode(" + arg0 + ")");
	}

	@Override
	public void receive_gap_end_procedure(int arg0) {
		System.out.println("receive_gap_end_procedure(" + arg0 + ")");
	}

	@Override
	public void receive_system_whitelist_clear() {
		System.out.println("receive_system_whitelist_clear()");
	}

	@Override
	public void receive_flash_erase_page(int arg0) {
		System.out.println("receive_flash_erase_page(" + arg0 + ")");
	}

	@Override
	public void receive_gap_connect_selective(int arg0, int arg1) {
		System.out.println("receive_gap_connect_selective(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_gap_set_filtering(int arg0) {
		System.out.println("receive_gap_set_filtering(" + arg0 + ")");
	}

	@Override
	public void receive_gap_set_scan_parameters(int arg0) {
		System.out.println("receive_gap_set_scan_parameters(" + arg0 + ")");
	}

	@Override
	public void receive_system_endpoint_tx() {
		System.out.println("receive_system_endpoint_tx()");
	}

	@Override
	public void receive_attributes_write(int arg0) {
		System.out.println("receive_attributes_write(" + arg0 + ")");
	}

	@Override
	public void receive_attributes_read_type(int arg0, int arg1, byte[] arg2) {
		System.out.println("receive_attributes_read_type(" + arg0 + ", " + arg1 + ", " + ByteUtils.bytesToString(arg2) + ")");
	}

	@Override
	public void receive_attclient_read_long(int arg0, int arg1) {
		System.out.println("receive_attclient_read_long(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attributes_user_response() {
		System.out.println("receive_attributes_user_response()");
	}

	private final Map<MaybeUUID, Group> groups = new HashMap<>();

	public static class Group {
		public final Map<MaybeUUID, Integer> atts = new HashMap<>();
		public final int start, end;
		public final MaybeUUID uuid;

		public Group(int start, int end, MaybeUUID uuid) {
			this.start = start;
			this.end = end;
			this.uuid = uuid;
		}
	}

	@Override
	public void receive_attclient_group_found(int connection, int start, int end, byte[] uuid) {
		System.out.println("receive_attclient_group_found(" + connection + ", " + start + ", " + end + ", " + new MaybeUUID(uuid) + ")");
		MaybeUUID m = new MaybeUUID(uuid);
		groups.put(m, new Group(start, end, m));
	}

	@Override
	public void receive_attributes_user_request(int arg0, int arg1, int arg2) {
		System.out.println("receive_attributes_user_request(" + arg0 + ", " + arg1 + ", " + arg2 + ")");
	}

	@Override
	public void receive_sm_passkey_entry(int arg0) {
		System.out.println("receive_sm_passkey_entry(" + arg0 + ")");
	}

	@Override
	public void receive_connection_disconnected(int arg0, int arg1) {
		System.out.println("receive_connection_disconnected(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attclient_reserved() {
		System.out.println("receive_attclient_reserved()");
	}

	@Override
	public void receive_system_get_info(int major, int minor, int patch, int build, int ll_version, int protocol_version, int hw) {
		System.out.println("receive_system_get_info(" + major + ", " + minor + ", " + patch + ", " + build + ", " + ll_version + ", " + protocol_version + ", " + hw + ")");
    	api.send_gap_set_mode(0, 0);
    	api.send_connection_disconnect(0);
    	api.send_gap_end_procedure();
		api.send_gap_set_scan_parameters(0xC8, 0xC8, 1);
    	api.send_gap_discover(1);
	}

	@Override
	public void receive_attributes_read(int arg0, int arg1, int arg2, byte[] arg3) {
		System.out.println("receive_attributes_read(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + ByteUtils.bytesToString(arg3) + ")");
	}

	@Override
	public void receive_connection_get_status(int arg0) {
		System.out.println("receive_connection_get_status(" + arg0 + ")");
	}

	@Override
	public void receive_connection_feature_ind(int arg0, byte[] arg1) {
		System.out.println("receive_connection_feature_ind(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_system_whitelist_append(int arg0) {
		System.out.println("receive_system_whitelist_append(" + arg0 + ")");
	}

	@Override
	public void receive_connection_get_rssi(int arg0, int arg1) {
		System.out.println("receive_connection_get_rssi(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_flash_ps_erase() {
		System.out.println("receive_flash_ps_erase()");
	}

	@Override
	public void receive_attclient_prepare_write(int arg0, int arg1) {
		System.out.println("receive_attclient_prepare_write(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_connection_raw_tx(int arg0) {
		System.out.println("receive_connection_raw_tx(" + arg0 + ")");
	}

	@Override
	public void receive_connection_version_ind(int arg0, int arg1, int arg2, int arg3) {
		System.out.println("receive_connection_version_ind(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ")");
	}

	@Override
	public void receive_connection_disconnect(int arg0, int arg1) {
		System.out.println("receive_connection_disconnect(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_system_boot(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
		System.out.println("receive_system_boot(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ", " + arg4 + ", " + arg5 + ", " + arg6 + ")");
	}

	@Override
	public void receive_sm_encrypt_start(int arg0, int arg1) {
		System.out.println("receive_sm_encrypt_start(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_connection_update(int arg0, int arg1) {
		System.out.println("receive_connection_update(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_sm_set_bondable_mode() {
		System.out.println("receive_sm_set_bondable_mode()");
	}

	@Override
	public void receive_attclient_read_multiple(int arg0, int arg1) {
		System.out.println("receive_attclient_read_multiple(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_sm_delete_bonding(int arg0) {
		System.out.println("receive_sm_delete_bonding(" + arg0 + ")");
	}

	@Override
	public void receive_sm_get_bonds(int arg0) {
		System.out.println("receive_sm_get_bonds(" + arg0 + ")");
	}

	@Override
	public void receive_sm_set_oob_data() {
		System.out.println("receive_sm_set_oob_data()");
	}

	@Override
	public void receive_flash_ps_load(int arg0, byte[] arg1) {
		System.out.println("receive_flash_ps_load(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_system_endpoint_rx(int arg0, byte[] arg1) {
		System.out.println("receive_system_endpoint_rx(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_connection_raw_rx(int arg0, byte[] arg1) {
		System.out.println("receive_connection_raw_rx(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_system_whitelist_remove(int arg0) {
		System.out.println("receive_system_whitelist_remove(" + arg0 + ")");
	}

	@Override
	public void receive_sm_smp_data(int arg0, int arg1, byte[] arg2) {
		System.out.println("receive_sm_smp_data(" + arg0 + ", " + arg1 + ", " + ByteUtils.bytesToString(arg2) + ")");
	}

	@Override
	public void receive_attributes_value(int arg0, int arg1, int arg2, int arg3, byte[] arg4) {
		System.out.println("receive_attributes_value(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ", " + ByteUtils.bytesToString(arg4) + ")");
	}

	@Override
	public void receive_flash_ps_key(int arg0, byte[] arg1) {
		System.out.println("receive_flash_ps_key(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_flash_ps_save(int arg0) {
		System.out.println("receive_flash_ps_save(" + arg0 + ")");
	}

	private static final int IDLE = 0;
    private static final int SERVICES = 1;
    private static final int ATTRIBUTES = 2;

	@Override
	public void receive_connection_status(int connection, int flags, BDAddr address, int address_type, int conn_interval, int timeout, int latency, int bonding) {
		System.out.println("receive_connection_status(" + connection + ", " + flags + ", " + address + ", " + address_type + ", " + conn_interval + ", " + timeout + ", " + latency + ", " + bonding + ")");
		state = SERVICES;
		api.send_attclient_read_by_group_type(connection, 0x0001, 0xffff, new byte[] { 0x00, 0x28 });
	}

	@Override
	public void receive_attclient_execute_write(int arg0, int arg1) {
		System.out.println("receive_attclient_execute_write(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_sm_passkey_display(int arg0, int arg1) {
		System.out.println("receive_sm_passkey_display(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_sm_passkey_request(int arg0) {
		System.out.println("receive_sm_passkey_request(" + arg0 + ")");
	}

	@Override
	public void receive_system_debug(byte[] arg0) {
		System.out.println("receive_system_debug(" + ByteUtils.bytesToString(arg0) + ")");
	}

	@Override
	public void receive_flash_ps_defrag() {
		System.out.println("receive_flash_ps_defrag()");
	}

	@Override
	public void receive_flash_ps_erase_all() {
		System.out.println("receive_flash_ps_erase_all()");
	}

	@Override
	public void receive_connection_features_get(int arg0, int arg1) {
		System.out.println("receive_connection_features_get(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attclient_read_by_type(int arg0, int arg1) {
		System.out.println("receive_attclient_read_by_type(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_attclient_write_command(int arg0, int arg1) {
		System.out.println("receive_attclient_write_command(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_flash_ps_dump() {
		System.out.println("receive_flash_ps_dump()");
	}

	@Override
	public void receive_sm_bond_status(int arg0, int arg1, int arg2, int arg3) {
		System.out.println("receive_sm_bond_status(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ")");
	}

	@Override
	public void receive_sm_bonding_fail(int arg0, int arg1) {
		System.out.println("receive_sm_bonding_fail(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_gap_set_privacy_flags() {
		System.out.println("receive_gap_set_privacy_flags()");
	}

	@Override
	public void receive_gap_discover(int arg0) {
		System.out.println("receive_gap_discover(" + arg0 + ")");
	}

	@Override
	public void receive_attclient_read_by_handle(int arg0, int arg1) {
		System.out.println("receive_attclient_read_by_handle(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_sm_set_parameters() {
		System.out.println("receive_sm_set_parameters()");
	}

	@Override
	public void receive_gap_connect_direct(int result, int connection_handle) {
		System.out.println("receive_gap_connect_direct(0x" + Integer.toHexString(result) + ", " + connection_handle + ")");
	}

	@Override
	public void receive_test_phy_rx() {
		System.out.println("receive_test_phy_rx()");
	}

	@Override
	public void receive_gap_set_adv_data(int arg0) {
		System.out.println("receive_gap_set_adv_data(" + arg0 + ")");
	}

	@Override
	public void receive_test_phy_end(int arg0) {
		System.out.println("receive_test_phy_end(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_set_txpower() {
		System.out.println("receive_hardware_set_txpower()");
	}

	@Override
	public void receive_hardware_soft_timer(int arg0) {
		System.out.println("receive_hardware_soft_timer(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_set_soft_timer(int arg0) {
		System.out.println("receive_hardware_set_soft_timer(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_adc_read(int arg0) {
		System.out.println("receive_hardware_adc_read(" + arg0 + ")");
	}

	@Override
	public void receive_gap_mode_changed(int arg0, int arg1) {
		System.out.println("receive_gap_mode_changed(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_hardware_io_port_write(int arg0) {
		System.out.println("receive_hardware_io_port_write(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_spi_transfer(int arg0, int arg1, byte[] arg2) {
		System.out.println("receive_hardware_spi_transfer(" + arg0 + ", " + arg1 + ", " + ByteUtils.bytesToString(arg2) + ")");
	}

	@Override
	public void receive_hardware_io_port_status(int arg0, int arg1, int arg2, int arg3) {
		System.out.println("receive_hardware_io_port_status(" + arg0 + ", " + arg1 + ", " + arg2 + ", " + arg3 + ")");
	}

	@Override
	public void receive_gap_set_adv_parameters(int arg0) {
		System.out.println("receive_gap_set_adv_parameters(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_io_port_read(int arg0, int arg1, int arg2) {
		System.out.println("receive_hardware_io_port_read(" + arg0 + ", " + arg1 + ", " + arg2 + ")");
	}

	@Override
	public void receive_hardware_spi_config(int arg0) {
		System.out.println("receive_hardware_spi_config(" + arg0 + ")");
	}

	@Override
	public void receive_gap_scan_response(int rssi, int packet_type, BDAddr sender, int address_type, int bond, byte[] data) {
		System.out.println("receive_gap_scan_response(" + rssi + ", " + packet_type + ", " + sender + ", " + address_type + ", " + bond + ", \"" + ByteUtils.bytesToString(data) + "\")");
		if (!sender.toString().equals("d7:1c:70:15:bc:a4"))
			return;
		/*String name;
		try {
			int i = 0;
			int a = data[i++] & 0xff;
			int b = data[i++] & 0xff;
			int j = i;
			while (data[i++] != 0);
			name = new String(data, j, i - j - 1, StandardCharsets.UTF_8);
			System.out.println(j + ", " + i);
		} catch (Exception x) {
			name = null;
		}
		System.out.println("Name: \"" + name + "\"");
		if (name == null || !name.equals("ONU Myo"))
			return;*/
		api.send_gap_connect_direct(sender, address_type, 0x20, 0x30, 0x100, 0);
	}

	@Override
	public void receive_hardware_i2c_read(int arg0, byte[] arg1) {
		System.out.println("receive_hardware_i2c_read(" + arg0 + ", " + ByteUtils.bytesToString(arg1) + ")");
	}

	@Override
	public void receive_hardware_i2c_write(int arg0) {
		System.out.println("receive_hardware_i2c_write(" + arg0 + ")");
	}

	@Override
	public void receive_hardware_adc_result(int arg0, int arg1) {
		System.out.println("receive_hardware_adc_result(" + arg0 + ", " + arg1 + ")");
	}

	@Override
	public void receive_test_phy_tx() {
		System.out.println("receive_test_phy_tx()");
	}

	@Override
	public void receive_test_get_channel_map(byte[] arg0) {
		System.out.println("receive_test_get_channel_map(" + ByteUtils.bytesToString(arg0) + ")");
	}

	@Override
	public void receive_test_phy_reset() {
		System.out.println("receive_test_phy_reset()");
	}
}
