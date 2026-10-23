package vn.edu.usth.flickrimagebrowser.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

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
import vn.edu.usth.flickrimagebrowser.databinding.FragmentSearchBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Fragment allowing users to search public Flickr photos and view Loading/Empty/Error/Retry states.
 */
public class SearchFragment extends Fragment implements PhotoAdapter.OnPhotoActionListener {

    private static final int GRID_SPAN_COUNT = 2;

    @Nullable
    private FragmentSearchBinding binding;

    private SearchViewModel viewModel;
    private PhotoAdapter photoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppViewModelFactory factory = ((FlickrApplication) requireActivity().getApplication())
                .getAppContainer()
                .getViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(SearchViewModel.class);

        photoAdapter = new PhotoAdapter(this);
        if (binding != null) {
            binding.recyclerPhotos.setLayoutManager(new GridLayoutManager(requireContext(), GRID_SPAN_COUNT));
            binding.recyclerPhotos.setAdapter(photoAdapter);
            binding.recyclerPhotos.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView,
                                       int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0 && recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                        GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
                        int total = photoAdapter.getItemCount();
                        if (total > 0 && manager.findLastVisibleItemPosition() >= total - 6) {
                            viewModel.loadNextPage();
                        }
                    }
                }
            });

            binding.buttonSearch.setOnClickListener(v -> triggerSearch());
            binding.buttonRetry.setOnClickListener(v -> viewModel.retryLastQuery());
            binding.editQuery.setOnEditorActionListener((textView, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    triggerSearch();
                    return true;
                }
                return false;
            });
        }

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.isLoadingMore().observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) {
                binding.progressLoadMore.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void triggerSearch() {
        if (binding == null || binding.editQuery.getText() == null) {
            return;
        }
        String query = binding.editQuery.getText().toString();
        viewModel.submitQuery(query, false);
    }

    private void renderState(@Nullable Resource<List<Photo>> resource) {
        if (binding == null || resource == null) {
            return;
        }

        switch (resource.getStatus()) {
            case LOADING:
                binding.progressLoading.setVisibility(View.VISIBLE);
                binding.recyclerPhotos.setVisibility(View.GONE);
                binding.layoutState.setVisibility(View.GONE);
                break;

            case SUCCESS:
                binding.progressLoading.setVisibility(View.GONE);
                binding.layoutState.setVisibility(View.GONE);
                binding.recyclerPhotos.setVisibility(View.VISIBLE);
                photoAdapter.submitList(
                        resource.getData() != null ? resource.getData() : Collections.emptyList()
                );
                break;

            case EMPTY:
                binding.progressLoading.setVisibility(View.GONE);
                binding.recyclerPhotos.setVisibility(View.GONE);
                binding.layoutState.setVisibility(View.VISIBLE);
                binding.textStateMessage.setText(R.string.search_empty_state);
                binding.buttonRetry.setVisibility(View.GONE);
                photoAdapter.submitList(Collections.emptyList());
                break;

            case ERROR:
                binding.progressLoading.setVisibility(View.GONE);
                if (resource.getData() != null && !resource.getData().isEmpty()) {
                    binding.recyclerPhotos.setVisibility(View.VISIBLE);
                    binding.layoutState.setVisibility(View.GONE);
                    photoAdapter.submitList(resource.getData());
                    Snackbar.make(binding.getRoot(),
                                    resource.getMessage() != null ? resource.getMessage() : getString(R.string.action_retry),
                                    Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_retry, v -> viewModel.loadNextPage())
                            .show();
                } else {
                    binding.recyclerPhotos.setVisibility(View.GONE);
                    binding.layoutState.setVisibility(View.VISIBLE);
                    binding.textStateMessage.setText(
                            resource.getMessage() != null
                                    ? resource.getMessage()
                                    : getString(R.string.search_empty_state)
                    );
                    binding.buttonRetry.setVisibility(View.VISIBLE);
                    photoAdapter.submitList(Collections.emptyList());
                }
                break;
        }
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
