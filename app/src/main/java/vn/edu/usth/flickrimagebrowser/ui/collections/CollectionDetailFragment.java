package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentCollectionDetailBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.common.JustifiedGridLayoutManager;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Fragment displaying photos in an album using a Flickr-style justified photo grid.
 * Supports adding photos via a multi-select Favorites picker, multi-select removal
 * with visual selection overlays and accurate destructive confirmation, description display,
 * and resilient async result observation.
 */
public class CollectionDetailFragment extends Fragment
        implements PhotoAdapter.OnPhotoActionListener, Toolbar.OnMenuItemClickListener {

    public static final String ARG_COLLECTION_ID = "arg_collection_id";
    public static final String ARG_COLLECTION_NAME = "arg_collection_name";

    private static final String STATE_SELECTION_MODE = "state_selection_mode";
    private static final String STATE_SELECTED_IDS = "state_selected_ids";

    @Nullable
    private FragmentCollectionDetailBinding binding;

    private CollectionDetailViewModel viewModel;
    private AlbumJustifiedPhotoAdapter photoAdapter;
    private JustifiedGridLayoutManager justifiedLayoutManager;

    private long collectionId = -1L;
    private String collectionName = "";
    private String collectionDescription = "";

    private boolean isSelectionMode = false;
    private final Set<String> selectedPhotoIds = new HashSet<>();
    private List<Photo> currentPhotosList = new ArrayList<>();
    private boolean isBusy = false;
    private androidx.activity.OnBackPressedCallback onBackPressedCallback;

    @NonNull
    public static CollectionDetailFragment newInstance(long collectionId,
                                                       @NonNull String collectionName) {
        CollectionDetailFragment fragment = new CollectionDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_COLLECTION_ID, collectionId);
        args.putString(ARG_COLLECTION_NAME, collectionName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCollectionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppViewModelFactory factory = ((FlickrApplication) requireActivity().getApplication())
                .getAppContainer()
                .getViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(CollectionDetailViewModel.class);

        Bundle args = getArguments();
        collectionId = args != null ? args.getLong(ARG_COLLECTION_ID, -1L) : -1L;
        collectionName = args != null ? args.getString(ARG_COLLECTION_NAME, "") : "";

        if (binding != null) {
            // Toolbar setup
            binding.toolbarCollectionDetail.setTitle(collectionName);
            binding.toolbarCollectionDetail.setNavigationIcon(R.drawable.ic_arrow_back);
            binding.toolbarCollectionDetail.setNavigationOnClickListener(v -> handleBackNavigation());
            binding.toolbarCollectionDetail.inflateMenu(R.menu.menu_collection_detail);
            binding.toolbarCollectionDetail.setOnMenuItemClickListener(this);

            // Justified Grid Layout & AlbumJustifiedPhotoAdapter
            justifiedLayoutManager = new JustifiedGridLayoutManager(requireContext());
            binding.recyclerCollectionPhotos.setLayoutManager(justifiedLayoutManager);
            photoAdapter = new AlbumJustifiedPhotoAdapter(this);
            binding.recyclerCollectionPhotos.setAdapter(photoAdapter);

            // Selection Bar Actions
            binding.buttonCancelSelection.setOnClickListener(v -> exitSelectionMode());
            binding.buttonConfirmRemove.setOnClickListener(v -> confirmRemoveSelectedPhotos());
        }

        // Listen for Favorites picker results across recreation
        getChildFragmentManager().setFragmentResultListener(
                FavoritesPickerDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    @SuppressWarnings("unchecked")
                    ArrayList<Photo> selectedPhotos = (ArrayList<Photo>)
                            bundle.getSerializable(FavoritesPickerDialogFragment.RESULT_SELECTED_PHOTOS);
                    if (selectedPhotos != null && !selectedPhotos.isEmpty()) {
                        addPhotosToAlbum(selectedPhotos);
                    }
                }
        );

        // Restore selection state across orientation change
        if (savedInstanceState != null) {
            isSelectionMode = savedInstanceState.getBoolean(STATE_SELECTION_MODE, false);
            ArrayList<String> savedIds = savedInstanceState.getStringArrayList(STATE_SELECTED_IDS);
            if (savedIds != null) {
                selectedPhotoIds.addAll(savedIds);
            }
            if (isSelectionMode) {
                photoAdapter.setSelectionMode(true);
                photoAdapter.setSelectedIds(selectedPhotoIds);
                if (binding != null) {
                    binding.layoutSelectionBar.setVisibility(View.VISIBLE);
                }
                updateSelectionBar();
            }
        }

        onBackPressedCallback = new androidx.activity.OnBackPressedCallback(isSelectionMode) {
            @Override
            public void handleOnBackPressed() {
                if (isSelectionMode) {
                    exitSelectionMode();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        viewModel.setCollectionId(collectionId);

        // Observe collection details to dynamically render title and description
        viewModel.getCollection().observe(getViewLifecycleOwner(), this::renderCollectionMetadata);

        // Observe album photos
        viewModel.getPhotos().observe(getViewLifecycleOwner(), this::renderPhotos);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SELECTION_MODE, isSelectionMode);
        outState.putStringArrayList(STATE_SELECTED_IDS, new ArrayList<>(selectedPhotoIds));
    }

    private void renderCollectionMetadata(@Nullable Collection collection) {
        if (collection == null || binding == null) return;
        collectionName = collection.getName();
        collectionDescription = collection.getDescription();
        binding.toolbarCollectionDetail.setTitle(collectionName);

        if (!collectionDescription.isEmpty()) {
            binding.textCollectionDetailDescription.setText(collectionDescription);
            binding.textCollectionDetailDescription.setVisibility(View.VISIBLE);
        } else {
            binding.textCollectionDetailDescription.setVisibility(View.GONE);
        }
    }

    private void renderPhotos(@Nullable List<Photo> photos) {
        if (binding == null) return;
        currentPhotosList = photos != null ? new ArrayList<>(photos) : Collections.emptyList();
        photoAdapter.setPhotos(currentPhotosList);

        boolean isEmpty = currentPhotosList.isEmpty();
        binding.textCollectionDetailEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerCollectionPhotos.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isSelectionMode) {
            // Prune any selected IDs no longer present
            Set<String> validIds = new HashSet<>();
            for (Photo p : currentPhotosList) {
                validIds.add(p.getId());
            }
            selectedPhotoIds.retainAll(validIds);
            photoAdapter.setSelectedIds(selectedPhotoIds);
            updateSelectionBar();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (isBusy) return true;
        int id = item.getItemId();
        if (id == R.id.action_add_to_album) {
            openFavoritesPicker();
            return true;
        } else if (id == R.id.action_select_mode) {
            enterSelectionMode();
            return true;
        } else if (id == R.id.action_edit_album) {
            CollectionFormDialogFragment.newInstanceForEdit(
                    collectionId, collectionName, collectionDescription
            ).show(getChildFragmentManager(), CollectionFormDialogFragment.TAG);
            return true;
        } else if (id == R.id.action_delete_album) {
            confirmDeleteAlbum();
            return true;
        }
        return false;
    }

    private void openFavoritesPicker() {
        List<String> currentIds = new ArrayList<>();
        for (Photo p : currentPhotosList) {
            currentIds.add(p.getId());
        }
        FavoritesPickerDialogFragment dialog = FavoritesPickerDialogFragment.newInstance(currentIds);
        dialog.show(getChildFragmentManager(), FavoritesPickerDialogFragment.TAG);
    }

    private void addPhotosToAlbum(@NonNull List<Photo> selectedPhotos) {
        setBusy(true);
        viewModel.addPhotosToCollection(selectedPhotos).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setBusy(true);
                    break;
                case SUCCESS:
                    setBusy(false);
                    if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                getString(R.string.photos_added_to_album, selectedPhotos.size()),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    break;
                case ERROR:
                    setBusy(false);
                    String msg = resource.getMessage() != null
                            ? resource.getMessage()
                            : "Failed to add photos to album.";
                    if (getContext() != null) {
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case EMPTY:
                    break;
            }
        });
    }

    private void enterSelectionMode() {
        if (currentPhotosList.isEmpty()) {
            Toast.makeText(requireContext(), R.string.collection_detail_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        isSelectionMode = true;
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(true);
        }
        selectedPhotoIds.clear();
        photoAdapter.setSelectionMode(true);
        photoAdapter.clearSelection();
        updateSelectionBar();
        if (binding != null) {
            binding.layoutSelectionBar.setVisibility(View.VISIBLE);
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(false);
        }
        selectedPhotoIds.clear();
        photoAdapter.setSelectionMode(false);
        photoAdapter.clearSelection();
        if (binding != null) {
            binding.layoutSelectionBar.setVisibility(View.GONE);
        }
    }

    private void togglePhotoSelection(@NonNull String photoId) {
        photoAdapter.toggleSelection(photoId);
        selectedPhotoIds.clear();
        selectedPhotoIds.addAll(photoAdapter.getSelectedIds());
        updateSelectionBar();
    }

    private void updateSelectionBar() {
        if (binding == null) return;
        int count = selectedPhotoIds.size();
        String text = count == 1
                ? "1 selected"
                : count + " selected";
        binding.textSelectionCount.setText(text);
        binding.buttonConfirmRemove.setEnabled(count > 0 && !isBusy);
    }

    private void confirmRemoveSelectedPhotos() {
        if (selectedPhotoIds.isEmpty() || isBusy) return;

        final int count = selectedPhotoIds.size();
        final List<String> idsToRemove = new ArrayList<>(selectedPhotoIds);
        String message = count == 1
                ? getString(R.string.dialog_remove_photo_single_message, collectionName)
                : getString(R.string.dialog_remove_photos_multi_message, count, collectionName);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_remove_photos_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_remove_selected, (dialog, which) -> {
                    setBusy(true);
                    viewModel.removePhotosFromCollection(idsToRemove).observe(getViewLifecycleOwner(), resource -> {
                        if (resource == null) return;
                        switch (resource.getStatus()) {
                            case LOADING:
                                setBusy(true);
                                break;
                            case SUCCESS:
                                setBusy(false);
                                if (getContext() != null) {
                                    Toast.makeText(
                                            getContext(),
                                            getString(R.string.photos_removed_from_album, count),
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                                exitSelectionMode();
                                break;
                            case ERROR:
                                setBusy(false);
                                String errorMsg = resource.getMessage() != null
                                        ? resource.getMessage()
                                        : "Failed to remove photo(s). Please try again.";
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case EMPTY:
                                break;
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmDeleteAlbum() {
        if (isBusy) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_album_title)
                .setMessage(getString(R.string.dialog_delete_album_message, collectionName))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    setBusy(true);
                    ((FlickrApplication) requireActivity().getApplication())
                            .getAppContainer()
                            .getPhotoRepository()
                            .deleteCollection(collectionId)
                            .observe(getViewLifecycleOwner(), resource -> {
                                if (resource == null) return;
                                switch (resource.getStatus()) {
                                    case LOADING:
                                        setBusy(true);
                                        break;
                                    case SUCCESS:
                                        setBusy(false);
                                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                                            getParentFragmentManager().popBackStack();
                                        }
                                        break;
                                    case ERROR:
                                        setBusy(false);
                                        String errorMsg = resource.getMessage() != null
                                                ? resource.getMessage()
                                                : "Failed to delete album. Please try again.";
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                                        }
                                        break;
                                    case EMPTY:
                                        break;
                                }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setBusy(boolean busy) {
        this.isBusy = busy;
        if (binding != null) {
            binding.buttonConfirmRemove.setEnabled(!busy && !selectedPhotoIds.isEmpty());
            binding.buttonCancelSelection.setEnabled(!busy);
            binding.toolbarCollectionDetail.setEnabled(!busy);
        }
    }

    private void handleBackNavigation() {
        if (isBusy) return;
        if (isSelectionMode) {
            exitSelectionMode();
            return;
        }
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onPhotoClicked(@NonNull Photo photo) {
        if (isBusy) return;
        if (isSelectionMode) {
            togglePhotoSelection(photo.getId());
        } else {
            startActivity(PhotoDetailActivity.newIntent(requireContext(), photo));
        }
    }

    @Override
    public void onFavoriteClicked(@NonNull Photo photo) {
        if (isBusy) return;
        viewModel.toggleFavorite(photo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
