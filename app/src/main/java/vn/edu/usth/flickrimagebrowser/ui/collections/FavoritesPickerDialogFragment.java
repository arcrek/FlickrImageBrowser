package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Multi-select dialog for choosing photos from the user's favorites to add into an album.
 * Displays photo count on the confirmation button and supports cancelation.
 */
public class FavoritesPickerDialogFragment extends DialogFragment {

    public static final String TAG = "FavoritesPickerDialogFragment";
    public static final String REQUEST_KEY = "request_picker_photos";
    public static final String RESULT_SELECTED_PHOTOS = "result_selected_photos";
    private static final String ARG_EXCLUDE_PHOTO_IDS = "arg_exclude_photo_ids";
    private static final String STATE_SELECTED_IDS = "state_selected_ids";

    public interface OnPhotosSelectedListener {
        void onPhotosSelected(@NonNull List<Photo> selectedPhotos);
    }

    private OnPhotosSelectedListener listener;
    private final Set<String> excludeIds = new HashSet<>();
    private final LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
    private final List<Photo> availablePhotos = new ArrayList<>();

    private PickerAdapter adapter;
    private Button confirmButton;
    private TextView emptyTextView;

    @NonNull
    public static FavoritesPickerDialogFragment newInstance(@Nullable List<String> currentPhotoIds) {
        FavoritesPickerDialogFragment fragment = new FavoritesPickerDialogFragment();
        Bundle args = new Bundle();
        if (currentPhotoIds != null) {
            args.putStringArrayList(ARG_EXCLUDE_PHOTO_IDS, new ArrayList<>(currentPhotoIds));
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnPhotosSelectedListener(@Nullable OnPhotosSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_EXCLUDE_PHOTO_IDS)) {
            ArrayList<String> list = args.getStringArrayList(ARG_EXCLUDE_PHOTO_IDS);
            if (list != null) {
                excludeIds.addAll(list);
            }
        }

        if (savedInstanceState != null) {
            ArrayList<String> restored = savedInstanceState.getStringArrayList(STATE_SELECTED_IDS);
            if (restored != null) {
                selectedIds.addAll(restored);
            }
        }

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_favorites_picker, null, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_picker_photos);
        emptyTextView = view.findViewById(R.id.text_picker_empty);
        confirmButton = view.findViewById(R.id.button_picker_confirm);
        Button cancelButton = view.findViewById(R.id.button_picker_cancel);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new PickerAdapter();
        recyclerView.setAdapter(adapter);

        cancelButton.setOnClickListener(v -> dismiss());
        confirmButton.setOnClickListener(v -> {
            ArrayList<Photo> result = new ArrayList<>();
            for (Photo p : availablePhotos) {
                if (selectedIds.contains(p.getId())) {
                    result.add(p);
                }
            }
            Bundle resultBundle = new Bundle();
            resultBundle.putSerializable(RESULT_SELECTED_PHOTOS, result);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, resultBundle);

            if (listener != null) {
                listener.onPhotosSelected(result);
            } else if (getParentFragment() instanceof OnPhotosSelectedListener) {
                ((OnPhotosSelectedListener) getParentFragment()).onPhotosSelected(result);
            } else if (getActivity() instanceof OnPhotosSelectedListener) {
                ((OnPhotosSelectedListener) getActivity()).onPhotosSelected(result);
            }
            dismiss();
        });

        // Observe favorites
        ((FlickrApplication) requireActivity().getApplication())
                .getAppContainer()
                .getPhotoRepository()
                .observeFavorites()
                .observe(this, this::updateFavorites);

        updateConfirmButton();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED_IDS, new ArrayList<>(selectedIds));
    }

    private void updateFavorites(@Nullable List<Photo> photos) {
        availablePhotos.clear();
        if (photos != null) {
            for (Photo p : photos) {
                if (!excludeIds.contains(p.getId())) {
                    availablePhotos.add(p);
                }
            }
        }

        adapter.submitList(new ArrayList<>(availablePhotos));
        boolean isEmpty = availablePhotos.isEmpty();
        if (emptyTextView != null) {
            emptyTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private void updateConfirmButton() {
        if (confirmButton == null) return;
        int count = selectedIds.size();
        confirmButton.setEnabled(count > 0);
        if (count > 0) {
            confirmButton.setText(getString(R.string.picker_favorites_confirm, count));
        } else {
            confirmButton.setText(R.string.picker_favorites_confirm_zero);
        }
    }

    private class PickerAdapter extends ListAdapter<Photo, PickerAdapter.PickerViewHolder> {

        PickerAdapter() {
            super(new DiffUtil.ItemCallback<Photo>() {
                @Override
                public boolean areItemsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @Override
        public void onCurrentListChanged(@NonNull List<Photo> previousList, @NonNull List<Photo> currentList) {
            super.onCurrentListChanged(previousList, currentList);
            if (emptyTextView != null) {
                emptyTextView.setVisibility(currentList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }

        @NonNull
        @Override
        public PickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_picker_photo, parent, false);
            return new PickerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PickerViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        class PickerViewHolder extends RecyclerView.ViewHolder {
            final ImageView thumbView;
            final View scrimView;
            final ImageView checkView;

            PickerViewHolder(@NonNull View itemView) {
                super(itemView);
                thumbView = itemView.findViewById(R.id.image_picker_thumb);
                scrimView = itemView.findViewById(R.id.view_picker_selected_scrim);
                checkView = itemView.findViewById(R.id.image_picker_check);
            }

            void bind(@NonNull Photo photo) {
                Glide.with(itemView.getContext())
                        .load(photo.getThumbnailUrl())
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .centerCrop()
                        .into(thumbView);

                boolean isSelected = selectedIds.contains(photo.getId());
                scrimView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                checkView.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                itemView.setOnClickListener(v -> {
                    if (selectedIds.contains(photo.getId())) {
                        selectedIds.remove(photo.getId());
                    } else {
                        selectedIds.add(photo.getId());
                    }
                    notifyItemChanged(getBindingAdapterPosition());
                    updateConfirmButton();
                });
            }
        }
    }
}
