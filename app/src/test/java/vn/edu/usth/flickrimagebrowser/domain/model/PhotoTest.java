package vn.edu.usth.flickrimagebrowser.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests verifying Pinterest-style aspect ratio calculation, clamping boundaries,
 * and dimension formatting in the Photo domain model.
 */
public class PhotoTest {

    @Test
    public void getAspectRatio_naturalAspectRatiosWithinBounds() {
        // Standard landscape 4:3
        Photo landscape = new Photo("1", "L", "User", "", "http://thumb", "http://detail", 800, 600, false, 0);
        assertEquals(1.3333f, landscape.getAspectRatio(), 0.001f);
        assertEquals("H,800:600", landscape.getDimensionRatio());

        // Standard portrait 2:3
        Photo portrait = new Photo("2", "P", "User", "", "http://thumb", "http://detail", 600, 900, false, 0);
        assertEquals(0.6667f, portrait.getAspectRatio(), 0.001f);
        assertEquals("H,600:900", portrait.getDimensionRatio());

        // Square 1:1
        Photo square = new Photo("3", "S", "User", "", "http://thumb", "http://detail", 500, 500, false, 0);
        assertEquals(1.0f, square.getAspectRatio(), 0.001f);
        assertEquals("H,500:500", square.getDimensionRatio());
    }

    @Test
    public void getAspectRatio_clampsExtremeTallInfographics() {
        // Ultra-tall 1:10 (100x1000)
        Photo ultraTall = new Photo("4", "Tall", "User", "", "http://thumb", "http://detail", 100, 1000, false, 0);
        assertEquals(0.5f, ultraTall.getAspectRatio(), 0.0001f);
        assertEquals("H,1:2", ultraTall.getDimensionRatio());
    }

    @Test
    public void getAspectRatio_clampsExtremeWidePanoramas() {
        // Ultra-wide 10:1 (2000x200)
        Photo ultraWide = new Photo("5", "Wide", "User", "", "http://thumb", "http://detail", 2000, 200, false, 0);
        assertEquals(2.0f, ultraWide.getAspectRatio(), 0.0001f);
        assertEquals("H,2:1", ultraWide.getDimensionRatio());
    }

    @Test
    public void getAspectRatio_handlesZeroOrMissingDimensionsGracefully() {
        Photo zeroDim = new Photo("6", "Zero", "User", "", "http://thumb", "http://detail", 0, 0, false, 0);
        assertEquals(1.0f, zeroDim.getAspectRatio(), 0.0001f);
        assertEquals("H,1:1", zeroDim.getDimensionRatio());
    }

    @Test
    public void withFavorite_preservesDimensions() {
        Photo original = new Photo("7", "Fav", "User", "", "http://thumb", "http://detail", 720, 1280, false, 100L);
        Photo favorited = original.withFavorite(true, 200L);

        assertTrue(favorited.isFavorite());
        assertEquals(720, favorited.getWidth());
        assertEquals(1280, favorited.getHeight());
        assertEquals(original.getDimensionRatio(), favorited.getDimensionRatio());
    }

    @Test
    public void equalsAndHashCode_differentiateByDimensions() {
        Photo photoA = new Photo("8", "A", "User", "", "http://thumb", "http://detail", 400, 300, false, 0);
        Photo photoB = new Photo("8", "A", "User", "", "http://thumb", "http://detail", 400, 500, false, 0);

        assertNotEquals(photoA, photoB);
        assertNotEquals(photoA.hashCode(), photoB.hashCode());
    }
}
