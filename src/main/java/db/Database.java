package db;

import model.Article;
import model.User;

import java.util.Collection;

public interface Database {
    void addUser(User user);

    User findUserById(String userId);

    Collection<User> findAll();

    void addSession(String sid, String userId);

    String findUserIdBySid(String sid);

    int addArticle(Article article);

    Article findArticleById(int articleId);

    Collection<Article> findAllArticles();
}