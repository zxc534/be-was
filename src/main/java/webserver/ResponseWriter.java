package webserver;

import http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResponseWriter {
    private static final Logger logger = LoggerFactory.getLogger(ResponseWriter.class);

    private final DataOutputStream dos;

    public ResponseWriter(OutputStream os) {
        this.dos = new DataOutputStream(os);
    }

    public void streamOutResponse(Response rsp) {
        responseHeader(rsp);
        if (rsp.body != null) responseBody(rsp.body);
    }

    private void responseHeader(Response rsp) {
        try {
            dos.writeBytes("HTTP/1.1 " + rsp.resultCode.code() + " " + rsp.resultCode.text() + "\r\n");
            if (rsp.contentType != null) {
                dos.writeBytes("Content-Type: " + rsp.contentType.mimeType() + "\r\n");
                dos.writeBytes("Content-Length: " + rsp.body.length + "\r\n");
            }
            for (String h : rsp.header) {
                dos.writeBytes(h + "\r\n");
            }
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error("Failed to response header", e);
        }
    }

    private void responseBody(byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            logger.error("Failed to response body", e);
        }
    }
}
