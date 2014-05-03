package MiniThreadManager;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

public class MiniThreadManager {

	String label;

	MiniThread[] threadsToProcess;
	long[] threadKillTimes;

	int threadsStarted;
	int threadsCompleted;
	int activeThreadCount;
	int threadsToComplete;
	int DEBUG_LEVEL;
	int THREAD_UPDATE_DELAY;
	int THREAD_MAX_ACTIVE;

	DateTime startTime;
	DateTime endTime;

	boolean killIfLongRunning = false;
	long killTime;

	boolean freeMemoryOnComplete = false;

	public MiniThreadManager(int DEBUG_LEVEL, String label, List<MiniThread> incomingThreads,
			int THREAD_MAX_ACTIVE, int THREAD_UPDATE_DELAY, boolean killIfLongRunning,
			long killTimeInMilli, boolean freeMemoryOnComplete) {
		this.label = label;
		this.threadsToProcess = incomingThreads.toArray(new MiniThread[incomingThreads.size()]);
		this.THREAD_MAX_ACTIVE = THREAD_MAX_ACTIVE;
		threadsToComplete = incomingThreads.size();
		this.DEBUG_LEVEL = DEBUG_LEVEL;
		this.THREAD_UPDATE_DELAY = THREAD_UPDATE_DELAY;

		activeThreadCount = 0;

		this.killTime = killTimeInMilli;
		this.killIfLongRunning = killIfLongRunning;

		// if killTime is zero or less
		// do not use this functionality
		if (killTimeInMilli <= 0) {
			this.killIfLongRunning = false;
		}

		// make a new list to track the threads with associated start times
		// TODO ensure this isn't hidden somewhere, when I checked I didn't see it
		// http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Thread.html
		// join does not do what I want, time does not appear to be tracked anywhere
		threadKillTimes = new long[incomingThreads.size()];

		this.freeMemoryOnComplete = freeMemoryOnComplete;
	}

	@SuppressWarnings("deprecation")
	public boolean start() throws InterruptedException {

		// get start time
		startTime = DateTime.now();

		// start message
		if (DEBUG_LEVEL >= 2) {
			System.out.println(label + ": started an instance of MiniThreadManager at "
					+ startTime.toString());
			System.out.println(label + ": number of threads to process: " + threadsToComplete);
		}

		// set up delay and print time for the first iteration
		int sleepDelay = THREAD_UPDATE_DELAY;
		int half_THREAD_MAX_ACTIVE = THREAD_MAX_ACTIVE / 2;
		long currentTime = System.currentTimeMillis();
		long nextPrintTime = currentTime + THREAD_UPDATE_DELAY;

		// TODO need to add endTime after we notice a thread completed
		// TODO need to add endMessage after we notice a thread completed
		// FIXME hacked a solution for the above for the moment

		// run all threads until the list is empty
		while (threadsStarted < threadsToComplete || activeThreadCount != 0) {

			// update out system time at start of loop
			currentTime = System.currentTimeMillis();

			// count how many of our threads are running at this time
			activeThreadCount = 0;
			for (Thread t : threadsToProcess) {
				if (t != null) {
					if (t.isAlive()) {
						activeThreadCount++;
						
						//FIXME see endTime and endMessage hack
						((MiniThread) t).endMessage = "Running successfully at last endTime.";
						((MiniThread) t).endTime = currentTime;
					}
				}
			}

			// kill any threads that have been running for too long
			// TODO checking all now, but should only check the active, move to the above for loop??
			if (killIfLongRunning) {
				for (int i = 0; i < threadsToComplete; i++) {
					MiniThread t = threadsToProcess[i];
					if (t != null) {
						if (t.isAlive()) {
							if (currentTime > threadKillTimes[i]) {
								// stop was deprecated, but it is what we want here
								// we do not care what happens to the data generated by this thread
								// TODO except for memory leaks?? or is this handled by JVM?
								t.stop();

								// notify the user that a thread stopped via the endMessage
								t.endMessage = "Killed due to surpassing the killTime";
							}
						}
					}
				}
			}

			// try to start as many threads until we hit the THREAD_MAX_ACTIVE
			for (int i = activeThreadCount; i < THREAD_MAX_ACTIVE; i++) {

				// if the active number of threads is half or less of the THREAD_MAX_ACTIVE
				// threads are being completed very quickly, so finish faster
				// or we are at the end and we have extra cpu to do this a lot
				if (activeThreadCount / THREAD_MAX_ACTIVE <= half_THREAD_MAX_ACTIVE) {
					// hurry up
					sleepDelay = 10;
				} else {
					// normal speed from config
					sleepDelay = THREAD_UPDATE_DELAY;
				}

				if (threadsStarted < threadsToProcess.length) {
					// start the next thread in the list
					(threadsToProcess[threadsStarted]).start();

					threadsToProcess[threadsStarted].startTime = currentTime;

					// set the time to kill this thread if it is still running
					if (killIfLongRunning) {
						threadKillTimes[threadsStarted] = currentTime + killTime;
					}

					// if we know a thread has completed
					// set it to null to free memory faster
					threadsCompleted = threadsStarted - THREAD_MAX_ACTIVE - 1; // - 1 for lag safety
					if (freeMemoryOnComplete && threadsCompleted > -1) {
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
				nextPrintTime = currentTime + THREAD_UPDATE_DELAY;
			}

			Thread.sleep(sleepDelay);
		} // end of while loop

		// done working, get time finished
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

		// completed successfully
		// TODO where should this be false
		return true;
	}
}
