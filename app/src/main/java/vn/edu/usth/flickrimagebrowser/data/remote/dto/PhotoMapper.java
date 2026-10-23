package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Pure Java mapper converting between remote Flickr DTOs, Room entities, and domain models.
 */
public final class PhotoMapper {

    private PhotoMapper() {
    }

    @Nullable
    public static Photo fromDto(@Nullable PhotoDto dto, boolean isFavorite) {
        if (dto == null || isBlank(dto.getId())) {
            return null;
        }

        String id = dto.getId().trim();
        String thumbnailUrl = firstNonBlank(
                dto.getUrlQ(),
                dto.getUrlM(),
                buildStaticFlickrUrl(dto.getServer(), id, dto.getSecret(), "q")
        );
        String detailUrl = firstNonBlank(
                dto.getUrlZ(),
                dto.getUrlM(),
                buildStaticFlickrUrl(dto.getServer(), id, dto.getSecret(), "z"),
                thumbnailUrl
        );

        if (isBlank(thumbnailUrl)) {
            return null;
        }

        String descriptionText = dto.getDescription() != null && dto.getDescription().getContent() != null
                ? stripHtml(dto.getDescription().getContent())
                : "";
        String owner = firstNonBlank(dto.getOwnerName(), dto.getOwner(), "Unknown Author");

        return new Photo(
                id,
                dto.getTitle(),
                owner,
                descriptionText,
                thumbnailUrl,
                detailUrl,
                isFavorite,
                0L
        );
    }

    @NonNull
    public static List<Photo> fromDtoList(@Nullable List<PhotoDto> dtos, @Nullable Set<String> favoriteIds) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> favSet = favoriteIds != null ? favoriteIds : Collections.emptySet();
        List<Photo> result = new ArrayList<>(dtos.size());
        for (PhotoDto dto : dtos) {
            boolean isFav = dto != null && dto.getId() != null && favSet.contains(dto.getId().trim());
            Photo mapped = fromDto(dto, isFav);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    @NonNull
    public static Photo fromEntity(@NonNull PhotoEntity entity) {
        return new Photo(
                entity.getId(),
                entity.getTitle(),
                entity.getOwnerName(),
                entity.getDescription(),
                entity.getThumbnailUrl(),
                entity.getDetailUrl(),
                entity.isFavorite(),
                entity.getSavedAt()
        );
    }

    @NonNull
    public static List<Photo> fromEntityList(@Nullable List<PhotoEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Photo> list = new ArrayList<>(entities.size());
        for (PhotoEntity entity : entities) {
            if (entity != null) {
                list.add(fromEntity(entity));
            }
        }
        return list;
    }

    @NonNull
    public static PhotoEntity toEntity(@NonNull Photo photo, boolean isFavorite, long savedAt) {
        return new PhotoEntity(
                photo.getId(),
                photo.getTitle(),
                photo.getOwnerName(),
                photo.getDescription(),
                photo.getThumbnailUrl(),
                photo.getDetailUrl(),
                isFavorite,
                savedAt
        );
    }

    @NonNull
    public static List<Collection> fromCollectionRelations(@Nullable List<CollectionWithPhotos> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Collection> result = new ArrayList<>(relations.size());
        for (CollectionWithPhotos relation : relations) {
            if (relation != null && relation.collection != null) {
                int count = relation.photos != null ? relation.photos.size() : 0;
                result.add(new Collection(
                        relation.collection.getId(),
                        relation.collection.getName(),
                        relation.collection.getDescription(),
                        relation.collection.getCreatedAt(),
                        count
                ));
            }
        }
        return result;
    }

    @Nullable
    static String buildStaticFlickrUrl(@Nullable String server,
                                       @Nullable String id,
                                       @Nullable String secret,
                                       @NonNull String sizeSuffix) {
        if (isBlank(server) || isBlank(id) || isBlank(secret)) {
            return null;
        }
        return String.format(
                Locale.US,
                "https://live.staticflickr.com/%s/%s_%s_%s.jpg",
                server.trim(),
                id.trim(),
                secret.trim(),
                sizeSuffix
        );
    }

    @Nullable
    private static String firstNonBlank(@Nullable String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (!isBlank(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    @NonNull
    private static String stripHtml(@NonNull String value) {
        return value.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }
}
