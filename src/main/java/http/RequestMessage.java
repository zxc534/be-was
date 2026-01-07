package http;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestMessage {
    public RequestMethod method;
    public String requestTarget;
    public String httpVersion;
    Map<String, String> header = new HashMap<>(20);
    public byte[] body;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n====== HTTP Request Message ======\n")
                .append(method).append(" ")
                .append(requestTarget).append(" ")
                .append(httpVersion).append("\r\n");

        for (String fieldName : header.keySet()) {
            String fieldValue = header.get(fieldName);
            sb.append(fieldName).append(": ").append(fieldValue).append("\r\n");
        }
        if (body != null) {
            sb.append("================BODY==============\n");
            sb.append(new String(body, StandardCharsets.UTF_8));
        }
        sb.append("\n==================================\n");

        return sb.toString();
    }
}
