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
import java.util.UUID;
import java.util.function.Function;

public class ActionMap {
    private static final Logger logger = LoggerFactory.getLogger(ActionMap.class);

    // Action Map
    private final Map<String, Function<Request, Response>> GET = new HashMap<>();
    private final Map<String, Function<Request, Response>> POST = new HashMap<>();

    public ActionMap() {
        // 이곳에서 액션을 정의
        //GET.put("/create", this::handleGetCreate);
        GET.put("/", this::mainPage);
        GET.put("/main", this::mainPage);
        GET.put("/index.html", this::mainPage);
        GET.put("/mypage", this::myPage);
        GET.put("/article", this::writePage);

        POST.put("/user/create", this::createUser);
        POST.put("/user/login", this::loginUser);
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
    private Response writePage(Request req) {
        // TODO 로그인 확인 중복 코드 메소드화
        String userId = "";
        String cookieVal = req.getHeader("cookie");
        if (cookieVal != null) {
            Map<String, String> cookie = Util.parseParams(cookieVal);
            String sid = cookie.get("sid");
            userId = Database.findUserIdBySid(sid);
        }

        if (userId.isEmpty()) {
            // 로그인하지 않은 사용자

        } else {
            // 로그인한 사용자

        }

        return null;
    }

    private Response mainPage (Request req) {
        String userId = "";
        String cookieVal = req.getHeader("cookie");
        if (cookieVal != null) {
            Map<String, String> cookie = Util.parseParams(cookieVal);
            String sid = cookie.get("sid");
            userId = Database.findUserIdBySid(sid);
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("userId", userId);

        Response rsp = new Response();
        rsp.resultCode = ResultCode.OK;
        rsp.body = DynamicHtmlLoader.load("./static/index.html", variables);
        rsp.contentType = ContentType.HTML;

        return rsp;
    }

    private Response myPage(Request req) {
        // TODO 중복 코드 제거
        // TODO if문 중첩 별론데?
        String userId = "";
        String cookieVal = req.getHeader("cookie");
        if (cookieVal != null) {
            Map<String, String> cookie = Util.parseParams(cookieVal);
            String sid = cookie.get("sid");
            userId = Database.findUserIdBySid(sid);
        }

        if (userId.isEmpty()) {
            Response rsp = new Response();
            rsp.resultCode = ResultCode.FOUND;
            rsp.header.add("Location: /login");
            return rsp;
        } else {
            Map<String, String> variables = new HashMap<>();
            variables.put("userId", userId);

            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/mypage/index.html", variables);
            rsp.contentType = ContentType.HTML;

            return rsp;
        }
    }

    private Response createUser(Request req) {
        try {
            // 회원가입 정상 처리
            String body = new String(req.body, StandardCharsets.UTF_8);
            req.params = Util.parseParams(body);

            String userId = req.params.get("userId");
            String password = req.params.get("password");
            String name = req.params.get("name");
            String email= req.params.get("email");

            // null 처리
            if(userId==null || password==null || name==null || email==null) {
                Response failRsp = new Response(ResultCode.BAD_REQUEST);
                failRsp.contentType = ContentType.TXT;
                failRsp.body = "모든 필드를 입력해야합니다.".getBytes(StandardCharsets.UTF_8);
            }

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
            logger.error("Failed to register user", e);
            return new Response(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Response loginUser(Request req) {
        try {
            // TODO contentType이 urlendcoded 일 때, body를 파싱해서 Params에 넣는 것 자동으로?
            String body = new String(req.body, StandardCharsets.UTF_8);
            req.params = Util.parseParams(body);

            String userId = req.params.get("userId");
            String password = req.params.get("password");

            User user = Database.findUserById(userId);
            String failReason;

            if (user != null) {
                if (user.getPassword().equals(password)) {
                    // 로그인 성공
                    // Session ID를 쿠키로 설정, 메인 페이지로 리다이렉트
                    String sid = UUID.randomUUID().toString();
                    Database.addSession(sid, userId);

                    Response rsp = new Response(ResultCode.FOUND);
                    rsp.header.add("Set-Cookie: sid=" + sid + "; Path=/");
                    rsp.header.add("Location: /index.html");
                    return rsp;
                } else {
                    failReason = "비밀번호가 올바르지 않습니다.";
                }
            } else {
                failReason = "존재하지 않는 ID입니다.";
            }

            // 실패 응답
            Response rsp = new Response(ResultCode.UNAUTHORIZED);
            rsp.contentType = ContentType.TXT;
            rsp.body = failReason.getBytes(StandardCharsets.UTF_8);
            return rsp;
        } catch (Exception e) {
            // TODO 액션을 인터페이스로 묶고 에러 핸들링을 공통으로 처리할 수 있지 않을까?
            // 로그인 처리중 에러 발생
            logger.error("Failed to login", e);
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
