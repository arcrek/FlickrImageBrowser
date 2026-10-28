package vn.edu.usth.flickrimagebrowser.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentSearchBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.common.JustifiedGridLayoutManager;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Fragment presenting top compact search input, Flickr-style justified rows,
 * Loading/Empty/Error states, and endless-scroll pagination without retry storms.
 */
public class SearchFragment extends Fragment implements PhotoAdapter.OnPhotoActionListener {

    private static final int PAGINATION_THRESHOLD = 4;

    private static final String[] RECOMMENDATION_PROMPTS = {
            "Mùa thu Hà Nội",
            "Vịnh Hạ Long",
            "Phố cổ Hội An",
            "Cầu Vàng Đà Nẵng",
            "Ruộng bậc thang Sa Pa",
            "Hoàng hôn Phú Quốc",
            "Chợ nổi Cái Răng",
            "Hoa anh đào Đà Lạt"
    };

    @Nullable
    private FragmentSearchBinding binding;

    private SearchViewModel viewModel;
    private PhotoAdapter photoAdapter;
    private JustifiedGridLayoutManager justifiedLayoutManager;
    private String currentRecommendation = "Mùa thu Hà Nội";

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

        photoAdapter = new PhotoAdapter(this, R.layout.item_photo);
        if (binding != null) {
            justifiedLayoutManager = new JustifiedGridLayoutManager(requireContext());
            binding.recyclerPhotos.setLayoutManager(justifiedLayoutManager);
            binding.recyclerPhotos.setAdapter(photoAdapter);

            binding.recyclerPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy <= 0) {
                        return;
                    }
                    int totalItemCount = justifiedLayoutManager.getItemCount();
                    int lastVisible = justifiedLayoutManager.findLastVisibleItemPosition();

                    if (lastVisible != RecyclerView.NO_POSITION
                            && lastVisible >= totalItemCount - PAGINATION_THRESHOLD
                            && totalItemCount > 0) {
                        if (!viewModel.hasFailedOnNextPage() && !viewModel.isLoadingNextPage()) {
                            viewModel.loadNextPage();
                        }
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
            binding.buttonRetryPagination.setOnClickListener(v -> viewModel.retryLastQuery());
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
        binding.recyclerPhotos.scrollToPosition(0);
        viewModel.submitQuery(query, false);
    }

    private void renderState(@Nullable Resource<List<Photo>> resource) {
        if (binding == null || resource == null) {
            return;
        }

        boolean hasExistingData = resource.getData() != null && !resource.getData().isEmpty();

        switch (resource.getStatus()) {
            case LOADING:
                if (!hasExistingData) {
                    binding.progressLoading.setVisibility(View.VISIBLE);
                    binding.recyclerPhotos.setVisibility(View.GONE);
                    binding.recyclerPhotos.scrollToPosition(0);
                    binding.layoutState.setVisibility(View.GONE);
                } else {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.recyclerPhotos.setVisibility(View.VISIBLE);
                    binding.layoutState.setVisibility(View.GONE);
                    photoAdapter.submitList(resource.getData());
                }
                binding.layoutPaginationError.setVisibility(View.GONE);
                break;

            case SUCCESS:
                binding.progressLoading.setVisibility(View.GONE);
                binding.layoutState.setVisibility(View.GONE);
                binding.recyclerPhotos.setVisibility(View.VISIBLE);
                binding.layoutPaginationError.setVisibility(View.GONE);
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
                binding.layoutPaginationError.setVisibility(View.GONE);
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
                    binding.layoutPaginationError.setVisibility(View.GONE);
                    photoAdapter.submitList(Collections.emptyList());
                } else {
                    binding.progressLoading.setVisibility(View.GONE);
                    binding.layoutState.setVisibility(View.GONE);
                    binding.recyclerPhotos.setVisibility(View.VISIBLE);
                    photoAdapter.submitList(resource.getData());
                    binding.layoutPaginationError.setVisibility(View.VISIBLE);
                    binding.textPaginationError.setText(
                            resource.getMessage() != null
                                    ? resource.getMessage()
                                    : getString(R.string.search_error_generic)
                    );
                }
                break;
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
        viewModel.toggleFavorite(photo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
