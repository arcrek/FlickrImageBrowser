package vn.edu.usth.flickrimagebrowser.data.remote.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Unit tests verifying Pixabay DTO-to-domain and entity-to-domain mapping behavior.
 */
public class PhotoMapperTest {

    @Test
    public void fromDto_usesCdnPreviewForSignedImagesAndPreservesItWhenSaved() {
        PixabayHitDto dto = new PixabayHitDto(101L, "Photo", "Author", null,
                "https://cdn.pixabay.com/photo/101_150.jpg",
                "https://pixabay.com/get/signed_640.jpg",
                "https://pixabay.com/get/signed_1280.jpg");

        Photo photo = PhotoMapper.fromDto(dto, false);

        assertNotNull(photo);
        assertEquals(dto.getPreviewUrl(), photo.getThumbnailUrl());
        assertEquals(dto.getLargeImageUrl(), photo.getDetailUrl());
        Photo saved = PhotoMapper.fromEntity(PhotoMapper.toEntity(photo, true, 100L));
        assertEquals(photo.getThumbnailUrl(), saved.getThumbnailUrl());
        assertEquals(photo.getDetailUrl(), saved.getDetailUrl());
    }

    @Test
    public void fromDto_keepsSignedImageWhenPreviewMissing() {
        PixabayHitDto dto = new PixabayHitDto(101L, "Photo", "Author", null,
                "  ", "https://pixabay.com/get/signed_640.jpg", null);

        Photo photo = PhotoMapper.fromDto(dto, false);

        assertNotNull(photo);
        assertEquals(dto.getWebformatUrl(), photo.getThumbnailUrl());
        assertEquals(dto.getWebformatUrl(), photo.getDetailUrl());
    }

    @Test
    public void fromEntity_removesQueryFromLegacyDemoUrlsWithoutChangingSavedIdentity() {
        PhotoEntity entity = new PhotoEntity("demo_medical_condition_31", "Title", "Author", "Description",
                "https://picsum.photos/seed/demo_medical_condition_31/300/260",
                "https://picsum.photos/seed/demo_medical_condition_31/800/600", true, 100L);
        Photo photo = PhotoMapper.fromEntity(entity);
        assertEquals("demo_medical_condition_31", photo.getId());
        assertEquals("https://picsum.photos/seed/demo_31/300/260", photo.getThumbnailUrl());
        assertEquals("https://picsum.photos/seed/demo_31/800/600", photo.getDetailUrl());
        assertTrue(photo.isFavorite());
        assertEquals(100L, photo.getSavedAt());
        assertEquals("https://picsum.photos/seed/demo_medical_condition_31/300/260", entity.getThumbnailUrl());
        Photo roundTrip = PhotoMapper.fromEntity(PhotoMapper.toEntity(photo, true, 100L));
        assertEquals(photo.getThumbnailUrl(), roundTrip.getThumbnailUrl());
        assertEquals(photo.getDetailUrl(), roundTrip.getDetailUrl());
    }

    @Test
    public void fromDto_mapsExplicitUrlsAndMetadata() {
        PixabayHitDto dto = new PixabayHitDto(
                101L,
                "sunset, west lake, hanoi",
                "usth_photographer",
                "https://pixabay.com/photos/sunset-west-lake-101/",
                "https://cdn.pixabay.com/photo/101_150.jpg",
                "https://cdn.pixabay.com/photo/101_640.jpg",
                "https://cdn.pixabay.com/photo/101_1280.jpg"
        );

        Photo photo = PhotoMapper.fromDto(dto, true);

        assertNotNull(photo);
        assertEquals("101", photo.getId());
        assertEquals("sunset, west lake, hanoi", photo.getTitle());
        assertEquals("usth_photographer", photo.getOwnerName());
        assertEquals("sunset, west lake, hanoi", photo.getDescription());
        assertEquals("https://cdn.pixabay.com/photo/101_640.jpg", photo.getThumbnailUrl());
        assertEquals("https://cdn.pixabay.com/photo/101_1280.jpg", photo.getDetailUrl());
        assertTrue(photo.isFavorite());
    }

    @Test
    public void fromDto_fallsBackWhenPrimarySizesAndMetadataMissing() {
        PixabayHitDto dto = new PixabayHitDto(
                202L,
                "",
                null,
                "https://pixabay.com/photos/202/",
                "https://cdn.pixabay.com/photo/202_150.jpg",
                null,
                null
        );

        Photo photo = PhotoMapper.fromDto(dto, false);

        assertNotNull(photo);
        assertEquals("202", photo.getId());
        assertEquals("Untitled Photo", photo.getTitle());
        assertEquals("Unknown Author", photo.getOwnerName());
        assertEquals("https://pixabay.com/photos/202/", photo.getDescription());
        assertEquals("https://cdn.pixabay.com/photo/202_150.jpg", photo.getThumbnailUrl());
        assertEquals("https://cdn.pixabay.com/photo/202_150.jpg", photo.getDetailUrl());
        assertFalse(photo.isFavorite());
    }

    @Test
    public void fromDto_returnsNullWhenIdOrUrlsCannotBeResolved() {
        PixabayHitDto missingId = new PixabayHitDto(
                null, "tags", "Owner", null,
                "https://cdn.pixabay.com/photo/1_150.jpg", null, null
        );
        PixabayHitDto missingUrls = new PixabayHitDto(
                303L, "tags", "Owner", null,
                null, null, null
        );

        assertNull(PhotoMapper.fromDto(missingId, false));
        assertNull(PhotoMapper.fromDto(missingUrls, false));
    }

    @Test
    public void fromDtoList_marksFavoritesAndFiltersInvalidEntries() {
        PixabayHitDto validFav = new PixabayHitDto(
                1L, "First", "Alice", null,
                null, "https://cdn.pixabay.com/photo/1_640.jpg", "https://cdn.pixabay.com/photo/1_1280.jpg"
        );
        PixabayHitDto invalidDto = new PixabayHitDto(
                null, null, null, null, null, null, null
        );
        PixabayHitDto validNonFav = new PixabayHitDto(
                2L, "Second", "Bob", null,
                null, "https://cdn.pixabay.com/photo/2_640.jpg", "https://cdn.pixabay.com/photo/2_1280.jpg"
        );

        Set<String> favoriteIds = new HashSet<>(Collections.singletonList("1"));
        List<Photo> mapped = PhotoMapper.fromDtoList(
                Arrays.asList(validFav, invalidDto, validNonFav),
                favoriteIds
        );

        assertEquals(2, mapped.size());
        assertTrue(mapped.get(0).isFavorite());
        assertFalse(mapped.get(1).isFavorite());
    }

    @Test
    public void entityRoundTrip_preservesAllFields() {
        Photo original = new Photo(
                "555",
                "Ha Long Bay",
                "Traveler",
                "Limestone karsts",
                "https://example.com/thumb.jpg",
                "https://example.com/detail.jpg",
                true,
                1700000000L
        );

        PhotoEntity entity = PhotoMapper.toEntity(original, true, 1700000000L);
        Photo roundTrip = PhotoMapper.fromEntity(entity);

        assertEquals(original, roundTrip);
    }
}
