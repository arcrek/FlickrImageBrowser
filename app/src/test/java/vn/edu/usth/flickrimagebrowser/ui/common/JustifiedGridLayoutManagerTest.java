package vn.edu.usth.flickrimagebrowser.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.ui.common.JustifiedGridLayoutManager.RowFrame;

/**
 * Pure geometry tests verifying Flickr-style justified row calculations.
 * Exercises production JustifiedGridLayoutManager.calculateRowGeometry with RowFrame (no Android Rect stub).
 */
public class JustifiedGridLayoutManagerTest {

    @Test
    public void calculateRowGeometry_uniformRowHeightAcrossItemsInSameRow() {
        int availableWidth = 1000;
        int paddingLeft = 10;
        int paddingTop = 10;
        int gap = 4;
        int targetRowHeight = 160;
        // Three landscape photos
        List<Float> ratios = Arrays.asList(1.5f, 1.8f, 1.6f);
        List<RowFrame> frames = new ArrayList<>();

        int totalHeight = JustifiedGridLayoutManager.calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                ratios,
                1.35f,
                frames
        );

        assertEquals(3, frames.size());
        int firstItemHeight = frames.get(0).height();
        for (RowFrame frame : frames) {
            assertEquals("All items in row must have identical height", firstItemHeight, frame.height());
            assertEquals(paddingTop, frame.top);
        }
        assertTrue("Total height must advance by row height + gap", totalHeight > paddingTop + firstItemHeight);
    }

    @Test
    public void calculateRowGeometry_justifiesEdgeToEdgeWithoutGapOverflow() {
        int availableWidth = 800;
        int paddingLeft = 0;
        int paddingTop = 0;
        int gap = 3;
        int targetRowHeight = 160;
        // Two landscape photos filling a row
        List<Float> ratios = Arrays.asList(2.0f, 2.5f);
        List<RowFrame> frames = new ArrayList<>();

        JustifiedGridLayoutManager.calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                ratios,
                1.35f,
                frames
        );

        assertEquals(2, frames.size());
        assertEquals(0, frames.get(0).left);
        assertEquals(frames.get(0).right + gap, frames.get(1).left);
        assertEquals(availableWidth, frames.get(1).right);
    }

    @Test
    public void calculateRowGeometry_proportionalWidthsMatchAspectRatios() {
        int availableWidth = 900;
        int paddingLeft = 0;
        int paddingTop = 0;
        int gap = 0;
        int targetRowHeight = 300;
        // Ratio 2:1 and Ratio 1:1 -> Photo 1 width should be double Photo 2 width
        List<Float> ratios = Arrays.asList(2.0f, 1.0f);
        List<RowFrame> frames = new ArrayList<>();

        JustifiedGridLayoutManager.calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                ratios,
                1.35f,
                frames
        );

        assertEquals(2, frames.size());
        int width1 = frames.get(0).width();
        int width2 = frames.get(1).width();
        assertEquals(900, width1 + width2);
        assertTrue("Photo with 2.0 aspect must be wider than photo with 1.0 aspect", width1 > width2);
        assertEquals(2.0f, (float) width1 / width2, 0.05f);
    }

    @Test
    public void calculateRowGeometry_lastIncompleteRowDoesNotStretchExcessively() {
        int availableWidth = 1000;
        int paddingLeft = 0;
        int paddingTop = 0;
        int gap = 4;
        int targetRowHeight = 160;
        // Single portrait photo alone on the last row
        List<Float> ratios = Arrays.asList(0.75f);
        List<RowFrame> frames = new ArrayList<>();

        JustifiedGridLayoutManager.calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                ratios,
                1.35f,
                frames
        );

        assertEquals(1, frames.size());
        RowFrame singlePhotoFrame = frames.get(0);
        // Height must not blow up; it must remain at targetRowHeight
        assertEquals(targetRowHeight, singlePhotoFrame.height());
        assertTrue("Single item on incomplete row must not stretch across full width",
                singlePhotoFrame.width() < availableWidth);
    }

    @Test
    public void calculateRowGeometry_extremeWidePanoramaPlacedAloneWithoutExceedingRowWidth() {
        int availableWidth = 500;
        int paddingLeft = 0;
        int paddingTop = 0;
        int gap = 3;
        int targetRowHeight = 160;
        // Panorama 4:1 followed by normal 1:1 photo
        List<Float> ratios = Arrays.asList(4.0f, 1.0f);
        List<RowFrame> frames = new ArrayList<>();

        JustifiedGridLayoutManager.calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                ratios,
                1.35f,
                frames
        );

        assertEquals(2, frames.size());
        // First photo (panorama) exceeds natural width at target height, placed on row 0
        RowFrame panoramaFrame = frames.get(0);
        assertEquals(0, panoramaFrame.top);
        assertEquals(500, panoramaFrame.right);

        // Second photo placed on next row (row 1)
        RowFrame secondFrame = frames.get(1);
        assertTrue("Second photo must be placed below panorama", secondFrame.top > panoramaFrame.bottom);
        assertTrue("Second photo width must be within available width", secondFrame.width() <= 500);
    }

    @Test
    public void calculateRowGeometry_mixedPortraitSquarePanorama_flushesPriorRowAndKeepsTileWidthAboveMinimum() {
        int availableWidth = 800;
        int paddingLeft = 0;
        int paddingTop = 0;
        int gap = 3;
        int targetRowHeight = 160;
        int minTileWidth = 96;
        // Mixed sequence: portrait 0.56, square 1.0, wide panorama 4.0
        List<Float> ratios = Arrays.asList(0.56f, 1.0f, 4.0f);
        List<RowFrame> frames = new ArrayList<>();

        JustifiedGridLayoutManager.calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                minTileWidth,
                ratios,
                1.35f,
                frames
        );

        assertEquals(3, frames.size());
        RowFrame portraitFrame = frames.get(0);
        RowFrame squareFrame = frames.get(1);
        RowFrame panoramaFrame = frames.get(2);

        // Portrait and square are in row 0
        assertEquals(0, portraitFrame.top);
        assertEquals(0, squareFrame.top);

        // Panorama is placed in a separate row below prior items
        assertTrue("Panorama must be placed in a separate row below prior items",
                panoramaFrame.top > portraitFrame.bottom);

        // Portrait width must not be squashed below minTileWidth
        assertTrue("Portrait width must be >= minTileWidth", portraitFrame.width() >= minTileWidth);
        assertTrue("Square width must be >= minTileWidth", squareFrame.width() >= minTileWidth);
        assertTrue("Panorama width must be within available width", panoramaFrame.width() <= availableWidth);
    }
}
