package company.vk.edu.distrib.compute.aldor7705.exceptions;

import java.io.Serial;

public class MethodNotAllowedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MethodNotAllowedException(String message) {
        super(message);
    }
}
