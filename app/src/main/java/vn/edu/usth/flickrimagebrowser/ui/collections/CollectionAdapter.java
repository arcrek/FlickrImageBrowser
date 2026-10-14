package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ItemCollectionBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;

/**
 * RecyclerView adapter displaying user-created photo collections.
 */
public class CollectionAdapter extends ListAdapter<Collection, CollectionAdapter.CollectionViewHolder> {

    public CollectionAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCollectionBinding binding = ItemCollectionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CollectionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {

        private final ItemCollectionBinding binding;

        CollectionViewHolder(@NonNull ItemCollectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Collection collection) {
            binding.textCollectionName.setText(collection.getName());
            binding.textCollectionCount.setText(
                    binding.getRoot().getContext().getString(
                            R.string.collection_photo_count,
                            collection.getPhotoCount()
                    )
            );
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
