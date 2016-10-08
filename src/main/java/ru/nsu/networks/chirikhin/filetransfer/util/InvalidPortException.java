package ru.nsu.networks.chirikhin.filetransfer.util;

import org.apache.log4j.Logger;

public class InvalidPortException extends Exception {
    public InvalidPortException(String message) {
        super(message);
    }

    public InvalidPortException(String message, Throwable cause) {
        super(message, cause);
    }
}
