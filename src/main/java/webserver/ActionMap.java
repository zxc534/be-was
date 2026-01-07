package webserver;

import db.Database;
import http.ContentType;
import http.Request;
import http.Response;
import http.ResultCode;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Util;

import java.nio.charset.StandardCharsets;
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

//    private ResultCode handleGetCreate(Map<String, String> params) {
//        String userId = params.get("userId");
//        String password = params.get("password");
//        String name = params.get("name");
//        String email= params.get("email");
//
//        User user = new User(userId, password, name, email);
//        Database.addUser(user);
//
//        printAllUsers();
//
//        return ResultCode.OK;
//    }

    private Response createUser(Request req) {
        try {
            // 회원가입 정상 처리
            String body = new String(req.body, StandardCharsets.UTF_8);
            req.params = Util.parseParams(body);

            String userId = req.params.get("userId");
            String password = req.params.get("password");
            String name = req.params.get("name");
            String email= req.params.get("email");

            User user = new User(userId, password, name, email);
            Database.addUser(user);
            printAllUsers();

            // 리다이렉트 응답
            Response rsp = new Response();
            rsp.resultCode = ResultCode.FOUND;
            rsp.header.add("Location: /index.html");
            return rsp;
        } catch (Exception e) {
            // 회원가입 처리중 에러 발생
            logger.error(e.getMessage());
            return new Response(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void printAllUsers() {
        logger.debug("==== USERS ====");
        for (User user : Database.findAll()) {
            logger.debug(user.toString());
        }
    }
}
