package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Top-level DTO for Flickr's flickr.photos.search REST response.
 * Note: Flickr returns HTTP 200 even for application-level errors with stat="fail".
 */
public class PhotosSearchResponse {

    @SerializedName("photos")
    private PhotosPageDto photos;

    @SerializedName("stat")
    private String stat;

    @SerializedName("code")
    private Integer code;

    @SerializedName("message")
    private String message;

    public PhotosSearchResponse() {
    }

    public PhotosSearchResponse(@Nullable PhotosPageDto photos,
                                @Nullable String stat,
                                @Nullable Integer code,
                                @Nullable String message) {
        this.photos = photos;
        this.stat = stat;
        this.code = code;
        this.message = message;
    }

    @Nullable
    public PhotosPageDto getPhotos() {
        return photos;
    }

    @Nullable
    public String getStat() {
        return stat;
    }

    @Nullable
    public Integer getCode() {
        return code;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public boolean isOk() {
        return "ok".equalsIgnoreCase(stat);
    }
}
