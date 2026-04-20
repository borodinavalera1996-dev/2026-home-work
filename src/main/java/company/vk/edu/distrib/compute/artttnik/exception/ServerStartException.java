package company.vk.edu.distrib.compute.artttnik.exception;

import java.io.Serial;

public final class ServerStartException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ServerStartException(String message, Throwable cause) {
        super(message, cause);
    }
}
