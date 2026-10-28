package vn.edu.usth.flickrimagebrowser.ui.common;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ItemPhotoBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Shared RecyclerView adapter for rendering Pinterest-style masonry photo grids.
 * Uses Glide with cross-fade animation to smoothly load image thumbnails.
 */
public class PhotoAdapter extends ListAdapter<Photo, PhotoAdapter.PhotoViewHolder> {

    public interface OnPhotoActionListener {
        void onPhotoClicked(@NonNull Photo photo);

        void onFavoriteClicked(@NonNull Photo photo);
    }

    private final OnPhotoActionListener listener;

    public PhotoAdapter(@NonNull OnPhotoActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhotoBinding binding = ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PhotoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ItemPhotoBinding binding;

        PhotoViewHolder(@NonNull ItemPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Photo photo, @NonNull OnPhotoActionListener listener) {
            binding.textPhotoTitle.setVisibility(View.GONE);
            binding.textPhotoTitle.setText(photo.getTitle());
            binding.textPhotoOwner.setText(photo.getOwnerName());

            if (photo.isFavorite()) {
                binding.buttonItemFavorite.setImageResource(R.drawable.ic_favorite_filled);
                binding.buttonItemFavorite.clearColorFilter();
            } else {
                binding.buttonItemFavorite.setImageResource(R.drawable.ic_favorite_border);
                binding.buttonItemFavorite.setColorFilter(Color.WHITE);
            }

            Glide.with(binding.imageThumbnail.getContext())
                    .load(photo.getThumbnailUrl())
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .transition(DrawableTransitionOptions.withCrossFade(250))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(binding.imageThumbnail);

            binding.getRoot().setOnClickListener(v -> listener.onPhotoClicked(photo));
            binding.buttonItemFavorite.setOnClickListener(v -> listener.onFavoriteClicked(photo));
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
