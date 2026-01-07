package http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Response {
    private static final Logger logger = LoggerFactory.getLogger(Response.class);

    public ResultCode resultCode;
    public ContentType contentType;
    public List<String> header = new LinkedList<>();
    public byte[] body;

    public Response() {};
    public Response(ResultCode resultCode) {
        this.resultCode = resultCode;
    }
    public Response(ResultCode resultCode, byte[] body) {
        this.resultCode = resultCode;
        this.body = body;
    }

    public void streamOutResponse(DataOutputStream dos) {
        responseHeader(dos);
        if (body != null) responseBody(dos, body);
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void responseHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 " + resultCode.code() + " " + resultCode.text() + "\r\n");
            if (this.contentType != null) {
                dos.writeBytes("Content-Type: " + contentType.mimeType() + "\r\n");
                dos.writeBytes("Content-Length: " + body.length + "\r\n");
            }
            for (String h : header) {
                dos.writeBytes(h + "\r\n");
            }
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
