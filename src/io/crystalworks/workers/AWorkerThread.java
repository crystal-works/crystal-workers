package io.crystalworks.workers;

public abstract class AWorkerThread <T extends AWorkRequest<R>, R> extends Thread {

	private boolean run = false;
	
	protected T currentRequest;
	IThreadListener listener;
	
	protected abstract R doWork (T request) ;
	
	public void runRequest (T request) {
		// already running
		if(run) {
			//TODO return proper error
			System.err.println("Worker thread already running: " + this.getName());
			return;
		}
		
		currentRequest = request;
		run = true;
	}
	
	private void setRunning (boolean running) {
		this.run = running;
	}
	
	
	public void run() {

		while(true) {
			
			if(!run) {
				try {
					synchronized(this) {
						wait();	
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
				
			try {
				R response = doWork(currentRequest);
				currentRequest.sendResponse(response);
			} finally {
				this.setRunning(false);;
				currentRequest.setState(AWorkRequest.State.DONE);
				listener.done();
			}
		}
	}
}
