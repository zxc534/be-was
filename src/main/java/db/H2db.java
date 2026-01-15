package db;

import model.Article;
import model.User;
import org.h2.tools.Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class H2db implements Database {
    private static final String URL = "jdbc:h2:./data/appdb;";
    private static final String USER = "sa";
    private static final String PASS = "";


    public H2db() {
        init();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        String createUsers = """
                CREATE TABLE IF NOT EXISTS USERS (
                  user_id VARCHAR(50) PRIMARY KEY,
                  password VARCHAR(100) NOT NULL,
                  name VARCHAR(50),
                  email VARCHAR(100)
                );
                """;

        String createSessions = """
                CREATE TABLE IF NOT EXISTS SESSIONS (
                  sid VARCHAR(100) PRIMARY KEY,
                  user_id VARCHAR(50) NOT NULL,
                  CONSTRAINT fk_sessions_user
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                    ON DELETE CASCADE
                );
                """;

        String createArticles = """
                CREATE TABLE IF NOT EXISTS ARTICLES (
                  article_id INT AUTO_INCREMENT PRIMARY KEY,
                  user_id VARCHAR(50) NOT NULL,
                  img_file_name VARCHAR(255),
                  content CLOB NOT NULL,
                  like_count INT NOT NULL DEFAULT 0
                );
                """;

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {
            st.execute(createUsers);
            st.execute(createSessions);
            st.execute(createArticles);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // h2 console (Web UI)
        try {
            Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addUser(User user) {
        String sql = """
                INSERT INTO USERS(user_id, password, name, email) VALUES(?, ?, ?, ?)
                """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getUserId());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getName());
            ps.setString(4, user.getEmail());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public User findUserById(String userId) {
        String sql = """
                SELECT user_id, password, name, email
                FROM USERS
                WHERE user_id = ?
                """;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.execute();

            ResultSet rs = ps.executeQuery();
            // TODO user 조회 실패 null 반환 괜찮은지 확인
            if (!rs.next()) return null;

            String id = rs.getString("user_id");
            String pw = rs.getString("password");
            String name = rs.getString("name");
            String email = rs.getString("email");

            return new User(id, pw, name, email);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<User> findAll() {
        String sql = """
                SELECT user_id, password, name, email
                FROM USERS
                ORDER BY user_id
                """;

        List<User> users = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("user_id");
                String pw = rs.getString("password");
                String name = rs.getString("name");
                String email = rs.getString("email");

                users.add(new User(id, pw, name, email));
            }

            return users;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addSession(String sid, String userId) {
        String sql = """
                MERGE INTO SESSIONS (sid, user_id)
                KEY (sid)
                VALUES (?, ?)
                """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sid);
            ps.setString(2, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteSession(String sid) {
        String sql = "DELETE FROM sessions WHERE sid = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sid);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String findUserIdBySid(String sid) {
        String sql = """
                SELECT user_id
                FROM SESSIONS
                WHERE sid = ?
                """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sid);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "";
                return rs.getString("user_id");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int addArticle(Article article) {
        String sql = """
                INSERT INTO ARTICLES (user_id, img_file_name, content)
                VALUES (?, ?, ?)
                """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, article.getUserId());
            ps.setString(2, article.getImgFileName()); // null 가능
            ps.setString(3, article.getContent());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    article.setArticleId(id);
                    return id;
                }
            }

            throw new RuntimeException("Failed to get generated article_id");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Article findArticleById(int articleId) {
        String sql = """
                SELECT article_id, user_id, img_file_name, content, like_count
                FROM ARTICLES
                WHERE article_id = ?
                """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, articleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                int gotId = rs.getInt("article_id");
                String userId = rs.getString("user_id");
                String imgFileName = rs.getString("img_file_name");
                String content = rs.getString("content");
                int likeCount = rs.getInt("like_count");

                return new Article(gotId, userId, imgFileName, content, likeCount);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Article findLatestArticle() {
        String sql = """
                SELECT article_id, user_id, img_file_name, content, like_count
                FROM articles
                ORDER BY article_id DESC
                LIMIT 1
                """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return null;

            int gotId = rs.getInt("article_id");
            String userId = rs.getString("user_id");
            String imgFileName = rs.getString("img_file_name");
            String content = rs.getString("content");
            int likeCount = rs.getInt("like_count");

            return new Article(gotId, userId, imgFileName, content, likeCount);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Article> findAllArticles() {
        String sql = """
                SELECT article_id, user_id, img_file_name, content, like_count
                FROM ARTICLES
                ORDER BY article_id DESC
                """;

        List<Article> articles = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int gotId = rs.getInt("article_id");
                String userId = rs.getString("user_id");
                String imgFileName = rs.getString("img_file_name");
                String content = rs.getString("content");
                int likeCount = rs.getInt("like_count");

                Article article = new Article(gotId, userId, imgFileName, content, likeCount);
                articles.add(article);
            }

            return articles;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int increaseLikeCount(int articleId) {
        return 0;
    }
}
