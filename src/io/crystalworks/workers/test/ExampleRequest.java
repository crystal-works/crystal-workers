package io.crystalworks.workers.test;

import io.crystalworks.workers.AWorkRequest;

public class ExampleRequest extends AWorkRequest<String>{

	public String inputMessage;
	
	public ExampleRequest (String input, String token) {
		super(token, new ExampleResponseHandler());
		this.inputMessage = input;
	}
}
