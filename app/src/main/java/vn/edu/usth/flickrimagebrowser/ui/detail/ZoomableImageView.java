package vn.edu.usth.flickrimagebrowser.ui.detail;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Custom ImageView supporting pinch-to-zoom, double-tap zoom, single-tap callbacks,
 * and bounded panning. Ensures the full image is visible with no cropping (fitCenter base),
 * prevents panning outside image bounds when zoomed, preserves normalized scale/center on resize,
 * and automatically resets on photo replacement or orientation changes.
 */
public class ZoomableImageView extends AppCompatImageView {

    public static final float MIN_SCALE = 1.0f;
    public static final float TARGET_DOUBLE_TAP_SCALE = 2.5f;
    public static final float MAX_SCALE = 4.0f;

    public interface OnPhotoTapListener {
        void onPhotoTap();
    }

    private final Matrix currentMatrix = new Matrix();
    private final RectF displayRect = new RectF();

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private OnPhotoTapListener onPhotoTapListener;

    private float currentScale = MIN_SCALE;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private final PointF lastTouchPoint = new PointF();
    private boolean isDragging = false;
    private ValueAnimator zoomAnimator;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float previousScale = currentScale;
                float newScale = currentScale * scaleFactor;

                if (newScale < MIN_SCALE) {
                    newScale = MIN_SCALE;
                } else if (newScale > MAX_SCALE) {
                    newScale = MAX_SCALE;
                }

                if (Math.abs(newScale - previousScale) > 1e-4f) {
                    float factorToApply = newScale / previousScale;
                    currentScale = newScale;
                    currentMatrix.postScale(factorToApply, factorToApply, detector.getFocusX(), detector.getFocusY());
                    checkMatrixBounds();
                    setImageMatrix(currentMatrix);
                }
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (onPhotoTapListener != null) {
                    onPhotoTapListener.onPhotoTap();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float targetScale = (currentScale > MIN_SCALE + 0.1f) ? MIN_SCALE : TARGET_DOUBLE_TAP_SCALE;
                animateZoom(targetScale, e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    public void setOnPhotoTapListener(@Nullable OnPhotoTapListener listener) {
        this.onPhotoTapListener = listener;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        if (getWidth() > 0 && getHeight() > 0) {
            resetZoom();
        } else {
            post(this::resetZoom);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            if (oldw > 0 && oldh > 0 && currentScale > MIN_SCALE + 0.01f) {
                recalibrateToNewDimensions(w, h, oldw, oldh);
            } else {
                resetZoom();
            }
        }
    }

    private void recalibrateToNewDimensions(int newW, int newH, int oldW, int oldH) {
        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            resetZoom();
            return;
        }

        int dw = drawable.getIntrinsicWidth();
        int dh = drawable.getIntrinsicHeight();

        float oldBaseScale = Math.min((float) oldW / dw, (float) oldH / dh);
        float newBaseScale = Math.min((float) newW / dw, (float) newH / dh);

        if (oldBaseScale <= 0f) {
            resetZoom();
            return;
        }

        float relativeZoom = currentScale / MIN_SCALE;

        Matrix inverse = new Matrix();
        if (currentMatrix.invert(inverse)) {
            float[] centerPoint = new float[]{oldW * 0.5f, oldH * 0.5f};
            inverse.mapPoints(centerPoint);

            currentMatrix.reset();
            float effectiveScale = newBaseScale * relativeZoom;
            currentMatrix.postScale(effectiveScale, effectiveScale);

            float targetX = newW * 0.5f - centerPoint[0] * effectiveScale;
            float targetY = newH * 0.5f - centerPoint[1] * effectiveScale;
            currentMatrix.postTranslate(targetX, targetY);

            checkMatrixBounds();
            setImageMatrix(currentMatrix);
        } else {
            resetZoom();
        }
    }

    /**
     * Resets scale and translation so the entire photo is centered and visible without cropping.
     */
    public void resetZoom() {
        if (zoomAnimator != null && zoomAnimator.isRunning()) {
            zoomAnimator.cancel();
        }
        Drawable drawable = getDrawable();
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (drawable == null || viewWidth <= 0 || viewHeight <= 0) {
            currentMatrix.reset();
            displayRect.setEmpty();
            setImageMatrix(currentMatrix);
            currentScale = MIN_SCALE;
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        if (drawableWidth <= 0 || drawableHeight <= 0) {
            currentMatrix.reset();
            displayRect.setEmpty();
            setImageMatrix(currentMatrix);
            currentScale = MIN_SCALE;
            return;
        }

        currentMatrix.reset();
        // Fit center calculation: scale without cropping
        float scale = Math.min((float) viewWidth / drawableWidth, (float) viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * scale) * 0.5f;
        float dy = (viewHeight - drawableHeight * scale) * 0.5f;

        currentMatrix.postScale(scale, scale);
        currentMatrix.postTranslate(dx, dy);
        currentScale = MIN_SCALE;

        displayRect.set(0, 0, drawableWidth, drawableHeight);
        currentMatrix.mapRect(displayRect);

        setImageMatrix(currentMatrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) {
            return super.onTouchEvent(event);
        }

        boolean handled = gestureDetector.onTouchEvent(event);
        handled = scaleGestureDetector.onTouchEvent(event) || handled;

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                lastTouchPoint.set(event.getX(), event.getY());
                isDragging = false;
                if (currentScale > MIN_SCALE + 0.01f && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleGestureDetector.isInProgress() && currentScale > MIN_SCALE + 0.01f) {
                    int activeIndex = event.findPointerIndex(activePointerId);
                    if (activeIndex != -1) {
                        float x = event.getX(activeIndex);
                        float y = event.getY(activeIndex);
                        float dx = x - lastTouchPoint.x;
                        float dy = y - lastTouchPoint.y;

                        if (Math.hypot(dx, dy) > 4.0f) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            currentMatrix.postTranslate(dx, dy);
                            checkMatrixBounds();
                            setImageMatrix(currentMatrix);
                            if (getParent() != null) {
                                getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }
                        lastTouchPoint.set(x, y);
                    }
                } else if (scaleGestureDetector.isInProgress()) {
                    int activeIndex = event.findPointerIndex(activePointerId);
                    if (activeIndex != -1) {
                        lastTouchPoint.set(event.getX(activeIndex), event.getY(activeIndex));
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newPointerIndex = (pointerIndex == 0) ? 1 : 0;
                    activePointerId = event.getPointerId(newPointerIndex);
                    lastTouchPoint.set(event.getX(newPointerIndex), event.getY(newPointerIndex));
                }
                isDragging = false;
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                isDragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        return handled || super.onTouchEvent(event);
    }

    /**
     * Bounded pan & zoom: ensures that when the image is larger than the view,
     * it cannot be panned beyond viewport bounds; when smaller, it is centered.
     */
    public void checkMatrixBounds() {
        Drawable drawable = getDrawable();
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (drawable == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }

        displayRect.set(0, 0, drawableWidth, drawableHeight);
        currentMatrix.mapRect(displayRect);

        float deltaX = 0f;
        float deltaY = 0f;

        // Horizontal bounds
        if (displayRect.width() >= viewWidth) {
            if (displayRect.left > 0) {
                deltaX = -displayRect.left;
            } else if (displayRect.right < viewWidth) {
                deltaX = viewWidth - displayRect.right;
            }
        } else {
            deltaX = (viewWidth - displayRect.width()) * 0.5f - displayRect.left;
        }

        // Vertical bounds
        if (displayRect.height() >= viewHeight) {
            if (displayRect.top > 0) {
                deltaY = -displayRect.top;
            } else if (displayRect.bottom < viewHeight) {
                deltaY = viewHeight - displayRect.bottom;
            }
        } else {
            deltaY = (viewHeight - displayRect.height()) * 0.5f - displayRect.top;
        }

        if (deltaX != 0f || deltaY != 0f) {
            currentMatrix.postTranslate(deltaX, deltaY);
            displayRect.set(0, 0, drawableWidth, drawableHeight);
            currentMatrix.mapRect(displayRect);
        }
    }

    private void animateZoom(float targetScale, float focusX, float focusY) {
        if (zoomAnimator != null && zoomAnimator.isRunning()) {
            zoomAnimator.cancel();
        }

        boolean animationsEnabled = areAnimationsEnabled(getContext());
        if (!animationsEnabled) {
            float factor = targetScale / currentScale;
            currentScale = targetScale;
            currentMatrix.postScale(factor, factor, focusX, focusY);
            if (currentScale <= MIN_SCALE + 0.01f) {
                resetZoom();
            } else {
                checkMatrixBounds();
                setImageMatrix(currentMatrix);
            }
            return;
        }

        float startScale = currentScale;
        zoomAnimator = ValueAnimator.ofFloat(startScale, targetScale);
        zoomAnimator.setDuration(220);
        zoomAnimator.addUpdateListener(animation -> {
            float animatedScale = (float) animation.getAnimatedValue();
            if (currentScale > 0) {
                float factor = animatedScale / currentScale;
                currentScale = animatedScale;
                currentMatrix.postScale(factor, factor, focusX, focusY);
                if (currentScale <= MIN_SCALE + 0.01f && animatedScale == targetScale) {
                    resetZoom();
                } else {
                    checkMatrixBounds();
                    setImageMatrix(currentMatrix);
                }
            }
        });
        zoomAnimator.start();
    }

    public static boolean areAnimationsEnabled(@Nullable Context context) {
        if (context == null) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
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

    public float getCurrentScale() {
        return currentScale;
    }

    public boolean isZoomed() {
        return currentScale > MIN_SCALE + 0.01f;
    }

    public RectF getDisplayRect() {
        Drawable d = getDrawable();
        RectF rect = new RectF();
        if (d != null && d.getIntrinsicWidth() > 0 && d.getIntrinsicHeight() > 0) {
            rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            currentMatrix.mapRect(rect);
        }
        return rect;
    }
}
