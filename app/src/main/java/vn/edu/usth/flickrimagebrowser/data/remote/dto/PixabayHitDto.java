package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO matching a single image hit entry inside the Pixabay REST API search response.
 */
public class PixabayHitDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("pageURL")
    private String pageUrl;

    @SerializedName("type")
    private String type;

    @SerializedName("tags")
    private String tags;

    @SerializedName("previewURL")
    private String previewUrl;

    @SerializedName("previewWidth")
    private int previewWidth;

    @SerializedName("previewHeight")
    private int previewHeight;

    @SerializedName("webformatURL")
    private String webformatUrl;

    @SerializedName("webformatWidth")
    private int webformatWidth;

    @SerializedName("webformatHeight")
    private int webformatHeight;

    @SerializedName("largeImageURL")
    private String largeImageUrl;

    @SerializedName("imageWidth")
    private int imageWidth;

    @SerializedName("imageHeight")
    private int imageHeight;

    @SerializedName("views")
    private int views;

    @SerializedName("downloads")
    private int downloads;

    @SerializedName("likes")
    private int likes;

    @SerializedName("user")
    private String user;

    @SerializedName("userImageURL")
    private String userImageUrl;

    public PixabayHitDto() {
    }

    public PixabayHitDto(@Nullable Long id,
                         @Nullable String tags,
                         @Nullable String user,
                         @Nullable String pageUrl,
                         @Nullable String previewUrl,
                         @Nullable String webformatUrl,
                         @Nullable String largeImageUrl) {
        this.id = id;
        this.tags = tags;
        this.user = user;
        this.pageUrl = pageUrl;
        this.previewUrl = previewUrl;
        this.webformatUrl = webformatUrl;
        this.largeImageUrl = largeImageUrl;
    }

    @Nullable
    public Long getId() {
        return id;
    }

    @Nullable
    public String getPageUrl() {
        return pageUrl;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getTags() {
        return tags;
    }

    @Nullable
    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    @Nullable
    public String getWebformatUrl() {
        return webformatUrl;
    }

    public int getWebformatWidth() {
        return webformatWidth;
    }

    public int getWebformatHeight() {
        return webformatHeight;
    }

    @Nullable
    public String getLargeImageUrl() {
        return largeImageUrl;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getViews() {
        return views;
    }

    public int getDownloads() {
        return downloads;
    }

    public int getLikes() {
        return likes;
    }

    @Nullable
    public String getUser() {
        return user;
    }

    @Nullable
    public String getUserImageUrl() {
        return userImageUrl;
    }
}
