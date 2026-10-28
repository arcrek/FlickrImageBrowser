package vn.edu.usth.flickrimagebrowser;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.search.SearchViewModel;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/** Instrumented coverage for tab navigation, toolbar titles, and tab restoration. */
@RunWith(AndroidJUnit4.class)
public class MainActivityNavigationTest {

    private ActivityScenario<MainActivity> scenario;

    @Before
    public void launchActivity() {
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
    }

    @After
    public void closeActivity() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void bottomNavigation_switchesScreensAndToolbarTitle() {
        assertToolbarTitle(R.string.app_name);

        onView(withId(R.id.nav_favorites)).perform(click());
        scenario.onActivity(activity -> assertEquals(R.id.nav_favorites,
                activity.findViewById(R.id.bottom_navigation) instanceof BottomNavigationView
                        ? ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).getSelectedItemId()
                        : 0));
        assertToolbarTitle(R.string.nav_favorites);

        onView(withId(R.id.nav_collections)).perform(click());
        assertToolbarTitle(R.string.nav_collections);

        onView(withId(R.id.nav_search)).perform(click());
        assertToolbarTitle(R.string.app_name);
    }

    @Test
    public void repeatedTabSwitching_keepsNavigationResponsive() {
        int[] tabs = {R.id.nav_favorites, R.id.nav_collections, R.id.nav_search};
        int[] titles = {R.string.nav_favorites, R.string.nav_collections, R.string.app_name};

        for (int cycle = 0; cycle < 4; cycle++) {
            for (int i = 0; i < tabs.length; i++) {
                onView(withId(tabs[i])).perform(click());
                assertToolbarTitle(titles[i]);
            }
        }
    }

    @Test
    public void selectedTab_isRestoredAfterScreenRotation() {
        onView(withId(R.id.nav_collections)).perform(click());
        assertToolbarTitle(R.string.nav_collections);

        final int[] originalOrientation = new int[1];
        scenario.onActivity(activity -> {
            int orientation = activity.getResources().getConfiguration().orientation;
            originalOrientation[0] = orientation;
            int requestedOrientation = orientation == Configuration.ORIENTATION_PORTRAIT
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            activity.setRequestedOrientation(requestedOrientation);
        });

        onView(withId(R.id.bottom_navigation)).check((view, noViewFoundException) ->
                assertEquals(R.id.nav_collections,
                        ((BottomNavigationView) view).getSelectedItemId()));
        assertToolbarTitle(R.string.nav_collections);

        scenario.onActivity(activity -> activity.setRequestedOrientation(
                originalOrientation[0] == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        onView(withId(R.id.bottom_navigation)).check((view, noViewFoundException) ->
                assertEquals(R.id.nav_collections,
                        ((BottomNavigationView) view).getSelectedItemId()));
    }

    @Test
    public void searchRetainedRows_surviveRecreationDuringLoading() {
        assertSearchRowsAfterRecreation(false);
    }

    @Test
    public void searchRetainedRows_surviveRecreationAfterPaginationError() {
        assertSearchRowsAfterRecreation(true);
    }

    private void assertSearchRowsAfterRecreation(boolean error) {
        scenario.onActivity(activity -> {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag("search");
            SearchViewModel model = new ViewModelProvider(fragment).get(SearchViewModel.class);
            try {
                // Detach the initial request so the retained state is deterministic without a live API.
                Field resultsField = model.getClass().getDeclaredField("searchResults");
                resultsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                MediatorLiveData<Resource<List<Photo>>> results =
                        (MediatorLiveData<Resource<List<Photo>>>) resultsField.get(model);
                Field sourceField = model.getClass().getDeclaredField("activePageSource");
                sourceField.setAccessible(true);
                LiveData<?> source = (LiveData<?>) sourceField.get(model);
                if (source != null) {
                    results.removeSource(source);
                    sourceField.set(model, null);
                }
                String url = "android.resource://" + activity.getPackageName() + "/" + R.drawable.ic_photo_placeholder;
                List<Photo> photos = Collections.singletonList(
                        new Photo(
                                "retained", "Retained", "Author", "", url, url, false, 0L));
                results.setValue(error
                        ? Resource.error("Timeout", photos)
                        : Resource.loading(photos));
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        });
        scenario.recreate();
        onView(withId(R.id.recycler_photos)).check((view, exception) -> {
            org.junit.Assert.assertNotNull(view);
            assertEquals(View.VISIBLE, view.getVisibility());
            assertEquals(1, ((RecyclerView) view).getAdapter().getItemCount());
        });
    }

    @Test
    public void imageActions_haveAtLeast48dpBounds() {
        scenario.onActivity(activity -> {
            int[][] targets = {
                    {R.layout.item_photo, R.id.button_item_favorite},
                    {R.layout.item_collection, R.id.button_collection_menu},
                    {R.layout.item_collection_photo, R.id.button_remove_from_collection},
                    {R.layout.activity_photo_detail, R.id.button_favorite}
            };
            float density = activity.getResources().getDisplayMetrics().density;
            for (int[] target : targets) {
                View root = activity.getLayoutInflater().inflate(target[0], null, false);
                root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(360 * density),
                                View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                View button = root.findViewById(target[1]);
                org.junit.Assert.assertTrue(button.getWidth() >= Math.round(48 * density));
                org.junit.Assert.assertTrue(button.getHeight() >= Math.round(48 * density));
            }
        });
    }

    private void assertToolbarTitle(int titleResId) {
        onView(withId(R.id.toolbar)).check((view, noViewFoundException) ->
                assertEquals(view.getResources().getString(titleResId),
                        ((MaterialToolbar) view).getTitle().toString()));
    }
}