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

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentCollectionsBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;

/**
 * Fragment displaying and creating local photo collections stored in Room.
 */
public class CollectionsFragment extends Fragment {

    @Nullable
    private FragmentCollectionsBinding binding;

    private CollectionsViewModel viewModel;
    private CollectionAdapter collectionAdapter;

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

        collectionAdapter = new CollectionAdapter();
        if (binding != null) {
            binding.recyclerCollections.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerCollections.setAdapter(collectionAdapter);
            binding.buttonCreateCollection.setOnClickListener(v -> {
                if (binding.editCollectionName.getText() == null) {
                    return;
                }
                String name = binding.editCollectionName.getText().toString().trim();
                if (!name.isEmpty()) {
                    viewModel.createCollection(name, "");
                    binding.editCollectionName.setText("");
                }
            });
        }

        viewModel.getCollections().observe(getViewLifecycleOwner(), this::renderCollections);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
