package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ItemCollectionBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;

/**
 * RecyclerView adapter displaying user-created photo collections with options to rename and delete.
 */
public class CollectionAdapter extends ListAdapter<Collection, CollectionAdapter.CollectionViewHolder> {

    /**
     * Listener callback for collection item actions.
     */
    public interface OnCollectionActionListener {
        void onCollectionClick(@NonNull Collection collection);
        void onRenameCollection(@NonNull Collection collection);
        void onDeleteCollection(@NonNull Collection collection);
    }

    @Nullable
    private final OnCollectionActionListener actionListener;

    public CollectionAdapter(@Nullable OnCollectionActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    public CollectionAdapter() {
        this(null);
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCollectionBinding binding = ItemCollectionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CollectionViewHolder(binding, actionListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {

        private final ItemCollectionBinding binding;
        @Nullable
        private final OnCollectionActionListener actionListener;

        CollectionViewHolder(@NonNull ItemCollectionBinding binding,
                             @Nullable OnCollectionActionListener actionListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.actionListener = actionListener;
        }

        void bind(@NonNull Collection collection) {
            Context context = binding.getRoot().getContext();
            binding.textCollectionName.setText(collection.getName());
            binding.imageCollectionCover.setContentDescription(
                    context.getString(R.string.content_desc_album_cover, collection.getName())
            );
            binding.buttonCollectionMenu.setContentDescription(
                    context.getString(R.string.content_desc_album_menu, collection.getName())
            );

            int count = collection.getPhotoCount();
            String countStr = context.getResources().getQuantityString(
                    R.plurals.album_photo_count,
                    count,
                    count
            );
            binding.textCollectionCount.setText(countStr);

            String desc = collection.getDescription();
            if (!desc.isEmpty()) {
                binding.textCollectionDescription.setText(desc);
                binding.textCollectionDescription.setVisibility(View.VISIBLE);
            } else {
                binding.textCollectionDescription.setVisibility(View.GONE);
            }

            String coverUrl = collection.getCoverPhotoUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                Glide.with(context)
                        .load(coverUrl)
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .error(R.drawable.ic_photo_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .centerCrop()
                        .into(binding.imageCollectionCover);
            } else {
                Glide.with(context).clear(binding.imageCollectionCover);
                binding.imageCollectionCover.setImageResource(R.drawable.ic_photo_placeholder);
            }
            binding.getRoot().setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onCollectionClick(collection);
                }
            });

            binding.buttonCollectionMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 1, 0, R.string.action_rename);
                popup.getMenu().add(0, 2, 1, R.string.action_delete);
                popup.setOnMenuItemClickListener(item -> {
                    if (actionListener == null) {
                        return false;
                    }
                    if (item.getItemId() == 1) {
                        actionListener.onRenameCollection(collection);
                        return true;
                    } else if (item.getItemId() == 2) {
                        actionListener.onDeleteCollection(collection);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private static final DiffUtil.ItemCallback<Collection> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Collection>() {
                @Override
                public boolean areItemsTheSame(@NonNull Collection oldItem, @NonNull Collection newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Collection oldItem, @NonNull Collection newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
