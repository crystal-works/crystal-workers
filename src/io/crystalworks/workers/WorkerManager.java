package io.crystalworks.workers;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.crystalworks.workers.AWorkRequest.State;

public class WorkerManager <T extends AWorkerThread, R extends AWorkRequest> {

	WorkerQueue<R>      queue;
	WorkerThreadPool<T> pool;
	
	HashMap<String, R> tokenToQueuingRequests;
	HashMap<String, R> tokenToProcessingRequests;
	
	Class<T> threadClass;
	Class<R> requestClass;
	
	Log log = LogFactory.getLog("WorkerManager");
	
	// Policies
	public static boolean ONE_PER_TOKEN = false;
	
	/**
	 * 
	 * @param queueMaximum max items that can be in the queue
	 * @param poolMax      max threads in the thread pool
	 * @param poolMin      min threads in the thread pool
	 * @param poolMaxIdle  max idle threads in the thread pool
	 * @param threadClass  the non-abstract class of the worker thread. Must extend io.crystalworks.workers.AWorkerThread
	 * @param requestClass the non-abstract class of the work request. Must extend io.crystalworks.workers.AWorkRequest
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WorkerManager (int queueMaximum, int poolMax, int poolMin, int poolMaxIdle, 
			Class<T> threadClass, Class<R> requestClass) throws InstantiationException, IllegalAccessException {
		
		queue = new WorkerQueue (queueMaximum, requestClass);
										// thread pool policies
		pool  = new  WorkerThreadPool<T> (poolMax, poolMin, poolMaxIdle, threadClass,
				// listens to the thread pool and continues assigning work once a thread
				// is done and released
				new IThreadListener() {
			
			@Override
			public void done() {
				assignWork();
			}
		});
			
		tokenToQueuingRequests    =  new HashMap<String, R>();
		tokenToProcessingRequests = new HashMap<String, R>();
		
		// used to instantiate the threads
		this.threadClass = threadClass;
		this.requestClass = requestClass;
	}
	
	public int scheduleRequest (R request, String token) {
		
		ErrorHolder error = new ErrorHolder();
		
		// if only one per token policy is activated
		// then the existing request is replaced with
		// the new one and the older one is lost
		if(ONE_PER_TOKEN) {
			if(tokenToQueuingRequests.containsKey(token)) {
				
				R existingRequest = tokenToQueuingRequests.get(token);
				// replaces queueing request
				synchronized (queue) {
					if(existingRequest.getState () == AWorkRequest.State.QUEUEING) {
						
						// copy request state into existing request
						return error.code;
					}
				}
			}
		}	
		
		request = queue.enqueue(request, error);
		
		if(error.code == 0) { 
			tokenToQueuingRequests.put(token, request);
			assignWork();	
		} else {
			//TODO report error
		}
	
		return error.code;
	}
	
	public void releaseThread (R request, T thread) {
		pool.freeThread(thread);
		assignWork();
	}
	
	/**
	 * This is a callback function called whenever new items are added to the queue
	 * or whenever a worker thread is free. It will assign new work both the queue
	 * has queueing request AND the pool can provide worker threads
	 * 
	 */
	ArrayList<R> toRequeue = new ArrayList<R>();
	
	@SuppressWarnings("unchecked")
	public void assignWork () {
		synchronized (queue) {
			
			toRequeue.clear();
			
			while (!queue.isEmpty()) {
				
				T thread = pool.takeAvailableThread();
				
				// all threads are taken, wait for next
				// callback to assign more work
				if(thread == null) {
					break;
				}
				
				synchronized (queue) {
				
					R request = queue.dequeue();
					String token = request.getToken();
					
					synchronized (tokenToProcessingRequests) {
						R processingRequest = tokenToProcessingRequests.put(token, request);
						
						if(processingRequest == null || !ONE_PER_TOKEN) {
							synchronized (thread) {
								thread.runRequest(request);
								thread.notify();
							}
							return;
						}
						
						if(processingRequest.getState() == State.PROCESSING) {
							// if this user has a processing request, the request is put back
							// at the bottom of the queue
							// being synchronized on the queue this should never fail
							log.debug("pushing request back because user has a processing request");
							pool.freeThread(thread);
							toRequeue.add(request);
						} else if (processingRequest.getState() == State.DONE) {
							//cleans up done request and runs
							tokenToProcessingRequests.remove(token);
							thread.runRequest(request);
							thread.notify();
						} else {
							log.error("Processing request with faulty state!");
						}
					}
				}
			}
			
			ErrorHolder holder = new ErrorHolder();
			
			for(R request : toRequeue) {
				queue.enqueue(request, holder);
			}
		}
	}
	
	public int getQueueSize () {
		return queue.dataCount;
	}
	
	public static String getErrorMessage (int errorCode) {
		return WorkerQueue.getErrorMessage(errorCode);
	}
	
	public void printState () {
		log.info("Regeneration framework state");
		queue.printQueue();
		pool.printPoolState();
	}
	
}