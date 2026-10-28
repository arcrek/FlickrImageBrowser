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
                1700000000L,
                0,
                0
        );

        PhotoEntity entity = PhotoMapper.toEntity(original, true, 1700000000L);
        Photo roundTrip = PhotoMapper.fromEntity(entity);

        assertEquals(original, roundTrip);
    }

    @Test
    public void fromCollectionRelations_extractsFirstPhotoCoverAndCount() {
        vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity entity =
                new vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionEntity(42L, "Vacation", "Summer trip", 5000L);
        vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos relation =
                new vn.edu.usth.flickrimagebrowser.data.local.entity.CollectionWithPhotos();
        relation.collection = entity;
        relation.photos = Arrays.asList(
                new PhotoEntity("p1", "Beach", "Author", "Desc", "https://example.com/cover.jpg", "https://example.com/large1.jpg", false, 100L),
                new PhotoEntity("p2", "Hotel", "Author", "Desc", "https://example.com/thumb2.jpg", "https://example.com/large2.jpg", false, 200L)
        );

        List<vn.edu.usth.flickrimagebrowser.domain.model.Collection> mapped =
                PhotoMapper.fromCollectionRelations(Collections.singletonList(relation));

        assertNotNull(mapped);
        assertEquals(1, mapped.size());
        vn.edu.usth.flickrimagebrowser.domain.model.Collection collection = mapped.get(0);
        assertEquals(42L, collection.getId());
        assertEquals("Vacation", collection.getName());
        assertEquals("Summer trip", collection.getDescription());
        assertEquals(2, collection.getPhotoCount());
        assertEquals("https://example.com/cover.jpg", collection.getCoverPhotoUrl());
    }
}
