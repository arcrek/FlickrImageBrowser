package vn.edu.usth.flickrimagebrowser.ui.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test verifying real ZoomableImageView geometry, fitCenter aspect ratio,
 * gesture handling, and matrix bounds clamping executed on the Android UI main thread.
 */
@RunWith(AndroidJUnit4.class)
public class ZoomableImageViewTest {

    private Context context;
    private ZoomableImageView zoomableImageView;
    private Bitmap testBitmap;

    private void runOnUiThread(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    @Before
    public void setUp() {
        runOnUiThread(() -> {
            context = ApplicationProvider.getApplicationContext();
            zoomableImageView = new ZoomableImageView(context);
            // Create 800x600 bitmap (4:3 aspect ratio)
            testBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), testBitmap);
            zoomableImageView.setImageDrawable(drawable);

            // Layout the view to 400x800 (portrait viewport)
            zoomableImageView.measure(
                    View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
            );
            zoomableImageView.layout(0, 0, 400, 800);
        });
    }

    @Test
    public void resetZoom_centersLandscapePhotoInPortraitViewWithoutCropping() {
        runOnUiThread(() -> {
            zoomableImageView.resetZoom();

            assertEquals(1.0f, zoomableImageView.getCurrentScale(), 1e-3f);
            assertFalse(zoomableImageView.isZoomed());

            RectF rect = zoomableImageView.getDisplayRect();
            // Scale should be 400 / 800 = 0.5f -> width = 400, height = 300
            assertEquals(0f, rect.left, 1e-2f);
            assertEquals(400f, rect.right, 1e-2f);
            assertEquals(400f, rect.width(), 1e-2f);
            assertEquals(300f, rect.height(), 1e-2f);

            // Vertically centered: (800 - 300) / 2 = 250f
            assertEquals(250f, rect.top, 1e-2f);
            assertEquals(550f, rect.bottom, 1e-2f);
        });
    }

    @Test
    public void doubleTapZoom_andDragGesture_clampsBoundsToViewportEdges() {
        // Step 1: Reset and dispatch double-tap gesture on UI thread
        runOnUiThread(() -> {
            zoomableImageView.resetZoom();

            long downTime = SystemClock.uptimeMillis();
            MotionEvent down1 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 200f, 400f, 0);
            MotionEvent up1 = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, 200f, 400f, 0);
            zoomableImageView.onTouchEvent(down1);
            zoomableImageView.onTouchEvent(up1);
            down1.recycle();
            up1.recycle();

            MotionEvent down2 = MotionEvent.obtain(downTime + 80, downTime + 80, MotionEvent.ACTION_DOWN, 200f, 400f, 0);
            MotionEvent up2 = MotionEvent.obtain(downTime + 80, downTime + 100, MotionEvent.ACTION_UP, 200f, 400f, 0);
            zoomableImageView.onTouchEvent(down2);
            zoomableImageView.onTouchEvent(up2);
            down2.recycle();
            up2.recycle();
        });

        // Step 2: Wait outside UI thread for zoom animation (220ms) to tick and complete
        long deadline = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            final float[] scaleHolder = new float[1];
            runOnUiThread(() -> scaleHolder[0] = zoomableImageView.getCurrentScale());
            if (scaleHolder[0] > ZoomableImageView.MIN_SCALE + 0.1f) {
                break;
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {}
        }

        // Step 3: Verify zoom succeeded and test drag bounds clamping on UI thread
        runOnUiThread(() -> {
            assertTrue("View must be zoomed in past MIN_SCALE after double-tap",
                    zoomableImageView.getCurrentScale() > ZoomableImageView.MIN_SCALE);

            RectF rectAfterZoom = zoomableImageView.getDisplayRect();
            assertTrue(rectAfterZoom.width() >= 400f);

            // Simulate drag right attempt to expose blank space on left
            long dragTime = SystemClock.uptimeMillis();
            MotionEvent dragDown = MotionEvent.obtain(dragTime, dragTime, MotionEvent.ACTION_DOWN, 100f, 400f, 0);
            MotionEvent dragMove = MotionEvent.obtain(dragTime, dragTime + 30, MotionEvent.ACTION_MOVE, 350f, 400f, 0);
            MotionEvent dragUp = MotionEvent.obtain(dragTime, dragTime + 60, MotionEvent.ACTION_UP, 350f, 400f, 0);

            zoomableImageView.onTouchEvent(dragDown);
            zoomableImageView.onTouchEvent(dragMove);
            zoomableImageView.onTouchEvent(dragUp);

            dragDown.recycle();
            dragMove.recycle();
            dragUp.recycle();

            // Clamping check: left edge must remain clamped at or before viewport 0
            RectF rectAfterDrag = zoomableImageView.getDisplayRect();
            assertTrue("Left edge must remain clamped at or before viewport 0", rectAfterDrag.left <= 1e-2f);
            assertTrue("Right edge must remain at or beyond viewport width 400", rectAfterDrag.right >= 400f - 1e-2f);
        });
    }
}
