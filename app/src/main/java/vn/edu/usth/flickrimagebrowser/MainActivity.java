package vn.edu.usth.flickrimagebrowser;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportFragmentManager().addOnBackStackChangedListener(
                () -> updateNavigationChrome(getSupportFragmentManager().getBackStackEntryCount() == 0));
        updateNavigationChrome(getSupportFragmentManager().getBackStackEntryCount() == 0);

        setSupportActionBar(binding.toolbar);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Keep the detail back-stack screen active while its bottom navigation is hidden.
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                return true;
            }
            int itemId = item.getItemId();
            if (itemId == R.id.nav_search) {
                switchFragment(new SearchFragment(), "search");
                updateToolbarTitle(R.string.app_name);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                switchFragment(new FavoritesFragment(), "favorites");
                updateToolbarTitle(R.string.nav_favorites);
                return true;
            } else if (itemId == R.id.nav_collections) {
                switchFragment(new CollectionsFragment(), "collections");
                updateToolbarTitle(R.string.nav_collections);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_search);
        } else {
            int restoredId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_search);
            binding.bottomNavigation.setSelectedItemId(restoredId);
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
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existing = fragmentManager.findFragmentByTag(tag);
        if (existing != null && existing.isVisible()) {
            return;
        }
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment current : fragmentManager.getFragments()) {
            if (current.getId() == R.id.fragment_container && !current.isHidden()) {
                transaction.hide(current);
            }
        }
        if (existing == null) {
            transaction.add(R.id.fragment_container, fragment, tag);
        } else {
            transaction.show(existing);
        }
        transaction.setReorderingAllowed(true).commit();
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
        binding.bottomNavigation.setVisibility(visibility);
    }

    private void updateToolbarForTab(int itemId) {
        if (itemId == R.id.nav_favorites) {
            updateToolbarTitle(R.string.nav_favorites);
        } else if (itemId == R.id.nav_collections) {
            updateToolbarTitle(R.string.nav_collections);
        } else {
            updateToolbarTitle(R.string.app_name);
        }
    }

    private void updateToolbarTitle(int titleResId) {
        if (binding != null) {
            binding.toolbar.setTitle(titleResId);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleResId);
        }
    }
}
