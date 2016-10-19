package ru.nsu.networks.chirikhin.filetransfer.server;

public class CanNotReceiveFileException extends Exception {
    public CanNotReceiveFileException(String message) {
        super(message);
    }
}
