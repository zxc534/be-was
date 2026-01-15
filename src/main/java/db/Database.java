package db;

import model.Article;
import model.Comment;
import model.User;

import java.util.Collection;

public interface Database {
    void addUser(User user);

    User findUserById(String userId);

    Collection<User> findAll();

    void addSession(String sid, String userId);

    void deleteSession(String sid);

    String findUserIdBySid(String sid);

    int addArticle(Article article);

    Article findArticleById(int articleId);

    Article findLatestArticle();

    Collection<Article> findAllArticles();

    int increaseLikeCount(int articleId);

    void addComment(int articleId, String userId, String content);

    Collection<Comment> findCommentsByArticleId(int articleId);
}