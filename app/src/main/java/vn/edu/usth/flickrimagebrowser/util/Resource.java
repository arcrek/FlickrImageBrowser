package vn.edu.usth.flickrimagebrowser.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Generic state wrapper representing Loading, Success, Empty, or Error states for UI observation.
 *
 * @param <T> Payload type.
 */
public final class Resource<T> {

    public enum Status {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }

    @NonNull
    private final Status status;

    @Nullable
    private final T data;

    @Nullable
    private final String message;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.data = data;
        this.message = message;
    }

    @NonNull
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    @NonNull
    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    @NonNull
    public static <T> Resource<T> empty() {
        return new Resource<>(Status.EMPTY, null, null);
    }

    @NonNull
    public static <T> Resource<T> error(@NonNull String message, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, message);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}
