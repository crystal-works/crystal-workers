package io.crystalworks.workers;

import java.lang.reflect.Array;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkerQueue<R extends AWorkRequest> {
	
	R [] data;
	int frontIndex = 0;
	int rearIndex = 0;
	int dataCount = 0;
	int max;
	
	Log log = LogFactory.getLog("certa-view : reg-queue");
	
	@SuppressWarnings("unchecked")
	public WorkerQueue (int max, Class<R> clazz) {
		
		data = (R[]) Array.newInstance(clazz, max);;
		this.max = max;
		
	}
	
	public synchronized R enqueue (R request, ErrorHolder error) {
	
		error.code = Error.NONE.getCode();
		
		if(data[rearIndex] !=  null && data[rearIndex].isTaken()) {
			error.code = Error.QUEUE_FULL.getCode();
			return null;
		} 
		
		data[rearIndex] = request;	
		
		data[rearIndex].setState(AWorkRequest.State.QUEUEING);
		
		rearIndex++;
		dataCount++;
		
		//wrap around
		if(rearIndex > (max-1)) rearIndex = 0;
		 
		return request;
	}
	
	public synchronized R dequeue () {
		
		if (dataCount == 0) {
			log.warn("trying to dequeue an empty queue");
			return null;
		}
		
		int returnIndex = frontIndex;

		//add and wrap around
		frontIndex++;
		if(frontIndex > (max-1)) frontIndex = 0;
		
		R request = data[returnIndex];
		request.setState(AWorkRequest.State.NONE);
		
		dataCount--;
		
		return request; 
	}
	
	// TODO unit testing
	public boolean isEmpty () {
		if (dataCount == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public enum Error {
		NONE(0, "No error"),
		QUEUE_FULL(1, "Queue is full!"),
		POLICY_ERROR(2, "Policy doesn't allow enqueue");
		
		int code;
		String message;
		
		private Error (int code, String message) {
			this.code = code;
			this.message = message;
		}
		
		public int getCode () {
			return this.code;
		}
		
		public String getMessage () {
			return this.message;
		}
	}
	
	public void printQueue () {
		
		System.out.println(" -- Regeneration request Queue --");
		System.out.println(" Front Index: " + frontIndex);
		System.out.println(" Rear Index: " + rearIndex);
		System.out.println(" Queue Size: " + max);
		System.out.println(" Queue Taken: " + dataCount);
		System.out.println(" -- -- -- -- --");
		
		for(int i = 0; i < max; i++) {
			String id = "null";
			if(data[i] != null) id = data[i].getId();
			System.out.println("[" + i + "] - " + id + " - " + data[i].getState().name);
		}
		
	}
	
	public static String getErrorMessage (int code) {
		for (Error error : Error.values()) {
			if(error.getCode() == code) {
				return error.getMessage();
			}
		}
		return "";
	}
	
}
