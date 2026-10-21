package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO matching a single photo entry inside the Pexels REST search response.
 */
public class PexelsPhotoDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;

    @SerializedName("url")
    private String url;

    @SerializedName("photographer")
    private String photographer;

    @SerializedName("photographer_url")
    private String photographerUrl;

    @SerializedName("alt")
    private String alt;

    @SerializedName("src")
    private PexelsPhotoSrcDto src;

    public PexelsPhotoDto() {
    }

    public PexelsPhotoDto(@Nullable Long id,
                          @Nullable String alt,
                          @Nullable String photographer,
                          @Nullable String url,
                          @Nullable PexelsPhotoSrcDto src) {
        this.id = id;
        this.alt = alt;
        this.photographer = photographer;
        this.url = url;
        this.src = src;
    }

    @Nullable
    public Long getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getPhotographer() {
        return photographer;
    }

    @Nullable
    public String getPhotographerUrl() {
        return photographerUrl;
    }

    @Nullable
    public String getAlt() {
        return alt;
    }

    @Nullable
    public PexelsPhotoSrcDto getSrc() {
        return src;
    }
}
