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
}
