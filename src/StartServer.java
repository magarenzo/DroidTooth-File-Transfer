import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.util.Base64;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.bluetooth.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.swing.JFileChooser;

/**
 * Driver method attempts creating a PCServer object and starting it.
 */
public class StartServer {
	
	// For error logging within StartServer.
	private static Logger logger = Logger.getLogger("StartServerLog");
	private static FileHandler fh;
	private static SimpleFormatter sf;

	// Files, file paths, and Strings representing both encrypted files.
	private static File ENCRYPTED_TLD_FILE;
	private static String PATH_TO_ENCRYPTED_TLD_FILE = "Files\\ENCRYPTED_TLD.txt";
	private static String TLD;
	private static File ENCRYPTED_UDA_FILE;
	private static String PATH_TO_ENCRYPTED_UDA_FILE = "Files\\ENCRYPTED_UDA.txt";
	private static String UDA;

	// Private key for encryption and decryption of TLD and UDA files.
	private static File KEY_FILE;
	private static String PATH_TO_KEY_FILE = "Files\\KEY_FILE.txt";

	private static JFileChooser chooser; // File chooser for selected TLD.

	// Algorithm for encryption and decryption.
	private static String ALGO = "AES";

	// Streams used for input and output.
	private static FileWriter writer;
	private static FileOutputStream writer2;
	private static BufferedReader reader;

	/**
	 * Encrypts passed-in String.
	 * 
	 * @param data
	 *            String to encrypt.
	 * @return String storing encrypted data.
	 */
	public static String encrypt(String data) {

		byte[] encValue = null;
		
		try {
			reader = new BufferedReader(new FileReader(KEY_FILE));
			byte[] keyValue = reader.readLine().getBytes();
			Key key = new SecretKeySpec(keyValue, ALGO);
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.ENCRYPT_MODE, key);
			encValue = c.doFinal(data.getBytes());
		} catch (Exception e) {
			System.out.println("Error encrypting " + data + ": " + e + "\nExiting...");
			logger.info("Error encrypting " + data + ": " + e);
			System.exit(0);
		}
		
		return Base64.getEncoder().encodeToString(encValue);

	}

	/**
	 * Decrypts passed-in String.
	 * 
	 * @param data
	 *            String to decrypt.
	 * @return String storing decrypted data.
	 */
	public static String decrypt(String data) {
		
		byte[] decValue = null;

		try {
			reader = new BufferedReader(new FileReader(KEY_FILE));
			byte[] keyValue = reader.readLine().getBytes();
			Key key = new SecretKeySpec(keyValue, ALGO);
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] decodedValue = Base64.getDecoder().decode(data);
			decValue = c.doFinal(decodedValue);
		} catch (Exception e) {
			System.out.println("Error decrypting " + data + ": " + e + "\nExiting...");
			logger.info("Error decrypting " + data + ": " + e);
			System.exit(0);
		}
		
		return new String(decValue);

	}

	/**
	 * The first method run by the driver method. This checks that the initial
	 * registration process has been completed. More specifically, checks if our TLD
	 * and UDA files exist. If so, decrpyt contents of both files to start the
	 * program. If one or both of the files don't exist, we create them and exit the
	 * program, telling the user to restart.
	 */
	public static void setUp() {

		KEY_FILE = new File(PATH_TO_KEY_FILE);
		ENCRYPTED_TLD_FILE = new File(PATH_TO_ENCRYPTED_TLD_FILE);
		ENCRYPTED_UDA_FILE = new File(PATH_TO_ENCRYPTED_UDA_FILE);

		// If private key file doesn't exist, create new secret key for encryption and
		// decrpytion.
		if (!KEY_FILE.exists()) {
			ENCRYPTED_TLD_FILE.delete();
			ENCRYPTED_UDA_FILE.delete();
			System.out.println("Generating a private key to be used for file encryption and decryption...");
			byte[] keyValue = new byte[16];
			String alphabet = "012345679abcdefghijklmnopqrstuvwxyz";
			Random r = new Random();
			for (int i = 0; i < 16; i++) {
				keyValue[i] = (byte) alphabet.charAt(r.nextInt(alphabet.length()));
			}
			KEY_FILE = new File(PATH_TO_KEY_FILE);
			try {
				KEY_FILE.createNewFile();
				writer2 = new FileOutputStream(new File(PATH_TO_KEY_FILE));
				writer2.write(keyValue);
				writer2.close();
			} catch (Exception e) {
				System.out.println("Error creating and/or writing private key to key file: " + e + "\nExiting...");
				logger.info("Error creating and/or writing private key to key file: " + e);
				System.exit(0);
			}
		}

		// If TLD doesn't exist, create and encrypt it with existing private key.
		if (!ENCRYPTED_TLD_FILE.exists()) {
			System.out.println("TLD does not exist! Creating...");
			chooser = new JFileChooser(); // Select TLD.
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			String path = "";
			if (chooser.showOpenDialog(chooser) == JFileChooser.APPROVE_OPTION) {
				path = chooser.getSelectedFile().toString();
			} else {
				System.out.println("TLD not selected! Exiting");
				System.exit(0);
			}
			// Encrypt TLD.
			String encryptedPath = encrypt(path);
			ENCRYPTED_TLD_FILE = new File(PATH_TO_ENCRYPTED_TLD_FILE);
			try {
				ENCRYPTED_TLD_FILE.createNewFile();
				writer = new FileWriter(ENCRYPTED_TLD_FILE);
				writer.write(encryptedPath);
				writer.close();
			} catch (Exception e) {
				System.out.println("Error creating and/or writing to TLD file: " + e + "\nExting...");
				logger.info("Error creating and/or writing to TLD file: " + e);
				System.exit(0);
			}
			System.out.println("TLD file has been created");
		}

		// If UDA doesn't exist, create and encrypt it with existing private key.
		if (!ENCRYPTED_UDA_FILE.exists()) {
			System.out.println("UDA does not exist! Creating...");
			System.out.println("Connecting your nearby Android Server to encrypt UDA details...");
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
			// Encrypt UDA.
			String encryptedUDA = "";
			try {
				String udaDebugging = RemoteDevice.getRemoteDevice(connection).getBluetoothAddress();
				System.out.println("Before encryption UDA: " + udaDebugging);
				encryptedUDA = encrypt(udaDebugging);
			} catch (IOException e) {
				System.out.println("Error getting UDA and/or encrypting it: " + e + "\nExiting...");
				logger.info("Error getting UDA and/or encrypting it: " + e);
				System.exit(0);
			}
			ENCRYPTED_UDA_FILE = new File(PATH_TO_ENCRYPTED_UDA_FILE);
			try {
				ENCRYPTED_UDA_FILE.createNewFile();
				writer = new FileWriter(ENCRYPTED_UDA_FILE);
				writer.write(encryptedUDA);
				writer.close();
			} catch (IOException e) {
				System.out.println("Error creating and/or writing to UDA file: " + e + "\nExiting...");
				logger.info("Error creating and/or writing to UDA file: " + e);
				System.exit(0);
			}
			System.out.println("UDA file has been created");
			try {
				connection.close();
				streamNotifier.close();
			} catch (IOException e) {
				System.out.println("Error closing connection: " + e);
				logger.info("Error closing connection: " + e);
			}
		}

		// Read files, store encrypted Strings, decrypt Strings and store in local
		// variables.
		try {
			if (ENCRYPTED_TLD_FILE.exists() && ENCRYPTED_UDA_FILE.exists()) {
				ENCRYPTED_TLD_FILE = new File(PATH_TO_ENCRYPTED_TLD_FILE);
				reader = new BufferedReader(new FileReader(ENCRYPTED_TLD_FILE));
				String encryptedPath = reader.readLine();
				TLD = decrypt(encryptedPath);
				ENCRYPTED_UDA_FILE = new File(PATH_TO_ENCRYPTED_UDA_FILE);
				reader = new BufferedReader(new FileReader(ENCRYPTED_UDA_FILE));
				encryptedPath = reader.readLine();
				UDA = decrypt(encryptedPath);
			}
		} catch (Exception e) {
			System.out.println("Error finishing setup: " + e + "\nExiting...");
			logger.info("Error finishing setup: " + e);
			System.exit(0);
		}
		
		System.out.println("Setup passed");

	}

	/**
	 * Attempts creating and starting PCServer.
	 * 
	 * @param args.
	 */
	public static void main(String[] args) {
		
		// Set up error logging.
		try {
			fh = new FileHandler("Logs\\StartServer.log");
			logger.addHandler(fh);
			logger.setUseParentHandlers(false);
			sf = new SimpleFormatter();
			fh.setFormatter(sf);
			logger.info("StartServer log started.");
		} catch (IOException e) {
			System.out.println("Error starting error logging: " + e);
		}

		// Welcome message.
		System.out.println("CloudTooth File Transfer starting up...");

		// Ensure that Bluetooth is on, else terminate program.
		if (!LocalDevice.isPowerOn()) {
			System.out.println("Bluetooth is off, turn on and re-execute program");
			System.exit(0);
		}

		// Display PC Server Bluetooth Address and Device Name.
		LocalDevice pcServer = null;
		try {
			pcServer = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e) {
			System.out.println("Error getting local device information: " + e + "\nExiting...");
			logger.info("Error getting local device information: " + e);
			System.exit(0);
		}
		System.out.println("PC Server Bluetooth Address: " + pcServer.getBluetoothAddress());
		System.out.println("PC Server Device Name: " + pcServer.getFriendlyName());
		// Remind user to keep Bluetooth turned on
		System.out.println("Make sure to keep your Bluetooth turned on");

		setUp(); // Run initial setup.

		PCServer server = new PCServer(TLD, UDA); // Create PC Server object.
		

		// Start thread to continuously check that (1) the Local Device's Bluetooth is
		// on, and (2) the current state of PC Server.
		Thread bluetoothThread = new Thread(new CheckBluetoothRunnable(server));
		bluetoothThread.start();
		System.out.println("CheckBluetoothRunnable thread starting up...");

		while (true) {
			
			server.begin(); // Start PC Server.
		}

	}

}