package webserver;

import action.PageActions;
import db.Database;
import http.ContentType;
import http.Request;
import http.Response;
import http.ResultCode;
import model.Article;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileSaver;
import util.MultipartParser;
import util.Util;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class ActionMap {
    private static final Logger logger = LoggerFactory.getLogger(ActionMap.class);

    private final Database db;

    // Action Map
    private final Map<String, Function<Request, Response>> GET = new HashMap<>();
    private final Map<String, Function<Request, Response>> POST = new HashMap<>();

    public ActionMap(Database db) {
        this.db = db;

        PageActions pageActions = new PageActions();

        // 이곳에서 액션을 정의
        GET.put("/", pageActions::mainPage);
        GET.put("/main", pageActions::mainPage);
        GET.put("/index.html", pageActions::mainPage);
        GET.put("/mypage", pageActions::myPage);
        GET.put("/write", pageActions::writePage);
        GET.put("/article", pageActions::articlePage);
        GET.put("/user/logout", this::logoutUser);

        POST.put("/user/create", this::createUser);
        POST.put("/user/login", this::loginUser);
        POST.put("/article", this::createArticle);
    }

    public Function<Request, Response> GET(String path) {
        return GET.get(path);
    }

    public Function<Request, Response> POST(String path) {
        return POST.get(path);
    }

    private Response createUser(Request req) {
        try {
            // 회원가입 정상 처리
            String body = new String(req.body, StandardCharsets.UTF_8);
            req.params = Util.parseParams(body);

            String userId = req.params.getOrDefault("userId", "");
            String password = req.params.getOrDefault("password", "");
            String name = req.params.getOrDefault("name", "");
            String email = req.params.getOrDefault("email", "");

            if (userId.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty()) {
                Response failRsp = new Response(ResultCode.BAD_REQUEST);
                failRsp.contentType = ContentType.TXT;
                failRsp.body = "모든 필드를 입력해야합니다.".getBytes(StandardCharsets.UTF_8);
                return failRsp;
            }

            // 길이 조건 확인
            if (userId.length()<4 || password.length()<4 || name.length()<4) {
                Response failRsp = new Response(ResultCode.BAD_REQUEST);
                failRsp.contentType = ContentType.TXT;
                failRsp.body = "아이디/닉네임/비밀번호는 4글자 이상이어야합니다.".getBytes(StandardCharsets.UTF_8);
                return failRsp;
            }

            // 아이디 중복 확인
            if (db.findUserById(userId) != null) {
                Response failRsp = new Response(ResultCode.BAD_REQUEST);
                failRsp.contentType = ContentType.TXT;
                failRsp.body = "존재하는 아이디입니다.".getBytes(StandardCharsets.UTF_8);
                return failRsp;
            }

            User user = new User(userId, password, name, email);
            db.addUser(user);

            return Response.redirect("/login");
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

            User user = db.findUserById(userId);
            String failReason;

            if (user != null) {
                if (user.getPassword().equals(password)) {
                    // 로그인 성공
                    // Session ID를 쿠키로 설정, 메인 페이지로 리다이렉트
                    String sid = UUID.randomUUID().toString();
                    db.addSession(sid, userId);

                    Response rsp = new Response(ResultCode.FOUND);
                    rsp.header.add("Set-Cookie: sid=" + sid + "; Path=/");
                    rsp.header.add("Location: /");
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

    private Response logoutUser(Request req) {
        String sid = Util.getSidFromCookie(req);
        if (sid != null) {
            db.deleteSession(sid);
        }

        return Response.redirect("/");
    }

    private Response createArticle(Request req) {
        String userId = Util.getUserIdFromCookie(req);

        if (userId.isEmpty()) {
            // 로그인되지 않은 요청 -> 로그인 페이지로 리다이렉트
            return Response.redirect("/login");
        }

        // 로그인된 요청
        // multipart/form-data 헤더 파싱 로직
        String contentType = req.getHeader("content-type");
        if (contentType == null) {
            // TODO 가장 외부에서 TRY CATCH로 받고 해당하는 내용을 실패 응답으로 반환하는 코드 한곳에서 관리
            // TODO 가능하다면 이 기능을 Action을 관리하는 부분에서 일괄 관리
            throw new IllegalArgumentException("content type 없음");
        }
        String[] tb = Util.splitOnce(contentType, ';');

        if (tb.length != 2) {
            throw new IllegalArgumentException("올바르지 않은 헤더, contentType 길이가 2가 아님");
        }

        if (!tb[0].equals(ContentType.MULTIPART.mimeType())) {
            throw new IllegalArgumentException("content type이 multipart/form-data가 아님");
        }

        String boundary = Util.splitOnce(tb[1], '=')[1];

        // body 파싱
        MultipartParser.MultipartForm form = MultipartParser.parse(req.body, boundary);
        String content = form.fields.getOrDefault("content", "");
        if (content.isEmpty()) {
            // 올바르지 않은 요청
            Response failRsp = new Response(ResultCode.BAD_REQUEST);
            failRsp.contentType = ContentType.TXT;
            failRsp.body = "모든 필드를 입력해야합니다.".getBytes(StandardCharsets.UTF_8);
            return failRsp;
        }

        // 첫 이미지만 저장
        MultipartParser.FilePart filePart = form.files.get(0);
        FileSaver.Result result = FileSaver.saveImg(filePart.bytes, filePart.filename, "article");

        // 이미지 저장 실패
        if (!result.isSuccess()) {
            throw new IllegalArgumentException(result.getMessage());
        }

        String imgFileName = result.getMessage();
        Article article = new Article(userId, imgFileName, content);
        int articleId = db.addArticle(article);

        return Response.redirect("/article?articleId=" + articleId);
    }
}