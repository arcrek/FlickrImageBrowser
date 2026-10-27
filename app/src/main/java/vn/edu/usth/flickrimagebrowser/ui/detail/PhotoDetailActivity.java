package vn.edu.usth.flickrimagebrowser.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ActivityPhotoDetailBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.AppViewModelFactory;

/**
 * Activity displaying a selected Flickr photo's larger image, metadata, favorite toggle,
 * collection assignment, and share action.
 */
public class PhotoDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO = "vn.edu.usth.flickrimagebrowser.EXTRA_PHOTO";

    private ActivityPhotoDetailBinding binding;
    private DetailViewModel viewModel;
    private List<Collection> availableCollections = new ArrayList<>();

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull Photo photo) {
        Intent intent = new Intent(context, PhotoDetailActivity.class);
        intent.putExtra(EXTRA_PHOTO, photo);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarDetail.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Apply system window insets so toolbar and content fit all types of devices
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            binding.toolbarDetail.setPadding(insets.left, insets.top, insets.right, 0);
            binding.detailScrollView.setPadding(insets.left, 0, insets.right, insets.bottom);
            return windowInsets;
        });

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
        viewModel.getCollections().observe(this, collections -> {
            if (collections != null) {
                this.availableCollections = collections;
            }
        });

        binding.buttonFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
        binding.buttonAddToCollection.setOnClickListener(v -> showAddToCollectionDialog(availableCollections));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add_to_collection) {
            showAddToCollectionDialog(availableCollections);
            return true;
        } else if (itemId == R.id.action_share) {
            sharePhoto();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                .thumbnail(Glide.with(this).load(photo.getThumbnailUrl()))
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_broken_image)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.imageDetailPhoto);
    }

    private void showAddToCollectionDialog(@Nullable List<Collection> collections) {
        if (collections == null || collections.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.action_add_to_collection)
                    .setMessage(R.string.dialog_no_collections_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String[] names = new String[collections.size()];
        for (int i = 0; i < collections.size(); i++) {
            names[i] = collections.get(i).getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_select_collection_title)
                .setItems(names, (dialog, which) -> {
                    Collection selected = collections.get(which);
                    viewModel.addPhotoToCollection(selected.getId());
                    Toast.makeText(
                            this,
                            getString(R.string.dialog_photo_added_to_collection, selected.getName()),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
