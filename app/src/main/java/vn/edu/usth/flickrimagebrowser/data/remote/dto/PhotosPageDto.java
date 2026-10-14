package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * DTO matching the paginated "photos" object inside Flickr's flickr.photos.search response.
 */
public class PhotosPageDto {

    @SerializedName("page")
    private int page;

    @SerializedName("pages")
    private int pages;

    @SerializedName("perpage")
    private int perPage;

    @SerializedName("total")
    private int total;

    @SerializedName("photo")
    private List<PhotoDto> photo;

    public PhotosPageDto() {
    }

    public PhotosPageDto(int page, int pages, int perPage, int total, @Nullable List<PhotoDto> photo) {
        this.page = page;
        this.pages = pages;
        this.perPage = perPage;
        this.total = total;
        this.photo = photo;
    }

    public int getPage() {
        return page;
    }

    public int getPages() {
        return pages;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotal() {
        return total;
    }

    @Nullable
    public List<PhotoDto> getPhotoList() {
        return photo;
    }
}
