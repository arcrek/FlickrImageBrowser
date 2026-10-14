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
 * Unit tests verifying DTO-to-domain and entity-to-domain mapping behavior.
 */
public class PhotoMapperTest {

    @Test
    public void fromDto_mapsExplicitUrlsAndMetadata() {
        PhotoDto dto = new PhotoDto(
                "101",
                "owner_id",
                "sec123",
                "65535",
                66,
                "Sunset over West Lake",
                "usth_photographer",
                new PhotoDto.DescriptionDto("Golden hour reflection"),
                "https://live.staticflickr.com/65535/101_sec123_q.jpg",
                "https://live.staticflickr.com/65535/101_sec123_m.jpg",
                "https://live.staticflickr.com/65535/101_sec123_z.jpg"
        );

        Photo photo = PhotoMapper.fromDto(dto, true);

        assertNotNull(photo);
        assertEquals("101", photo.getId());
        assertEquals("Sunset over West Lake", photo.getTitle());
        assertEquals("usth_photographer", photo.getOwnerName());
        assertEquals("Golden hour reflection", photo.getDescription());
        assertEquals("https://live.staticflickr.com/65535/101_sec123_q.jpg", photo.getThumbnailUrl());
        assertEquals("https://live.staticflickr.com/65535/101_sec123_z.jpg", photo.getDetailUrl());
        assertTrue(photo.isFavorite());
    }

    @Test
    public void fromDto_constructsStaticFlickrUrlsWhenExtrasMissing() {
        PhotoDto dto = new PhotoDto(
                "202",
                "owner_fallback",
                "abc999",
                "7001",
                7,
                "",
                null,
                null,
                null,
                null,
                null
        );

        Photo photo = PhotoMapper.fromDto(dto, false);

        assertNotNull(photo);
        assertEquals("Untitled Photo", photo.getTitle());
        assertEquals("owner_fallback", photo.getOwnerName());
        assertEquals("https://live.staticflickr.com/7001/202_abc999_q.jpg", photo.getThumbnailUrl());
        assertEquals("https://live.staticflickr.com/7001/202_abc999_z.jpg", photo.getDetailUrl());
        assertFalse(photo.isFavorite());
    }

    @Test
    public void fromDto_returnsNullWhenIdOrUrlsCannotBeResolved() {
        PhotoDto missingId = new PhotoDto(null, "u", "s", "srv", 1, "Title", "Owner", null, null, null, null);
        PhotoDto missingUrls = new PhotoDto("303", "u", null, null, 1, "Title", "Owner", null, null, null, null);

        assertNull(PhotoMapper.fromDto(missingId, false));
        assertNull(PhotoMapper.fromDto(missingUrls, false));
    }

    @Test
    public void fromDtoList_marksFavoritesAndFiltersInvalidEntries() {
        PhotoDto validFav = new PhotoDto(
                "1", "u1", "s1", "srv1", 1, "First", "Alice", null,
                "https://example.com/1_q.jpg", null, "https://example.com/1_z.jpg"
        );
        PhotoDto invalidDto = new PhotoDto(null, null, null, null, 0, null, null, null, null, null, null);
        PhotoDto validNonFav = new PhotoDto(
                "2", "u2", "s2", "srv2", 1, "Second", "Bob", null,
                "https://example.com/2_q.jpg", null, "https://example.com/2_z.jpg"
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
