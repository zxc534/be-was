package model;

public class Article {
    private int articleId;
    private String userId;
    private String title;
    private String content;

    public Article(String userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public int getArticleId() { return articleId; }

    public void setArticleId(int articleId) { this.articleId = articleId; }

    public String getUserId() { return userId; }

    public String getTitle() { return title; }

    public String getContent() { return content; }
}
