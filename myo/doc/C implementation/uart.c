// Bluegiga ANSI C BGLib UART interface source file
// BLE SDK v1.3.2-122
//
// http://www.bluegiga.com
//
// This is free software distributed under the terms of the MIT license reproduced below.
//
// Copyright (c) 2015 Bluegiga Technologies
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

#include <stdio.h>
#include "uart.h"

#ifdef PLATFORM_WIN
    #ifdef _MSC_VER
        #define snprintf _snprintf
    #endif

// Windows implementation of UART access
// see https://msdn.microsoft.com/en-us/library/aa363194(v=vs.85).aspx for reference

#include <windows.h>
#include <setupapi.h>

HANDLE serial_handle;
DCB dcb;

// Windows-only serial port device listing
void uart_list_devices() {
    char name[] = "Bluegiga Bluetooth Low Energy";

    BYTE* pbuf = NULL;
    DWORD reqSize = 0;
    DWORD n = 0;
    HDEVINFO hDevInfo;

    //guid for ports
    static const GUID guid = { 0x4d36e978, 0xe325, 0x11ce, { 0xbf, 0xc1, 0x08, 0x00, 0x2b, 0xe1, 0x03, 0x18 } };

    char *str;
    char tmp[MAX_PATH + 1];
    int i;
    SP_DEVINFO_DATA DeviceInfoData;

    snprintf(tmp, MAX_PATH, "%s (COM%%d)", name);

    DeviceInfoData.cbSize=sizeof(SP_DEVINFO_DATA);
    hDevInfo = SetupDiGetClassDevs(&guid,   //Retrieve all ports
                                      0L,
                                     NULL, DIGCF_PRESENT);
    if(hDevInfo==INVALID_HANDLE_VALUE)
        return;

    while (1) {
        if (!SetupDiEnumDeviceInfo(hDevInfo, n++, &DeviceInfoData))
        {
            SetupDiDestroyDeviceInfoList(hDevInfo);
            return;
        }
        reqSize = 0;
        SetupDiGetDeviceRegistryPropertyA(hDevInfo, &DeviceInfoData, SPDRP_FRIENDLYNAME, NULL, NULL, 0, &reqSize);
        pbuf = (BYTE*)malloc(reqSize > 1 ? reqSize : 1);
        if (!SetupDiGetDeviceRegistryPropertyA(hDevInfo, &DeviceInfoData, SPDRP_FRIENDLYNAME, NULL, pbuf, reqSize, NULL))
        {
            free(pbuf);
            continue;
        }
        str = (char*)pbuf;
        if(sscanf(str, tmp, &i) == 1)
        {
            printf("%s\n", str);
            //emit DeviceFound(str,QString("\\\\.\\COM%1").arg(i));
        }
        free(pbuf);
    }
    return;
}

int uart_find_serialport(char *name) {
    BYTE* pbuf = NULL;
    DWORD reqSize = 0;
    DWORD n = 0;
    HDEVINFO hDevInfo;
    // GUID for ports
    static const GUID guid = { 0x4d36e978, 0xe325, 0x11ce, { 0xbf, 0xc1, 0x08, 0x00, 0x2b, 0xe1, 0x03, 0x18 } };
    char *str;
    char tmp[MAX_PATH+1];
    int i;
    SP_DEVINFO_DATA DeviceInfoData;

    snprintf(tmp, MAX_PATH, "%s (COM%%d)", name);

    DeviceInfoData.cbSize = sizeof(SP_DEVINFO_DATA);
    hDevInfo = SetupDiGetClassDevs(&guid, 0L, NULL, DIGCF_PRESENT);
    if(hDevInfo == INVALID_HANDLE_VALUE)
        return -1;
    
    while(1) {
        if (!SetupDiEnumDeviceInfo(hDevInfo, n++, &DeviceInfoData))
        {
            SetupDiDestroyDeviceInfoList(hDevInfo);
            return -1;
        }
        reqSize = 0;
        SetupDiGetDeviceRegistryPropertyA(hDevInfo, &DeviceInfoData, SPDRP_FRIENDLYNAME, NULL, NULL, 0, &reqSize);
        pbuf = malloc(reqSize > 1 ? reqSize : 1);
        if (!SetupDiGetDeviceRegistryPropertyA(hDevInfo, &DeviceInfoData, SPDRP_FRIENDLYNAME, NULL, pbuf, reqSize, NULL))
        {
            free(pbuf);
            continue;
        }
        str = (char*)pbuf;
        if (sscanf(str, tmp, &i) == 1)
        {
            free(pbuf);
            SetupDiDestroyDeviceInfoList(hDevInfo);
            return i;
        }
        free(pbuf);
    }
    return -1;
}

int uart_open(char *port) {
    char str[20];

    snprintf(str, sizeof(str) - 1, "\\\\.\\%s", port);
    serial_handle = CreateFileA(str,
        GENERIC_READ | GENERIC_WRITE,
        FILE_SHARE_READ | FILE_SHARE_WRITE,
        NULL,
        OPEN_EXISTING,
        0, //FILE_FLAG_OVERLAPPED,
        NULL);

    if (serial_handle == INVALID_HANDLE_VALUE) {
        return -1;
    }

    SecureZeroMemory(&dcb, sizeof(DCB));
    dcb.DCBlength = sizeof(DCB);
    //  115200 bps, 8 data bits, no parity, and 1 stop bit
    dcb.BaudRate = CBR_115200;      // baud rate
    dcb.ByteSize = 8;               // data size, xmit and rcv
    dcb.Parity   = NOPARITY;        // parity bit
    dcb.StopBits = ONESTOPBIT;      // stop bit
    dcb.fOutxCtsFlow = 1;           // CTS flow control monitoring
    dcb.fRtsControl = 1;            // RTS flow control output
    
    if (!SetCommState(serial_handle, &dcb)) {
        return -2;
    }

    return 0;
}

void uart_close() {
    CloseHandle(serial_handle);
}

int uart_tx(int len, unsigned char *data) {
    DWORD r, written;
    while (len) {
        r = WriteFile(serial_handle, data, len, &written, NULL);
        if (!r) {
            return -1;
        }
        len -= written;
        data += written;
    }
    return 0;
}

int uart_rx(int len, unsigned char *data, int timeout_ms) {
    int l = len;
    DWORD r, rread;
    COMMTIMEOUTS timeouts;
    
    timeouts.ReadIntervalTimeout = MAXDWORD;
    timeouts.ReadTotalTimeoutMultiplier = 0;
    timeouts.ReadTotalTimeoutConstant = timeout_ms;
    timeouts.WriteTotalTimeoutMultiplier = 0;
    timeouts.WriteTotalTimeoutConstant = 0;
    SetCommTimeouts(serial_handle, &timeouts);
    
    while (len) {
        r = ReadFile(serial_handle, data, len, &rread, NULL);
        if (!r) {
            l = GetLastError();
            if (l == ERROR_SUCCESS) {
                return 0;
            }
            return -1;
        } else {
            if (rread == 0) {
                return 0;
            }
        }
        len -= rread;
        data += rread;
    }

    return l;
}

#else // POSIX or Mac OS X

#include <stdio.h>
#include <termios.h>
#include <fcntl.h>
#include <unistd.h>

int serial_handle;

int uart_open(char *port) {
    struct termios options;
    int i;

    #ifdef PLATFORM_OSX
        serial_handle = open(port, (O_RDWR | O_NOCTTY | O_NDELAY));
    #else
        serial_handle = open(port, (O_RDWR | O_NOCTTY /*| O_NDELAY*/));
    #endif

    if (serial_handle < 0) {
        return -1;
    }

    /*
     * Get the current options for the port...
     */
    tcgetattr(serial_handle, &options);

    /*
     * Set the baud rates to 115200...
     */
    cfsetispeed(&options, B115200);
    cfsetospeed(&options, B115200);

    /*
     * Enable the receiver and set parameters ...
     */
    options.c_cflag &= ~(PARENB | CSTOPB | CSIZE | CRTSCTS | HUPCL);
    options.c_cflag |= (CS8 | CLOCAL | CREAD);
    options.c_lflag &= ~(ICANON | ISIG | ECHO | ECHOE | ECHOK | ECHONL | ECHOCTL | ECHOPRT | ECHOKE | IEXTEN);
    options.c_iflag &= ~(INPCK | IXON | IXOFF | IXANY | ICRNL);
    options.c_oflag &= ~(OPOST | ONLCR);

    //printf( "size of c_cc = %d\n", sizeof( options.c_cc ) );
    for (i = 0; i < sizeof(options.c_cc); i++)
        options.c_cc[i] = _POSIX_VDISABLE;

    options.c_cc[VTIME] = 0;
    options.c_cc[VMIN] = 1;

    /*
     * Set the new options for the port...
     */
    tcsetattr(serial_handle, TCSAFLUSH, &options);

    return 0;
}

void uart_close() {
    close(serial_handle);
}

int uart_tx(int len, unsigned char *data) {
    ssize_t written;

    while (len) {
        written = write(serial_handle, data, len);
        if (written < 1) {
            return -1;
        }
        len -= written;
        data += written;
    }

    return 0;
}

int uart_rx(int len, unsigned char *data, int timeout_ms) {
    int l = len;
    ssize_t rread;
    struct termios options;

    tcgetattr(serial_handle, &options);
    options.c_cc[VTIME] = timeout_ms / 100;
    options.c_cc[VMIN] = 0;
    tcsetattr(serial_handle, TCSANOW, &options);

    while (len) {
        rread = read(serial_handle, data, len);

        if (!rread) {
            return 0;
        } else if (rread < 0) {
            return -1;
        }
        len -= rread;
        data += rread;
    }

    return l;
}

#endif
