import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.bluetooth.*;
import javax.microedition.io.*;

import cloudtoothpackage.CloudToothMessage;

/**
 * Listens for connection from Android Server and communicates with it.
 */
public class PCServer {

	// For error logging within PCServer.
	private static Logger logger = Logger.getLogger("PCServerLog");
	private static FileHandler fh;
	private static SimpleFormatter sf;

	// Possible states of PC Server.
	private int currentState;
	private static int DISCONNECTED_STATE = -100;
	private static int CONNECTED_STATE = 100;
	private static int SENDING_FILE = 200;
	private static int TERMINATED = -1;
	private static int TIME_OUT = -2;

	// Protocols to be received.
	private static String SEND_FILE_NAMES = "ls";
	private static String EXIT_CONNECTION = "exit";

	// Values retrieved from encrypted files.
	private File TLD;
	private String UDA;

	// Streams used for input and output.
	private InputStream inputStream;
	private BufferedReader bReader;
	private OutputStream outputStream;
	private ObjectOutputStream objectOutputStream;
	private PrintWriter pWriter;

	// Queue of files to be sent.
	private Queue<File> filesToSend = new LinkedList<File>();
	private FileInputStream fis;

	/**
	 * PC Server default constructor for testing, skipping the registration process.
	 */
	public PCServer() {

	}

	/**
	 * PC Server constructor which needs TLD and UDA to be made.
	 * 
	 * @param TLD_PATH
	 *            String storing path to TLD file.
	 * @param MAC_ADDRESS
	 *            String storing path to UDA file.
	 */
	public PCServer(String TLD_PATH, String MAC_ADDRESS) {

		TLD = new File(TLD_PATH);
		UDA = MAC_ADDRESS;

		currentState = TERMINATED; // Initial state.

	}

	/**
	 * Change current state of PC Server to passed-in parameter.
	 * 
	 * @param state
	 *            Int storing new state of PC Server.
	 */
	public void newCurrentState(int state) {

		if (state == -100 || state == 100 || state == 200 || state == -1 || state == -2)
			currentState = state;

	}

	/**
	 * Check if the current state of the PC Server object is disconnected.
	 * 
	 * @return Boolean True if disconnected; False if not.
	 */
	public boolean isDisconnected() {
		if (currentState == DISCONNECTED_STATE)
			return true;
		return false;
	}

	/**
	 * Check if the current state of the PC Server object is connected.
	 * 
	 * @return Boolean True if connected; False if not.
	 */
	public boolean isConnected() {
		if (currentState == CONNECTED_STATE)
			return true;
		return false;
	}

	/**
	 * Check if the current state of the PC Server object is sending file.
	 * 
	 * @return Boolean True if sending file; False if not.
	 */
	public boolean isSendingFile() {
		if (currentState == SENDING_FILE)
			return true;
		return false;
	}

	/**
	 * Check if the current state of the PC Server object is terminated.
	 * 
	 * @return Boolean True if terminated; False if not.
	 */
	public boolean isTerminated() {
		if (currentState == TERMINATED)
			return true;
		return false;
	}

	/**
	 * Check if the current state of the PC Server object is timed out.
	 * 
	 * @return Boolean True if timed out; False if not.
	 */
	public boolean isTimedOut() {
		if (currentState == TIME_OUT)
			return true;
		return false;
	}

	/**
	 * Getter method for private field filesToSend.
	 * 
	 * @return Queue<File> storing all files to be sent.
	 */
	public Queue<File> getFilesToSend() {

		return filesToSend;

	}

	/**
	 * Getter method for private field outputStream.
	 * 
	 * @return OutputStream storing output stream built on current connection.
	 */
	public OutputStream getOutputStream() {

		return outputStream;

	}

	/**
	 * Get all files underneath TLD.
	 * 
	 * @param root
	 *            File storing file asked for.
	 * @return List<String> storing all files underneath TLD.
	 */
	public ArrayList<String> getAllFileNames(File root) {

		Queue<File> myQueue = new LinkedList<File>();
		myQueue.add(root);

		ArrayList<String> finalList = new ArrayList<String>();

		// If the current file is a directory, add it to the queue to address it later.
		// If the current file is a file, add it to the master list to send back to the
		// user.
		while (!myQueue.isEmpty()) {
			File cur = myQueue.remove();
			if (cur.isFile()) {
				finalList.add("\n" + cur.getAbsolutePath());
			}
			if (cur.isDirectory()) {
				File[] arr = cur.listFiles();
				for (int i = 0; i < arr.length; i++) {
					myQueue.add(arr[i]);
				}
			}
		}

		return finalList;

	}

	/**
	 * Makes sure our file is within the hierarchy of TLD.
	 * 
	 * @param input
	 *            String storing specified path.
	 * @return True if within hierarchy of TLD; False if not.
	 */
	public boolean isFileWithinTLD(String input) {

		String pathToTLD = TLD.getAbsolutePath();

		// Return True if file exists and if file path starts with TLD's path.
		File f = new File(input);
		if (f.exists() && input.startsWith(pathToTLD))
			return true;

		return false; // Otherwise, return False.

	}

	/**
	 * Checks if we can send the file, determined by how many requests are already
	 * queued.
	 * 
	 * @return True if we can send file; False if not.
	 */
	public boolean canWeSendFile() {

		if (filesToSend.size() < 5)
			return true;

		return false;

	}

	/**
	 * Adds passed-in file to queue of files to be sent.
	 * 
	 * @param f
	 *            File to be added to queue of files to be sent.
	 */
	public void addFileToQueue(File f) {

		// If we cannot send the file, notify the user that the queue is full and to try
		// again soon.
		if (!canWeSendFile()) {
			String cannotSendFile = "Queue is full. Please try again soon.";
			CloudToothMessage message = new CloudToothMessage(CloudToothMessage.TEXT, cannotSendFile);
			try {
				objectOutputStream.writeObject(message);
				objectOutputStream.flush();
			} catch (IOException e) {
				System.out.println("Error sending unrecognized protocol: " + e);
				logger.info("Error sending unrecognized protocol: " + e);
			}
		}

		filesToSend.add(f); // Add file to queue.

	}

	/**
	 * PC Server responds to protocol input.
	 * 
	 * @param input
	 *            String storing user input.
	 */
	public void respond(String input) {

		if (input == null || input.equals("") || input.equals("\n"))
			return;

		// Appropriately close program.
		if (input.equals(EXIT_CONNECTION)) {
			System.out.println("Changing state to terminated...");
			currentState = TERMINATED;
			return;
		}

		// Send list of all files underneath TLD hierarchy.
		if (input.equals(SEND_FILE_NAMES)) {
			System.out.println("Sending a list of files...");
			ArrayList<String> fileNames = getAllFileNames(TLD);
			CloudToothMessage message = new CloudToothMessage(CloudToothMessage.FILE_LISTING, fileNames);
			try {
				objectOutputStream.writeObject(message);
				objectOutputStream.flush();
			} catch (IOException e) {
				System.out.println("Error writing CloudToothMessage to object output stream: " + e);
				logger.info("Error writing CloudToothMessage to object output stream: " + e);
			}
		} else { // Add the file to the queue of files to be sent.
			System.out.println(input);
			File f = new File(input);
			File f1 = new File("Files\\KEY_FILE.txt");
			File f2 = new File("Files\\ENCRYPTED_TLD.txt");
			File f3 = new File("Files\\\\ENCRYPTED_UDA.txt");
			// If f is a file and is within the TLD hierarchy, send it, unless it is File
			// f1, f2, or f3.
			if (f.exists() && isFileWithinTLD(input) && !f.getName().equals(f1.getAbsolutePath())
					&& !f.getName().equals(f2.getAbsolutePath()) && !f.getName().equals(f3.getAbsolutePath())) {
				System.out.println("File request recognized");
				try {
					fis = new FileInputStream(f);
					int chunk_size = 4096;
					byte[] chunk = new byte[chunk_size]; // 2 KB
					// send first chunk
					int bytesRead = fis.read(chunk);
					if (bytesRead == -1)
						return;
					String fileName = input;
					long fileSize = f.length();

					byte[] allBytes = Files.readAllBytes(f.toPath());

					CloudToothMessage fileMessage = new CloudToothMessage(CloudToothMessage.FILE_BEGIN, fileName,
							allBytes, fileSize);
					objectOutputStream.writeObject(fileMessage);
					objectOutputStream.flush();

					// send end of file message
					fileMessage = new CloudToothMessage(CloudToothMessage.FILE_END, fileName, new byte[1], fileSize);
					objectOutputStream.writeObject(fileMessage);
					objectOutputStream.flush();
				} catch (IOException e) {
					System.out.println("Error getting file bytes: " + e);
					logger.info("Error getting file bytes: " + e);
				} catch (Exception e) {
					System.out.println("Error sending file bytes: " + e);
					logger.info("Error sending file bytes: " + e);
				}
				System.out.println("Changing state to connected state...");
				currentState = CONNECTED_STATE; // Update current state.
			} else { // Else, send "unrecognized protocol."
				byte[] unrecognizedBytes = new String("Unrecognized protocol").getBytes();
				try {
					outputStream.write(unrecognizedBytes);
					outputStream.flush();
				} catch (IOException e) {
					System.out.println(
							"Error sending unrecognized protocol : " + e + "\nChanging state to connected state...");
					logger.info("Error sending unrecognized protocol: " + e);
					currentState = CONNECTED_STATE; // Update current state.
				}
			}
		}

	}

	public byte[] copyByteArray(byte[] chunk, int bytesRead) {
		byte[] copy = new byte[bytesRead];
		for (int i = 0; i < bytesRead; i++) {
			copy[i] = chunk[i];
		}
		return copy;
	}

	/**
	 * Starts PC Server object.
	 */
	public void begin() {

		// Set up error logging.
		try {
			fh = new FileHandler("Logs\\PCServer.log");
			logger.addHandler(fh);
			logger.setUseParentHandlers(false);
			sf = new SimpleFormatter();
			fh.setFormatter(sf);
			logger.info("PCServer log started.");
		} catch (IOException e) {
			System.out.println("Error starting error logging: " + e);
		}

		UUID uuid = new UUID("1101", true); // Create a UUID for PC Server.
		String connectionString = "btspp://localhost:" + uuid + ";name=PC Server"; // Create the service URL.
		StreamConnectionNotifier streamNotifier = null;
		StreamConnection connection = null;
		try {
			// Open server URL.
			streamNotifier = (StreamConnectionNotifier) Connector.open(connectionString);
			// Wait for connection from Android Server.
			System.out.println("PC Server started. Waiting for Android Server to connect...");
			connection = streamNotifier.acceptAndOpen();
		} catch (IOException e) {
			System.out.println("Error opening connection: " + e);
			logger.info("Error opening connection: " + e);
		}

		// Make sure current connected device is the one we expected. If not, terminate
		// connection.
		String currentUDA = "";
		try {
			currentUDA = RemoteDevice.getRemoteDevice(connection).getBluetoothAddress();
		} catch (IOException e) {
			System.out.println("Error getting UDA: " + e);
			logger.info("Error getting UDA: " + e);
		}
		if (!currentUDA.equals(UDA)) {
			System.out.println("Unrecognized device connected; changing state to terminated state...");
			currentState = TERMINATED;
		}

		// Store information of connected Android Server.
		RemoteDevice androidServer = null;
		try {
			androidServer = RemoteDevice.getRemoteDevice(connection);
		} catch (IOException e) {
			System.out.println("Error creating local AndroidServer remote device: " + e);
			logger.info("Error creating local AndroidServer remote device: " + e);
		}
		System.out.println("Changing state to connected state...");
		currentState = CONNECTED_STATE; // Update current state of PC Server.

		// Print out Android Server's Bluetooth Address and Device Name.
		System.out.println("Android Server Bluetooth Address: " + androidServer.getBluetoothAddress());

		// Set output stream and writer.
		try {
			outputStream = connection.openOutputStream();
			pWriter = new PrintWriter(new OutputStreamWriter(outputStream));
			objectOutputStream = new ObjectOutputStream(outputStream);
		} catch (IOException e) {
			System.out.println("Error readying object output stream: " + e);
			logger.info("Error readying object output stream: " + e);
		}

		// Send initial hello.
		String message = "Send back 'ls' for a listing of all files. Send back the file name to receive the file. Send back 'exit' to end connection\n";
		CloudToothMessage helloMessage = new CloudToothMessage(CloudToothMessage.TEXT, message);
		try {
			objectOutputStream.writeObject(helloMessage);
			objectOutputStream.flush();
		} catch (IOException e) {
			System.out.println("Error writing hello message to Android Server: " + e);
			logger.info("Error writing hello message to Android Server: " + e);
		}

		// Set input stream and writer.
		try {
			inputStream = connection.openInputStream();
		} catch (IOException e) {
			System.out.println("Error opening input stream from Android Server: " + e);
			logger.info("Error opening input stream from Android Server: " + e);
		}
		bReader = new BufferedReader(new InputStreamReader(inputStream));
		
		try { // Store and sanitize input.
			String input = "";
			// Buffered reader will read in null when connection is disconnected from
			// client's side.
			while ((input = bReader.readLine()) != null && currentState == CONNECTED_STATE) {
				System.out.println("Input: " + input);
				respond(input); // Send to protocol method which handles input.
			}
		} catch (IOException e) {
			System.out.println("Changing state to terminated...");
			logger.info("No error: Ignore next error.");
			currentState = TERMINATED;
		} catch (Exception e) {
			logger.info("Error reading and/or responding to input: " + e);
		}

		// Closing statements.
		pWriter.flush();
		pWriter.close();
		try {
			streamNotifier.close();
		} catch (IOException e) {
			System.out.println("Error closing stream notifier connection: " + e);
			logger.info("Error closing stream notifier connection: " + e);
		}
		System.out.println("Changing state to terminated...");
		currentState = TERMINATED; // Update current state.

	}

	/**
	 * Method for testing a PC Server test object.
	 */
	public void test() {

	}

}
