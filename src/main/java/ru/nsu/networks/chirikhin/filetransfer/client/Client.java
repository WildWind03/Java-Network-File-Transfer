package ru.nsu.networks.chirikhin.filetransfer.client;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client implements Runnable {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final int SIZE_OF_LONG = 8;

    private final File file;
    private final SocketChannel socketChannel;

    public Client(String pathToFile, String ip, int port) {
        file = new File(pathToFile);
        logger.info("Current working dir: " + Paths.get(".").toAbsolutePath().normalize().toString());
        if (!file.exists()) {
            throw new IllegalArgumentException("This file doesn't exist");
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("This is not a common file. Can't send it");
        }

        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(ip, port));
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    @Override
    public void run() {
        long size = file.length();
        ByteBuffer byteBufferSize = ByteBuffer.allocate(SIZE_OF_LONG);
        byteBufferSize.putLong(size);
        byteBufferSize.flip();

        String name = file.getName();
        logger.info("Name: " + name);
        byte[] nameBytes = name.getBytes(Charset.forName("UTF-8"));
        logger.info("Bytes for name: " + nameBytes.length);

        ByteBuffer byteBufferNameLength = ByteBuffer.allocate(4);
        byteBufferNameLength.putInt(nameBytes.length);
        byteBufferNameLength.flip();

        ByteBuffer byteBufferName = ByteBuffer.allocate(nameBytes.length);
        byteBufferName.put(nameBytes);
        byteBufferName.flip();

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            ByteBuffer byteBufferFile = ByteBuffer.allocate(fileBytes.length);
            byteBufferFile.put(fileBytes);
            byteBufferFile.flip();

            int countOfSentBytes = 0;

            logger.info ("Start to write");
            while (countOfSentBytes < 8) {
                countOfSentBytes += socketChannel.write(byteBufferSize);
            }

            countOfSentBytes = 0;

            while (countOfSentBytes < 4) {
                countOfSentBytes += socketChannel.write(byteBufferNameLength);
            }

            countOfSentBytes = 0;

            while (countOfSentBytes < nameBytes.length) {
                countOfSentBytes += socketChannel.write(byteBufferName);
            }

            countOfSentBytes = 0;

            while(countOfSentBytes < fileBytes.length) {
                countOfSentBytes += socketChannel.write(byteBufferFile);
            }

            int countOfReadBytes = 0;
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            while (countOfReadBytes < 4) {
                countOfReadBytes += socketChannel.read(byteBuffer);
            }

            socketChannel.close();
            System.out.println("The file is transferred");



        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }
}
