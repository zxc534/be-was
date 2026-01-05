package http;

import java.util.HashMap;
import java.util.Map;

public class RequestMessage {
    public String raw;
    public RequestMethod method;
    public String requestTarget;
    public String httpVersion;
    Map<String, String> header = new HashMap<>(20);

    public RequestMessage() {}

    public RequestMessage(RequestMethod method, String requestTarget, String httpVersion) {
        this.method = method;
        this.requestTarget = requestTarget;
        this.httpVersion = httpVersion;
    }

    public void putHeader(String key, String value) {
        header.put(key, value);
    }

    public String getHeader(String key) {
        return header.get(key);
    }

    public String toString() {
        return raw;
    }
}
