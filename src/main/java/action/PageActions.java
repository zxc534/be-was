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

        Article article = WebServer.db.findLatestArticle();

        if (article == null) {
            variables.put("article", "작성된 글이 없습니다😭😭😭");
        } else {
            int likeCount = article.getLikeCount();
            String likeCountStr;
            if (likeCount >= 999) {
                likeCountStr = "999+";
            } else {
                likeCountStr = String.valueOf(likeCount);
            }
            String articleHtml = String.format("""
                    <div class="post">
                      <div class="post__account">
                        <img class="post__account__img" />
                        <p class="post__account__nickname">%s</p>
                      </div>
                      <img class="post__img" src="img/article/%s">
                      <div class="post__menu">
                        <ul class="post__menu__personal">
                          <li>
                            <form action="/article/like?articleId=%s" method="post">
                            <button class="post__menu__btn" type="submit">
                              <img src="../img/like.svg" />
                            </button>
                            </form>
                            <span>%s</span>
                          </li>
                          <li>
                            <button class="post__menu__btn">
                              <img src="../img/comment.svg" />
                            </button>
                          </li>
                        </ul>
                        <button class="post__menu__btn">
                          <img src="../img/bookMark.svg" />
                        </button>
                      </div>
                      <p class="post__article">
                        %s
                      </p>
                    </div>
                    <ul class="comment">
                    </ul>
                    <nav class="nav">
                      <ul class="nav__menu">
                        <li class="nav__menu__item">
                          <a class="nav__menu__item__btn" href="">
                            <img
                              class="nav__menu__item__img"
                              src="../img/ci_chevron-left.svg"
                            />
                            이전 글
                          </a>
                        </li>
                        <li class="nav__menu__item">
                          <a class="btn btn_ghost btn_size_m" href="/comment">댓글 작성</a>
                        </li>
                        <li class="nav__menu__item">
                          <a class="nav__menu__item__btn" href="">
                            다음 글
                            <img
                              class="nav__menu__item__img"
                              src="../img/ci_chevron-right.svg"
                            />
                          </a>
                        </li>
                      </ul>
                    </nav>
                    """, article.getUserId(), article.getImgFileName(), article.getArticleId(), likeCountStr, article.getContent());
            variables.put("article", articleHtml);
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

    public Response commentPage(Request req) {
        String userId = Util.getUserIdFromCookie(req);

        if (userId.isEmpty()) {
            return Response.redirect("/login");
        } else {
            Map<String, String> variables = new HashMap<>();

            int articleId = 1;
            variables.put("requestPath", "/comment?articleId="+articleId);

            Response rsp = new Response();
            rsp.resultCode = ResultCode.OK;
            rsp.body = DynamicHtmlLoader.load("./static/comment/index.html", variables);
            rsp.contentType = ContentType.HTML;
            return rsp;
        }
    }
}
