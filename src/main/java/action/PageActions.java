package action;

import http.ContentType;
import http.Request;
import http.Response;
import http.ResultCode;
import model.Article;
import model.Comment;
import util.Util;
import webserver.DynamicHtmlLoader;
import webserver.WebServer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PageActions {

    // TODO page 코드에 html이 넘어오는 것 수정 필요 -> 다른 html을 여러개 정의해놓고 기존 dynamic loader만 사용하여 처리하도록
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

        // 메인 페이지, Article 페이지 분기
        String articleIdStr = req.params.getOrDefault("articleId", "0");
        int articleId = Integer.parseInt(articleIdStr);
        Article article;

        if (articleId == 0) {
            article = WebServer.db.findLatestArticle();
            articleId = article.getArticleId();
        } else {
            article = WebServer.db.findArticleById(articleId);
        }

        if (article == null) {
            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/empty.html", variables);
            rsp.contentType = ContentType.HTML;

            return rsp;
        } else {
            int likeCount = article.getLikeCount();
            String likeCountStr;
            if (likeCount >= 999) {
                likeCountStr = "999+";
            } else {
                likeCountStr = String.valueOf(likeCount);
            }

            Collection<Comment> comments = WebServer.db.findCommentsByArticleId(article.getArticleId());

            StringBuilder sb = new StringBuilder();
            boolean showAllComments = req.params.getOrDefault("showAllComments", "false").equals("true");
            if (showAllComments) {
                for (Comment c : comments) {
                    sb.append("<li class=\"comment__item\">")
                            .append("<div class=\"comment__item__user\">")
                            .append("<img class=\"comment__item__user__img\" />")
                            .append("<p class=\"comment__item__user__nickname\">")
                            .append(c.getUserId())
                            .append("</p></div>")
                            .append("<p class=\"comment__item__article\">")
                            .append(c.getContent())
                            .append("</p></li>");
                }
            } else {
                Iterator<Comment> it = comments.iterator();
                for (int i = 0; i < 3 && it.hasNext(); i++) {
                    Comment c = it.next();
                    sb.append("<li class=\"comment__item\">")
                            .append("<div class=\"comment__item__user\">")
                            .append("<img class=\"comment__item__user__img\" />")
                            .append("<p class=\"comment__item__user__nickname\">")
                            .append(c.getUserId())
                            .append("</p></div>")
                            .append("<p class=\"comment__item__article\">")
                            .append(c.getContent())
                            .append("</p></li>");
                }

                if (comments.size() > 3) {
                    sb.append("<a href=\"/article?articleId=").append(article.getArticleId()).append("&showAllComments=true\">");
                    sb.append("<button id=\"show-all-btn\" class=\"btn btn_ghost btn_size_m\">");
                    sb.append("모든 댓글 보기(").append(comments.size()).append("개)");
                    sb.append("</button>");
                    sb.append("</a>");
                }
            }
            String commentsHtml = sb.toString();

            String prevArticleHtml = "";
            if (1 == articleId) {
                prevArticleHtml = "<a class=\"nav__menu__item__btn disable\">";
            } else {
                prevArticleHtml = "<a class=\"nav__menu__item__btn\" href=\"/article?articleId=" + (articleId-1) + "\">";
            }

            int numArticles = WebServer.db.countArticles();
            String nextArticleHtml = "";
            if (numArticles == articleId) {
                nextArticleHtml = "<a class=\"nav__menu__item__btn disable\">";
            } else {
                nextArticleHtml = "<a class=\"nav__menu__item__btn\" href=\"/article?articleId=" + (articleId+1) + "\">";
            }

            variables.put("userId", article.getUserId());
            variables.put("articleImgFileName", article.getImgFileName());
            variables.put("articleId", String.valueOf(article.getArticleId()));
            variables.put("likeCount", likeCountStr);
            variables.put("commentCount", String.valueOf(comments.size()));
            variables.put("content", article.getContent());
            variables.put("commentsHtml", commentsHtml);
            variables.put("prevArticleHtml", prevArticleHtml);
            variables.put("nextArticleHtml", nextArticleHtml);

            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/index.html", variables);
            rsp.contentType = ContentType.HTML;

            return rsp;
        }
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

    public Response commentPage(Request req) {
        String userId = Util.getUserIdFromCookie(req);
        int articleId = 0;

        try {
            // articleId 값이 올바르지 않은 경우
            // 숫자가 아님, 0 보다 작은 경우
            articleId = Integer.parseInt(req.params.getOrDefault("articleId", ""));
        } catch (NumberFormatException ignore) {
        }

        if (userId.isEmpty()) {
            return Response.redirect("/login");
        } else if (articleId <= 0) {
            Response failRsp = new Response();
            failRsp.resultCode = ResultCode.BAD_REQUEST;
            failRsp.contentType = ContentType.TXT;
            failRsp.body = "articleId 값이 이상한데요?".getBytes(StandardCharsets.UTF_8);
            return failRsp;
        } else {
            Map<String, String> variables = new HashMap<>();
            variables.put("requestPath", "/comment?articleId=" + articleId);

            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/comment/index.html", variables);
            rsp.contentType = ContentType.HTML;
            return rsp;
        }
    }
}
