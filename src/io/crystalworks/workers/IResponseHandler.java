package io.crystalworks.workers;

/**
 * @author mwinocur
 * @param <T> the type of the response. Must match the workers response type.
 * contains the result of what was processed in the worker thread
 */
public interface IResponseHandler<T> {

	public void handleResponse(T response);
	
}
