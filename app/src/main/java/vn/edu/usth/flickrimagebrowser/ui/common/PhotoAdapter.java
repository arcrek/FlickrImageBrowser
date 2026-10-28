package vn.edu.usth.flickrimagebrowser.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ItemPhotoBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Shared RecyclerView adapter for rendering Flickr-style justified photo rows.
 * Uses bounded Glide image decoding without distorting TransitionDrawables,
 * accessible 48dp favorite touch targets, and reduced-motion-aware subtle interactions.
 */
public class PhotoAdapter extends ListAdapter<Photo, PhotoAdapter.PhotoViewHolder> {

    public interface OnPhotoActionListener {
        void onPhotoClicked(@NonNull Photo photo);

        void onFavoriteClicked(@NonNull Photo photo);
    }

    private final OnPhotoActionListener listener;
    private final int photoLayout;

    @Nullable
    private RecyclerView attachedRecyclerView;
    private boolean invalidatePending = false;

    public PhotoAdapter(@NonNull OnPhotoActionListener listener) {
        this(listener, R.layout.item_photo);
    }

    public PhotoAdapter(@NonNull OnPhotoActionListener listener, int photoLayout) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.photoLayout = photoLayout;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.attachedRecyclerView = recyclerView;
        syncPhotosToLayoutManager(getCurrentList());
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.attachedRecyclerView = null;
    }

    public void setPhotos(@Nullable List<Photo> photos) {
        submitList(photos);
    }

    @Override
    public void onCurrentListChanged(@NonNull List<Photo> previousList, @NonNull List<Photo> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        for (Photo p : currentList) {
            if (p != null && p.hasDimensions()) {
                PhotoDimensionCache.putAspectRatio(p.getThumbnailUrl(), p.getAspectRatio());
            }
        }
        syncPhotosToLayoutManager(currentList);
    }

    private void syncPhotosToLayoutManager(@Nullable List<Photo> list) {
        if (attachedRecyclerView != null) {
            RecyclerView.LayoutManager lm = attachedRecyclerView.getLayoutManager();
            if (lm instanceof JustifiedGridLayoutManager) {
                ((JustifiedGridLayoutManager) lm).setPhotos(list);
            }
        }
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(photoLayout, parent, false);
        ItemPhotoBinding binding = ItemPhotoBinding.bind(itemView);
        return new PhotoViewHolder(binding);
    }

    private final Runnable invalidationCallback = () -> {
        if (attachedRecyclerView != null && !invalidatePending) {
            invalidatePending = true;
            attachedRecyclerView.post(() -> {
                invalidatePending = false;
                if (attachedRecyclerView != null) {
                    RecyclerView.LayoutManager lm = attachedRecyclerView.getLayoutManager();
                    if (lm instanceof JustifiedGridLayoutManager) {
                        ((JustifiedGridLayoutManager) lm).invalidateDimensions();
                    }
                }
            });
        }
    };

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(getItem(position), listener, invalidationCallback);
    }

    @Override
    public void onViewRecycled(@NonNull PhotoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.resetAnimationsAndState();
        Glide.with(holder.binding.imageThumbnail.getContext()).clear(holder.binding.imageThumbnail);
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ItemPhotoBinding binding;
        private final int hideLabelThresholdPx;

        PhotoViewHolder(@NonNull ItemPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            this.hideLabelThresholdPx = Math.round(110 * density);
            itemView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                    updateLabelVisibility(right - left, hideLabelThresholdPx));
        }

        void bind(@NonNull Photo photo,
                  @NonNull OnPhotoActionListener listener,
                  @Nullable Runnable onDimensionResolved) {
            resetAnimationsAndState();

            binding.textPhotoTitle.setVisibility(View.GONE);
            binding.textPhotoTitle.setText(photo.getTitle());
            binding.textPhotoOwner.setText(photo.getOwnerName());

            Context context = binding.getRoot().getContext();
            int width = itemView.getWidth();
            if (width <= 0 && itemView.getLayoutParams() != null) {
                width = itemView.getLayoutParams().width;
            }
            updateLabelVisibility(width, hideLabelThresholdPx);
            binding.getRoot().setContentDescription(photo.getTitle() + ", " + photo.getOwnerName());
            String favoriteActionDesc = photo.isFavorite()
                    ? context.getString(R.string.gallery_remove_favorite, photo.getTitle())
                    : context.getString(R.string.gallery_add_favorite, photo.getTitle());
            binding.buttonItemFavorite.setContentDescription(favoriteActionDesc);
            ViewCompat.setStateDescription(
                    binding.buttonItemFavorite,
                    photo.isFavorite()
                            ? context.getString(R.string.gallery_state_favorite_saved)
                            : context.getString(R.string.gallery_state_favorite_not_saved)
            );
            binding.buttonItemFavorite.setSelected(photo.isFavorite());
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
                    .dontAnimate()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            binding.imageThumbnail.setAlpha(1.0f);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (dataSource != DataSource.MEMORY_CACHE && areAnimationsEnabled(binding.imageThumbnail.getContext())) {
                                binding.imageThumbnail.setAlpha(0f);
                                binding.imageThumbnail.animate().alpha(1.0f).setDuration(150).start();
                            } else {
                                binding.imageThumbnail.setAlpha(1.0f);
                            }

                            if (resource != null && resource.getIntrinsicWidth() > 0 && resource.getIntrinsicHeight() > 0) {
                                float ratio = (float) resource.getIntrinsicWidth() / resource.getIntrinsicHeight();
                                String thumbUrl = photo.getThumbnailUrl();
                                Float existing = PhotoDimensionCache.getAspectRatio(thumbUrl);
                                Float effective = existing != null ? existing : (photo.hasDimensions() ? photo.getAspectRatio() : null);
                                PhotoDimensionCache.putAspectRatio(thumbUrl, ratio);
                                if (effective == null || Math.abs(effective - ratio) > 0.05f) {
                                    if (onDimensionResolved != null) {
                                        onDimensionResolved.run();
                                    }
                                }
                            }
                            return false;
                        }
                    })
                    .into(binding.imageThumbnail);

            binding.getRoot().setOnClickListener(v -> listener.onPhotoClicked(photo));

            binding.buttonItemFavorite.setOnClickListener(v -> {
                if (areAnimationsEnabled(v.getContext())) {
                    v.animate()
                            .scaleX(1.18f)
                            .scaleY(1.18f)
                            .setDuration(120)
                            .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                            .start();
                }
                listener.onFavoriteClicked(photo);
            });
        }

        private void updateLabelVisibility(int widthPx, int thresholdPx) {
            if (widthPx > 0 && widthPx < thresholdPx) {
                binding.textPhotoOwner.setVisibility(View.GONE);
                binding.viewScrim.setVisibility(View.GONE);
            } else {
                binding.textPhotoOwner.setVisibility(View.VISIBLE);
                binding.viewScrim.setVisibility(View.VISIBLE);
            }
        }


        void resetAnimationsAndState() {
            binding.imageThumbnail.animate().cancel();
            binding.buttonItemFavorite.animate().cancel();
            binding.imageThumbnail.setAlpha(1.0f);
            binding.buttonItemFavorite.setScaleX(1.0f);
            binding.buttonItemFavorite.setScaleY(1.0f);
        }
    }

    public static boolean areAnimationsEnabled(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        } else {
            try {
                float durationScale = Settings.Global.getFloat(
                        context.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1.0f
                );
                return durationScale > 0f;
            } catch (Exception e) {
                return true;
            }
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
