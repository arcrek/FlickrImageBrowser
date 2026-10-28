package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.data.repository.PhotoRepository;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Reusable DialogFragment for creating, renaming, and editing album details.
 * Supports draft persistence across configuration changes, scrollable inputs,
 * input validation, and awaiting async repository mutation results before dismissing.
 */
public class CollectionFormDialogFragment extends DialogFragment {

    public static final String TAG = "CollectionFormDialogFragment";
    public static final String REQUEST_KEY = "collection_form_result";
    public static final String RESULT_COLLECTION_ID = "collection_id";
    public static final String RESULT_COLLECTION_NAME = "collection_name";

    private static final String ARG_MODE = "arg_mode";
    private static final String ARG_COLLECTION_ID = "arg_collection_id";
    private static final String ARG_INITIAL_NAME = "arg_initial_name";
    private static final String ARG_INITIAL_DESCRIPTION = "arg_initial_description";
    private static final String ARG_ATTACH_PHOTO = "arg_attach_photo";

    private static final String STATE_DRAFT_NAME = "state_draft_name";
    private static final String STATE_DRAFT_DESC = "state_draft_desc";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    /**
     * Optional callback listener for callers not using FragmentResultListener.
     */
    public interface OnCollectionResultListener {
        void onCollectionSuccess(long collectionId, @NonNull String name);
    }

    private PhotoRepository repository;
    private CollectionFormViewModel formViewModel;
    private OnCollectionResultListener resultListener;

    private TextInputLayout nameLayout;
    private TextInputEditText nameEditText;
    private TextInputLayout descLayout;
    private TextInputEditText descEditText;
    private TextView errorTextView;
    private ProgressBar progressBar;

    private int mode = MODE_CREATE;
    private long collectionId = -1L;
    private Photo attachPhoto;
    private boolean isSubmitting = false;

    @NonNull
    public static CollectionFormDialogFragment newInstance() {
        return newInstance(MODE_CREATE, -1L, "", "", null);
    }

    @NonNull
    public static CollectionFormDialogFragment newInstanceForPhoto(@NonNull Photo photo) {
        return newInstance(MODE_CREATE, -1L, "", "", photo);
    }

    @NonNull
    public static CollectionFormDialogFragment newInstanceForEdit(long collectionId,
                                                                 @NonNull String currentName,
                                                                 @Nullable String currentDescription) {
        return newInstance(MODE_EDIT, collectionId, currentName, currentDescription, null);
    }

    @NonNull
    private static CollectionFormDialogFragment newInstance(int mode,
                                                           long collectionId,
                                                           @Nullable String initialName,
                                                           @Nullable String initialDescription,
                                                           @Nullable Photo photo) {
        CollectionFormDialogFragment fragment = new CollectionFormDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        args.putLong(ARG_COLLECTION_ID, collectionId);
        args.putString(ARG_INITIAL_NAME, initialName != null ? initialName : "");
        args.putString(ARG_INITIAL_DESCRIPTION, initialDescription != null ? initialDescription : "");
        if (photo != null) {
            args.putSerializable(ARG_ATTACH_PHOTO, photo);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnCollectionResultListener(@Nullable OnCollectionResultListener listener) {
        this.resultListener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        vn.edu.usth.flickrimagebrowser.util.AppContainer container =
                ((FlickrApplication) requireActivity().getApplication()).getAppContainer();
        repository = container.getPhotoRepository();
        formViewModel = new ViewModelProvider(this, container.getViewModelFactory())
                .get(CollectionFormViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() != null ? getArguments() : Bundle.EMPTY;
        mode = args.getInt(ARG_MODE, MODE_CREATE);
        collectionId = args.getLong(ARG_COLLECTION_ID, -1L);
        if (args.containsKey(ARG_ATTACH_PHOTO)) {
            attachPhoto = (Photo) args.getSerializable(ARG_ATTACH_PHOTO);
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_collection_form, null, false);
        nameLayout = dialogView.findViewById(R.id.dialog_collection_name_layout);
        nameEditText = dialogView.findViewById(R.id.dialog_collection_name_input);
        descLayout = dialogView.findViewById(R.id.dialog_collection_description_layout);
        descEditText = dialogView.findViewById(R.id.dialog_collection_description_input);
        errorTextView = dialogView.findViewById(R.id.text_form_error);
        progressBar = dialogView.findViewById(R.id.progress_saving);

        final String initialName = (savedInstanceState != null)
                ? savedInstanceState.getString(STATE_DRAFT_NAME, args.getString(ARG_INITIAL_NAME, ""))
                : args.getString(ARG_INITIAL_NAME, "");
        final String initialDesc = (savedInstanceState != null)
                ? savedInstanceState.getString(STATE_DRAFT_DESC, args.getString(ARG_INITIAL_DESCRIPTION, ""))
                : args.getString(ARG_INITIAL_DESCRIPTION, "");

        if (nameEditText != null && initialName != null && nameEditText.getText().length() == 0) {
            nameEditText.setText(initialName);
            nameEditText.setSelection(initialName.length());
        }
        if (descEditText != null && initialDesc != null && descEditText.getText().length() == 0) {
            descEditText.setText(initialDesc);
        }

        if (nameEditText != null) {
            nameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (nameLayout != null && nameLayout.getError() != null) {
                        nameLayout.setError(null);
                    }
                    if (errorTextView != null && errorTextView.getVisibility() == View.VISIBLE) {
                        errorTextView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        int titleRes = (mode == MODE_EDIT)
                ? R.string.dialog_edit_album_title
                : R.string.dialog_create_album_title;
        int actionRes = (mode == MODE_EDIT)
                ? R.string.action_save_album
                : R.string.action_create_album;

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(dialogView)
                .setPositiveButton(actionRes, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> submitForm(dialog));

            if (formViewModel != null && formViewModel.isSubmitting()) {
                setSubmittingState(dialog, true);
                if (mode == MODE_CREATE && formViewModel.getCreateResultLiveData() != null) {
                    observeCreateMutation(dialog, initialName);
                } else if (mode == MODE_EDIT && formViewModel.getUpdateResultLiveData() != null) {
                    observeUpdateMutation(dialog, initialName);
                }
            }
        });
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (nameEditText != null && nameEditText.getText() != null) {
            outState.putString(STATE_DRAFT_NAME, nameEditText.getText().toString());
        }
        if (descEditText != null && descEditText.getText() != null) {
            outState.putString(STATE_DRAFT_DESC, descEditText.getText().toString());
        }
    }

    private void submitForm(@NonNull AlertDialog dialog) {
        if (isSubmitting) {
            return;
        }

        String name = nameEditText != null && nameEditText.getText() != null
                ? nameEditText.getText().toString().trim()
                : "";
        String description = descEditText != null && descEditText.getText() != null
                ? descEditText.getText().toString().trim()
                : "";

        if (name.isEmpty()) {
            if (nameLayout != null) {
                nameLayout.setError(getString(R.string.error_empty_album_name));
            }
            return;
        }

        setSubmittingState(dialog, true);

        if (formViewModel != null && formViewModel.getCreatedCollectionId() > 0L && attachPhoto != null) {
            final long createdId = formViewModel.getCreatedCollectionId();
            formViewModel.attachPhoto(createdId, attachPhoto).observe(this, photoRes -> {
                if (photoRes == null) return;
                if (photoRes.getStatus() == Resource.Status.SUCCESS) {
                    formViewModel.reset();
                    completeSuccess(dialog, createdId, name);
                } else if (photoRes.getStatus() == Resource.Status.ERROR) {
                    formViewModel.setSubmitting(false);
                    setSubmittingState(dialog, false);
                    String msg = photoRes.getMessage() != null
                            ? photoRes.getMessage()
                            : "Album created, but failed to add photo. Please try again.";
                    showError(msg);
                }
            });
            return;
        }

        if (mode == MODE_CREATE) {
            performCreate(dialog, name, description);
        } else {
            performUpdate(dialog, name, description);
        }
    }

    private void performCreate(@NonNull AlertDialog dialog,
                              @NonNull String name,
                              @NonNull String description) {
        formViewModel.submitCreate(name, description);
        observeCreateMutation(dialog, name);
    }

    private void observeCreateMutation(@NonNull AlertDialog dialog, @NonNull String name) {
        final LiveData<Resource<Long>> mutation = formViewModel.getCreateResultLiveData();
        if (mutation == null) return;
        mutation.removeObservers(this);
        mutation.observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setSubmittingState(dialog, true);
                    break;
                case SUCCESS:
                    Long createdId = resource.getData();
                    long targetId = createdId != null ? createdId : -1L;
                    if (attachPhoto != null && targetId > 0L) {
                        formViewModel.setCreatedCollectionId(targetId);
                        formViewModel.attachPhoto(targetId, attachPhoto).observe(this, photoRes -> {
                            if (photoRes != null && photoRes.getStatus() == Resource.Status.SUCCESS) {
                                formViewModel.reset();
                                completeSuccess(dialog, targetId, name);
                            } else if (photoRes != null && photoRes.getStatus() == Resource.Status.ERROR) {
                                formViewModel.setSubmitting(false);
                                setSubmittingState(dialog, false);
                                String msg = photoRes.getMessage() != null
                                        ? photoRes.getMessage()
                                        : "Album created, but failed to add photo. Please try again.";
                                showError(msg);
                            }
                        });
                    } else {
                        formViewModel.reset();
                        completeSuccess(dialog, targetId, name);
                    }
                    break;
                case ERROR:
                    mutation.removeObservers(this);
                    if (formViewModel != null) formViewModel.setSubmitting(false);
                    setSubmittingState(dialog, false);
                    String msg = resource.getMessage() != null
                            ? resource.getMessage()
                            : getString(R.string.error_duplicate_album_name);
                    showError(msg);
                    break;
                case EMPTY:
                    break;
            }
        });
    }

    private void performUpdate(@NonNull AlertDialog dialog,
                              @NonNull String name,
                              @NonNull String description) {
        formViewModel.submitUpdate(collectionId, name, description);
        observeUpdateMutation(dialog, name);
    }

    private void observeUpdateMutation(@NonNull AlertDialog dialog, @NonNull String name) {
        final LiveData<Resource<Void>> mutation = formViewModel.getUpdateResultLiveData();
        if (mutation == null) return;
        mutation.removeObservers(this);
        mutation.observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setSubmittingState(dialog, true);
                    break;
                case SUCCESS:
                    formViewModel.reset();
                    completeSuccess(dialog, collectionId, name);
                    break;
                case ERROR:
                    mutation.removeObservers(this);
                    if (formViewModel != null) formViewModel.setSubmitting(false);
                    setSubmittingState(dialog, false);
                    String msg = resource.getMessage() != null
                            ? resource.getMessage()
                            : getString(R.string.error_duplicate_album_name);
                    showError(msg);
                    break;
                case EMPTY:
                    break;
            }
        });
    }

    private void setSubmittingState(@NonNull AlertDialog dialog, boolean submitting) {
        isSubmitting = submitting;
        if (formViewModel != null) {
            formViewModel.setSubmitting(submitting);
        }
        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positive != null) positive.setEnabled(!submitting);
        if (negative != null) negative.setEnabled(!submitting);
        if (nameEditText != null) nameEditText.setEnabled(!submitting);
        if (descEditText != null) descEditText.setEnabled(!submitting);
        if (progressBar != null) {
            progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(@NonNull String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(View.VISIBLE);
        } else if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void completeSuccess(@NonNull AlertDialog dialog, long id, @NonNull String name) {
        Bundle result = new Bundle();
        result.putLong(RESULT_COLLECTION_ID, id);
        result.putString(RESULT_COLLECTION_NAME, name);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);

        deliverResult(id, name);
        dialog.dismiss();
    }

    private void deliverResult(long id, @NonNull String name) {
        if (resultListener != null) {
            resultListener.onCollectionSuccess(id, name);
            return;
        }
        if (getTargetFragment() instanceof OnCollectionResultListener) {
            ((OnCollectionResultListener) getTargetFragment()).onCollectionSuccess(id, name);
            return;
        }
        if (getParentFragment() instanceof OnCollectionResultListener) {
            ((OnCollectionResultListener) getParentFragment()).onCollectionSuccess(id, name);
            return;
        }
        if (getActivity() instanceof OnCollectionResultListener) {
            ((OnCollectionResultListener) getActivity()).onCollectionSuccess(id, name);
        }
    }
}
