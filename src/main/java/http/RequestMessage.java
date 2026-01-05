package http;

import java.util.HashMap;
import java.util.Map;

public class RequestMessage {
    String raw;
    Map<String, String> header = new HashMap<>(20);
    RequestMethod method;

    public RequestMessage() {
        //TODO implement request header parsing logic

    }

    public String toString() {
        return raw;
    }
}
