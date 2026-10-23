package vn.edu.usth.flickrimagebrowser.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ActivityPhotoDetailBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;

/**
 * Activity displaying a selected Flickr photo's larger image, metadata, and favorite toggle.
 */
public class PhotoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "vn.edu.usth.flickrimagebrowser.EXTRA_PHOTO";

    private ActivityPhotoDetailBinding binding;
    private DetailViewModel viewModel;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull Photo photo) {
        Intent intent = new Intent(context, PhotoDetailActivity.class);
        intent.putExtra(EXTRA_PHOTO, photo);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarDetail.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        AppViewModelFactory factory = ((FlickrApplication) getApplication())
                .getAppContainer()
                .getViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(DetailViewModel.class);

        Photo photoExtra = androidx.core.content.IntentCompat.getSerializableExtra(
                getIntent(), EXTRA_PHOTO, Photo.class
        );
        if (photoExtra != null) {
            viewModel.setPhoto(photoExtra);
        } else {
            finish();
            return;
        }

        viewModel.getPhoto().observe(this, this::renderPhotoDetails);
        viewModel.getIsFavorite().observe(this, isFav -> {
            boolean favorite = Boolean.TRUE.equals(isFav);
            if (favorite) {
                binding.buttonFavorite.setImageResource(R.drawable.ic_favorite_filled);
                binding.buttonFavorite.clearColorFilter();
            } else {
                binding.buttonFavorite.setImageResource(R.drawable.ic_favorite_border);
                binding.buttonFavorite.setColorFilter(Color.WHITE);
            }
        });

        binding.buttonFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
    }

    private void renderPhotoDetails(@Nullable Photo photo) {
        if (photo == null) {
            return;
        }
        binding.textDetailTitle.setText(photo.getTitle());
        binding.textDetailOwner.setText(getString(R.string.detail_by_owner, photo.getOwnerName()));
        String description = photo.getDescription().isEmpty()
                ? getString(R.string.detail_no_description)
                : photo.getDescription();
        binding.textDetailDescription.setText(description);

        Glide.with(this)
                .load(photo.getDetailUrl())
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_broken_image)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.imageDetailPhoto);
    }
}
