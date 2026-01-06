package http;

public class Response {
    public ResultCode resultCode;
    public ContentType contentType;
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
