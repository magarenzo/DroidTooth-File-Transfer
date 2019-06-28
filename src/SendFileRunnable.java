import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Implementation of runnable which separates the task of sending a file from
 * the rest of the program, making it asynchronous. This allows the user to
 * perform other actions (such as queuing other files to be sent) while a file
 * transfer is ongoing. Multiple file transfers are not allowed to occur at the
 * same time. A maximum of 5 files can be queued to be sent.
 */
public class SendFileRunnable implements Runnable {

	private PCServer server; // Instance of PC Server.
	private File cur; // Current file.
	private byte[] fileBytes; // Bytes of cur to be sent.
	private OutputStream outputStream; // Output stream to send out files.

	/**
	 * Sets private parameter to passed-in PCServer object.
	 * 
	 * @param pc
	 *            Current instance of PC Server.
	 */
	public SendFileRunnable(PCServer pc) {

		server = pc;

	}

	/**
	 * Sends the file to the connected Android Server device.
	 */
	public void run() {

		outputStream = server.getOutputStream();

		while (true) {
			if (!server.getFilesToSend().isEmpty()) {
				cur = server.getFilesToSend().remove();
				try { // Get bytes of file to be sent.
					fileBytes = Files.readAllBytes(cur.toPath());
				} catch (IOException e) {
					System.out.println("Error getting bytes of files to send");
					continue;
				}
				try { // Send the bytes.
					outputStream.write(fileBytes);
					outputStream.flush();
				} catch (Exception e) {
					System.out.println("Error sending file bytes in asynchronous file transfer thread");
				}
				System.out.println(fileBytes.length + " bytes sent");
			}
		}

	}

}