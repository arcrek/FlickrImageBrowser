package vn.edu.usth.flickrimagebrowser;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented regression coverage for tab navigation, retained fragment lifecycle state,
 * and collection detail backstack restoration for both system Back and toolbar Back.
 */
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
    public void bottomNavigation_switchesScreensAndMaintainsBehavior() {
        assertSelectedTab(R.id.nav_search);
        assertChromeVisible();

        onView(withId(R.id.nav_favorites)).perform(click());
        assertSelectedTab(R.id.nav_favorites);
        assertChromeVisible();

        onView(withId(R.id.nav_collections)).perform(click());
        assertSelectedTab(R.id.nav_collections);
        assertChromeVisible();

        onView(withId(R.id.nav_search)).perform(click());
        assertSelectedTab(R.id.nav_search);
        assertChromeVisible();
    }

    @Test
    public void repeatedTabSwitching_keepsNavigationResponsive() {
        int[] tabs = {R.id.nav_favorites, R.id.nav_collections, R.id.nav_search};

        for (int cycle = 0; cycle < 3; cycle++) {
            for (int tabId : tabs) {
                onView(withId(tabId)).perform(click());
                assertSelectedTab(tabId);
                assertChromeVisible();
            }
        }
    }

    @Test
    public void tabUserState_isRetainedAcrossTabSwitchesAndRecreate() {
        // Switch to Favorites tab
        onView(withId(R.id.nav_favorites)).perform(click());
        assertSelectedTab(R.id.nav_favorites);

        // Enter filter query and select A-Z sorting chip
        onView(withId(R.id.edit_favorite_search)).perform(typeText("Sunset"), closeSoftKeyboard());
        onView(withId(R.id.chip_sort_az)).perform(click());

        // Verify initial entered state
        onView(withId(R.id.edit_favorite_search)).check(matches(withText("Sunset")));
        onView(withId(R.id.chip_sort_az)).check(matches(isChecked()));

        // Switch to Search tab
        onView(withId(R.id.nav_search)).perform(click());
        assertSelectedTab(R.id.nav_search);

        // Switch to Collections tab
        onView(withId(R.id.nav_collections)).perform(click());
        assertSelectedTab(R.id.nav_collections);

        // Switch back to Favorites tab
        onView(withId(R.id.nav_favorites)).perform(click());
        assertSelectedTab(R.id.nav_favorites);

        // Assert query text and checked chip survived tab switches
        onView(withId(R.id.edit_favorite_search)).check(matches(withText("Sunset")));
        onView(withId(R.id.chip_sort_az)).check(matches(isChecked()));

        // Recreate activity to simulate process restoration/configuration lifecycle
        scenario.recreate();

        // Assert query text and checked chip survived recreation
        assertSelectedTab(R.id.nav_favorites);
        onView(withId(R.id.edit_favorite_search)).check(matches(withText("Sunset")));
        onView(withId(R.id.chip_sort_az)).check(matches(isChecked()));
    }

    @Test
    public void selectedTab_isRestoredAfterScreenRotation() {
        onView(withId(R.id.nav_collections)).perform(click());
        assertSelectedTab(R.id.nav_collections);

        final int[] originalOrientation = new int[1];
        scenario.onActivity(activity -> {
            int orientation = activity.getResources().getConfiguration().orientation;
            originalOrientation[0] = orientation;
            int requestedOrientation = orientation == Configuration.ORIENTATION_PORTRAIT
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            activity.setRequestedOrientation(requestedOrientation);
        });

        assertSelectedTab(R.id.nav_collections);
        assertChromeVisible();

        scenario.onActivity(activity -> activity.setRequestedOrientation(
                originalOrientation[0] == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));

        assertSelectedTab(R.id.nav_collections);
        assertChromeVisible();
    }

    @Test
    public void systemBack_fromCollectionDetail_restoresToolbarAndNavigation() {
        // Start on Collections tab
        onView(withId(R.id.nav_collections)).perform(click());
        assertSelectedTab(R.id.nav_collections);

        // Open collection detail
        scenario.onActivity(activity ->
                activity.openCollectionDetail(101L, "Sample Album"));

        // Verify chrome is hidden while inside detail screen
        assertChromeGone();
        scenario.onActivity(activity ->
                assertEquals(1, activity.getSupportFragmentManager().getBackStackEntryCount()));

        // Trigger system Back (exercises KEYCODE_BACK and OnBackPressedDispatcher callback integration;
        // note: predictive back gesture animation lifecycle is verified during manual/AVD integration).
        pressBack();

        // Verify chrome is completely restored
        assertChromeVisible();
        assertSelectedTab(R.id.nav_collections);
        scenario.onActivity(activity ->
                assertEquals(0, activity.getSupportFragmentManager().getBackStackEntryCount()));
    }

    @Test
    public void toolbarBack_fromCollectionDetail_restoresToolbarAndNavigation() {
        // Start on Collections tab
        onView(withId(R.id.nav_collections)).perform(click());
        assertSelectedTab(R.id.nav_collections);

        // Open collection detail
        scenario.onActivity(activity ->
                activity.openCollectionDetail(202L, "Nature Album"));

        // Verify chrome is hidden
        assertChromeGone();

        // Tap the back button on the collection detail toolbar
        onView(withContentDescription(R.string.action_back)).perform(click());

        // Verify chrome is completely restored
        assertChromeVisible();
        assertSelectedTab(R.id.nav_collections);
        scenario.onActivity(activity ->
                assertEquals(0, activity.getSupportFragmentManager().getBackStackEntryCount()));
    }

    @Test
    public void navigateToSearch_fromNestedCollectionDetail_popsBackstackAndSelectsSearch() {
        // Open collection detail
        scenario.onActivity(activity ->
                activity.openCollectionDetail(303L, "Urban Album"));

        assertChromeGone();

        // Call navigateToSearch()
        scenario.onActivity(MainActivity::navigateToSearch);

        // Verify backstack popped and Search tab selected with chrome visible
        assertChromeVisible();
        assertSelectedTab(R.id.nav_search);
        scenario.onActivity(activity ->
                assertEquals(0, activity.getSupportFragmentManager().getBackStackEntryCount()));
    }

    private void assertSelectedTab(int expectedTabId) {
        scenario.onActivity(activity -> {
            BottomNavigationView nav = activity.findViewById(R.id.bottom_navigation);
            assertNotNull(nav);
            assertEquals("Expected selected tab id does not match", expectedTabId, nav.getSelectedItemId());
        });
    }

    private void assertChromeVisible() {
        onView(withId(R.id.toolbar)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.bottom_navigation)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    private void assertChromeGone() {
        onView(withId(R.id.toolbar)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.bottom_navigation)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }
}
