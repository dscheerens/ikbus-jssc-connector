package net.novazero.lib.ikbus.io;

import net.novazero.lib.ikbus.IKBusPacket;
import net.novazero.lib.ikbus.io.IKBusConnection;
import net.novazero.lib.ikbus.io.IKBusIOException;
import net.novazero.lib.ikbus.io.IKBusPacketReader;
import net.novazero.lib.ikbus.io.IKBusPacketWriter;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Class that provides an I/K-bus communication link via a serial port.
 */
public class SerialPortIKBusConnection implements IKBusConnection {
	
	/** The serial port which is used for communication with the I/K-bus. */
	private final SerialPort serialPort;
	
	/** The I/K-bus packet reader for the serial port connection. */
	private final SerialPortIKBusPacketReader reader;
	
	/** Indicates whether the I/K-bus communication link has been closed. */
	private boolean closed = false;
	
	/**
	 * Creates a new I/K-bus connection that uses the serial port with the specified name to establish a communication link. When the
	 * constructor is invoked the port will be opened immediately using the correct serial communication parameters for the I/K-bus
	 * protocol. Hardware flow control will also be enabled automatically using the RS-232 RTS and CTS pins.
	 * 
	 * After successful instantiation of a SerialPortIKBusConnection a communication link is opened immediately, so there is no option to
	 * explicitly open the connection. Via the {@link #close()} method the communication link can be closed. 
	 * 
	 * The names of the serial ports that are available on the system can be retrieved via the {@link #getAvailableSerialPorts()} method.
	 * Note that the port names vary per operation system.
	 * 
	 * @param   portName          System name of the serial port for which an I/K-bus communication link is to be opened.
	 * @throws  IKBusIOException  If an I/O error occurs while trying to open the serial port.
	 */
	public SerialPortIKBusConnection(String portName) throws IKBusIOException {
		serialPort = new SerialPort(portName);
		
		try {
			// Open the port.
			serialPort.openPort();
			
			// Set the serial communication parameters for the I/K-bus.
			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
			
			// Set hardware flow control mode (RTS/CTS).
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
			
			// Create packet reader for the serial port.
			reader = new SerialPortIKBusPacketReader(serialPort);
			
		} catch (SerialPortException e) {
			throw new IKBusIOException("Failed to open port: \"" + portName + "\"", e);
		}
	}
	
	/**
	 * Retrieves the name of the serial port which is used for the I/K-bus communication link.
	 * 
	 * @return  The name of the serial port which is used for the I/K-bus communication link.
	 */
	public String getPortName() {
		return serialPort.getPortName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IKBusPacketReader getPacketReader() throws IKBusIOException {
		if (closed) {
			throw new IKBusIOException("Cannot obtain reader for a closed connection");
		} else {
			return reader;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IKBusPacketWriter getPacketWriter() throws IKBusIOException {
		if (closed) {
			throw new IKBusIOException("Cannot obtain writer for a closed connection");
		} else {
			return new SerialPortIKBusPacketWriter(serialPort);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IKBusIOException {
		if (!closed) {
			closed = true;
			
			// Attempt to close the reader, (temporarily) ignoring any exceptions it might throw.
			IKBusIOException readerCloseException = null;
			try {
				reader.close();
			} catch (IKBusIOException e) {
				readerCloseException = e;
			}
			
			// Close the serial port.
			try {
				serialPort.closePort();
			} catch (SerialPortException e) {
				throw new IKBusIOException("Failed to close port: \"" + getPortName() + "\"", e);
			}
			
			// If an exception was thrown when closing the reader, rethrow it now (after the port has been closed).
			if (readerCloseException != null) {
				throw readerCloseException;
			}
		}
		
	}

	/**
	 * Retrieves the names of the serial ports that are available on the system. When no serial ports are available on the system an empty
	 * array will be returned.
	 * 
	 * @return  An array containing the names of the serial ports that are available.
	 */
	public static String[] getAvailableSerialPorts() {
		return SerialPortList.getPortNames();
	}
	
	/**
	 * An I/K-bus packet writer that writes the packet data to a serial port.
	 */
	private static class SerialPortIKBusPacketWriter implements IKBusPacketWriter {
		
		/** Serial port to which the I/K-bus packets should be written. */
		private SerialPort serialPort;
		
		/** Whether the writer has been closed. */
		private boolean closed = false;
		
		/**
		 * Creates a new {@link SerialPortIKBusPacketWriter} that writes the packets to the specified serial port.
		 * 
		 * @param  serialPort  Serial port to which the I/K-bus packets should be written.
		 */
		private SerialPortIKBusPacketWriter(SerialPort serialPort) {
			this.serialPort = serialPort;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void write(IKBusPacket packet) throws IKBusIOException {
			if (closed) {
				throw new IKBusIOException("Cannot write I/K-bus packet to closed writer");
			}
			
			try {
				serialPort.writeBytes(packet.toRaw());
			} catch (SerialPortException e) {
				throw new IKBusIOException("Failed to write packet", e);
			}
			
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() {
			if (!closed) {
				closed = true;
				serialPort = null;
			}
		}
		
	}
	
	/**
	 * An I/K-bus packet reader that reads the packet data from a serial port.
	 */
	private static class SerialPortIKBusPacketReader implements IKBusPacketReader {
		
		/** Internal I/K-bus packet reader that wraps a {@link SerialPortInputStream} from which the packets are read. */
		private final InputStreamIKBusPacketReader reader;
		
		/** Indicates whether the packet reader is closed. */
		private boolean closed = false;
		
		/**
		 * Creates a new {@link SerialPortIKBusPacketReader} that reads I/K-bus packets from the specified serial port. The serial port is
		 * expected to be opened and configured for the correct serial communication parameters to interface with the I/K-bus.
		 * 
		 * @param   serialPort        Serial port from which the I/K-bus packets should be read.
		 * @throws  IKBusIOException  If there was an error while setting up the reader.
		 */
		public SerialPortIKBusPacketReader(SerialPort serialPort) throws IKBusIOException {
			try {
				reader = new InputStreamIKBusPacketReader(new SerialPortInputStream(serialPort));
			} catch (SerialPortException e) {
				throw new IKBusIOException("Failed to create input stream for serial port \"" + serialPort.getPortName() + "\"", e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IKBusPacketStreamElement read() throws IKBusIOException {
			return closed ? null : reader.read();
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws IKBusIOException {
			if (!closed) {
				closed = true;
				reader.close();
			}
			
		}
		
	}
	
}
