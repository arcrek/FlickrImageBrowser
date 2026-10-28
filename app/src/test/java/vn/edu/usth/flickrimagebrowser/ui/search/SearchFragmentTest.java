package vn.edu.usth.flickrimagebrowser.ui.search;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vn.edu.usth.flickrimagebrowser.databinding.FragmentSearchBinding;
import vn.edu.usth.flickrimagebrowser.domain.model.Photo;
import vn.edu.usth.flickrimagebrowser.ui.common.PhotoAdapter;
import vn.edu.usth.flickrimagebrowser.util.Resource;

public class SearchFragmentTest {
    @Test
    public void newQueryLoading_dismissesPreviousPaginationRetry() throws Exception {
        SearchFragment fragment = new SearchFragment();
        Snackbar snackbar = mock(Snackbar.class);
        FragmentSearchBinding binding = mock(FragmentSearchBinding.class);
        for (String name : new String[]{"progressLoading", "recyclerPhotos", "layoutState", "swipeRefresh"}) {
            Field field = FragmentSearchBinding.class.getField(name);
            field.setAccessible(true);
            field.set(binding, mock(field.getType()));
        }
        field("binding").set(fragment, binding);
        field("paginationErrorSnackbar").set(fragment, snackbar);
        Method render = SearchFragment.class.getDeclaredMethod("renderState", Resource.class);
        render.setAccessible(true);

        render.invoke(fragment, Resource.loading(null));

        verify(snackbar).dismiss();
        assertNull(field("paginationErrorSnackbar").get(fragment));
    }

    @Test
    public void destroyView_dismissesPreviousPaginationRetry() throws Exception {
        SearchFragment fragment = new SearchFragment();
        Snackbar snackbar = mock(Snackbar.class);
        field("paginationErrorSnackbar").set(fragment, snackbar);

        fragment.onDestroyView();

        verify(snackbar).dismiss();
        assertNull(field("paginationErrorSnackbar").get(fragment));
    }

    @Test
    public void freshAdapter_receivesRetainedLoadingAndErrorData() throws Exception {
        List<Photo> photos = Collections.singletonList(
                new Photo(
                        "42", "Lake", "Author", "", "thumb", "detail", false, 0L));
        for (Resource<List<Photo>> state :
                Arrays.asList(Resource.loading(photos), Resource.error("Timeout", photos))) {
            SearchFragment fragment = new SearchFragment();
            FragmentSearchBinding binding = mock(FragmentSearchBinding.class);
            for (String name : new String[]{"progressLoading", "recyclerPhotos", "layoutState", "swipeRefresh"}) {
                Field field = FragmentSearchBinding.class.getField(name);
                field.setAccessible(true);
                field.set(binding, mock(field.getType()));
            }
            PhotoAdapter adapter = mock(
                    PhotoAdapter.class);
            field("binding").set(fragment, binding);
            field("photoAdapter").set(fragment, adapter);
            Snackbar snackbar = mock(Snackbar.class);
            Mockito.when(snackbar.setAction(ArgumentMatchers.anyInt(),
                    ArgumentMatchers.any())).thenReturn(snackbar);
            try (MockedStatic<Snackbar> factory = Mockito.mockStatic(Snackbar.class)) {
                factory.when(() -> Snackbar.make(ArgumentMatchers.any(),
                        ArgumentMatchers.any(CharSequence.class), ArgumentMatchers.anyInt()))
                        .thenReturn(snackbar);
                Method render = SearchFragment.class.getDeclaredMethod("renderState", Resource.class);
                render.setAccessible(true);
                render.invoke(fragment, state);
            }
            verify(binding.swipeRefresh).setEnabled(true);
            verify(adapter).submitList(photos);
            verify(binding.recyclerPhotos).setVisibility(View.VISIBLE);
        }
    }

    @Test
    public void searchButton_refreshesOnlyWhenSubmittingActiveQuery() throws Exception {
        for (boolean sameQuery : new boolean[]{false, true}) {
            SearchFragment fragment = new SearchFragment();
            FragmentSearchBinding binding = mock(FragmentSearchBinding.class);
            Field editQuery = FragmentSearchBinding.class.getField("editQuery");
            editQuery.setAccessible(true);
            com.google.android.material.textfield.TextInputEditText input =
                    mock(com.google.android.material.textfield.TextInputEditText.class);
            android.text.Editable text = mock(android.text.Editable.class);
            Mockito.when(text.toString()).thenReturn(" cat ");
            Mockito.when(input.getText()).thenReturn(text);
            editQuery.set(binding, input);
            SearchViewModel viewModel = mock(SearchViewModel.class);
            Mockito.when(viewModel.getLastQuery()).thenReturn(sameQuery ? "cat" : "dog");
            field("binding").set(fragment, binding);
            field("viewModel").set(fragment, viewModel);
            Method trigger = SearchFragment.class.getDeclaredMethod("triggerSearch");
            trigger.setAccessible(true);
            trigger.invoke(fragment);
            verify(viewModel).submitQuery(" cat ", sameQuery);
        }
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = SearchFragment.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}