package ru.nsu.networks.chirikhin.filetransfer.client;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Client implements Runnable {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private static final int SIZE_OF_LONG = 8;
    private static final int SIZE_OF_INT = 4;
    private static final int SIZE_OF_FILE_BUFFER = 1024;

    private static final int CONFIRM_NUMBER = 1634;
    private static final int NO_ERROR = 1871;

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

        ByteBuffer byteBufferNameLength = ByteBuffer.allocate(SIZE_OF_INT);
        byteBufferNameLength.putInt(nameBytes.length);
        byteBufferNameLength.flip();

        ByteBuffer byteBufferName = ByteBuffer.allocate(nameBytes.length);
        byteBufferName.put(nameBytes);
        byteBufferName.flip();

        try {
            logger.info ("Start to write");

            int countOfSentBytes = 0;
            while (countOfSentBytes < SIZE_OF_LONG) {
                countOfSentBytes += socketChannel.write(byteBufferSize);
            }

            countOfSentBytes = 0;
            while (countOfSentBytes < SIZE_OF_INT) {
                countOfSentBytes += socketChannel.write(byteBufferNameLength);
            }

            countOfSentBytes = 0;
            while (countOfSentBytes < nameBytes.length) {
                countOfSentBytes += socketChannel.write(byteBufferName);
            }

            ByteBuffer byteBufferForError = ByteBuffer.allocate(SIZE_OF_INT);
            countOfSentBytes = 0;
            while (countOfSentBytes < SIZE_OF_INT) {
                countOfSentBytes += socketChannel.read(byteBufferForError);
            }
            byteBufferForError.flip();
            int errorCode = byteBufferForError.asIntBuffer().get();

            if (NO_ERROR != errorCode) {
                logger.error("Can not send file. Error from server");
                return;
            }

            logger.info("Start to write the file");
            ByteBuffer byteBufferFile = ByteBuffer.allocate(SIZE_OF_FILE_BUFFER);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file.getName(), "r");
            FileChannel fileChannel = randomAccessFile.getChannel();

            int countOfReadBytesFromFile;
            int maxBufferFilling = SIZE_OF_FILE_BUFFER;
            boolean isContinue = true;

            while (isContinue) {
                countOfReadBytesFromFile = 0;

                while (countOfReadBytesFromFile < maxBufferFilling) {
                    int countOfReadSymbols = fileChannel.read(byteBufferFile);

                    if (countOfReadSymbols < 0) {
                        maxBufferFilling = countOfReadBytesFromFile;
                        isContinue = false;
                        logger.info ("End of file");
                    } else {
                        countOfReadBytesFromFile += countOfReadSymbols;
                    }
                }

                logger.info("Have read from file");

                byteBufferFile.flip();

                countOfSentBytes = 0;
                while (countOfSentBytes < maxBufferFilling) {
                    countOfSentBytes += socketChannel.write(byteBufferFile);
                }

                byteBufferFile.clear();

                logger.info ("Next Step");
            }

            logger.info("File channel is closed");

            fileChannel.close();

            int countOfReadBytes = 0;
            ByteBuffer byteBuffer = ByteBuffer.allocate(SIZE_OF_INT);
            while (countOfReadBytes < SIZE_OF_INT) {
                countOfReadBytes += socketChannel.read(byteBuffer);
            }

            logger.info("Information is read to the buffer");

            byteBuffer.flip();

            int confirmNumber = byteBuffer.asIntBuffer().get();

            if (CONFIRM_NUMBER == confirmNumber) {
                logger.info ("The file is transferred successfully");
            } else {
                logger.info ("There is error while transferring");
            }

            socketChannel.close();
            System.out.println("The file is transferred");



        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }
}
