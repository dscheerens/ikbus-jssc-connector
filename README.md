I/K-bus jSSC connector
======================

A package that enables I/K-bus communication links for serial ports by providing an [`IKBusConnection`](https://dscheerens.github.io/docs/ikbus-core/latest/javadoc/index.html?net/novazero/lib/ikbus/io/IKBusConnection.html) implementation using [jSSC (Java Simple Serial Connector)](https://github.com/scream3r/java-simple-serial-connector).
With this package it is possible to read and write I/K-bus packets from/to your vehicle using an I/K-bus interface device, for example [Rolf Resler's IBUS interface](http://www.reslers.de/IBUS/).

This package is to be used in conjunction with the [`ibus-core`](https://github.com/dscheerens/ikbus-core) package.

Usage
-----

_** NOTE: This package is currently not yet available on Maven. I will fix this a.s.a.p. **_

To use this package in your application add the following Maven dependency:
```xml
<dependency>
	<groupId>net.novazero.ikbus</groupId>
	<artifactId>ikbus-jssc-connector</artifactId>
	<version>1.0.0</version>
</dependency>
```

Reading I/K-bus packets
-----------------------

```java
final String serialPortName = "ttyS0"; // For windows use something like "COM1".

try (IKBusConnection connection = new SerialPortIKBusConnection(serialPortName)) {
	
	IKBusPacketReader reader = connection.getPacketReader();
	
	IKBusPacketStreamElement packetStreamElement;
	while ((packetStreamElement = reader.read()) != null) {
		
		if (packetStreamElement.isValidPacket()) {
			System.out.println("[OK] " + packetStreamElement.getPacket());
		} else {
			System.out.println("[ERR] " + IKBusUtils.bytesToHex(packetStreamElement.getData()));
		}
		
	}
	
} catch (IKBusIOException e) {
	System.err.println("Failed to read from " + serialPortName + " due to: " + e);
}
```

Note that you can use the `SerialPortIKBusConnection.getAvailableSerialPorts()` method to find the names of the serial ports which are available on your system.

Writing I/K-bus packets
-----------------------

```java
IKBusPacket packet = new IKBusPacket((byte)0x50, (byte)0x68, new byte[]{(byte)0x32, (byte)0x11});

try (IKBusConnection connection = new SerialPortIKBusConnection(serialPortName)) {
	
	IKBusPacketWriter writer = connection.getPacketWriter();
	
	writer.write(packet);
	
	// Give the interface some time to send/flush the packet.
	try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		throw new RuntimeException(e);
	}
	
} catch (IKBusIOException e) {
	System.err.println("Failed to write to " + serialPortName + " due to: " + e);
}
```