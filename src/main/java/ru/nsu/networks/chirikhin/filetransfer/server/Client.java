package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private static final int SIZE_OF_LONG = 8;
    private static final int SIZE_OF_INT = 4;
    private static final int SIZE_OF_FILE_BUFFER = 4;
    private static final String UPLOADS_PATH = "./uploads/";
    private static final int CONFIRM_NUMBER = 1634;
    private static final int CAN_RECEIVE_FILE_NUMBER = 1871;
    private static final int CAN_NOT_RECEIVE_FILE_NUMBER = 1872;

    private final SocketChannel socketChannel;
    private ByteBuffer fileByteBuffer;
    private boolean isInfoAboutFileIsWritten = false;
    private long size;
    private long currentSize = 0;
    private RandomAccessFile randomAccessFile;
    private FileChannel fileChannel;


    public Client (SocketChannel socketChannel) {
        if (null == socketChannel) {
            throw new IllegalArgumentException("socket channel can not be null");
        }

        this.socketChannel = socketChannel;
    }

    public void confirmReceiving() {
        logger.info ("Start confirming!");
        ByteBuffer byteBufferInt = ByteBuffer.allocate(SIZE_OF_INT);
        byteBufferInt.putInt(CONFIRM_NUMBER);

        byteBufferInt.flip();

        try {
            int countOfSentBytes = 0;
            while (countOfSentBytes < SIZE_OF_INT) {
                countOfSentBytes += socketChannel.write(byteBufferInt);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }

    public boolean letWork() throws ReadFromBufferException, CanNotReceiveFileException {
        logger.info("Client started to work");
        if (!isInfoAboutFileIsWritten) {
            ByteBuffer bufferForSizeOfFile = ByteBuffer.allocate(SIZE_OF_LONG);
            ByteBuffer bufferForSizeOfName = ByteBuffer.allocate(SIZE_OF_INT);

            try {
                int countOfReadBytes = 0;
                while (countOfReadBytes < SIZE_OF_LONG) {
                    countOfReadBytes += socketChannel.read(bufferForSizeOfFile);
                }

                bufferForSizeOfFile.flip();
                size = bufferForSizeOfFile.asLongBuffer().get();
                logger.info("Size of file is: " + size);

                countOfReadBytes = 0;
                while (countOfReadBytes < SIZE_OF_INT) {
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


                int countOfWrittenBytes = 0;
                ByteBuffer bufferForErrorNumber = ByteBuffer.allocate(SIZE_OF_INT);
                if (Files.exists(Paths.get(UPLOADS_PATH + filename))) {
                    bufferForErrorNumber.putInt(CAN_NOT_RECEIVE_FILE_NUMBER);

                    bufferForErrorNumber.flip();
                    while (countOfWrittenBytes < SIZE_OF_INT) {
                        countOfWrittenBytes += socketChannel.write(bufferForErrorNumber);
                    }

                    throw new CanNotReceiveFileException("Can not receive file. File with such name exists");

                } else {
                    bufferForErrorNumber.putInt(CAN_RECEIVE_FILE_NUMBER);
                }

                bufferForErrorNumber.flip();
                while (countOfWrittenBytes < SIZE_OF_INT) {
                    countOfWrittenBytes += socketChannel.write(bufferForErrorNumber);
                }

                randomAccessFile = new RandomAccessFile(UPLOADS_PATH + filename, "rw");
                fileChannel = randomAccessFile.getChannel();
                fileByteBuffer = ByteBuffer.allocate(SIZE_OF_FILE_BUFFER);
                isInfoAboutFileIsWritten = true;

            } catch (CanNotReceiveFileException canNotReceiveFileException) {
                throw canNotReceiveFileException;

            } catch (Throwable e) {
                throw new ReadFromBufferException(e.getMessage(), e);
            }

        } else {
            try {
                int maxSizeOfData = SIZE_OF_FILE_BUFFER;
                boolean isExit = false;
                int countOfReadBytes = 0;

                logger.info("Start to read file");
                while (countOfReadBytes < maxSizeOfData) {
                    int actualReadBytes = socketChannel.read(fileByteBuffer);

                    if (actualReadBytes < 0) {
                        maxSizeOfData = countOfReadBytes;
                        isExit = true;

                        logger.info ("The file is successfully transferred!");
                    } else {
                        if (0 == actualReadBytes) {
                            maxSizeOfData = countOfReadBytes;
                        } else {
                            countOfReadBytes += actualReadBytes;
                            currentSize += actualReadBytes;
                        }
                    }
                }

                fileByteBuffer.flip();

                int countOfWrittenBytes = 0;

                while (countOfWrittenBytes < maxSizeOfData) {
                    countOfWrittenBytes += fileChannel.write(fileByteBuffer);
                }

                fileByteBuffer.clear();

                if (currentSize >= size) {
                    return true;
                }

                return isExit;

            } catch (Throwable t) {
                logger.error(t.getMessage());
            }
        }

        return false;
    }
}
