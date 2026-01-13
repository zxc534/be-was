package model;

public class Article {
    private int articleId;
    private String userId;
    private String imgFileName;
    private String content;

    public Article(String userId, String imgFileName, String content) {
        this.userId = userId;
        this.imgFileName = imgFileName;
        this.content = content;
    }

    public int getArticleId() { return articleId; }

    public void setArticleId(int articleId) { this.articleId = articleId; }

    public String getUserId() { return userId; }

    public String getImgFileName() { return imgFileName; }

    public String getContent() { return content; }
}
