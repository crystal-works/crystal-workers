# crystal-workers
A request-response based threading worker library.

This library is a WIP and it's not production ready!

## Usage

implement IResponseHandler<T> where T is the type of the service response
    implement the function handleResponse (T response)

```java
public class ExampleResponseHandler implements IResponseHandler<String>{

    @Override
    public void handleResponse(String response) {
    ...
```

extend AWorkerRequest<T> where T is the type of the response and must match
the handlers response.
    override the constructor and pass to the superclass an instance of the response handler.

```java
public class ExampleRequest extends AWorkRequest<String>{

    public String inputMessage;
    
    public ExampleRequest (String input, String token) {
        super(token, new ExampleResponseHandler());
    ...
```

extend AWorkerThread<R, T> where R is the sub-class of AWorkerRequest and T is the type of the 
respose. Must match the Request response type.
    implement R doWork (T request) to process the request and return the response. Actual work comes here.

```java
public class ExampleWorkerThread extends AWorkerThread<ExampleRequest, String> {

    @Override
    public String doWork(ExampleRequest request) {
    ...
```

create an instance of WorkerManager<T, R> where T is the type you created for the thread and
R is the type of the request. Pass the thread pool parameters and the class objects for T and R.

```java
WorkerManager manager = new WorkerManager<ExampleWorkerThread, ExampleRequest>
                (100 // max queue size
                 ,5  // max threads in pool
                 ,2  // min threads in pool
                 ,2  // max idle threads in pool
                 , ExampleWorkerThread.class, ExampleRequest.class);
            
```

create a request intance and call scheduleRequest(R request, String id) on the worker manager, and watch it go!
dont forget to check for errors

```java
    ExampleRequest request = new ExampleRequest("This is a request message", "12345");
            
    int error = manager.scheduleRequest(request, "aaa");
            
    if (error != 0) {
        System.err.println("ERROR: " + WorkerManager.getErrorMessage(error));
    }
```

check out the examples in the test folder

## TODO

this is a WIP library and will be expanded in time.
