package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentCollectionDetailBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.detail.PhotoDetailActivity;

/**
 * Fragment showing all photos in a single collection, with the ability to remove each photo.
 * Opened when the user taps a collection item in CollectionsFragment.
 */
public class CollectionDetailFragment extends Fragment
        implements CollectionPhotoAdapter.OnCollectionPhotoActionListener {

    public static final String ARG_COLLECTION_ID = "arg_collection_id";
    public static final String ARG_COLLECTION_NAME = "arg_collection_name";

    @Nullable
    private FragmentCollectionDetailBinding binding;

    private CollectionDetailViewModel viewModel;
    private CollectionPhotoAdapter adapter;

    /**
     * Factory method to create a new instance carrying the collection's ID and name.
     *
     * @param collectionId   Room primary key of the collection.
     * @param collectionName Display name used for the toolbar title.
     */
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

        // Read args
        Bundle args = getArguments();
        long collectionId = args != null ? args.getLong(ARG_COLLECTION_ID, -1L) : -1L;
        String collectionName = args != null ? args.getString(ARG_COLLECTION_NAME, "") : "";

        // Toolbar
        if (binding != null) {
            binding.toolbarCollectionDetail.setTitle(collectionName);
            binding.toolbarCollectionDetail.setNavigationIcon(R.drawable.ic_arrow_back);
            binding.toolbarCollectionDetail.setNavigationOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });

            // RecyclerView
            adapter = new CollectionPhotoAdapter(this);
            binding.recyclerCollectionPhotos.setLayoutManager(
                    new LinearLayoutManager(requireContext()));
            binding.recyclerCollectionPhotos.setAdapter(adapter);
        }

        // Tell the ViewModel which collection we're viewing
        viewModel.setCollectionId(collectionId);

        // Observe photos
        viewModel.getPhotos().observe(getViewLifecycleOwner(), this::renderPhotos);
    }

    private void renderPhotos(@Nullable List<Photo> photos) {
        if (binding == null) return;
        List<Photo> safeList = photos != null ? photos : Collections.emptyList();
        adapter.submitList(safeList);
        boolean isEmpty = safeList.isEmpty();
        binding.textCollectionDetailEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerCollectionPhotos.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPhotoClicked(@NonNull Photo photo) {
        startActivity(PhotoDetailActivity.newIntent(requireContext(), photo));
    }

    @Override
    public void onRemoveFromCollection(@NonNull Photo photo) {
        // Show a confirmation dialog before removing
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_remove_photo_title)
                .setMessage(getString(R.string.dialog_remove_photo_message, photo.getTitle()))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.removePhotoFromCollection(photo))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
