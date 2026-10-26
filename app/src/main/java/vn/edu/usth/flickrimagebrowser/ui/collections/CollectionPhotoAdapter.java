package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ItemCollectionPhotoBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * RecyclerView adapter displaying photos inside a single collection.
 * Each item shows a thumbnail, title, owner, and a "Remove from Collection" button.
 */
public class CollectionPhotoAdapter extends ListAdapter<Photo, CollectionPhotoAdapter.CollectionPhotoViewHolder> {

    /**
     * Callback for actions triggered from each photo item.
     */
    public interface OnCollectionPhotoActionListener {
        /** Called when the user taps a photo thumbnail to view details. */
        void onPhotoClicked(@NonNull Photo photo);

        /** Called when the user taps the remove (×) button on a photo. */
        void onRemoveFromCollection(@NonNull Photo photo);
    }

    @Nullable
    private final OnCollectionPhotoActionListener actionListener;

    public CollectionPhotoAdapter(@Nullable OnCollectionPhotoActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public CollectionPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCollectionPhotoBinding binding = ItemCollectionPhotoBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CollectionPhotoViewHolder(binding, actionListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionPhotoViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CollectionPhotoViewHolder extends RecyclerView.ViewHolder {

        private final ItemCollectionPhotoBinding binding;
        @Nullable
        private final OnCollectionPhotoActionListener actionListener;

        CollectionPhotoViewHolder(@NonNull ItemCollectionPhotoBinding binding,
                                  @Nullable OnCollectionPhotoActionListener actionListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.actionListener = actionListener;
        }

        void bind(@NonNull Photo photo) {
            binding.textCollectionPhotoTitle.setText(photo.getTitle());
            binding.textCollectionPhotoOwner.setText(photo.getOwnerName());

            Glide.with(binding.imageCollectionPhotoThumb.getContext())
                    .load(photo.getThumbnailUrl())
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .centerCrop()
                    .into(binding.imageCollectionPhotoThumb);

            binding.getRoot().setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onPhotoClicked(photo);
                }
            });

            binding.buttonRemoveFromCollection.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRemoveFromCollection(photo);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Photo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Photo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
            return oldItem.equals(newItem);
        }
    };
}
