package model;

public class Article {
    private String userId;
    private String title;
    private String content;

    public Article(String userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public String getUserId() { return userId; }

    public String getTitle() { return title; }

    public String getContent() { return content; }
}
