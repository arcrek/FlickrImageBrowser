package vn.edu.usth.flickrimagebrowser.ui.collections;

import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import java.util.HashSet;
import java.util.Set;

import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;

/**
 * Adapter subclass supporting visual selection overlays (selection stroke and alpha scrim)
 * for multi-select deletion inside albums.
 */
public class AlbumJustifiedPhotoAdapter extends PhotoAdapter {

    private final Set<String> selectedIds = new HashSet<>();
    private boolean isSelectionMode = false;

    public AlbumJustifiedPhotoAdapter(@NonNull OnPhotoActionListener listener) {
        super(listener);
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectedIds(@Nullable Set<String> ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(@NonNull String photoId) {
        if (selectedIds.contains(photoId)) {
            selectedIds.remove(photoId);
        } else {
            selectedIds.add(photoId);
        }
        notifyDataSetChanged();
    }

    @NonNull
    public Set<String> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Photo photo = getItem(position);
        boolean isSelected = isSelectionMode && photo != null && selectedIds.contains(photo.getId());

        View root = holder.itemView;
        if (root instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) root;
            DisplayMetrics dm = card.getContext().getResources().getDisplayMetrics();
            if (isSelected) {
                card.setStrokeWidth(Math.round(3.5f * dm.density));
                card.setStrokeColor(Color.parseColor("#1688FF"));
                card.setAlpha(0.72f);
            } else if (isSelectionMode) {
                card.setStrokeWidth(Math.round(1f * dm.density));
                card.setStrokeColor(Color.parseColor("#40808080"));
                card.setAlpha(0.92f);
            } else {
                card.setStrokeWidth(0);
                card.setAlpha(1.0f);
            }
        }
    }
}
