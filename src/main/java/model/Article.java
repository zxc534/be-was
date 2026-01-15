package model;

public class Article {
    private int articleId;
    private String userId;
    private String imgFileName;
    private String content;
    private int likeCount;

    public Article(int articleId, String userId, String imgFileName, String content, int likeCount) {
        this.articleId = articleId;
        this.userId = userId;
        this.imgFileName = imgFileName;
        this.content = content;
        this.likeCount = likeCount;
    }

    public int getArticleId() { return articleId; }

    public void setArticleId(int articleId) { this.articleId = articleId; }

    public String getUserId() { return userId; }

    public String getImgFileName() { return imgFileName; }

    public String getContent() { return content; }

    public int getLikeCount() { return likeCount; }
}
