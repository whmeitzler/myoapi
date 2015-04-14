// Bluegiga ANSI C BGLib demo application: Health Thermometer Collector
//
// Contact: support@bluegiga.com
//
// This is free software distributed under the terms of the MIT license reproduced below.
//
// Copyright (c) 2013 Bluegiga Technologies
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

// =============================================================================

/*
BASIC ARCHITECTURAL OVERVIEW:
    The program starts, initializes the dongle to a known state, then starts
    scanning. Each time an advertisement packet is found, a scan response
    event packet is generated. These packets are read by polling the serial
    port to which the BLE(D)11x is attached.

    The basic process is as follows:
      a. Scan for devices
      b. If the desired UUID is found in an ad packet, connect to that device
      c. Search for all "service" descriptors to find the target service handle range
      d. Search through the target service to find the thermometer measurement attribute handle
      e. Enable notifications on the thermometer measurement attribute
      f. Read and display incoming thermometer values until terminated (Ctrl+C)

FUNCTION ANALYSIS:

1. main:
    Parses and validates command-line arguments, then initializes the serial
    port (if directed to) and begins running specified action (serial port
    list, device info, peripheral scan, or direct connection to a known MAC
    address). In the case of a connection it sends commands to cause the
    device to disconnect, stop advertising, and stop scanning (i.e. return to
    a known idle/standby state). Some of these commands will fail since the
    device cannot be doing all of these things at the same time, but this is
    not a problem. This function finishes
    by setting scan parameters and initiating a scan with the "gap_discover"
    command.

2. ble_evt_gap_scan_response:
    Raised during scanning whenever an advertisement packet is detected. The
    data provided includes the MAC address, RSSI, and ad packet data payload.
    This payload includes fields which contain any services being advertised,
    which allows us to scan for a specific service. In this demo, the service
    we are searching for has a standard 16-bit UUID which is contained in the
    "uuid_htm_service" variable. Once a match is found, the script initiates
    a connection request with the "gap_connect_direct" command.

3. ble_evt_connection_status
    Raised when the connection status is updated. This happens when the
    connection is first established, and the "flags" byte will contain 0x05 in
    this instance. However, it will also happen if the connected devices bond
    (i.e. pair), or if encryption is enabled (e.g. with "sm_encrypt_start").
    Once a connection is established, the script begins a service discovery
    with the "attclient_read_by_group_type" command.

4. ble_evt_attclient_group_found
    Raised for each group found during the search started in #3. If the right
    service is found (matched by UUID), then its start/end handle values are
    stored for usage later. We cannot use them immediately because the ongoing
    read-by-group-type procedure must finish first.

5. ble_evt_attclient_find_information_found
    Raised for each attribute found during the search started after the service
    search completes. We look for two specific attributes during this process;
    the first is the unique health thermometer measurement attribute which has
    a standard 16-bit UUID (contained in the "uuid_htm_measurement_characteristic"
    variable), and the second is the corresponding "client characteristic
    configuration" attribute with a UUID of 0x2902. The correct attribute here
    will always be the first 0x2902 attribute after the measurement attribute
    in question. Typically the CCC handle value will be either +1 or +2 from
    the original handle.

6. ble_evt_attclient_procedure_completed
    Raised when an attribute client procedure finishes, which in this script
    means when the "attclient_read_by_group_type" (service search) or the
    "attclient_find_information" (descriptor search) completes. Since both
    processes terminate with this same event, we must keep track of the state
    so we know which one has actually just finished. The completion of the
    service search will (assuming the service is found) trigger the start of
    the descriptor search, and the completion of the descriptor search will
    (assuming the attributes are found) trigger enabling indications on the
    measurement characteristic.

7. ble_evt_attclient_attribute_value
    Raised each time the remote device pushes new data via notifications or
    indications. (Notifications and indications are basically the same, except
    that indications are acknowledged while notifications are not--like TCP vs.
    UDP.) In this script, the remote slave device pushes temperature
    measurements out as indications approximately once per second. These values
    are displayed to the console.

*/

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include <math.h>

// command definitions and UART communication implementation
#include "cmd_def.h"
#include "uart.h"

// uncomment the following line to show outgoing/incoming BGAPI packet data
#define DEBUG

// timeout for serial port read operations
#define UART_TIMEOUT 1000

// list all possible pending actions
enum actions {
    action_none,
    action_scan,
    action_connect,
    action_info,
};
enum actions action = action_none;

// list all possible states
typedef enum {
    state_disconnected,             // start/idle state
    state_connecting,               // connection in progress but not established
    state_connected,                // connection established
    state_finding_services,         // listing services (searching for HTM service)
    state_finding_attributes,       // listing attributes (searching for HTM measurement characteristic)
    state_listening_measurements,   // indications enabled, waiting for updates from sensor
    state_finish,                   // application process complete, will exit immediately
    state_last                      // enum tail placeholder
} states;
states state = state_disconnected;

// friendly names for above list of states
const char *state_names[state_last] = {
    "disconnected",
    "connecting",
    "connected",
    "finding_services",
    "finding_attributes",
    "listening_measurements",
    "finish"
};

// maximum number of devices to report while scanning
#define MAX_DEVICES 64

// count and storage array for devices found while scanning
int found_devices_count = 0;
int autoconnect_device = 0;
bd_addr found_devices[MAX_DEVICES];

// attribute handle search range (16-bit space, this includes all possibilities)
#define FIRST_HANDLE 0x0001
#define LAST_HANDLE  0xffff

// HTM service, measurement, and Client Characteristic Configuration UUIDs
// (these are *not* arbitrary; they are defined by the Bluetooth SIG)
#define THERMOMETER_SERVICE_UUID            0x1809  // https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.health_thermometer.xml
#define THERMOMETER_MEASUREMENT_UUID        0x2a1c  // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.temperature_measurement.xml
#define THERMOMETER_MEASUREMENT_CONFIG_UUID 0x2902  // https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml

// HTM measurement data contains a "flags" byte, which includes a "Fahrenheit" flag in the bit 0 position
#define THERMOMETER_FLAGS_FAHRENHEIT        0x1

// UUID of any "service" entry is 0x2800, used when searching for services
uint8 primary_service_uuid[] = {0x00, 0x28};

// variables used for storing handles for relevant attributes during service/attribute search
uint16 thermometer_handle_start = 0,
       thermometer_handle_end = 0,
       thermometer_handle_measurement = 0,
       thermometer_handle_configuration = 0;

// Bluetooth MAC address used when establishing a connection
// (will be populated from "gap_scan_response" event)
bd_addr connect_addr;

/**
 * Output application syntax
 *
 * @param exe_name Name of executable file
 */
void print_usage(char *exe_name)
{
    printf("Syntax:\n");
    #if defined(PLATFORM_WIN)
        printf("\t%s <COMx | list> [info | scan | auto | {ADDRESS}]\n\n", exe_name);
        printf("Examples:\n");
        printf("\t%s list\n\t\tShow available Bluegiga serial ports (Windows-only)\n", exe_name);
        printf("\t%s COM1 scan\n\t\tShow advertising Health Thermometer BLE devices\n", exe_name);
        printf("\t%s COM1 auto\n\t\tConnect to first available Health Thermometer sensor\n", exe_name);
        printf("\t%s COM1 00:07:80:4f:2a:81\n\t\tConnect to a specific Health Thermometer sensor\n\n", exe_name);
    #elif defined(PLATFORM_OSX)
        printf("\t%s <port> <info | scan | auto | {ADDRESS}>\n\n", exe_name);
        printf("Examples:\n");
        printf("\t%s /dev/tty.usbmodem1 scan\n\t\tShow advertising Health Thermometer BLE devices\n", exe_name);
        printf("\t%s /dev/tty.usbmodem1 auto\n\t\tConnect to first available Health Thermometer sensor\n", exe_name);
        printf("\t%s /dev/tty.usbmodem1 00:07:80:4f:2a:81\n\t\tConnect to a specific Health Thermometer sensor\n\n", exe_name);
    #else
        printf("\t%s <port> <info | scan | auto | {ADDRESS}>\n\n", exe_name);
        printf("Examples:\n");
        printf("\t%s /dev/ttyACM0 scan\n\t\tShow advertising Health Thermometer BLE devices\n", exe_name);
        printf("\t%s /dev/ttyACM0 auto\n\t\tConnect to first available Health Thermometer sensor\n", exe_name);
        printf("\t%s /dev/ttyACM0 00:07:80:4f:2a:81\n\t\tConnect to a specific Health Thermometer sensor\n\n", exe_name);
    #endif
}

/**
 * Change application state
 *
 * @param new_state New state to enter
 */
void change_state(states new_state)
{
    #ifdef DEBUG
        // show current and next state (friendly names, not IDs)
        printf("DEBUG: State changed: %s --> %s\n", state_names[state], state_names[new_state]);
    #endif

    state = new_state;
}

/**
 * Compare Bluetooth addresses
 *
 * @param first First address
 * @param second Second address
 * @return Zero if addresses are equal, 1 otherwise
 */
int cmp_bdaddr(bd_addr first, bd_addr second)
{
    int i;
    for (i = 0; i < sizeof(bd_addr); i++) {
        if (first.addr[i] != second.addr[i]) return 1;
    }
    return 0;
}

/**
 * Display Bluetooth MAC address in hexadecimal notation
 *
 * @param bdaddr Bluetooth MAC address
 */
void print_bdaddr(bd_addr bdaddr)
{
    printf("%02x:%02x:%02x:%02x:%02x:%02x",
            bdaddr.addr[5],
            bdaddr.addr[4],
            bdaddr.addr[3],
            bdaddr.addr[2],
            bdaddr.addr[1],
            bdaddr.addr[0]);
}

/**
 * Display raw BGAPI packet in hexadecimal notation
 *
 * @param data1 Fixed-length portion of BGAPI packet (should always be <len1> bytes long)
 * @param hdr BGAPI packet structure pointer
 * @param data Variable-length packet data payload (may be >0 bytes depending on whether uint8array is present in packet)
 */
void print_raw_packet(struct ble_header *hdr, unsigned char *data, uint8 outgoing)
{
    int i;
    printf(outgoing ? "TX => [ " : "RX <= [ ");
    for (i = 0; i < 4; i++) {   // display first 4 bytes, always present in every packet
        printf("%02X ", ((unsigned char *)hdr)[i]);
    }
    if (hdr -> lolen > 0) {
        printf("| ");           // display remaining data payload
        for (i = 0; i < hdr -> lolen; i++) {
            printf("%02X ", ((unsigned char *)hdr)[i + 4]);
        }
    }
    printf("]\n");
}

/**
 * Send BGAPI packet using UART interface
 *
 * @param len1 Length of fixed portion of packet (always at least 4)
 * @param data1 Fixed-length portion of BGAPI packet (should always be <len1> bytes long)
 * @param len2 Length of variable portion of packet data payload (trailing uint8array or uint16array)
 * @param data2 Variable-length portion of data payload (should always be <len2> bytes long)
 */
void send_api_packet(uint8 len1, uint8* data1, uint16 len2, uint8* data2)
{
    #ifdef DEBUG
        // display outgoing BGAPI packet
        print_raw_packet((struct ble_header *)data1, data2, 1);
    #endif

    // transmit complete packet via UART
    if (uart_tx(len1, data1) || uart_tx(len2, data2)) {
        // uart_tx returns non-zero on failure
        printf("ERROR: Writing to serial port failed\n");
        exit(1);
    }
}

/**
 * Receive BGAPI packet using UART interface
 *
 * @param timeout_ms Milliseconds to wait before timing out on the UART RX operation
 */
int read_api_packet(int timeout_ms)
{
    unsigned char data[256]; // enough for BLE
    struct ble_header hdr;
    int r;

    r = uart_rx(sizeof(hdr), (unsigned char *)&hdr, timeout_ms);
    if (!r) {
        return -1; // timeout
    }
    else if (r < 0) {
        printf("ERROR: Reading header failed. Error code:%d\n", r);
        return 1;
    }

    if (hdr.lolen) {
        r = uart_rx(hdr.lolen, data, timeout_ms);
        if (r <= 0) {
            printf("ERROR: Reading data failed. Error code:%d\n", r);
            return 1;
        }
    }

    // use BGLib function to create correct ble_msg structure based on the header
    // (header contains command class/ID info, used to identify the right structure to apply)
    const struct ble_msg *msg = ble_get_msg_hdr(hdr);

    #ifdef DEBUG
        // display incoming BGAPI packet
        print_raw_packet(&hdr, data, 0);
    #endif

    if (!msg) {
        printf("ERROR: Unknown message received\n");
        exit(1);
    }

    // call the appropriate handler function with any payload data
    // (this is what triggers the ble_evt_* and ble_rsp_* functions)
    msg -> handler(data);

    return 0;
}

/**
 * Enables indications on a specified Client Characteristic Configuration attribute
 * Writing the value 0x0002 (little-endian) enables indications.
 *
 * @param connection_handle Handle for open connection to use (always "0" in this demo)
 * @param client_configuration_handle 16-bit attribute handle of CCC attribute, used for subscribing to indications
 */
void enable_indications(uint8 connection_handle, uint16 client_configuration_handle)
{
    uint8 configuration[] = {0x02, 0x00}; // enable indications
    ble_cmd_attclient_attribute_write(connection_handle, thermometer_handle_configuration, 2, &configuration);
}

/**
 * "system_get_info" response handler
 * Occurs immediately after requesting system information.
 * (see "system_get_info" command in API reference guide)
 *
 * @param msg Event packet data payload
 */
void ble_rsp_system_get_info(const struct ble_msg_system_get_info_rsp_t *msg)
{
    printf("Build: %u, protocol_version: %u, hardware: ", msg->build, msg->protocol_version);
    switch (msg -> hw) {
        case 0x01: printf("BLE112"); break;
        case 0x02: printf("BLE113"); break;
        case 0x03: printf("BLED112"); break;
        default: printf("Unknown");
    }
    printf("\n");

    if (action == action_info) change_state(state_finish);
}

/**
 * "gap_scan_response" event handler
 * Occurs whenever an advertisement packet is detected while scanning
 * (see "gap_discover" command in API reference guide)
 *
 * @param msg Event packet data payload
 */
void ble_evt_gap_scan_response(const struct ble_msg_gap_scan_response_evt_t *msg)
{
    if (found_devices_count >= MAX_DEVICES) change_state(state_finish);

    int i, j;
    char *name = NULL;

    // check if this device already found (only happens in "scan" action)
    for (i = 0; i < found_devices_count; i++) {
        if (!cmp_bdaddr(msg -> sender, found_devices[i])) return;
    }
    
    // continue by parsing advertisement fields (see BT4.0 spec for ad field type data)
    // (we want to make sure we only display devices advertising the HTM service
    int matching_uuid = 0;
    for (i = 0; i < msg -> data.len; ) {
        int8 len = msg -> data.data[i++];
        if (!len) continue;
        if (i + len > msg -> data.len) break; // not enough data, incomplete field
        uint8 type = msg -> data.data[i++];
        switch (type) {
            case 0x01:
                // flags field
                break;

            case 0x02:
                // partial list of 16-bit UUIDs
            case 0x03:
                // complete list of 16-bit UUIDs
                for (j = 0; j < len - 1; j += 2)
                {
                    // loop through UUIDs 2 bytes at a time
                    uint16 test_uuid = msg -> data.data[i + j] + (msg -> data.data[i + j + 1] << 8);
                    if (test_uuid == THERMOMETER_SERVICE_UUID)
                    {
                        // found the thermometer service UUID in the list of advertised UUIDs!
                        matching_uuid = 1;
                    }
                }
                break;

            case 0x04:
                // partial list of 32-bit UUIDs
            case 0x05:
                // complete list of 32-bit UUIDs
                for (j = 0; j < len - 1; j += 4)
                {
                    // loop through UUIDs 4 bytes at a time
                    // TODO: test for desired UUID here, if 32-bit UUID
                }
                break;

            case 0x06:
                // partial list of 128-bit UUIDs
            case 0x07:
                // complete list of 128-bit UUIDs
                for (j = 0; j < len - 1; j += 16)
                {
                    // loop through UUIDs 16 bytes at a time
                    // TODO: test for desired UUID here, if 128-bit UUID
                }
                break;
            case 0x09:
                name = malloc(len);
                memcpy(name, msg->data.data + i, len - 1);
                name[len - 1] = '\0';
        }

        i += len - 1;
    }

    // only continue here if we're interested in the device
    if (!matching_uuid) return;

    memcpy(found_devices[found_devices_count].addr, msg -> sender.addr, sizeof(bd_addr));
    found_devices_count++;

    print_bdaddr(msg -> sender);
    printf(" RSSI=%d", (int8)(msg->rssi));
    printf(" Name=");
    if (name) printf("%s", name);
    else printf("Unknown");
    printf(" AddrType=");
    if (msg -> address_type) printf("RANDOM");
    else printf("PUBLIC");
    printf("\n");

    // automatically connect if in "auto" mode
    if (autoconnect_device) {
        // save this device
        memcpy(connect_addr.addr, msg -> sender.addr, sizeof(bd_addr));

        // send "gap_connect_direct" command
        // arguments:
        //  - MAC address
        //  - use detected address type (will work with either public or private addressing)
        //  - 32 = 32*0.625ms = 20ms minimum connection interval
        //  - 48 = 48*0.625ms = 30ms maximum connection interval
        //  - 100 = 100*10ms = 1000ms supervision timeout
        //  - 0 = no slave latency
        ble_cmd_gap_connect_direct(&connect_addr, msg -> address_type, 32, 48, 100, 0);
    }

    free(name);
}

/**
 * "connection_status" event handler
 * Occurs whenever a new connection is established, or an existing one is updated
 *
 * @param msg Event packet data payload
 */
void ble_evt_connection_status(const struct ble_msg_connection_status_evt_t *msg)
{
    // New connection
    if (msg->flags & connection_connected) {
        change_state(state_connected);
        printf("Connected\n");

        // Handle for Temperature Measurement configuration already known
        if (thermometer_handle_configuration) {
            change_state(state_listening_measurements);
            enable_indications(msg->connection, thermometer_handle_configuration);
        }
        // Find primary services
        else {
            change_state(state_finding_services);
            ble_cmd_attclient_read_by_group_type(msg->connection, FIRST_HANDLE, LAST_HANDLE, 2, primary_service_uuid);
        }
    }
}

/**
 * "attclient_group_found" event handler
 * Occurs whenever a GATT group search has returned a new entry (e.g. service)
 * (see "attclient_read_by_group_type" command in API reference guide)
 *
 * @param msg Event packet data payload
 */
void ble_evt_attclient_group_found(const struct ble_msg_attclient_group_found_evt_t *msg)
{
    if (msg->uuid.len == 0) return;
    uint16 uuid = (msg->uuid.data[1] << 8) | msg->uuid.data[0];

    // First thermometer service found
    if (state == state_finding_services && uuid == THERMOMETER_SERVICE_UUID && thermometer_handle_start == 0) {
        thermometer_handle_start = msg->start;
        thermometer_handle_end = msg->end;
    }
}

/**
 * "attclient_procedure_completed" event handler
 * Occurs whenever a service or attribute search has finished, or a few other
 * kinds of GATT client operations. Tracking state is important here since you
 * can end up in this event handler for many reasons.
 *
 * @param msg Event packet data payload
 */
void ble_evt_attclient_procedure_completed(const struct ble_msg_attclient_procedure_completed_evt_t *msg)
{
    if (state == state_finding_services) {
        // Thermometer service not found
        if (thermometer_handle_start == 0) {
            printf("No Health Thermometer service found\n");
            change_state(state_finish);
        }
        // Find thermometer service attributes
        else {
            change_state(state_finding_attributes);
            ble_cmd_attclient_find_information(msg->connection, thermometer_handle_start, thermometer_handle_end);
        }
    }
    else if (state == state_finding_attributes) {
        // Client characteristic configuration not found
        if (thermometer_handle_configuration == 0) {
            printf("No Client Characteristic Configuration found for Health Thermometer service\n");
            change_state(state_finish);
        }
        // Enable temperature notifications
        else {
            change_state(state_listening_measurements);
            enable_indications(msg->connection, thermometer_handle_configuration);
        }
    }
}

/**
 * "attclient_find_information_found" event handler
 * Occurs whenever an information search has returned a new entry (e.g. attribute)
 * (see "attclient_find_information" in API reference guide)
 *
 * @param msg Event packet data payload
 */
void ble_evt_attclient_find_information_found(const struct ble_msg_attclient_find_information_found_evt_t *msg)
{
    if (msg->uuid.len == 2) {
        uint16 uuid = (msg->uuid.data[1] << 8) | msg->uuid.data[0];

        if (uuid == THERMOMETER_MEASUREMENT_UUID) {
            thermometer_handle_measurement = msg->chrhandle;
        }
        else if (uuid == THERMOMETER_MEASUREMENT_CONFIG_UUID) {
            thermometer_handle_configuration = msg->chrhandle;
        }
    }
}

/**
 * "attclient_attribute_value" event handler
 * Occurs whenever the remote GATT server has pushed a new value to us via notifications or indications
 *
 * @param msg Event packet data payload
 */
void ble_evt_attclient_attribute_value(const struct ble_msg_attclient_attribute_value_evt_t *msg)
{
    if (msg->value.len < 5) {
        printf("Not enough fields in Temperature Measurement value");
        change_state(state_finish);
    }

    uint8 flags = msg->value.data[0];
    int8 exponent = msg->value.data[4];
    int mantissa = (msg->value.data[3] << 16) | (msg->value.data[2] << 8) | msg->value.data[1];

    float value = mantissa * pow(10, exponent);
    if (exponent >= 0)
        exponent = 0;
    else
        exponent = abs(exponent);
    printf("Temperature: %.*f ", exponent, value);

    if (flags & THERMOMETER_FLAGS_FAHRENHEIT)
        printf("F");
    else
        printf("C");
    printf("\n");
}

/**
 * "connection_disconnected" event handler
 * Occurs whenever the BLE connection to the peripheral device has been terminated
 *
 * @param msg Event packet data payload
 */
void ble_evt_connection_disconnected(const struct ble_msg_connection_disconnected_evt_t *msg)
{
    // this *might* occur at the beginning if we are pre-emptively disconnecting during
    // the "soft reset" process, so make sure we don't try to auto-reconnect if that's
    // the case, since it won't work--only reconnect if the state machine thinks we weren't
    // already DISconnected
    if (state != state_disconnected) {
        change_state(state_disconnected);
        printf("Connection terminated, trying to reconnect\n");
        change_state(state_connecting);

        // send "gap_connect_direct" command
        // arguments:
        //  - MAC address
        //  - use public address type (DOESN'T WORK WITH iOS IN PERIPHERAL MODE, OR OTHER RANDOM-ADDRESS DEVICES)
        //  - 32 = 32*0.625ms = 20ms minimum connection interval
        //  - 48 = 48*0.625ms = 30ms maximum connection interval
        //  - 100 = 100*10ms = 1000ms supervision timeout
        //  - 0 = no slave latency
        ble_cmd_gap_connect_direct(&connect_addr, gap_address_type_public, 32, 48, 100, 0);
    }
}

/**
 * Main program entry point
 * Validates and processes arguments, initializes serial port, and begins BLE application
 *
 * @param argc Number of arguments (1+, first is executable name)
 * @param argv Argument string content
 */
int main(int argc, char *argv[])
{
    char *uart_port;
    
    // check to make sure we have at least one argument
    if (argc <= 1)
    {
        // not enough command line arguments, so display correct usage and then exit
        print_usage(argv[0]);
        return 1;
    }

    // check for the first argument (either "list" or serial port)
    if (strcmp(argv[1], "list") == 0)
    {
        // "list" given, to show available serial port entries
        uart_list_devices();
        return 1;
    }
    else
    {
        // something else given, so treat it as a serial port name (e.g. "COM4" or "/dev/ttyACM0")
        uart_port = argv[1];
    }

    // process "action" argument
    if (argc > 2)
    {
        int i;
        for (i = 0; i < strlen(argv[2]); i++) {
            argv[2][i] = tolower(argv[2][i]);
        }

        // check for the specific action passed as 2nd argument
        if (strcmp(argv[2], "scan") == 0)
        {
            // user wants to scan for BLE peripherals
            action = action_scan;
        }
        else if (strcmp(argv[2], "info") == 0)
        {
            // user wants to display stack version info
            action = action_info;
        }
        else if (strcmp(argv[2], "auto") == 0)
        {
            // user wants to automatically connect to first available peripheral
            action = action_scan;
            autoconnect_device = 1;
        }
        else
        {
            // user wants to connect to a specific BLE peripheral
            int i;
            short unsigned int addr[6];
            if (sscanf(argv[2],
                    "%02hx:%02hx:%02hx:%02hx:%02hx:%02hx",
                    &addr[5],
                    &addr[4],
                    &addr[3],
                    &addr[2],
                    &addr[1],
                    &addr[0]) == 6) {

                for (i = 0; i < 6; i++) {
                    connect_addr.addr[i] = addr[i];
                }
                action = action_connect;
            }
        }
    }
    
    // check again for proper usage (possibly a valid 1st argument but invalid 2nd argument)
    if (action == action_none) {
        // display correct usage and then exit
        print_usage(argv[0]);
        return 1;
    }

    // set BGLib output function pointer to "send_api_packet" function
    bglib_output = send_api_packet;

    // open the serial port
    if (uart_open(uart_port)) {
        printf("ERROR: Unable to open serial port\n");
        return 1;
    }

    #if 1 // very soft "reset"
        // close current connection, stop scanning, stop advertising
        // (returns BLE device to a known state without a hard reset)
        ble_cmd_connection_disconnect(0);
        ble_cmd_gap_set_mode(0, 0);
        ble_cmd_gap_end_procedure();

    #else // full reset
        // reset BLE device to get it into known state
        ble_cmd_system_reset(0);
        
        // close the serial port, since reset causes USB to re-enumerate
        uart_close();
        
        // wait until USB re-enumerates and we can re-open the port
        // (not necessary if using a direct UART connection)
        do {
            usleep(500000); // 0.5s
        } while (uart_open(uart_port));
    #endif

    // execute requested action (info, scan, or connect)
    if (action == action_info)
    {
        // send "system_get_info" command, then exit
        ble_cmd_system_get_info();
    }
    else if (action == action_scan)
    {
        // send "gap_discover" command, exit if MAX_DEVICES peripherals are found
        ble_cmd_gap_discover(gap_discover_generic);
    }
    else if (action == action_connect)
    {
        // change application state
        change_state(state_connecting);

        // send "gap_connect_direct" command
        // arguments:
        //  - MAC address
        //  - use public address type (DOESN'T WORK WITH iOS IN PERIPHERAL MODE, OR OTHER RANDOM-ADDRESS DEVICES)
        //  - 32 = 32*0.625ms = 20ms minimum connection interval
        //  - 48 = 48*0.625ms = 30ms maximum connection interval
        //  - 100 = 100*10ms = 1000ms supervision timeout
        //  - 0 = no slave latency
        ble_cmd_gap_connect_direct(&connect_addr, gap_address_type_public, 32, 48, 100, 0);
    }

    // infinite loop which reads incoming UART data
    // (event handlers are triggered from within the "read_api_packet" function)
    while (state != state_finish)
    {
        // read forever until/unless we encouter a problem, or enter the "finish" state
        if (read_api_packet(UART_TIMEOUT) > 0) break;
    }

    // close the serial port
    uart_close();

    // exit with no error
    return 0;
}
