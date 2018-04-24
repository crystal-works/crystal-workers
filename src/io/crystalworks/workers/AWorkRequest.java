package io.crystalworks.workers;

public abstract class AWorkRequest <T> {

	public enum State {
		NONE("NONE"),
		QUEUEING("QUEUEING"),
		PROCESSING("PROCESSING"),
		DONE("DONE");
		
		public String name;
		
		private State (String name) {
			this.name = name;
		}
	}
	
	private IResponseHandler<T> responseHandler;
	
	private State state;
	private String token;

	// For testing use only
	public AWorkRequest (String token, IResponseHandler<T> handler) {
		this.token = token;
		this.state = State.NONE;
		responseHandler = handler;
	}
	
	public void setState (State state) {
		this.state = state;
	}
	
	public State getState () {
		return this.state;
	}
	
	//TODO
	public String getId () {
		return "blip";
	}
	
	public String getToken () {
		return token;
	}
	
	public void sendResponse (T response) {
		responseHandler.handleResponse(response);
	}
	
	// Already Done or Uninitialized requests are considered not taken
	// Queued requests or procesing ones (not persistent in the queue atm) are considered
	// taken
	public boolean isTaken () {
		if(this.state == State.NONE || this.state == State.DONE) {
			return false;
		} else if (this.state == State.PROCESSING || this.state == State.QUEUEING) {
			return true;
		}
		
		return false;
	}
	
	
}
