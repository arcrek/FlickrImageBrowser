package vn.edu.usth.flickrimagebrowser.ui.favorites;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.usth.flickrimagebrowser.MainActivity;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.data.local.AppDatabase;
import vn.edu.usth.flickrimagebrowser.data.local.PhotoDao;
import vn.edu.usth.flickrimagebrowser.data.local.entity.PhotoEntity;

/** Verifies that saved landscape and portrait photos retain their aspect ratios in Favorites. */
@RunWith(AndroidJUnit4.class)
public class FavoritesMasonryTest {

    private static final String TEST_PREFIX = "masonry-test-";

    @Test
    public void savedPhotos_keepOriginalAspectRatioAfterReordering() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PhotoDao photoDao = AppDatabase.getInstance(context).photoDao();
        File wide = createImage(context, "masonry-wide.png", 200, 100, Color.RED);
        File tall = createImage(context, "masonry-tall.png", 100, 200, Color.BLUE);
        File square = createImage(context, "masonry-square.png", 150, 150, Color.GREEN);
        long now = System.currentTimeMillis();
        try {
            savePhoto(photoDao, "wide", wide, now + 2);
            savePhoto(photoDao, "tall", tall, now + 1);
            savePhoto(photoDao, "square", square, now);

            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                onView(withId(R.id.nav_favorites)).perform(click());
                onView(withId(R.id.edit_favorite_search)).perform(typeText(TEST_PREFIX));
                assertRatios(scenario);

                onView(withId(R.id.chip_sort_az)).perform(click());
                assertRatios(scenario);
            }
        } finally {
            for (String name : new String[]{"wide", "tall", "square"}) {
                String id = TEST_PREFIX + name;
                photoDao.updateFavoriteState(id, false, 0L);
                photoDao.deleteOrphanIfUnreferenced(id);
            }
            wide.delete();
            tall.delete();
            square.delete();
        }
    }

    private static File createImage(Context context, String name, int width, int height, int color)
            throws Exception {
        File file = new File(context.getCacheDir(), name);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
        return file;
    }

    private static void savePhoto(PhotoDao dao, String name, File file, long savedAt) {
        String uri = file.toURI().toString();
        dao.upsertPhoto(new PhotoEntity(TEST_PREFIX + name, name, TEST_PREFIX + name, "",
                uri, uri, true, savedAt));
    }

    private static void assertRatios(ActivityScenario<MainActivity> scenario) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        AtomicBoolean ready = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline && !ready.get()) {
            scenario.onActivity(activity -> {
                Map<String, ImageView> images = new HashMap<>();
                collectImages(activity.findViewById(R.id.recycler_favorites), images);
                ready.set(hasRatio(images, TEST_PREFIX + "wide", 0.5f)
                        && hasRatio(images, TEST_PREFIX + "tall", 2.0f)
                        && hasRatio(images, TEST_PREFIX + "square", 1.0f));
            });
            if (!ready.get()) {
                Thread.sleep(100);
            }
        }
        assertTrue("Favorite images did not finish loading", ready.get());

        scenario.onActivity(activity -> {
            Map<String, ImageView> images = new HashMap<>();
            collectImages(activity.findViewById(R.id.recycler_favorites), images);
            assertImageRatio(images.get(TEST_PREFIX + "wide"), 0.5f);
            assertImageRatio(images.get(TEST_PREFIX + "tall"), 2.0f);
            assertImageRatio(images.get(TEST_PREFIX + "square"), 1.0f);
        });
    }

    private static boolean hasRatio(Map<String, ImageView> images, String owner, float expected) {
        ImageView image = images.get(owner);
        if (image == null || image.getWidth() == 0 || image.getDrawable() == null) {
            return false;
        }
        Drawable drawable = image.getDrawable();
        return drawable.getIntrinsicWidth() > 0
                && Math.abs((float) drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth()
                - expected) < 0.03f
                && Math.abs((float) image.getHeight() / image.getWidth() - expected) < 0.03f;
    }

    private static void collectImages(View view, Map<String, ImageView> images) {
        if (view == null) {
            return;
        }
        TextView owner = view.findViewById(R.id.text_photo_owner);
        ImageView image = view.findViewById(R.id.image_thumbnail);
        if (owner != null && image != null && owner.getText().toString().startsWith(TEST_PREFIX)) {
            images.put(owner.getText().toString(), image);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectImages(group.getChildAt(i), images);
            }
        }
    }

    private static void assertImageRatio(ImageView image, float expected) {
        assertNotNull(image);
        Drawable drawable = image.getDrawable();
        assertNotNull(drawable);
        assertTrue(drawable.getIntrinsicWidth() > 0);
        assertTrue(image.getWidth() > 0);
        assertEquals(expected, (float) image.getHeight() / image.getWidth(), 0.03f);
    }
}
