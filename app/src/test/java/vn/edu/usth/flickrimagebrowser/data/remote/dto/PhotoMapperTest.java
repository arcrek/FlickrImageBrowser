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
 * Unit tests verifying Pexels DTO-to-domain and entity-to-domain mapping behavior.
 */
public class PhotoMapperTest {

    @Test
    public void fromDto_mapsExplicitUrlsAndMetadata() {
        PexelsPhotoSrcDto src = new PexelsPhotoSrcDto(
                "https://images.pexels.com/photos/101/tiny.jpg",
                "https://images.pexels.com/photos/101/small.jpg",
                "https://images.pexels.com/photos/101/medium.jpg",
                "https://images.pexels.com/photos/101/large.jpg",
                "https://images.pexels.com/photos/101/large2x.jpg",
                "https://images.pexels.com/photos/101/original.jpg"
        );
        PexelsPhotoDto dto = new PexelsPhotoDto(
                101L,
                "Sunset over West Lake",
                "usth_photographer",
                "https://www.pexels.com/photo/101/",
                src
        );

        Photo photo = PhotoMapper.fromDto(dto, true);

        assertNotNull(photo);
        assertEquals("101", photo.getId());
        assertEquals("Sunset over West Lake", photo.getTitle());
        assertEquals("usth_photographer", photo.getOwnerName());
        assertEquals("Sunset over West Lake", photo.getDescription());
        assertEquals("https://images.pexels.com/photos/101/tiny.jpg", photo.getThumbnailUrl());
        assertEquals("https://images.pexels.com/photos/101/large.jpg", photo.getDetailUrl());
        assertTrue(photo.isFavorite());
    }

    @Test
    public void fromDto_fallsBackWhenPrimarySizesAndMetadataMissing() {
        PexelsPhotoSrcDto src = new PexelsPhotoSrcDto(
                null,
                "https://images.pexels.com/photos/202/small.jpg",
                "https://images.pexels.com/photos/202/medium.jpg",
                null,
                null,
                null
        );
        PexelsPhotoDto dto = new PexelsPhotoDto(
                202L,
                "",
                null,
                "https://www.pexels.com/photo/202/",
                src
        );

        Photo photo = PhotoMapper.fromDto(dto, false);

        assertNotNull(photo);
        assertEquals("202", photo.getId());
        assertEquals("Untitled Photo", photo.getTitle());
        assertEquals("Unknown Author", photo.getOwnerName());
        assertEquals("https://www.pexels.com/photo/202/", photo.getDescription());
        assertEquals("https://images.pexels.com/photos/202/small.jpg", photo.getThumbnailUrl());
        assertEquals("https://images.pexels.com/photos/202/medium.jpg", photo.getDetailUrl());
        assertFalse(photo.isFavorite());
    }

    @Test
    public void fromDto_returnsNullWhenIdOrUrlsCannotBeResolved() {
        PexelsPhotoSrcDto validSrc = new PexelsPhotoSrcDto(
                "https://images.pexels.com/1/tiny.jpg", null, null, null, null, null
        );
        PexelsPhotoSrcDto emptySrc = new PexelsPhotoSrcDto(null, null, null, null, null, null);

        PexelsPhotoDto missingId = new PexelsPhotoDto(null, "Title", "Owner", null, validSrc);
        PexelsPhotoDto missingSrc = new PexelsPhotoDto(303L, "Title", "Owner", null, null);
        PexelsPhotoDto missingUrls = new PexelsPhotoDto(303L, "Title", "Owner", null, emptySrc);

        assertNull(PhotoMapper.fromDto(missingId, false));
        assertNull(PhotoMapper.fromDto(missingSrc, false));
        assertNull(PhotoMapper.fromDto(missingUrls, false));
    }

    @Test
    public void fromDtoList_marksFavoritesAndFiltersInvalidEntries() {
        PexelsPhotoSrcDto src1 = new PexelsPhotoSrcDto(
                "https://images.pexels.com/1_tiny.jpg", null, null,
                "https://images.pexels.com/1_large.jpg", null, null
        );
        PexelsPhotoSrcDto src2 = new PexelsPhotoSrcDto(
                "https://images.pexels.com/2_tiny.jpg", null, null,
                "https://images.pexels.com/2_large.jpg", null, null
        );

        PexelsPhotoDto validFav = new PexelsPhotoDto(1L, "First", "Alice", null, src1);
        PexelsPhotoDto invalidDto = new PexelsPhotoDto(null, null, null, null, null);
        PexelsPhotoDto validNonFav = new PexelsPhotoDto(2L, "Second", "Bob", null, src2);

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
