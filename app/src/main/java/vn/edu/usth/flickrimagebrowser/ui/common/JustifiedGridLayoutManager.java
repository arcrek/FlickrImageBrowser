package vn.edu.usth.flickrimagebrowser.ui.common;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.domain.model.Photo;

/**
 * Custom RecyclerView LayoutManager implementing Flickr-style justified photo rows.
 * Photos within each row share an identical row height while width varies according
 * to each photo's source aspect ratio (including wide panoramas). 2-4dp gaps separate adjacent photos.
 */
public class JustifiedGridLayoutManager extends RecyclerView.LayoutManager {

    private static final int DEFAULT_GAP_DP = 3;
    private static final int PORTRAIT_TARGET_ROW_HEIGHT_DP = 160;
    private static final int LANDSCAPE_TARGET_ROW_HEIGHT_DP = 175;
    private static final int DEFAULT_MIN_TILE_WIDTH_DP = 96;
    private static final float DEFAULT_ASPECT_RATIO = 1.0f;
    private static final float MAX_ROW_EXPANSION_FACTOR = 1.35f;

    private final Context context;
    private final int gapPx;
    private int targetRowHeightPx;
    private int minTileWidthPx;

    private List<Photo> photos = new ArrayList<>();
    private final List<RowFrame> itemFrames = new ArrayList<>();
    private int totalContentHeight = 0;
    private int verticalScrollOffset = 0;
    private boolean layoutDirty = true;
    private int lastComputedWidth = -1;

    @Nullable
    private SavedState pendingSavedState;

    /**
     * Pure Java geometry structure avoiding Android Rect stubbing limitations in JVM unit tests.
     */
    public static class RowFrame {
        public int left;
        public int top;
        public int right;
        public int bottom;

        public RowFrame() {
        }

        public RowFrame(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }
    }

    public JustifiedGridLayoutManager(@NonNull Context context) {
        this.context = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        this.gapPx = Math.max(1, Math.round(DEFAULT_GAP_DP * dm.density));
        updateTargetRowHeight();
    }

    private void updateTargetRowHeight() {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidthDp = context.getResources().getConfiguration().screenWidthDp;
        int targetDp = screenWidthDp >= 600 ? LANDSCAPE_TARGET_ROW_HEIGHT_DP : PORTRAIT_TARGET_ROW_HEIGHT_DP;
        this.targetRowHeightPx = Math.round(targetDp * dm.density);
        this.minTileWidthPx = Math.max(1, Math.round(DEFAULT_MIN_TILE_WIDTH_DP * dm.density));
    }

    /**
     * Shared API for Albums and Search: updates photo collection for aspect ratio computations.
     */
    public void setPhotos(@Nullable List<Photo> newPhotos) {
        this.photos = newPhotos != null ? new ArrayList<>(newPhotos) : new ArrayList<>();
        this.layoutDirty = true;
        requestLayout();
    }

    /**
     * Called asynchronously when a photo's decoded dimensions become known.
     * Invalidates row geometry while preserving the current scroll anchor position.
     */
    public void invalidateDimensions() {
        if (pendingSavedState == null) {
            int anchor = findFirstVisibleItemPosition();
            if (anchor != RecyclerView.NO_POSITION && anchor < itemFrames.size()) {
                pendingSavedState = new SavedState();
                pendingSavedState.anchorPosition = anchor;
                pendingSavedState.anchorOffset =
                        verticalScrollOffset - (itemFrames.get(anchor).top - getPaddingTop());
                pendingSavedState.scrollOffset = verticalScrollOffset;
            }
        }
        this.layoutDirty = true;
        requestLayout();
    }

    @NonNull
    public List<Photo> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            itemFrames.clear();
            totalContentHeight = 0;
            verticalScrollOffset = 0;
            layoutDirty = false;
            return;
        }

        int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (availableWidth <= 0) {
            return;
        }

        updateTargetRowHeight();

        if (layoutDirty || availableWidth != lastComputedWidth || itemFrames.size() != state.getItemCount()) {
            computeLayoutFrames(availableWidth, state.getItemCount());
            lastComputedWidth = availableWidth;
            layoutDirty = false;
        }

        if (pendingSavedState != null) {
            if (pendingSavedState.anchorPosition >= 0 && pendingSavedState.anchorPosition < itemFrames.size()) {
                verticalScrollOffset = (itemFrames.get(pendingSavedState.anchorPosition).top - getPaddingTop())
                        + pendingSavedState.anchorOffset;
            } else {
                verticalScrollOffset = pendingSavedState.scrollOffset;
            }
            pendingSavedState = null;
        }

        int maxScroll = Math.max(0, totalContentHeight - (getHeight() - getPaddingTop() - getPaddingBottom()));
        verticalScrollOffset = Math.max(0, Math.min(verticalScrollOffset, maxScroll));

        detachAndScrapAttachedViews(recycler);
        fillInitial(recycler, state);
    }

    private void computeLayoutFrames(int availableWidth, int itemCount) {
        List<Float> ratios = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            float ratio = DEFAULT_ASPECT_RATIO;
            if (i < photos.size()) {
                Photo photo = photos.get(i);
                if (photo != null) {
                    if (photo.hasDimensions()) {
                        ratio = photo.getAspectRatio();
                    } else {
                        Float cached = PhotoDimensionCache.getAspectRatio(photo.getThumbnailUrl());
                        if (cached != null && cached > 0f && Float.isFinite(cached)) {
                            ratio = cached;
                        }
                    }
                }
            }
            if (ratio <= 0f || Float.isNaN(ratio) || Float.isInfinite(ratio)) {
                ratio = DEFAULT_ASPECT_RATIO;
            }
            ratios.add(ratio);
        }

        itemFrames.clear();
        totalContentHeight = calculateRowGeometry(
                availableWidth,
                getPaddingLeft(),
                getPaddingTop(),
                gapPx,
                targetRowHeightPx,
                minTileWidthPx,
                ratios,
                MAX_ROW_EXPANSION_FACTOR,
                itemFrames
        );
    }

    /**
     * Pure geometry calculation determining edge-to-edge justified rows.
     * Uses RowFrame to execute safely in JVM unit tests.
     */
    @VisibleForTesting
    public static int calculateRowGeometry(
            int availableWidth,
            int paddingLeft,
            int paddingTop,
            int gap,
            int targetRowHeight,
            int minTileWidth,
            List<Float> ratios,
            float maxExpansionFactor,
            List<RowFrame> outFrames
    ) {
        int currentY = paddingTop;
        int n = ratios.size();
        int rowStartIndex = 0;

        while (rowStartIndex < n) {
            float rowAspectSum = 0f;
            float minRatioInRow = Float.MAX_VALUE;
            int count = 0;

            for (int i = rowStartIndex; i < n; i++) {
                float r = ratios.get(i);
                float candidateAspectSum = rowAspectSum + r;
                float candidateMinRatio = Math.min(minRatioInRow, r);
                int candidateCount = count + 1;
                int candidateTotalGaps = (candidateCount - 1) * gap;
                int candidateRemainingWidth = Math.max(1, availableWidth - candidateTotalGaps);
                int naturalRowWidth = Math.round(candidateAspectSum * targetRowHeight) + candidateTotalGaps;

                if (count >= 1) {
                    float candidateRowHeight = candidateRemainingWidth / candidateAspectSum;
                    if (candidateRowHeight * candidateMinRatio < minTileWidth && naturalRowWidth >= availableWidth) {
                        break;
                    }
                }

                rowAspectSum = candidateAspectSum;
                minRatioInRow = candidateMinRatio;
                count++;

                // If adding this photo fills the row, or single photo is wider than row (e.g. panorama)
                if (naturalRowWidth >= availableWidth || i == n - 1) {
                    break;
                }
            }

            int totalGaps = (count - 1) * gap;
            int remainingWidthForPhotos = Math.max(1, availableWidth - totalGaps);
            boolean isLastRow = (rowStartIndex + count >= n);

            int actualRowHeight;
            if (rowAspectSum > 0f) {
                float calculatedHeight = remainingWidthForPhotos / rowAspectSum;
                if (isLastRow && calculatedHeight > (targetRowHeight * maxExpansionFactor)) {
                    actualRowHeight = targetRowHeight;
                } else {
                    actualRowHeight = Math.max(1, Math.round(calculatedHeight));
                }
            } else {
                actualRowHeight = targetRowHeight;
            }

            int currentX = paddingLeft;
            int allocatedWidth = 0;

            for (int j = 0; j < count; j++) {
                int index = rowStartIndex + j;
                float r = ratios.get(index);
                int itemWidth;

                if (isLastRow && actualRowHeight == targetRowHeight) {
                    int desiredWidth = Math.round(actualRowHeight * r);
                    itemWidth = Math.max(1, Math.min(remainingWidthForPhotos - allocatedWidth, desiredWidth));
                    allocatedWidth += itemWidth;
                } else if (j == count - 1) {
                    itemWidth = Math.max(1, remainingWidthForPhotos - allocatedWidth);
                } else {
                    itemWidth = Math.max(1, Math.round(actualRowHeight * r));
                    allocatedWidth += itemWidth;
                }

                RowFrame frame = new RowFrame(currentX, currentY, currentX + itemWidth, currentY + actualRowHeight);
                outFrames.add(frame);

                currentX += itemWidth + gap;
            }

            currentY += actualRowHeight + gap;
            rowStartIndex += count;
        }
        return currentY;
    }

    @VisibleForTesting
    public static int calculateRowGeometry(
            int availableWidth,
            int paddingLeft,
            int paddingTop,
            int gap,
            int targetRowHeight,
            List<Float> ratios,
            float maxExpansionFactor,
            List<RowFrame> outFrames
    ) {
        return calculateRowGeometry(
                availableWidth,
                paddingLeft,
                paddingTop,
                gap,
                targetRowHeight,
                Math.round(targetRowHeight * 0.6f),
                ratios,
                maxExpansionFactor,
                outFrames
        );
    }

    private void fillInitial(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int itemCount = Math.min(state.getItemCount(), itemFrames.size());
        if (itemCount <= 0) {
            return;
        }
        int viewportTop = verticalScrollOffset + getPaddingTop();
        int viewportBottom = verticalScrollOffset + getHeight() - getPaddingBottom();

        int firstPos = findFirstFrameIndex(viewportTop);
        int lastPos = findLastFrameIndex(viewportBottom, itemCount);

        for (int i = firstPos; i <= lastPos && i < itemCount; i++) {
            RowFrame frame = itemFrames.get(i);
            View view = recycler.getViewForPosition(i);
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            if (lp != null) {
                lp.width = frame.width();
                lp.height = frame.height();
            }
            addView(view);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(frame.width(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(frame.height(), View.MeasureSpec.EXACTLY);
            view.measure(widthSpec, heightSpec);

            int left = frame.left;
            int top = frame.top - verticalScrollOffset;
            int right = frame.right;
            int bottom = frame.bottom - verticalScrollOffset;
            layoutDecoratedWithMargins(view, left, top, right, bottom);
        }
    }

    private int findFirstFrameIndex(int viewportTop) {
        if (itemFrames.isEmpty()) {
            return 0;
        }
        int low = 0;
        int high = itemFrames.size() - 1;
        int result = itemFrames.size();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (itemFrames.get(mid).bottom >= viewportTop) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return Math.max(0, Math.min(result, itemFrames.size() - 1));
    }

    private int findLastFrameIndex(int viewportBottom, int itemCount) {
        if (itemFrames.isEmpty() || itemCount <= 0) {
            return -1;
        }
        int low = 0;
        int high = Math.min(itemFrames.size() - 1, itemCount - 1);
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (itemFrames.get(mid).top <= viewportBottom) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result >= 0 ? Math.min(result, itemCount - 1) : -1;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dy == 0) {
            return 0;
        }

        int maxScroll = Math.max(0, totalContentHeight - (getHeight() - getPaddingTop() - getPaddingBottom()));
        int targetScroll = verticalScrollOffset + dy;
        int clampedScroll = Math.max(0, Math.min(targetScroll, maxScroll));
        int consumed = clampedScroll - verticalScrollOffset;

        if (consumed == 0) {
            return 0;
        }

        verticalScrollOffset = clampedScroll;
        offsetChildrenVertical(-consumed);

        // Recycle off-screen children
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child != null) {
                if (getDecoratedBottom(child) < 0 || getDecoratedTop(child) > getHeight()) {
                    removeAndRecycleView(child, recycler);
                }
            }
        }

        int itemCount = Math.min(state.getItemCount(), itemFrames.size());
        if (itemCount <= 0) {
            return consumed;
        }

        // Fill newly uncovered items bounded by viewport
        int viewportTop = verticalScrollOffset + getPaddingTop();
        int viewportBottom = verticalScrollOffset + getHeight() - getPaddingBottom();

        int firstPos = findFirstFrameIndex(viewportTop);
        int lastPos = findLastFrameIndex(viewportBottom, itemCount);
        for (int i = firstPos; i <= lastPos && i < itemCount; i++) {
            if (findViewByPosition(i) == null) {
                RowFrame frame = itemFrames.get(i);
                View view = recycler.getViewForPosition(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (lp != null) {
                    lp.width = frame.width();
                    lp.height = frame.height();
                }
                addView(view);

                int widthSpec = View.MeasureSpec.makeMeasureSpec(frame.width(), View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(frame.height(), View.MeasureSpec.EXACTLY);
                view.measure(widthSpec, heightSpec);

                int left = frame.left;
                int top = frame.top - verticalScrollOffset;
                int right = frame.right;
                int bottom = frame.bottom - verticalScrollOffset;
                layoutDecoratedWithMargins(view, left, top, right, bottom);
            }
        }

        return consumed;
    }

    /**
     * Shared API for Albums / endless scroll: returns adapter position of last visible photo.
     */
    public int findLastVisibleItemPosition() {
        int viewportTop = verticalScrollOffset + getPaddingTop();
        int viewportBottom = verticalScrollOffset + getHeight() - getPaddingBottom();
        int lastPos = RecyclerView.NO_POSITION;

        for (int i = 0; i < itemFrames.size(); i++) {
            RowFrame frame = itemFrames.get(i);
            if (frame.bottom > viewportTop && frame.top < viewportBottom) {
                lastPos = i;
            }
        }
        return lastPos;
    }

    /**
     * Returns adapter position of first visible photo.
     */
    public int findFirstVisibleItemPosition() {
        int viewportTop = verticalScrollOffset + getPaddingTop();
        int viewportBottom = verticalScrollOffset + getHeight() - getPaddingBottom();

        for (int i = 0; i < itemFrames.size(); i++) {
            RowFrame frame = itemFrames.get(i);
            if (frame.bottom > viewportTop && frame.top < viewportBottom) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public void scrollToPosition(int position) {
        scrollToPositionWithOffset(position, getPaddingTop());
    }

    public void scrollToPositionWithOffset(int position, int offset) {
        if (position < 0) {
            return;
        }
        pendingSavedState = new SavedState();
        pendingSavedState.anchorPosition = position;
        pendingSavedState.anchorOffset = getPaddingTop() - offset;
        if (position < itemFrames.size()) {
            verticalScrollOffset = Math.max(0, itemFrames.get(position).top - offset);
        } else if (position == 0) {
            verticalScrollOffset = 0;
        }
        requestLayout();
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return verticalScrollOffset;
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return getHeight();
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return Math.max(getHeight(), totalContentHeight);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.scrollOffset = verticalScrollOffset;
        state.anchorPosition = findFirstVisibleItemPosition();
        if (state.anchorPosition != RecyclerView.NO_POSITION && state.anchorPosition < itemFrames.size()) {
            state.anchorOffset = verticalScrollOffset - (itemFrames.get(state.anchorPosition).top - getPaddingTop());
        }
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            this.pendingSavedState = (SavedState) state;
            requestLayout();
        }
    }

    public static class SavedState implements Parcelable {
        int scrollOffset;
        int anchorPosition = RecyclerView.NO_POSITION;
        int anchorOffset = 0;

        public SavedState() {
        }

        protected SavedState(Parcel in) {
            scrollOffset = in.readInt();
            anchorPosition = in.readInt();
            anchorOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(scrollOffset);
            dest.writeInt(anchorPosition);
            dest.writeInt(anchorOffset);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
