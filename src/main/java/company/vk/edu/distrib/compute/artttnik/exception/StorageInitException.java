package company.vk.edu.distrib.compute.artttnik.exception;

import java.io.Serial;

public final class StorageInitException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StorageInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
