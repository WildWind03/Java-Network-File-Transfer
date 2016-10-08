package ru.nsu.networks.chirikhin.filetransfer.client;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.SocketChannel;

public class Client implements Runnable {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final int SIZE_OF_LONG = 8;

    private final File file;
    private final SocketChannel socketChannel;

    public Client(String pathToFile, String ip, int port) {
        file = new File(pathToFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("This file doesn't exist");
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("This is not a common file. Can't send it");
        }

        try {
            socketChannel = SocketChannel.open();
            socketChannel.bind(new InetSocketAddress(ip, port));
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    @Override
    public void run() {
        long size = file.length();
        ByteBuffer byteBuffer = ByteBuffer.allocate(SIZE_OF_LONG);
        byteBuffer.putLong(size);

        try {
            int countOfSentBytes = 0;

            while (countOfSentBytes < 8) {
                countOfSentBytes+=socketChannel.write(byteBuffer);
            }

            while (!Thread.currentThread().isInterrupted()) {

            }
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }
}
