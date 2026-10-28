package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.MainActivity;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentCollectionsBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;

/**
 * Fragment displaying, creating, renaming, and deleting local photo collections stored in Room.
 */
public class CollectionsFragment extends Fragment implements CollectionAdapter.OnCollectionActionListener {

    @Nullable
    private FragmentCollectionsBinding binding;

    private CollectionsViewModel viewModel;
    private CollectionAdapter collectionAdapter;
    private GridLayoutManager gridLayoutManager;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCollectionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppViewModelFactory factory = ((FlickrApplication) requireActivity().getApplication())
                .getAppContainer()
                .getViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(CollectionsViewModel.class);

        collectionAdapter = new CollectionAdapter(this);
        if (binding != null) {
            int spanCount = getResponsiveSpanCount();
            gridLayoutManager = new GridLayoutManager(requireContext(), spanCount);
            binding.recyclerCollections.setLayoutManager(gridLayoutManager);
            binding.recyclerCollections.setAdapter(collectionAdapter);

            binding.buttonHeaderCreateCollection.setOnClickListener(v -> showCreateCollectionDialog());
        }
        viewModel.getCollections().observe(getViewLifecycleOwner(), this::renderCollections);
    }

    /**
     * Displays a MaterialAlertDialog prompting the user to create a new collection with name and description.
     */
    public void showCreateCollectionDialog() {
        CollectionFormDialogFragment.newInstance()
                .show(getChildFragmentManager(), CollectionFormDialogFragment.TAG);
    }

    private int getResponsiveSpanCount() {
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        return Math.max(2, screenWidthDp / 180);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (gridLayoutManager != null) {
            gridLayoutManager.setSpanCount(getResponsiveSpanCount());
        }
    }

    private void renderCollections(@Nullable List<Collection> collections) {
        if (binding == null) {
            return;
        }
        List<Collection> safeList = collections != null ? collections : Collections.emptyList();
        collectionAdapter.submitList(safeList);
        boolean isEmpty = safeList.isEmpty();
        binding.textEmptyCollections.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.recyclerCollections.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCollectionClick(@NonNull Collection collection) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCollectionDetail(
                    collection.getId(), collection.getName());
        }
    }

    @Override
    public void onRenameCollection(@NonNull Collection collection) {
        CollectionFormDialogFragment.newInstanceForEdit(
                collection.getId(),
                collection.getName(),
                collection.getDescription()
        ).show(getChildFragmentManager(), CollectionFormDialogFragment.TAG);
    }

    @Override
    public void onDeleteCollection(@NonNull Collection collection) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_album_title)
                .setMessage(getString(R.string.dialog_delete_album_message, collection.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.deleteCollection(collection);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
