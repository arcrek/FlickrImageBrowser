package vn.edu.usth.flickrimagebrowser.ui.search;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.android.material.snackbar.Snackbar;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import vn.edu.usth.flickrimagebrowser.databinding.FragmentSearchBinding;
import vn.edu.usth.flickrimagebrowser.util.Resource;

public class SearchFragmentTest {
    @Test
    public void newQueryLoading_dismissesPreviousPaginationRetry() throws Exception {
        SearchFragment fragment = new SearchFragment();
        Snackbar snackbar = mock(Snackbar.class);
        FragmentSearchBinding binding = mock(FragmentSearchBinding.class);
        for (String name : new String[]{"progressLoading", "recyclerPhotos", "layoutState"}) {
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

    private static Field field(String name) throws NoSuchFieldException {
        Field field = SearchFragment.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
