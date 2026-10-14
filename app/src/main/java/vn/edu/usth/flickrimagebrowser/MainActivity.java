package vn.edu.usth.flickrimagebrowser;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import vn.edu.usth.flickrimagebrowser.databinding.ActivityMainBinding;
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

        setSupportActionBar(binding.toolbar);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_search) {
                switchFragment(new SearchFragment(), "search");
                binding.toolbar.setTitle(R.string.app_name);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                switchFragment(new FavoritesFragment(), "favorites");
                binding.toolbar.setTitle(R.string.nav_favorites);
                return true;
            } else if (itemId == R.id.nav_collections) {
                switchFragment(new CollectionsFragment(), "collections");
                binding.toolbar.setTitle(R.string.nav_collections);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_search);
        } else {
            int restoredId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_search);
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

    private void updateToolbarForTab(int itemId) {
        if (itemId == R.id.nav_favorites) {
            binding.toolbar.setTitle(R.string.nav_favorites);
        } else if (itemId == R.id.nav_collections) {
            binding.toolbar.setTitle(R.string.nav_collections);
        } else {
            binding.toolbar.setTitle(R.string.app_name);
        }
    }
}
