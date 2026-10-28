package vn.edu.usth.flickrimagebrowser.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ActivityPhotoDetailBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;
import vn.edu.usth.flickrimagebrowser.ui.collections.CollectionFormDialogFragment;
import vn.edu.usth.flickrimagebrowser.util.Resource;

/**
 * Activity displaying a selected photo in full viewing mode on a permanently dark surface
 * without cropping, supporting pinch/double-tap zoom, bounded panning, single-tap fullscreen
 * toggle (with Back prioritizing exiting fullscreen and INVISIBLE chrome preserving image viewport),
 * photo metadata, ONE action row (Favorite, Add album, Share), loading/error retry preserving
 * thumbnail, gentle fade transitions, and lifecycle-safe album association with verifiable mutation results.
 */
public class PhotoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "vn.edu.usth.flickrimagebrowser.EXTRA_PHOTO";
    private static final String STATE_IS_FULLSCREEN = "state_is_fullscreen";

    private ActivityPhotoDetailBinding binding;
    private DetailViewModel viewModel;
    private List<Collection> availableCollections = null;
    private boolean isFavoriteObserverInitialized = false;
    private boolean isFullscreen = false;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull Photo photo) {
        Intent intent = new Intent(context, PhotoDetailActivity.class);
        intent.putExtra(EXTRA_PHOTO, photo);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Enforce dark system bars so status bar and navigation bar icons remain light in day & night modes
        EdgeToEdge.enable(
                this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT)
        );
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }

        // Gentle open transition respecting system animations setting
        if (ZoomableImageView.areAnimationsEnabled(this)) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(0, 0);
        }

        binding = ActivityPhotoDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            isFullscreen = savedInstanceState.getBoolean(STATE_IS_FULLSCREEN, false);
            if (isFullscreen) {
                binding.toolbarDetail.setVisibility(View.INVISIBLE);
                binding.toolbarDetail.setAlpha(0.0f);
                binding.detailScrollView.setVisibility(View.INVISIBLE);
                binding.detailScrollView.setAlpha(0.0f);
            }
        }

        setSupportActionBar(binding.toolbarDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbarDetail.setNavigationOnClickListener(v -> handleBackNavigation());

        // Back navigates out of fullscreen first before finishing activity
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        // Apply system window insets so toolbar and bottom action bar fit all screen cutouts/notches
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            binding.toolbarDetail.setPadding(insets.left, insets.top, insets.right, 0);
            binding.detailScrollView.setPadding(insets.left, 0, insets.right, insets.bottom);
            return windowInsets;
        });

        // Single tap on image toggles immersive fullscreen
        binding.imageDetailPhoto.setOnPhotoTapListener(this::toggleFullscreen);

        AppViewModelFactory factory = ((FlickrApplication) getApplication())
                .getAppContainer()
                .getViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(DetailViewModel.class);

        Photo photoExtra = IntentCompat.getSerializableExtra(
                getIntent(), EXTRA_PHOTO, Photo.class
        );
        if (photoExtra != null) {
            viewModel.setPhoto(photoExtra);
        } else {
            finish();
            return;
        }

        // Setup fragment result listener for inline album creation
        getSupportFragmentManager().setFragmentResultListener(
                CollectionFormDialogFragment.REQUEST_KEY,
                this,
                (requestKey, result) -> {
                    String collectionName = result.getString(CollectionFormDialogFragment.RESULT_COLLECTION_NAME, "");
                    long id = result.getLong(CollectionFormDialogFragment.RESULT_COLLECTION_ID, -1L);
                    if (id > 0 && !collectionName.isEmpty()) {
                        Toast.makeText(
                                this,
                                getString(R.string.detail_photo_added_success, collectionName),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        viewModel.getPhoto().observe(this, this::renderPhotoDetails);
        viewModel.getIsFavorite().observe(this, isFav -> {
            boolean favorite = Boolean.TRUE.equals(isFav);
            updateFavoriteUi(favorite, isFavoriteObserverInitialized);
            isFavoriteObserverInitialized = true;
        });
        viewModel.getCollections().observe(this, collections -> {
            this.availableCollections = collections;
        });

        // ONE row of actions: Favorite, Add to album, Share
        binding.buttonFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
        binding.buttonAddToCollection.setOnClickListener(v -> handleAddToCollection());
        binding.buttonShare.setOnClickListener(v -> sharePhoto());

        // Error retry
        binding.buttonRetryImage.setOnClickListener(v -> {
            Photo photo = viewModel.getPhoto().getValue();
            if (photo != null) {
                loadImage(photo);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_FULLSCREEN, isFullscreen);
    }

    private void handleBackNavigation() {
        if (isFullscreen) {
            setFullscreen(false);
        } else {
            finishWithTransition();
        }
    }

    private void toggleFullscreen() {
        setFullscreen(!isFullscreen);
    }

    private void setFullscreen(boolean fullscreen) {
        this.isFullscreen = fullscreen;
        boolean animationsEnabled = ZoomableImageView.areAnimationsEnabled(this);

        if (fullscreen) {
            if (animationsEnabled) {
                binding.toolbarDetail.animate()
                        .alpha(0.0f)
                        .setDuration(180)
                        .withEndAction(() -> binding.toolbarDetail.setVisibility(View.INVISIBLE))
                        .start();
                binding.detailScrollView.animate()
                        .alpha(0.0f)
                        .setDuration(180)
                        .withEndAction(() -> binding.detailScrollView.setVisibility(View.INVISIBLE))
                        .start();
            } else {
                binding.toolbarDetail.setAlpha(0.0f);
                binding.toolbarDetail.setVisibility(View.INVISIBLE);
                binding.detailScrollView.setAlpha(0.0f);
                binding.detailScrollView.setVisibility(View.INVISIBLE);
            }
        } else {
            binding.toolbarDetail.setVisibility(View.VISIBLE);
            binding.detailScrollView.setVisibility(View.VISIBLE);
            if (animationsEnabled) {
                binding.toolbarDetail.setAlpha(0.0f);
                binding.detailScrollView.setAlpha(0.0f);
                binding.toolbarDetail.animate()
                        .alpha(1.0f)
                        .setDuration(180)
                        .withEndAction(null)
                        .start();
                binding.detailScrollView.animate()
                        .alpha(1.0f)
                        .setDuration(180)
                        .withEndAction(null)
                        .start();
            } else {
                binding.toolbarDetail.setAlpha(1.0f);
                binding.detailScrollView.setAlpha(1.0f);
            }
        }
    }

    private void finishWithTransition() {
        finish();
        if (ZoomableImageView.areAnimationsEnabled(this)) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(0, 0);
        }
    }

    private void updateFavoriteUi(boolean isFavorite, boolean animatePulse) {
        if (isFavorite) {
            binding.buttonFavorite.setImageResource(R.drawable.ic_favorite_filled);
            int pinkColor = ContextCompat.getColor(this, R.color.brand_pink);
            binding.buttonFavorite.setColorFilter(pinkColor);
            binding.buttonFavorite.setContentDescription(getString(R.string.detail_action_remove_photo));
            if (animatePulse && ZoomableImageView.areAnimationsEnabled(this)) {
                binding.buttonFavorite.setScaleX(1.22f);
                binding.buttonFavorite.setScaleY(1.22f);
                binding.buttonFavorite.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180)
                        .start();
            }
        } else {
            binding.buttonFavorite.setImageResource(R.drawable.ic_favorite_border);
            binding.buttonFavorite.setColorFilter(Color.WHITE);
            binding.buttonFavorite.setContentDescription(getString(R.string.detail_action_save_photo));
        }
    }

    private void renderPhotoDetails(@Nullable Photo photo) {
        if (photo == null) {
            return;
        }

        binding.imageDetailPhoto.resetZoom();

        String title = photo.getTitle() != null ? photo.getTitle().trim() : "";
        String description = photo.getDescription() != null ? photo.getDescription().trim() : "";

        if (title.isEmpty()) {
            binding.textDetailTitle.setVisibility(View.GONE);
        } else {
            binding.textDetailTitle.setVisibility(View.VISIBLE);
            binding.textDetailTitle.setText(title);
        }

        binding.textDetailOwner.setText(getString(R.string.detail_by_owner, photo.getOwnerName()));

        if (description.isEmpty() || description.equalsIgnoreCase(title)) {
            binding.textDetailDescription.setVisibility(View.GONE);
        } else {
            binding.textDetailDescription.setVisibility(View.VISIBLE);
            binding.textDetailDescription.setText(description);
        }

        loadImage(photo);
    }

    private void loadImage(@NonNull Photo photo) {
        binding.progressDetailImage.setVisibility(View.VISIBLE);
        binding.layoutImageError.setVisibility(View.GONE);

        // Load detail URL; thumbnail loaded first. On detail failure, preserve thumbnail without overwriting.
        Glide.with(this)
                .load(photo.getDetailUrl())
                .thumbnail(Glide.with(this).load(photo.getThumbnailUrl()).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        binding.progressDetailImage.setVisibility(View.GONE);
                        binding.layoutImageError.setVisibility(View.VISIBLE);
                        // Returning false preserves any thumbnail already in the target
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        binding.progressDetailImage.setVisibility(View.GONE);
                        binding.layoutImageError.setVisibility(View.GONE);
                        // Do not resetZoom here before drawable is set on target; ZoomableImageView.setImageDrawable
                        // resets zoom synchronously when laid out with the new drawable.
                        return false;
                    }
                })
                .into(binding.imageDetailPhoto);
    }

    /**
     * Distinguishes loading vs empty vs populated album states, offering inline album creation.
     */
    private void handleAddToCollection() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Photo photo = viewModel.getPhoto().getValue();
        if (photo == null) {
            return;
        }

        // 1. Loading state: collections have not yet arrived from database
        if (availableCollections == null) {
            Toast.makeText(this, R.string.detail_albums_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Empty state: collections arrived, but user has zero albums
        if (availableCollections.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.detail_albums_empty_title)
                    .setMessage(R.string.detail_albums_empty_message)
                    .setPositiveButton(R.string.detail_action_create_album, (d, which) -> showCreateAlbumDialog(photo))
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return;
        }

        // 3. Populated state: list albums plus inline "Create new album" option at top
        List<String> options = new ArrayList<>();
        options.add("+ " + getString(R.string.detail_action_create_album));
        for (Collection c : availableCollections) {
            options.add(c.getName());
        }

        String[] items = options.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.detail_add_to_album_title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showCreateAlbumDialog(photo);
                    } else {
                        Collection selected = availableCollections.get(which - 1);
                        addPhotoToAlbum(selected);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showCreateAlbumDialog(@NonNull Photo photo) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        CollectionFormDialogFragment.newInstanceForPhoto(photo)
                .show(getSupportFragmentManager(), CollectionFormDialogFragment.TAG);
    }

    private void addPhotoToAlbum(@NonNull Collection collection) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        viewModel.addPhotoToCollection(collection.getId()).observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(
                        this,
                        getString(R.string.detail_photo_added_success, collection.getName()),
                        Toast.LENGTH_SHORT
                ).show();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                String errorMsg = resource.getMessage();
                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                    errorMsg = getString(R.string.detail_photo_add_failure);
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sharePhoto() {
        Photo photo = viewModel.getPhoto().getValue();
        if (photo == null) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, photo.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, photo.getTitle() + "\n" + photo.getDetailUrl());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_photo_title)));
    }
}
