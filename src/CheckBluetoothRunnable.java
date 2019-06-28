import javax.bluetooth.*;

/**
 * Implementation of Runnable which checks if the Local Device's Bluetooth is on
 * and also checks which state PC Server is currently in. Program will exit if
 * Bluetooth is turned off during execution. Program will listen for a new
 * connection if current connection terminates or reaches timeout period.
 */
public class CheckBluetoothRunnable implements Runnable {

	private PCServer server; // Instance of PC Server.

	/*
	 * Executor Service and storing the results for timeout task. TODO: Implement
	 * timeout period for connected state. private ExecutorService connectedTimeout;
	 * private Future<String> connectedFuture;
	 */

	/**
	 * Sets private parameter to passed-in PCServer object.
	 * 
	 * @param pc
	 *            Current instance of PC Server.
	 */
	public CheckBluetoothRunnable(PCServer pc) {

		server = pc;

	}

	/**
	 * Always checks if Local Device's Bluetooth is on, forcing program to exit if
	 * Bluetooth is turned off during execution. Always checks current state of PC
	 * Server and listens for a new connection if current connection terminates or
	 * reaches timeout period.
	 */
	public void run() {

		while (true) {

			if (!LocalDevice.isPowerOn()) {
				System.out.println("Bluetooth is off, turn on and re-execute program");
				System.exit(0);
			}
			if (server.isTerminated()) {
				server.newCurrentState(-100); // Set current state to disconnected.
			}
			if (server.isTimedOut()) {
				server.newCurrentState(-1); // Set current state to terminated.
			}
			
		}

	}

}