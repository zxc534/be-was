package http;

public class RequestHeader {
    String raw;
    RequestMethod method;

    public RequestHeader(String raw) {
        this.raw = raw;
        //TODO implement request header parsing logic
    }
}
