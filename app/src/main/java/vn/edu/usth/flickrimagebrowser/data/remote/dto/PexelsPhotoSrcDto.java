package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO matching the "src" image size variants inside a Pexels photo entry.
 */
public class PexelsPhotoSrcDto {

    @SerializedName("original")
    private String original;

    @SerializedName("large2x")
    private String large2x;

    @SerializedName("large")
    private String large;

    @SerializedName("medium")
    private String medium;

    @SerializedName("small")
    private String small;

    @SerializedName("tiny")
    private String tiny;

    public PexelsPhotoSrcDto() {
    }

    public PexelsPhotoSrcDto(@Nullable String tiny,
                             @Nullable String small,
                             @Nullable String medium,
                             @Nullable String large,
                             @Nullable String large2x,
                             @Nullable String original) {
        this.tiny = tiny;
        this.small = small;
        this.medium = medium;
        this.large = large;
        this.large2x = large2x;
        this.original = original;
    }

    @Nullable
    public String getOriginal() {
        return original;
    }

    @Nullable
    public String getLarge2x() {
        return large2x;
    }

    @Nullable
    public String getLarge() {
        return large;
    }

    @Nullable
    public String getMedium() {
        return medium;
    }

    @Nullable
    public String getSmall() {
        return small;
    }

    @Nullable
    public String getTiny() {
        return tiny;
    }
}
