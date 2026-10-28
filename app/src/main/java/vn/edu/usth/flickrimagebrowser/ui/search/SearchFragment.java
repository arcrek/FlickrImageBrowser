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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentSearchBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Fragment presenting search input, a Pinterest-style 2-column staggered photo grid,
 * Loading/Empty/Error states, and endless-scroll pagination.
 */
public class SearchFragment extends Fragment implements PhotoAdapter.OnPhotoActionListener {

    private static final int GRID_SPAN_COUNT = 2;
    private static final int PAGINATION_THRESHOLD = 4;

    private static final String[] RECOMMENDATION_PROMPTS = {
            "Mùa thu Hà Nội",
            "Cyberpunk Tokyo",
            "Minimalist Architecture",
            "Hồ Gươm hoàng hôn",
            "Mùa hoa cúc họa mi",
            "Vintage Film Photography",
            "Cappuccino & Rainy Morning",
            "Northern Lights Iceland",
            "Street Food Saigon",
            "Deep Ocean Coral Reef",
            "Cozy Alpine Cabin",
            "Sunset over Santorini"
    };

    @Nullable
    private FragmentSearchBinding binding;

    @Nullable
    private Snackbar paginationErrorSnackbar;

    private SearchViewModel viewModel;
    private PhotoAdapter photoAdapter;
    private String currentRecommendation = "Mùa thu Hà Nội";

    private final int[] lastPositionsReuse = new int[GRID_SPAN_COUNT];

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
            StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                    GRID_SPAN_COUNT, StaggeredGridLayoutManager.VERTICAL
            );
            layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            binding.recyclerPhotos.setLayoutManager(layoutManager);
            binding.recyclerPhotos.setAdapter(photoAdapter);

            binding.recyclerPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy <= 0) {
                        return;
                    }
                    int totalItemCount = layoutManager.getItemCount();
                    int[] lastPositions = layoutManager.findLastVisibleItemPositions(lastPositionsReuse);
                    int maxLastVisible = 0;
                    if (lastPositions != null) {
                        for (int pos : lastPositions) {
                            if (pos > maxLastVisible) {
                                maxLastVisible = pos;
                            }
                        }
                    }

                    if (maxLastVisible >= totalItemCount - PAGINATION_THRESHOLD && totalItemCount > 0) {
                        viewModel.loadNextPage();
                    }
                }
            });

            // Random recommendation banner per session
            int randomIndex = new Random().nextInt(RECOMMENDATION_PROMPTS.length);
            currentRecommendation = RECOMMENDATION_PROMPTS[randomIndex];
            binding.textRecommend.setText(getString(R.string.recommend_format, currentRecommendation));
            binding.layoutRecommend.setOnClickListener(v -> {
                binding.editQuery.setText(currentRecommendation);
                binding.editQuery.setSelection(currentRecommendation.length());
                triggerSearch();
            });

            binding.buttonSearch.setOnClickListener(v -> triggerSearch());
            binding.buttonRetry.setOnClickListener(v -> viewModel.retryLastQuery());
            binding.inputLayoutQuery.setEndIconOnClickListener(v -> {
                binding.editQuery.setText("");
                viewModel.submitQuery("", false);
            });
            binding.editQuery.setOnEditorActionListener((textView, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    triggerSearch();
                    return true;
                }
                return false;
            });
        }

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getIsLoadingMore().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null) {
                binding.progressPagination.setVisibility(
                        Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE
                );
            }
        });
        viewModel.loadInitialFeaturedIfNeeded();
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
        dismissPaginationError();

        boolean hasExistingData = resource.getData() != null && !resource.getData().isEmpty();

        switch (resource.getStatus()) {
            case LOADING:
                if (!hasExistingData) {
                    binding.progressLoading.setVisibility(View.VISIBLE);
                    binding.recyclerPhotos.setVisibility(View.GONE);
                    binding.layoutState.setVisibility(View.GONE);
                } else {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.recyclerPhotos.setVisibility(View.VISIBLE);
                    binding.layoutState.setVisibility(View.GONE);
                }
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
                if (!hasExistingData) {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.recyclerPhotos.setVisibility(View.GONE);
                    binding.layoutState.setVisibility(View.VISIBLE);
                    binding.textStateMessage.setText(R.string.search_empty_state);
                    binding.buttonRetry.setVisibility(View.GONE);
                    photoAdapter.submitList(Collections.emptyList());
                }
                break;

            case ERROR:
                if (!hasExistingData) {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.recyclerPhotos.setVisibility(View.GONE);
                    binding.layoutState.setVisibility(View.VISIBLE);
                    binding.textStateMessage.setText(
                            resource.getMessage() != null
                                    ? resource.getMessage()
                                    : getString(R.string.search_empty_state)
                    );
                    binding.buttonRetry.setVisibility(View.VISIBLE);
                    photoAdapter.submitList(Collections.emptyList());
                } else {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.layoutState.setVisibility(View.GONE);
                    binding.recyclerPhotos.setVisibility(View.VISIBLE);
                    paginationErrorSnackbar = Snackbar.make(
                            binding.getRoot(),
                            resource.getMessage() != null ? resource.getMessage() : "Error loading more photos",
                            Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.action_retry, v -> viewModel.retryLastQuery());
                    paginationErrorSnackbar.show();
                }
                break;
        }
    }

    private void dismissPaginationError() {
        if (paginationErrorSnackbar != null) {
            paginationErrorSnackbar.dismiss();
            paginationErrorSnackbar = null;
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
        dismissPaginationError();
        super.onDestroyView();
        binding = null;
    }
}
