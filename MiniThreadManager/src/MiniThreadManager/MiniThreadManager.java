//kkkkkkkkkk

package MiniThreadManager;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

public class MiniThreadManager {

	String label;

	Thread[] threadsToProcess;
	int threadsStarted;
	int threadsCompleted;
	int activeThreadCount;
	int threadsToComplete;
	int DEBUG_LEVEL;
	int THREAD_UPDATE_DELAY;
	int THREAD_MAX_ACTIVE;

	DateTime startTime;
	DateTime endTime;

	public MiniThreadManager(int DEBUG_LEVEL, String label, List<Thread> threadsToProcess,
			int THREAD_MAX_ACTIVE, int THREAD_UPDATE_DELAY) {
		this.label = label;
		this.threadsToProcess = threadsToProcess.toArray(new Thread[threadsToProcess.size()]);
		this.THREAD_MAX_ACTIVE = THREAD_MAX_ACTIVE;
		threadsToComplete = threadsToProcess.size();
		this.DEBUG_LEVEL = DEBUG_LEVEL;
		this.THREAD_UPDATE_DELAY = THREAD_UPDATE_DELAY;

		activeThreadCount = 0;
	}

	public void start() throws InterruptedException {

		// start message
		if (DEBUG_LEVEL >= 2)
			System.out.println(label + ": started an instance of MiniThreadManager");

		startTime = DateTime.now();

		if (DEBUG_LEVEL >= 2)
			System.out.println(label + ": number of threads to process: " + threadsToComplete);

		int sleepDelay = THREAD_UPDATE_DELAY;
		int halfTHREAD_MAX_ACTIVE = THREAD_MAX_ACTIVE / 2;
		long nextPrintTime = System.currentTimeMillis() + THREAD_UPDATE_DELAY;

		while (threadsStarted < threadsToComplete || activeThreadCount != 0) {
			// count how many of our threads are running at this time
			activeThreadCount = 0;
			for (Thread t : threadsToProcess) {
				if (t != null) {
					if (t.isAlive()) {
						activeThreadCount++;
					}
				}
			}

			// try to start as many threads until we hit the
			// maxActiveThreadCount
			for (int i = activeThreadCount; i < THREAD_MAX_ACTIVE; i++) {
				// if the active number of threads is half or less of the THREAD_MAX_ACTIVE
				// threads are being completed very quickly, so finish faster
				// or we are at the end and we have extra cpu to do this a lot
				if (activeThreadCount / THREAD_MAX_ACTIVE <= halfTHREAD_MAX_ACTIVE) {
					// hurry up
					sleepDelay = 10;
				} else {
					// normal speed from config
					sleepDelay = THREAD_UPDATE_DELAY;
				}

				if (threadsStarted < threadsToProcess.length) {
					// start the next thread in the list
					(threadsToProcess[threadsStarted]).start();

					// if we know a thread has completed
					// set it to null to free memory faster
					threadsCompleted = threadsStarted - THREAD_MAX_ACTIVE - 1; // - 1 for lag safety
					if (threadsCompleted > -1) {
						threadsToProcess[threadsCompleted] = null;
					}

					// track what we just did
					activeThreadCount++;
					threadsStarted++;
				}
			}

			// print if longer than the THREAD_UPDATE_DELAY has pasted
			if (DEBUG_LEVEL >= 2 && System.currentTimeMillis() > nextPrintTime) {
				System.out.println(label + ": number of threads started of total: "
						+ threadsStarted + "/" + threadsToComplete + " -- active: "
						+ activeThreadCount + "/" + THREAD_MAX_ACTIVE);
				nextPrintTime = System.currentTimeMillis() + THREAD_UPDATE_DELAY;
			}

			Thread.sleep(sleepDelay);
		}

		endTime = DateTime.now();

		// time to complete message
		if (DEBUG_LEVEL >= 2) {
			System.out.print(label + ": completed work on " + threadsToComplete + " threads in ");
			System.out.print(Days.daysBetween(startTime, endTime).getDays() + " days, ");
			System.out.print(Hours.hoursBetween(startTime, endTime).getHours() % 24 + " hours, ");
			System.out.print(Minutes.minutesBetween(startTime, endTime).getMinutes() % 60
					+ " minutes, ");
			System.out.print(Seconds.secondsBetween(startTime, endTime).getSeconds() % 60
					+ " seconds.\n");
		}

		// start message
		if (DEBUG_LEVEL >= 2)
			System.out.println(label + ": completed an instance of MiniThreadManager");
	}
}
