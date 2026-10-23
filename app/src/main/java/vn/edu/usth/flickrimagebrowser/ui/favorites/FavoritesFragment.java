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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentFavoritesBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;

/**
 * Fragment displaying locally saved favorite photos from Room.
 */
public class FavoritesFragment extends Fragment implements PhotoAdapter.OnPhotoActionListener {

    private static final int GRID_SPAN_COUNT = 2;

    @Nullable
    private FragmentFavoritesBinding binding;

    private FavoritesViewModel viewModel;
    private PhotoAdapter photoAdapter;

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

        photoAdapter = new PhotoAdapter(this);
        if (binding != null) {
            binding.recyclerFavorites.setLayoutManager(new GridLayoutManager(requireContext(), GRID_SPAN_COUNT));
            binding.recyclerFavorites.setAdapter(photoAdapter);
            binding.editFavoriteFilter.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setFilterQuery(s.toString());
                }
                @Override public void afterTextChanged(Editable s) { }
            });
            binding.buttonSortFavorites.setOnClickListener(v -> {
                viewModel.toggleSortOrder();
                updateSortButton();
            });
            updateSortButton();
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
        binding.textEmptyFavorites.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        boolean hasFilter = binding.editFavoriteFilter.getText() != null
                && binding.editFavoriteFilter.getText().length() > 0;
        binding.textEmptyFavorites.setText(hasFilter
                ? R.string.favorites_no_filter_match : R.string.favorites_empty_state);
        binding.recyclerFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPhotoClicked(@NonNull Photo photo) {
        Intent intent = PhotoDetailActivity.newIntent(requireContext(), photo);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClicked(@NonNull Photo photo) {
        viewModel.removeFavorite(photo);
        Snackbar.make(binding.getRoot(), R.string.favorite_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, v -> viewModel.restoreFavorite(photo))
                .show();
    }

    private void updateSortButton() {
        if (binding != null) {
            binding.buttonSortFavorites.setText(viewModel.isTitleAscending()
                    ? R.string.favorites_sort_title : R.string.favorites_sort_recent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
