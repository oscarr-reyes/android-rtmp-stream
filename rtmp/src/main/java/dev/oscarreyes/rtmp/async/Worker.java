package dev.oscarreyes.rtmp.async;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Worker implements Runnable {

	/**
	 * Thread priority
	 */
	public int threadPriority = Thread.NORM_PRIORITY;

	/**
	 * Thread worker
	 */
	private Thread worker;

	/**
	 * Retry period in ms
	 */
	private long retryPeriod = 5000;

	/**
	 * Thread cycle execution interval
	 */
	private long interval = 0;

	/**
	 * Number of attempts
	 */
	private int attempts = 0;

	/**
	 * Last error thrown
	 */
	private Exception lastException;

	/**
	 * The number of cycles that worker has run
	 */
	protected long cycleCount;

	/**
	 * Time in ms when the cycle was executed
	 */
	protected long cycleTime;

	private final String name;
	private final Logger log;

	public Worker(String name) {
		this.name = name;

		this.log = Logger.getLogger(this.name);
	}

	/**
	 * Used for capturing byte data
	 *
	 * @throws Exception
	 */
	protected abstract void capture() throws Exception;

	/**
	 * Used for processing the captured byte data
	 *
	 * @throws Exception
	 */
	protected abstract void process() throws Exception;

	@Override
	public void run() {
		this.log.info("STARTED");
		this.cycleCount = 1;

		try {
			this.cycleTime = System.currentTimeMillis();

			while (this.worker != null) {
				this.capture();
				this.process();
				this.pause();

				this.cycleCount++;
			}
		} catch (Exception ex) {
			if (this.worker != null) {
				this.lastException = ex;
			}
		}

		log.info(String.format("STOPPED AFTER %d CYCLES", this.cycleCount));

		this.worker = null;

		if (this.lastException != null) {
			if (this.attempts > 0) {
				this.onError(this.lastException, this.retryPeriod);
				this.sleep(this.retryPeriod);

				if (this.attempts > 0) {
					this.start();
					this.attempts--;
				}
			} else {
				this.onError(lastException, 0);
			}
		}
	}

	/**
	 * Starts worker in automated recovery mode
	 *
	 * @param period   Recovery period
	 * @param attempts Number of recovery attempts
	 */
	public void start(long period, int attempts) {
		this.retryPeriod = period;
		this.attempts = attempts;

		this.start();
	}

	/**
	 * Start worker execution
	 */
	public final synchronized void start() {
		if (this.worker == null) {
			this.worker = new Thread(this, this.name);
			this.worker.setDaemon(true);
			this.worker.setPriority(this.threadPriority);
			this.worker.start();
		}
	}

	/**
	 * Invoked when the worker thread is stopped due to an error
	 *
	 * @param ex
	 * @param recoveryTime
	 */
	protected void onError(Exception ex, long recoveryTime) {
		String message = ex.getMessage();

		if (message == null) {
			message = ex.getClass().getSimpleName();
		}

		this.log.log(Level.SEVERE, message, ex);

		if (recoveryTime > 0) {
			log.info(String.format("Thread cycle recovering in %d seconds.", recoveryTime / 1000));
		}
	}

	/**
	 * Pause the worker thread for a period.
	 * Invoked after processing.
	 */
	protected void pause() {
		if (this.interval >= 0) {
			long left = System.currentTimeMillis() - this.cycleTime;

			this.sleep(interval - left);
		} else {
			this.worker = null;
		}

		this.cycleTime = System.currentTimeMillis();
	}

	/**
	 * Sets the worker to sleep for the provided time
	 *
	 * @param time Period to sleep
	 */
	protected void sleep(long time) {
		Thread.interrupted();

		try {
			if (time > 0) {
				Thread.sleep(time);
			} else {
				Thread.yield();
			}
		} catch (InterruptedException e) {
			this.log.info("Thread sleep got interrupted");
		}
	}

	/**
	 * Stops the worker executing with an error
	 *
	 * @param ex Related exception error
	 */
	protected synchronized void stop(Exception ex) {
		if (this.worker != null) {
			this.lastException = ex;
			this.worker.interrupt();
			this.worker = null;
		}
	}

	/**
	 * Stop worker execution
	 */
	public final void stop() {
		this.attempts = 0;

		this.stop(null);
	}
}
