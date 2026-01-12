package db;

import model.Article;
import model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Database {
    private static Map<String, User> users = new HashMap<>();

    public static void addUser(User user) {
        users.put(user.getUserId(), user);
    }

    public static User findUserById(String userId) {
        return users.get(userId);
    }

    public static Collection<User> findAll() {
        return users.values();
    }

    // 로그인 정보 SessionId
    // Map<sid, userId>
    private static Map<String, String> sessions = new HashMap<>();

    public static void addSession(String sid, String userId) { sessions.put(sid, userId); }

    public static String findUserIdBySid(String sid) { return sessions.get(sid); }

    // Article
    private static Map<Integer, Article> articles = new HashMap<>();

    private static int articleId = 0;

    public static int addArticle(Article article) {
        articleId++;
        articles.put(articleId, article);
        return articleId;
    }

    public static Article findArticleById(int articleId) { return articles.get(articleId); }

    public static Collection<Article> findAllArticles() {
        return articles.values();
    }
}
