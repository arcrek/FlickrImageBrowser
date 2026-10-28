package vn.edu.usth.flickrimagebrowser.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentFavoritesBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.common.JustifiedGridLayoutManager;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;

/**
 * Fragment displaying locally saved favorite photos in Flickr-style justified rows.
 * Filter query, sort order, and scroll anchor survive device rotation, and Undo
 * restores original savedAt timestamps.
 */
public class FavoritesFragment extends Fragment implements PhotoAdapter.OnPhotoActionListener {

    @Nullable
    private FragmentFavoritesBinding binding;

    private FavoritesViewModel viewModel;
    private PhotoAdapter photoAdapter;
    private JustifiedGridLayoutManager layoutManager;

    private final TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int count, int after) {
            if (viewModel != null) {
                viewModel.setFilterQuery(s != null ? s.toString() : "");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppViewModelFactory factory = ((FlickrApplication) requireActivity().getApplication())
                .getAppContainer()
                .getViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(FavoritesViewModel.class);

        photoAdapter = new PhotoAdapter(this, R.layout.item_favorite_photo);
        if (binding != null) {
            layoutManager = new JustifiedGridLayoutManager(requireContext());
            binding.recyclerFavorites.setLayoutManager(layoutManager);
            binding.recyclerFavorites.setAdapter(photoAdapter);

            // Restore active filter query across rotation
            String currentFilter = viewModel.getFilterQuery().getValue();
            if (currentFilter != null && !currentFilter.isEmpty()) {
                binding.editFavoriteSearch.setText(currentFilter);
                binding.editFavoriteSearch.setSelection(currentFilter.length());
            }

            // Restore active sort order across rotation
            FavoritesViewModel.SortOrder currentSort = viewModel.getSortOrder().getValue();
            if (currentSort == FavoritesViewModel.SortOrder.TITLE_ASC) {
                binding.chipSortAz.setChecked(true);
            } else {
                binding.chipSortNewest.setChecked(true);
            }

            binding.editFavoriteSearch.addTextChangedListener(searchWatcher);
            binding.buttonEmptyExplore.setOnClickListener(v -> {
                if (requireActivity() instanceof vn.edu.usth.flickrimagebrowser.MainActivity) {
                    ((vn.edu.usth.flickrimagebrowser.MainActivity) requireActivity()).navigateToSearch();
                }
            });
            binding.chipGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chip_sort_az) {
                    viewModel.setSortOrder(FavoritesViewModel.SortOrder.TITLE_ASC);
                } else {
                    viewModel.setSortOrder(FavoritesViewModel.SortOrder.SAVED_DATE_DESC);
                }
            });
        }

        viewModel.getFavorites().observe(getViewLifecycleOwner(), this::renderFavorites);
    }

    private void renderFavorites(@Nullable List<Photo> favorites) {
        if (binding == null) {
            return;
        }
        List<Photo> safeList = favorites != null ? favorites : Collections.emptyList();
        photoAdapter.submitList(safeList);
        boolean isEmpty = safeList.isEmpty();
        if (isEmpty) {
            boolean isFilter = viewModel.isFilterActive();
            binding.textEmptyFavorites.setText(
                    isFilter
                            ? R.string.favorites_filter_empty_state
                            : R.string.favorites_empty_state
            );
            binding.buttonEmptyExplore.setVisibility(isFilter ? View.GONE : View.VISIBLE);
            binding.layoutEmptyFavorites.setVisibility(View.VISIBLE);
            binding.recyclerFavorites.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyFavorites.setVisibility(View.GONE);
            binding.recyclerFavorites.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPhotoClicked(@NonNull Photo photo) {
        Intent intent = PhotoDetailActivity.newIntent(requireContext(), photo);
        if (PhotoAdapter.areAnimationsEnabled(requireContext())) {
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(
                    requireContext(),
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            ).toBundle();
            startActivity(intent, bundle);
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onFavoriteClicked(@NonNull Photo photo) {
        viewModel.removeFavorite(photo);

        if (binding == null) {
            return;
        }
        View anchor = requireActivity().findViewById(R.id.bottom_navigation);
        Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                R.string.favorites_removed_snackbar,
                Snackbar.LENGTH_LONG
        );
        snackbar.setAction(R.string.action_undo, v -> viewModel.restoreFavorite(photo));
        if (anchor != null) {
            snackbar.setAnchorView(anchor);
        }
        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.editFavoriteSearch.removeTextChangedListener(searchWatcher);
        }
        binding = null;
    }
}
