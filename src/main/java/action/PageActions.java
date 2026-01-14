package action;

import http.ContentType;
import http.Request;
import http.Response;
import http.ResultCode;
import model.Article;
import util.Util;
import webserver.DynamicHtmlLoader;
import webserver.WebServer;

import java.util.HashMap;
import java.util.Map;

public class PageActions {

    public Response mainPage(Request req) {
        String userId = Util.getUserIdFromCookie(req);

        Map<String, String> variables = new HashMap<>();

        if (userId.isEmpty()) {
            String headerMenuDefault = """
                <li class="header__menu__item">
                    <a class="btn btn_contained btn_size_s" href="/login">로그인</a>
                </li>
                <li class="header__menu__item">
                    <a class="btn btn_ghost btn_size_s" href="/registration">회원 가입</a>
                </li>
                """;
            variables.put("headerMenu", headerMenuDefault);
        } else {
            String headerMenuForLoginUser = String.format("""
                <li class="header__menu__item">
                  <a class="post__account__nickname" href="/mypage">안녕하세요, %s님</a>
                </li>
                <li class="header__menu__item">
                    <a class="btn btn_contained btn_size_s" href="/write">글쓰기</a>
                </li>
                <li class="header__menu__item">
                    <a class="btn btn_ghost btn_size_s" href="/user/logout">로그아웃</a>
                </li>
                """, userId);
            variables.put("headerMenu", headerMenuForLoginUser);
        }

        Response rsp = new Response();
        rsp.resultCode = ResultCode.OK;
        rsp.body = DynamicHtmlLoader.load("./static/index.html", variables);
        rsp.contentType = ContentType.HTML;

        return rsp;
    }

    public Response myPage(Request req) {
        String userId = Util.getUserIdFromCookie(req);

        if (userId.isEmpty()) {
            return Response.redirect("/login");
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

    public Response articlePage(Request req) {
        // TODO 임시 아티클 페이지
        int articleId = Integer.parseInt(req.params.getOrDefault("articleId", "0"));

        if (articleId == 0) {
            // TODO 존재하지 않는 article
            return Response.redirect("/index.html");
        } else {
            Article article = WebServer.db.findArticleById(articleId);
            Map<String, String> variables = new HashMap<>();
            variables.put("userId", article.getUserId());
            variables.put("content", article.getContent());
            variables.put("imgFileName", "/img/article/" + article.getImgFileName());

            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/article/index.html", variables);
            rsp.contentType = ContentType.HTML;
            return rsp;
        }
    }

    public Response writePage(Request req) {
        String userId = Util.getUserIdFromCookie(req);

        if (userId.isEmpty()) {
            // 로그인하지 않은 사용자
            return Response.redirect("/login");
        } else {
            // 로그인한 사용자
            // TODO 불필요한 DynamicHtmlLoader 사용
            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/article/write.html", null);
            rsp.contentType = ContentType.HTML;
            return rsp;
        }
    }
}
