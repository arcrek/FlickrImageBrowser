package vn.edu.usth.flickrimagebrowser.ui.collections;

import static org.mockito.Mockito.*;

import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;

import vn.edu.usth.flickrimagebrowser.util.Resource;

public class CollectionsFragmentTest {
    @Rule
    public final InstantTaskExecutorRule executorRule = new InstantTaskExecutorRule();

    @Test
    public void save_keepsFormOpenOnFailureAndDismissesOnlyAfterSuccess() throws Exception {
        CollectionsFragment fragment = spy(new CollectionsFragment());
        LifecycleOwner owner = mock(LifecycleOwner.class);
        LifecycleRegistry lifecycle = new LifecycleRegistry(owner);
        when(owner.getLifecycle()).thenReturn(lifecycle);
        lifecycle.setCurrentState(Lifecycle.State.STARTED);
        doReturn(owner).when(fragment).getViewLifecycleOwner();
        AlertDialog dialog = mock(AlertDialog.class);
        Button button = mock(Button.class);
        TextInputLayout input = mock(TextInputLayout.class);
        when(dialog.getButton(AlertDialog.BUTTON_POSITIVE)).thenReturn(button);
        when(dialog.isShowing()).thenReturn(true);
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>(Resource.loading(null));
        Method observe = CollectionsFragment.class.getDeclaredMethod(
                "observeSave", AlertDialog.class, TextInputLayout.class, LiveData.class);
        observe.setAccessible(true);

        observe.invoke(fragment, dialog, input, result);
        verify(button).setEnabled(false);
        verify(dialog, never()).dismiss();
        result.setValue(Resource.error("Unable to save collection. Please try again.", null));
        verify(input).setError("Unable to save collection. Please try again.");
        verify(button).setEnabled(true);
        verify(dialog, never()).dismiss();
        org.junit.Assert.assertFalse(result.hasObservers());

        MutableLiveData<Resource<Boolean>> retry = new MutableLiveData<>(Resource.loading(null));
        observe.invoke(fragment, dialog, input, retry);
        retry.setValue(Resource.success(true));
        verify(dialog).dismiss();
        org.junit.Assert.assertFalse(retry.hasObservers());
    }
}