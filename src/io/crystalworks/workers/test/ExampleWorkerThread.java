package io.crystalworks.workers.test;

import io.crystalworks.workers.AWorkerThread;

public class ExampleWorkerThread extends AWorkerThread<ExampleRequest, String> {

	@Override
	public String doWork(ExampleRequest request) {
		
		// triple the string
		String message = request.inputMessage;
		message = message + message + message;
		return message;
	}

}
