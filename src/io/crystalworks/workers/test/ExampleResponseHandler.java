package io.crystalworks.workers.test;

import io.crystalworks.workers.IResponseHandler;

public class ExampleResponseHandler implements IResponseHandler<String>{

	@Override
	public void handleResponse(String response) {
		System.out.println("result: " + response);
	}

}
