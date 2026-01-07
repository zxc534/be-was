package http;

public enum ResultCode {
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),

    MULTIPLE_CHOICE(300, "Multiple Choice"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),

    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),

    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String text;

    ResultCode(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }
}
