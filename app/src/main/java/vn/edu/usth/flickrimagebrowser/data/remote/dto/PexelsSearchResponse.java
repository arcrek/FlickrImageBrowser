package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Top-level DTO for the Pexels REST API (/v1/search) response.
 */
public class PexelsSearchResponse {

    @SerializedName("page")
    private int page;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("total_results")
    private int totalResults;

    @SerializedName("next_page")
    private String nextPage;

    @SerializedName("photos")
    private List<PexelsPhotoDto> photos;

    @SerializedName("error")
    private String error;

    public PexelsSearchResponse() {
    }

    public PexelsSearchResponse(int page,
                                int perPage,
                                int totalResults,
                                @Nullable List<PexelsPhotoDto> photos,
                                @Nullable String error) {
        this.page = page;
        this.perPage = perPage;
        this.totalResults = totalResults;
        this.photos = photos;
        this.error = error;
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotalResults() {
        return totalResults;
    }

    @Nullable
    public String getNextPage() {
        return nextPage;
    }

    @Nullable
    public List<PexelsPhotoDto> getPhotos() {
        return photos;
    }

    @Nullable
    public String getError() {
        return error;
    }
}
