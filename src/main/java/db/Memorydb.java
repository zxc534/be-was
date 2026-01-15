package db;

import model.Article;
import model.Comment;
import model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Memorydb implements Database {

    private static Map<String, User> users = new HashMap<>();

    public void addUser(User user) {
        users.put(user.getUserId(), user);
    }

    public User findUserById(String userId) {
        return users.get(userId);
    }

    public Collection<User> findAll() {
        return users.values();
    }

    // 로그인 정보 SessionId
    // Map<sid, userId>
    private static Map<String, String> sessions = new HashMap<>();

    public void addSession(String sid, String userId) {
        sessions.put(sid, userId);
    }

    public void deleteSession(String sid) {
        sessions.remove(sid);
    }

    public String findUserIdBySid(String sid) {
        return sessions.get(sid);
    }

    // Article
    private static Map<Integer, Article> articles = new HashMap<>();

    private static int articleId = 0;

    public int addArticle(Article article) {
        articleId++;
        article.setArticleId(articleId);
        articles.put(articleId, article);
        return articleId;
    }

    public Article findArticleById(int articleId) {
        return articles.get(articleId);
    }

    public Article findLatestArticle() { return null; }

    public Collection<Article> findAllArticles() {
        return articles.values();
    }

    public int increaseLikeCount(int articleId) { return 0; }

    public void addComment(int articleId, String userId, String content) {}

    public Collection<Comment> findCommentsByArticleId(int articleId) { return List.of(); }
}
