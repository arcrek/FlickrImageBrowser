package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO matching a single photo entry inside Flickr's flickr.photos.search response.
 */
public class PhotoDto {

    @SerializedName("id")
    private String id;

    @SerializedName("owner")
    private String owner;

    @SerializedName("secret")
    private String secret;

    @SerializedName("server")
    private String server;

    @SerializedName("farm")
    private int farm;

    @SerializedName("title")
    private String title;

    @SerializedName("ownername")
    private String ownerName;

    @SerializedName("description")
    private DescriptionDto description;

    @SerializedName("url_q")
    private String urlQ;

    @SerializedName("url_m")
    private String urlM;

    @SerializedName("url_z")
    private String urlZ;

    public PhotoDto() {
    }

    public PhotoDto(@Nullable String id,
                    @Nullable String owner,
                    @Nullable String secret,
                    @Nullable String server,
                    int farm,
                    @Nullable String title,
                    @Nullable String ownerName,
                    @Nullable DescriptionDto description,
                    @Nullable String urlQ,
                    @Nullable String urlM,
                    @Nullable String urlZ) {
        this.id = id;
        this.owner = owner;
        this.secret = secret;
        this.server = server;
        this.farm = farm;
        this.title = title;
        this.ownerName = ownerName;
        this.description = description;
        this.urlQ = urlQ;
        this.urlM = urlM;
        this.urlZ = urlZ;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getOwner() {
        return owner;
    }

    @Nullable
    public String getSecret() {
        return secret;
    }

    @Nullable
    public String getServer() {
        return server;
    }

    public int getFarm() {
        return farm;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getOwnerName() {
        return ownerName;
    }

    @Nullable
    public DescriptionDto getDescription() {
        return description;
    }

    @Nullable
    public String getUrlQ() {
        return urlQ;
    }

    @Nullable
    public String getUrlM() {
        return urlM;
    }

    @Nullable
    public String getUrlZ() {
        return urlZ;
    }

    public static class DescriptionDto {
        @SerializedName("_content")
        private String content;

        public DescriptionDto() {
        }

        public DescriptionDto(@Nullable String content) {
            this.content = content;
        }

        @Nullable
        public String getContent() {
            return content;
        }
    }
}
