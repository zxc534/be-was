package model;

public class Comment {
    int id;
    int articleId;
    String userId;
    String content;

    public Comment(int id, int articleId, String userId, String content) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public int getArticleId() {
        return articleId;
    }

    public String getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }
}
