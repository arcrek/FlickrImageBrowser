package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Top-level DTO for the Pixabay REST API (https://pixabay.com/api/) search response.
 */
public class PixabaySearchResponse {

    @SerializedName("total")
    private int total;

    @SerializedName("totalHits")
    private int totalHits;

    @SerializedName("hits")
    private List<PixabayHitDto> hits;

    public PixabaySearchResponse() {
    }

    public PixabaySearchResponse(int total,
                                 int totalHits,
                                 @Nullable List<PixabayHitDto> hits) {
        this.total = total;
        this.totalHits = totalHits;
        this.hits = hits;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalHits() {
        return totalHits;
    }

    @Nullable
    public List<PixabayHitDto> getHits() {
        return hits;
    }
}
