package vn.edu.usth.flickrimagebrowser;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import vn.edu.usth.flickrimagebrowser.databinding.ActivityMainBinding;
import vn.edu.usth.flickrimagebrowser.ui.collections.CollectionDetailFragment;
import vn.edu.usth.flickrimagebrowser.ui.collections.CollectionsFragment;
import vn.edu.usth.flickrimagebrowser.ui.favorites.FavoritesFragment;
import vn.edu.usth.flickrimagebrowser.ui.search.SearchFragment;

/**
 * Single-Activity host managing navigation between Search, Favorites, and Collections screens.
 */
public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_tab_id";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportFragmentManager().addOnBackStackChangedListener(
                () -> updateNavigationChrome(getSupportFragmentManager().getBackStackEntryCount() == 0));
        updateNavigationChrome(getSupportFragmentManager().getBackStackEntryCount() == 0);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setTitle(R.string.app_name);

        // Apply system window insets so top header and bottom nav fit all types of devices,
        // accounting for status bar, display cutouts (notches / camera punch-holes), and navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMain, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            boolean showingMainChrome = binding.toolbar.getVisibility() == android.view.View.VISIBLE;
            binding.toolbar.setPadding(insets.left, insets.top, insets.right, 0);
            binding.bottomNavigation.setPadding(insets.left, 0, insets.right, insets.bottom);
            binding.fragmentContainer.setPadding(
                    insets.left,
                    showingMainChrome ? 0 : insets.top,
                    insets.right,
                    showingMainChrome ? 0 : insets.bottom
            );
            return windowInsets;
        });

        binding.btnActionSparkle.setOnClickListener(v -> {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_search);
        });

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                return true;
            }
            int itemId = item.getItemId();
            if (itemId == R.id.nav_search) {
                switchFragment(new SearchFragment(), "search");
                updateToolbarForTab(itemId);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                switchFragment(new FavoritesFragment(), "favorites");
                updateToolbarForTab(itemId);
                return true;
            } else if (itemId == R.id.nav_collections) {
                switchFragment(new CollectionsFragment(), "collections");
                updateToolbarForTab(itemId);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_search);
        } else {
            int restoredId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_search);
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                if (binding.bottomNavigation.getSelectedItemId() != restoredId) {
                    binding.bottomNavigation.setSelectedItemId(restoredId);
                }
            }
            updateToolbarForTab(restoredId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putInt(KEY_SELECTED_TAB, binding.bottomNavigation.getSelectedItemId());
        }
    }

    private void switchFragment(@NonNull Fragment fragment, @NonNull String tag) {
        Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);
        if (existing != null && existing.isVisible()) {
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    /** Opens a collection's photo list inside the single-activity navigation host. */
    public void openCollectionDetail(long collectionId, @NonNull String collectionName) {
        Fragment detail = CollectionDetailFragment.newInstance(collectionId, collectionName);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detail, "collection_detail")
                .addToBackStack("collection_detail")
                .commit();
        updateNavigationChrome(false);
    }

    private void updateNavigationChrome(boolean showNavigation) {
        if (binding == null) {
            return;
        }
        int visibility = showNavigation ? android.view.View.VISIBLE : android.view.View.GONE;
        binding.toolbar.setVisibility(visibility);
        binding.toolbarDivider.setVisibility(visibility);
        binding.bottomDivider.setVisibility(visibility);
        binding.bottomNavigation.setVisibility(visibility);
        ViewCompat.requestApplyInsets(binding.rootMain);
    }

    private void updateToolbarForTab(int itemId) {
        if (itemId == R.id.nav_favorites) {
            binding.toolbarTitle.setText(R.string.nav_favorites);
            binding.toolbarTagline.setText(R.string.tab_saved);
            binding.toolbar.setTitle(R.string.nav_favorites);
        } else if (itemId == R.id.nav_collections) {
            binding.toolbarTitle.setText(R.string.nav_collections);
            binding.toolbarTagline.setText(R.string.tab_boards);
            binding.toolbar.setTitle(R.string.nav_collections);
        } else {
            binding.toolbarTitle.setText(R.string.brand_name_display);
            binding.toolbarTagline.setText(R.string.brand_tagline);
            binding.toolbar.setTitle(R.string.app_name);
        }
    }
}
