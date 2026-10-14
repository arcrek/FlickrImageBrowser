package vn.edu.usth.flickrimagebrowser.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
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
        binding.recyclerFavorites.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPhotoClicked(@NonNull Photo photo) {
        Intent intent = PhotoDetailActivity.newIntent(requireContext(), photo);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClicked(@NonNull Photo photo) {
        viewModel.toggleFavorite(photo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
