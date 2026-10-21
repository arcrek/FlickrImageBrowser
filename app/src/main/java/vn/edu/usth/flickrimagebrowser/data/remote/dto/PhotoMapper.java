package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Pure Java mapper converting between remote Pexels DTOs, Room entities, and domain models.
 */
public final class PhotoMapper {

    private PhotoMapper() {
    }

    @Nullable
    public static Photo fromDto(@Nullable PexelsPhotoDto dto, boolean isFavorite) {
        if (dto == null || dto.getId() == null || dto.getId() <= 0L) {
            return null;
        }

        PexelsPhotoSrcDto src = dto.getSrc();
        if (src == null) {
            return null;
        }

        String id = String.valueOf(dto.getId());
        String thumbnailUrl = firstNonBlank(
                src.getTiny(),
                src.getSmall(),
                src.getMedium(),
                src.getLarge()
        );
        String detailUrl = firstNonBlank(
                src.getLarge(),
                src.getLarge2x(),
                src.getMedium(),
                thumbnailUrl
        );

        if (isBlank(thumbnailUrl)) {
            return null;
        }

        String title = dto.getAlt();
        String owner = firstNonBlank(dto.getPhotographer(), "Unknown Author");
        String descriptionText = firstNonBlank(dto.getAlt(), dto.getUrl());

        return new Photo(
                id,
                title,
                owner,
                descriptionText != null ? descriptionText : "",
                thumbnailUrl,
                detailUrl,
                isFavorite,
                0L
        );
    }

    @NonNull
    public static List<Photo> fromDtoList(@Nullable List<PexelsPhotoDto> dtos, @Nullable Set<String> favoriteIds) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> favSet = favoriteIds != null ? favoriteIds : Collections.emptySet();
        List<Photo> result = new ArrayList<>(dtos.size());
        for (PexelsPhotoDto dto : dtos) {
            boolean isFav = dto != null && dto.getId() != null && favSet.contains(String.valueOf(dto.getId()));
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
}
