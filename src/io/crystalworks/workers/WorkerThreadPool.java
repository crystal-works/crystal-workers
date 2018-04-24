package io.crystalworks.workers;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkerThreadPool<T extends AWorkerThread> {

	int maxThreads, maxIdle, minThreads;

	volatile ArrayList<T> availableThreads;
	volatile ArrayList<T> busyThreads;

	volatile int availableThreadCount = 0;
	volatile int busyThreadCount = 0;

	long lastReleased;
	long releaseTimeStep = 10000;

	int secuenceIdentifier = 0;

	volatile IThreadListener managerListener;
	
	Class<T> threadClass;
	
	Log log = LogFactory.getLog("ataman-worker : thread-pool");

	public WorkerThreadPool(int maxThreads, int minThreads, int maxIdle, 
									Class<T> threadClass, IThreadListener managerListener) 
									throws InstantiationException, IllegalAccessException {
		
		this.maxThreads = maxThreads;
		this.minThreads = minThreads;
		this.maxIdle = maxIdle;

		availableThreads = new ArrayList<T>(maxThreads);
		busyThreads = new ArrayList<T>(maxThreads);

		availableThreadCount = minThreads;

		this.threadClass = threadClass;
		this.managerListener = managerListener;
		
		for (int i = 0; i < minThreads; i++) {
			
			T thread = createThread();
			availableThreads.add(thread);
		}

		lastReleased = System.currentTimeMillis();
	}

	private T createThread () throws InstantiationException, IllegalAccessException {
		T thread = threadClass.newInstance();
		
		thread.listener = new IThreadListener() {
			
			@Override
			public void done() {
				freeThread(thread);
				synchronized(managerListener) {
					managerListener.done();
				}
			}
		};
		
		thread.start();
		return thread;
	}
	
	/**
	 * returns an available worker thread for regenerations.
	 * Will spin up a new thread if all available are busy and limits have not been met
	 * 
	 * @return available regeneration thread or null if all are busy and the maximum
	 *         does not allow to spin up new threads
	 *         
	 *         TODO error reporting
	 */
	public synchronized T takeAvailableThread() {

		if (availableThreadCount != 0) {
			T thread = availableThreads.remove(0);
			busyThreads.add(thread);
			availableThreadCount--;
			busyThreadCount++;
			return thread;
		}

		// spin up more threads if necessary
		if ((availableThreadCount + busyThreadCount) < maxThreads) {
			T thread;
			try {
				thread = createThread();
				
			// should never happen since the class constructor would have 
		    // failed in the first place
			} catch (InstantiationException e) {
				log.error("Failed to spawn thread");
				e.printStackTrace();
				return null;
			} catch (IllegalAccessException e) {
				log.error("Failed to spawn thread");
				e.printStackTrace();
				return null;
			}
			
			busyThreads.add(thread);
			busyThreadCount++;
			return thread;
		}

		return null;
	}

	/**
	 * gives a thread back to the pool if timestep is spent, triggers cleanup of the
	 * pool to downsize thread count
	 */
	public synchronized void freeThread(T thread) {

		busyThreads.remove(thread);
		availableThreads.add(thread);
		availableThreadCount++;
		busyThreadCount--;

		// -- release idle above maximum --

		// only releases at a timestep to avoid machine gun releases
		long now = System.currentTimeMillis();
		if ((now - lastReleased < releaseTimeStep)) {
			lastReleased = System.currentTimeMillis();
			return;
		}

		if (availableThreadCount > maxIdle) {
			int toRelease = availableThreadCount - maxIdle;
			assert (toRelease > 0);

			for (int i = 0; i < toRelease; i++) {
				availableThreads.remove(0);
			}

			log.info("destroyed " + toRelease + " idle threads");

			availableThreadCount -= toRelease;
			lastReleased = System.currentTimeMillis();
		}

	}

	public synchronized void printPoolState() {
		System.out.println(" -- Regeneration Thread Pool -- ");

		System.out.println("");
		System.out.println(" Available -- ");
		for (T thread : availableThreads) {
			System.out.println(" Thread: " + thread.getName());
		}

		System.out.println("");
		System.out.println(" Busy -- ");
		for (T thread : busyThreads) {
			System.out.println(" Thread: " + thread.getName());
		}
		System.out.println("");
	}

}
