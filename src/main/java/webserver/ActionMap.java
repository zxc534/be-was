package webserver;

import db.Database;
import http.Response;
import http.ResultCode;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActionMap {
    private static final Logger logger = LoggerFactory.getLogger(ActionMap.class);

    // Action Map
    private final Map<String, Function<Request, Response>> GET = new HashMap<>();
    private final Map<String, Function<Request, Response>> POST = new HashMap<>();

    public ActionMap() {
        // 이곳에서 액션을 정의
        //GET.put("/create", this::handleGetCreate);
        POST.put("/user/create", this::createUser);
    }

    public Function<Request, Response> GET(String path) {
        return GET.get(path);
    }
    public Function<Request, Response> POST(String path) {
        return POST.get(path);
    }

    private ResultCode handleGetCreate(Map<String, String> params) {
        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email= params.get("email");

        User user = new User(userId, password, name, email);
        Database.addUser(user);

        printAllUsers();

        return ResultCode.OK;
    }

    private ResultCode createUser(Map<String, String> params) {
        return ResultCode.OK;
    }

    private void printAllUsers() {
        logger.debug("==== USERS ====");
        for (User user : Database.findAll()) {
            logger.debug(user.toString());
        }
    }
}
