package vn.edu.usth.flickrimagebrowser;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private void assertToolbarTitle(int titleResId) {
        onView(withId(R.id.toolbar)).check((view, noViewFoundException) ->
                assertEquals(view.getResources().getString(titleResId),
                        ((MaterialToolbar) view).getTitle().toString()));
    }
}
