import java.util.concurrent.Callable;

/**
 * Implementation of Callable which attempts to sleep for 121 seconds when
 * invoked.
 */
public class ConnectedTimeoutTask implements Callable<String> {

	/**
	 * Attempts to compute sleeping for 121 seconds. Will throw TimeoutException
	 * when it doesn't.
	 */
	public String call() {
		System.out.println("Timeout period beginning for current connection (121 seconds)");
		while (!Thread.interrupted()) {
			try {
				Thread.sleep(121000); // 121 seconds.
			} catch (InterruptedException e) {
				System.out.println("ConnectedTimeoutTask thread interrupted");
			}
		}
		// Either state will change or TimeoutException will occur; this should never be
		// reached.
		return "";
	}

}