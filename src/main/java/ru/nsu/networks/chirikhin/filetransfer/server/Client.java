package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private final SocketChannel socketChannel;
    private boolean isInfoAboutFileIsWritten = false;
    private long size;
    private String name;
    private byte[] bytes;
    private int countOfReadBytes;


    public Client (SocketChannel socketChannel) {
        if (null == socketChannel) {
            throw new IllegalArgumentException("socket channel can not be null");
        }

        this.socketChannel = socketChannel;
    }

    public void letWork() throws ReadFromBufferException {
        logger.info("Client started to work");
        if (!isInfoAboutFileIsWritten) {
            ByteBuffer bufferForSizeOfFile = ByteBuffer.allocate(8);
            ByteBuffer bufferForSizeOfName = ByteBuffer.allocate(4);

            try {
                int countOfReadBytes = 0;
                while (countOfReadBytes < 8) {
                    countOfReadBytes += socketChannel.read(bufferForSizeOfFile);
                }
                bufferForSizeOfFile.flip();
                size = bufferForSizeOfFile.asLongBuffer().get();
                logger.info("Size of file is: " + size);

                countOfReadBytes = 0;
                while (countOfReadBytes < 4) {
                    countOfReadBytes += socketChannel.read(bufferForSizeOfName);
                }
                bufferForSizeOfName.flip();
                int sizeOfFilename = bufferForSizeOfName.asIntBuffer().get();
                logger.info("Length of filename: " + sizeOfFilename);

                countOfReadBytes = 0;
                ByteBuffer bufferForFilename = ByteBuffer.allocate(sizeOfFilename);
                while (countOfReadBytes < sizeOfFilename) {
                    countOfReadBytes += socketChannel.read(bufferForFilename);
                }
                bufferForFilename.flip();
                byte[] filenameBytes = bufferForFilename.array();
                String filename = new String(filenameBytes, Charset.forName("UTF-8"));
                logger.info("Filename is " + filename);
                isInfoAboutFileIsWritten = false;

            } catch (Throwable e) {
                throw new ReadFromBufferException(e.getMessage(), e);
            }

        }
    }
}
