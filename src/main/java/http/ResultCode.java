package http;

public enum ResultCode {
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    MULTIPLE_CHOICE(300),
    MOVED_PERMANENTLY(301),
    FOUND(302),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404)
    ;

    private final int num;
    ResultCode(int num) {
        this.num = num;
    }
}
