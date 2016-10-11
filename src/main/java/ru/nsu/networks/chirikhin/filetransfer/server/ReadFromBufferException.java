package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;

public class ReadFromBufferException extends Exception {
    private static final Logger logger = Logger.getLogger(ReadFromBufferException.class.getName());

    public ReadFromBufferException(String message) {
        super(message);
    }

    public ReadFromBufferException(String message, Throwable cause) {
        super(message, cause);
    }
}
