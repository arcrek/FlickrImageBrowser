package vn.edu.usth.flickrimagebrowser;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import vn.edu.usth.flickrimagebrowser.databinding.ActivityMainBinding;
import vn.edu.usth.flickrimagebrowser.ui.collections.CollectionDetailFragment;
import vn.edu.usth.flickrimagebrowser.ui.collections.CollectionsFragment;
import vn.edu.usth.flickrimagebrowser.ui.favorites.FavoritesFragment;
import vn.edu.usth.flickrimagebrowser.ui.search.SearchFragment;

/**
 * Single-Activity host managing navigation between Search, Favorites, and Collections screens.
 * Retains tab fragment instances using show/hide to preserve UI and scroll state across tab switches and rotations.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG_SEARCH = "search";
    public static final String TAG_FAVORITES = "favorites";
    public static final String TAG_COLLECTIONS = "collections";
    public static final String TAG_COLLECTION_DETAIL = "collection_detail";

    private static final String KEY_SELECTED_TAB = "selected_tab_id";

    @Nullable
    private ActivityMainBinding binding;
    private int currentTabId = R.id.nav_search;
    private boolean isRestoringState = false;
    private boolean isOpeningCollectionDetail = false;

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            FragmentManager fm = getSupportFragmentManager();
            if (!fm.isStateSaved() && fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate();
            }
            syncChrome();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setTitle(R.string.app_name);

        getOnBackPressedDispatcher().addCallback(this, backCallback);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            isOpeningCollectionDetail = false;
            syncChrome();
        });

        setupWindowInsets();

        binding.btnActionSparkle.setOnClickListener(v -> navigateToSearch());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (isRestoringState) {
                return true;
            }

            FragmentManager fm = getSupportFragmentManager();
            if (fm.isStateSaved()) {
                return false;
            }

            int itemId = item.getItemId();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            if (itemId == currentTabId && fm.getBackStackEntryCount() == 0) {
                return true;
            }

            selectTab(itemId);
            return true;
        });

        if (savedInstanceState == null) {
            currentTabId = R.id.nav_search;
            Fragment search = getSupportFragmentManager().findFragmentByTag(TAG_SEARCH);
            if (search == null) {
                search = new SearchFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, search, TAG_SEARCH)
                        .setMaxLifecycle(search, Lifecycle.State.RESUMED)
                        .commitNow();
            }
            binding.bottomNavigation.setSelectedItemId(R.id.nav_search);
            updateToolbarForTab(R.id.nav_search);
        } else {
            isRestoringState = true;
            currentTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_search);
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                restoreTabFragments(currentTabId);
            }

            binding.bottomNavigation.setSelectedItemId(currentTabId);
            updateToolbarForTab(currentTabId);
            isRestoringState = false;
        }

        syncChrome();
    }

    private void setupWindowInsets() {
        if (binding == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMain, (view, windowInsets) -> {
            if (binding == null) {
                return windowInsets;
            }

            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            boolean isImeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime()) || ime.bottom > 0;
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            Fragment detail = getSupportFragmentManager().findFragmentByTag(TAG_COLLECTION_DETAIL);
            boolean isDetailActive = backStackCount > 0
                    || (detail != null && detail.isAdded() && !detail.isHidden() && detail.isVisible());

            if (isDetailActive) {
                binding.toolbar.setVisibility(View.GONE);
                binding.toolbarDivider.setVisibility(View.GONE);
                binding.bottomNavigation.setVisibility(View.GONE);
                binding.bottomDivider.setVisibility(View.GONE);
            } else {
                binding.toolbar.setVisibility(View.VISIBLE);
                binding.toolbarDivider.setVisibility(View.VISIBLE);
                if (isImeVisible) {
                    binding.bottomNavigation.setVisibility(View.GONE);
                    binding.bottomDivider.setVisibility(View.GONE);
                } else {
                    binding.bottomNavigation.setVisibility(View.VISIBLE);
                    binding.bottomDivider.setVisibility(View.VISIBLE);
                }
            }

            binding.toolbar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            binding.bottomNavigation.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            int bottomPadding;
            if (isDetailActive) {
                bottomPadding = Math.max(systemBars.bottom, ime.bottom);
            } else if (isImeVisible) {
                bottomPadding = ime.bottom;
            } else {
                bottomPadding = 0;
            }

            int topPadding = isDetailActive ? systemBars.top : 0;

            binding.fragmentContainer.setPadding(
                    systemBars.left,
                    topPadding,
                    systemBars.right,
                    bottomPadding
            );

            return windowInsets;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, currentTabId);
    }

    private void selectTab(int itemId) {
        if (getSupportFragmentManager().isStateSaved()) {
            return;
        }

        String targetTag = getTagForId(itemId);
        String currentTag = getTagForId(currentTabId);
        if (targetTag == null || currentTag == null) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFrag = fm.findFragmentByTag(currentTag);
        Fragment targetFrag = fm.findFragmentByTag(targetTag);

        if (currentFrag == targetFrag && targetFrag != null && targetFrag.isVisible()) {
            return;
        }

        FragmentTransaction ft = fm.beginTransaction();
        if (areAnimationsEnabled(this)) {
            ft.setCustomAnimations(R.anim.nav_enter, R.anim.nav_exit);
        }

        if (currentFrag != null && currentFrag.isAdded()) {
            ft.hide(currentFrag);
            ft.setMaxLifecycle(currentFrag, Lifecycle.State.STARTED);
        }

        if (targetFrag == null) {
            targetFrag = createTabFragment(targetTag);
            ft.add(R.id.fragment_container, targetFrag, targetTag);
        } else {
            ft.show(targetFrag);
        }
        ft.setMaxLifecycle(targetFrag, Lifecycle.State.RESUMED);

        // Synchronous commit for root tabs without backstack
        ft.commitNow();

        currentTabId = itemId;
        updateToolbarForTab(itemId);
    }

    private void restoreTabFragments(int selectedTabId) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        String activeTag = getTagForId(selectedTabId);
        if (activeTag == null) {
            activeTag = TAG_SEARCH;
        }

        FragmentTransaction ft = fm.beginTransaction();
        String[] tags = {TAG_SEARCH, TAG_FAVORITES, TAG_COLLECTIONS};
        for (String tag : tags) {
            Fragment f = fm.findFragmentByTag(tag);
            if (f != null && f.isAdded()) {
                if (tag.equals(activeTag)) {
                    ft.show(f);
                    ft.setMaxLifecycle(f, Lifecycle.State.RESUMED);
                } else {
                    ft.hide(f);
                    ft.setMaxLifecycle(f, Lifecycle.State.STARTED);
                }
            }
        }

        Fragment activeFrag = fm.findFragmentByTag(activeTag);
        if (activeFrag == null) {
            activeFrag = createTabFragment(activeTag);
            ft.add(R.id.fragment_container, activeFrag, activeTag);
            ft.setMaxLifecycle(activeFrag, Lifecycle.State.RESUMED);
        }

        ft.commitNow();
    }

    @NonNull
    private Fragment createTabFragment(@NonNull String tag) {
        if (TAG_FAVORITES.equals(tag)) {
            return new FavoritesFragment();
        } else if (TAG_COLLECTIONS.equals(tag)) {
            return new CollectionsFragment();
        } else {
            return new SearchFragment();
        }
    }

    @Nullable
    private String getTagForId(int itemId) {
        if (itemId == R.id.nav_search) {
            return TAG_SEARCH;
        } else if (itemId == R.id.nav_favorites) {
            return TAG_FAVORITES;
        } else if (itemId == R.id.nav_collections) {
            return TAG_COLLECTIONS;
        }
        return null;
    }

    /** Opens a collection's photo list inside the single-activity navigation host. */
    public void openCollectionDetail(long collectionId, @NonNull String collectionName) {
        if (isOpeningCollectionDetail || getSupportFragmentManager().isStateSaved()) {
            return;
        }
        if (getSupportFragmentManager().findFragmentByTag(TAG_COLLECTION_DETAIL) != null
                || getSupportFragmentManager().getBackStackEntryCount() > 0) {
            return;
        }
        isOpeningCollectionDetail = true;

        FragmentManager fm = getSupportFragmentManager();
        String currentTag = getTagForId(currentTabId);
        Fragment currentFrag = currentTag != null ? fm.findFragmentByTag(currentTag) : null;

        Fragment detail = CollectionDetailFragment.newInstance(collectionId, collectionName);
        FragmentTransaction ft = fm.beginTransaction();
        if (areAnimationsEnabled(this)) {
            ft.setCustomAnimations(R.anim.nav_enter, R.anim.nav_exit, R.anim.nav_pop_enter, R.anim.nav_pop_exit);
        }
        if (currentFrag != null && currentFrag.isAdded()) {
            ft.hide(currentFrag);
            ft.setMaxLifecycle(currentFrag, Lifecycle.State.STARTED);
        }
        ft.add(R.id.fragment_container, detail, TAG_COLLECTION_DETAIL);
        ft.addToBackStack(TAG_COLLECTION_DETAIL);
        ft.commit();
        fm.executePendingTransactions();

        syncChrome();
    }

    /**
     * Navigates directly to the Search tab, popping any nested detail screens.
     * Exposed for empty state call-to-actions.
     */
    public void navigateToSearch() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) {
            return;
        }
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (binding != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_search);
        }
    }

    /** Pure chrome UI synchronization without initiating fragment transactions. */
    private void syncChrome() {
        if (isFinishing() || isDestroyed() || binding == null) {
            return;
        }

        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        Fragment detail = getSupportFragmentManager().findFragmentByTag(TAG_COLLECTION_DETAIL);

        boolean showingDetail = backStackCount > 0
                || (detail != null && detail.isAdded() && !detail.isHidden() && detail.isVisible());

        boolean showChrome = !showingDetail;

        int visibility = showChrome ? View.VISIBLE : View.GONE;
        binding.toolbar.setVisibility(visibility);
        binding.toolbarDivider.setVisibility(visibility);
        binding.bottomDivider.setVisibility(visibility);
        binding.bottomNavigation.setVisibility(visibility);

        backCallback.setEnabled(showingDetail);

        if (showChrome) {
            updateToolbarForTab(currentTabId);
        }

        ViewCompat.requestApplyInsets(binding.rootMain);
    }

    private void updateToolbarForTab(int itemId) {
        if (binding == null) {
            return;
        }
        if (itemId == R.id.nav_favorites) {
            binding.toolbarTitle.setText(R.string.nav_favorites);
            binding.toolbarTagline.setText(R.string.tab_saved);
            binding.toolbar.setTitle(R.string.nav_favorites);
            binding.btnActionSparkle.setVisibility(View.GONE);
        } else if (itemId == R.id.nav_collections) {
            binding.toolbarTitle.setText(R.string.nav_collections);
            binding.toolbarTagline.setText(R.string.tab_boards);
            binding.toolbar.setTitle(R.string.nav_collections);
            binding.btnActionSparkle.setVisibility(View.VISIBLE);
        } else {
            binding.toolbarTitle.setText(R.string.brand_name_display);
            binding.toolbarTagline.setText(R.string.brand_tagline);
            binding.toolbar.setTitle(R.string.app_name);
            binding.btnActionSparkle.setVisibility(View.GONE);
        }
    }

    private static boolean areAnimationsEnabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        try {
            float durationScale = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f);
            return durationScale > 0.0f;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
