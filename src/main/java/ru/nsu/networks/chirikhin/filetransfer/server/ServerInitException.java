package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;

public class ServerInitException extends Exception {
    private static final Logger logger = Logger.getLogger(ServerInitException.class.getName());

    public ServerInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
