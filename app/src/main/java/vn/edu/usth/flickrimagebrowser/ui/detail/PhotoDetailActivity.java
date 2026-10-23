package vn.edu.usth.flickrimagebrowser.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import vn.edu.usth.flickrimagebrowser.FlickrApplication;
import vn.edu.usth.flickrimagebrowser.R;
import vn.edu.usth.flickrimagebrowser.databinding.ActivityPhotoDetailBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.domain.model.Collection;
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
            binding.buttonFavorite.setImageResource(
                    favorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border
            );
        });

        binding.buttonFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
        binding.buttonAddToCollection.setOnClickListener(v -> observeCollectionsForPicker());
        binding.toolbarDetail.inflateMenu(R.menu.photo_detail_menu);
        binding.toolbarDetail.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share_photo) {
                shareCurrentPhoto();
                return true;
            }
            return false;
        });
    }

    private void observeCollectionsForPicker() {
        Observer<List<Collection>> observer = new Observer<List<Collection>>() {
            @Override
            public void onChanged(List<Collection> collections) {
                if (collections == null) {
                    return;
                }
                viewModel.getCollections().removeObserver(this);
                showCollectionPicker(collections);
            }
        };
        viewModel.getCollections().observe(this, observer);
    }

    private void showCollectionPicker(@NonNull List<Collection> collections) {
        if (collections.isEmpty()) {
            Toast.makeText(this, R.string.create_collection_before_adding_photo, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[collections.size()];
        for (int i = 0; i < collections.size(); i++) {
            names[i] = collections.get(i).getName();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_to_collection_title)
                .setItems(names, (dialog, which) -> {
                    Collection selected = collections.get(which);
                    viewModel.addPhotoToCollection(selected.getId());
                    Toast.makeText(this, getString(R.string.photo_added_to_collection, selected.getName()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void shareCurrentPhoto() {
        Photo photo = viewModel.getCurrentPhoto();
        if (photo == null) {
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, photo.getTitle() + "\n" + photo.getDetailUrl());
        try {
            startActivity(Intent.createChooser(share, getString(R.string.action_share)));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.share_unavailable, Toast.LENGTH_SHORT).show();
        }
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
