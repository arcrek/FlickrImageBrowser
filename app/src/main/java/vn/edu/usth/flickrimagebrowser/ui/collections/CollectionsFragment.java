package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.MainActivity;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.FragmentCollectionsBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Fragment displaying, creating, renaming, and deleting local photo collections stored in Room.
 */
public class CollectionsFragment extends Fragment implements CollectionAdapter.OnCollectionActionListener {

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

        collectionAdapter = new CollectionAdapter(this);
        if (binding != null) {
            binding.recyclerCollections.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerCollections.setAdapter(collectionAdapter);

            binding.fabCreateCollection.setOnClickListener(v -> showCreateCollectionDialog());
        }

        viewModel.getCollections().observe(getViewLifecycleOwner(), this::renderCollections);
    }

    /**
     * Displays a MaterialAlertDialog prompting the user to create a new collection with name and description.
     */
    public void showCreateCollectionDialog() {
        Context context = requireContext();
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_collection_form, null, false);
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.dialog_collection_name_layout);
        TextInputEditText nameEditText = dialogView.findViewById(R.id.dialog_collection_name_input);
        TextInputEditText descEditText = dialogView.findViewById(R.id.dialog_collection_description_input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_create_collection_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_create, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            createButton.setOnClickListener(v -> {
                Editable nameText = nameEditText.getText();
                String name = nameText != null ? nameText.toString().trim() : "";
                /*Kiểm tra k được để trống */
                if (name.isEmpty()) {
                    nameInputLayout.setError(getString(R.string.error_empty_collection_name));
                    return;
                }
                /*Kiểm tra k trùng lặp */
                if (!viewModel.isCollectionNameUnique(name, -1)) {
                    nameInputLayout.setError(getString(R.string.error_duplicate_collection_name));
                    return;
                }

                Editable descText = descEditText.getText();
                String description = descText != null ? descText.toString().trim() : "";

                observeSave(dialog, nameInputLayout,
                        viewModel.createCollectionResult(name, description));
            });
        });

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (nameInputLayout.getError() != null) {
                    nameInputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.show();
    }

    private void observeSave(AlertDialog dialog, TextInputLayout inputLayout,
                             LiveData<Resource<Boolean>> result) {
        Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveButton.setEnabled(false);
        Observer<Resource<Boolean>> observer = new Observer<Resource<Boolean>>() {
            @Override
            public void onChanged(Resource<Boolean> state) {
                if (!dialog.isShowing() || state == null) {
                    return;
                }
                if (state.getStatus() == Resource.Status.SUCCESS) {
                    result.removeObserver(this);
                    dialog.dismiss();
                } else if (state.getStatus() == Resource.Status.ERROR) {
                    result.removeObserver(this);
                    saveButton.setEnabled(true);
                    inputLayout.setError(state.getMessage());
                }
            }
        };
        dialog.setOnDismissListener(ignored -> result.removeObserver(observer));
        result.observe(getViewLifecycleOwner(), observer);
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
        Context context = requireContext();
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_collection_form, null, false);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.dialog_collection_name_layout);
        TextInputEditText editText = dialogView.findViewById(R.id.dialog_collection_name_input);
        View descriptionLayout = dialogView.findViewById(R.id.dialog_collection_description_layout);
        descriptionLayout.setVisibility(View.GONE);

        editText.setText(collection.getName());
        editText.setSelection(collection.getName().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_rename_collection_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                Editable text = editText.getText();
                String newName = text != null ? text.toString().trim() : "";

                if (newName.isEmpty()) {
                    inputLayout.setError(getString(R.string.error_empty_collection_name));
                    return;
                }

                if (newName.equals(collection.getName())) {
                    dialog.dismiss();
                    return;
                }

                if (!viewModel.isCollectionNameUnique(newName, collection.getId())) {
                    inputLayout.setError(getString(R.string.error_duplicate_collection_name));
                    return;
                }

                observeSave(dialog, inputLayout,
                        viewModel.renameCollectionResult(collection.getId(), newName));
            });
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (inputLayout.getError() != null) {
                    inputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.show();
    }

    @Override
    public void onDeleteCollection(@NonNull Collection collection) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_collection_title)
                .setMessage(getString(R.string.dialog_delete_collection_message, collection.getName()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.deleteCollection(collection);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}