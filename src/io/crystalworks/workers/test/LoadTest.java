package io.crystalworks.workers.test;

import io.crystalworks.workers.WorkerManager;

public class LoadTest {

	@org.junit.Test
	public void runTest () {
		
			
		/** the worker manager handles all the requests
		 * We must specify the thread class and the request class (and also pass them as arguments)
		 * we also pass the values for the manager policy which will determine how
		 * does the manager scale the thread pool and how it manages requests
		 */
		WorkerManager<ExampleWorkerThread, ExampleRequest> manager;
		try {
			manager = new WorkerManager<ExampleWorkerThread, ExampleRequest>
			(5000,5,2,2, ExampleWorkerThread.class, ExampleRequest.class);
		
			/** This exception might fire if the classes passed 
			 *	as arguments cannot be accessed or instantiated
			 */
			} catch (InstantiationException e) {
				e.printStackTrace();
				return;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
			
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		
		for (int i = 0; i < 1000; i++) {
		
			/*
			 * we create the request with the desired parameters
			 */
			ExampleRequest request = new ExampleRequest("req" + i, "12345");
			
			/*
			 * We schedule the manager to process the request with a 
			 * String id that will be the key for the request. This key is not
			 * Unique unless we define ONE_PER_TOKEN on the manager
			 */
			int error = manager.scheduleRequest(request, "aaa");
			
			/*
			 * Check for error code
			 */
			if (error != 0) {
				System.err.println("ERROR: " + WorkerManager.getErrorMessage(error));
			}
		}
		
		/**
		 * time to finish processing
		 */
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}			
	}
	
}
