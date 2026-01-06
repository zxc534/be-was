package webserver;

import db.Database;
import http.ResultCode;
import model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActionMap {

    // Action Map
    private final Map<String, Function<Map<String, String>, ResultCode>> GET = new HashMap<>();
    private final Map<String, Function<Map<String, String>, ResultCode>> POST = new HashMap<>();

    public ActionMap() {
        // 이곳에서 액션을 정의
        GET.put("/create", this::handleCreate);
    }

    public Function<Map<String, String>, ResultCode> GET(String path) {
        return GET.get(path);
    }

    public Function<Map<String, String>, ResultCode> POST(String path) {
        return POST.get(path);
    }

    private ResultCode handleCreate(Map<String, String> params) {
        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email= params.get("email");

        User user = new User(userId, password, name, email);
        Database.addUser(user);

        return ResultCode.OK;
    }
}
