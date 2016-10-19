package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {


    private enum ClientState {
        GET_SIZE, GET_NAME_LENGTH, GET_NAME, SEND_ERROR_REPORT, GET_FILE, SEND_CONFIRM
    }

    private ClientState clientState = ClientState.GET_SIZE;

    private ByteBuffer byteBufferSize = ByteBuffer.allocate(SIZE_OF_LONG);
    private ByteBuffer byteBufferNameLength = ByteBuffer.allocate(SIZE_OF_INT);
    private ByteBuffer byteBufferName;
    private ByteBuffer byteBufferErrorNumber = ByteBuffer.allocate(SIZE_OF_INT);
    private ByteBuffer byteBufferFile = ByteBuffer.allocate(SIZE_OF_FILE_BUFFER);
    private ByteBuffer byteBufferConfirm = ByteBuffer.allocate(SIZE_OF_INT);

    private long size;
    private int lengthOfFilename;

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private static final int SIZE_OF_LONG = 8;
    private static final int SIZE_OF_INT = 4;
    private static final int SIZE_OF_FILE_BUFFER = 4;
    private static final String UPLOADS_PATH = "./uploads/";
    private static final int CONFIRM_NUMBER = 1634;
    private static final int CAN_RECEIVE_FILE_NUMBER = 1871;
    private static final int CAN_NOT_RECEIVE_FILE_NUMBER = 1872;

    private final SocketChannel socketChannel;
    private RandomAccessFile randomAccessFile;
    private FileChannel fileChannel;


    public Client(SocketChannel socketChannel) {
        if (null == socketChannel) {
            throw new IllegalArgumentException("socket channel can not be null");
        }

        this.socketChannel = socketChannel;
    }

    public boolean letWork() throws CanNotReceiveFileException {
        try {
            switch (clientState) {
                case GET_SIZE:
                    socketChannel.read(byteBufferSize);

                    if (byteBufferSize.position() == byteBufferSize.capacity()) {
                        byteBufferSize.flip();

                        size = byteBufferSize.asLongBuffer().get();
                        clientState = ClientState.GET_NAME_LENGTH;

                        letWork();
                    }
                    break;
                case GET_NAME_LENGTH:
                    socketChannel.read(byteBufferNameLength);

                    if (byteBufferNameLength.position() == byteBufferNameLength.capacity()) {
                        byteBufferNameLength.flip();

                        lengthOfFilename = byteBufferNameLength.asIntBuffer().get();
                        clientState = ClientState.GET_NAME;
                        byteBufferName = ByteBuffer.allocate(lengthOfFilename);

                        letWork();
                    }
                    break;
                case GET_NAME:

                    socketChannel.read(byteBufferName);

                    if (byteBufferName.position() == byteBufferName.capacity()) {
                        byteBufferName.flip();

                        byte[] filenameBytes = byteBufferName.array();
                        String filename = new String(filenameBytes, Charset.forName("UTF-8"));
                        logger.info("Filename is " + filename);

                        if (Files.exists(Paths.get(UPLOADS_PATH + filename))) {
                            byteBufferErrorNumber.putInt(CAN_NOT_RECEIVE_FILE_NUMBER);

                        } else {
                            byteBufferErrorNumber.putInt(CAN_RECEIVE_FILE_NUMBER);
                        }

                        byteBufferErrorNumber.flip();

                        try {
                            randomAccessFile = new RandomAccessFile(UPLOADS_PATH + filename, "rw");
                        } catch (FileNotFoundException e) {
                            logger.error("Impossible error. File has to be exist");
                        }

                        fileChannel = randomAccessFile.getChannel();

                        clientState = ClientState.SEND_ERROR_REPORT;

                        letWork();
                    }

                    break;
                case SEND_ERROR_REPORT:
                    socketChannel.write(byteBufferErrorNumber);

                    if (byteBufferErrorNumber.position() == byteBufferErrorNumber.capacity()) {
                        clientState = ClientState.GET_FILE;
                        letWork();
                    }
                    break;
                case GET_FILE:

                    int resultOfReading = socketChannel.read(byteBufferFile);

                    if (byteBufferFile.position() == byteBufferFile.capacity() || resultOfReading < 0) {
                        byteBufferFile.flip();
                        fileChannel.write(byteBufferFile);
                        byteBufferFile.clear();
                    }

                    if (resultOfReading < 0) {
                        clientState = ClientState.SEND_CONFIRM;
                        byteBufferConfirm.putInt(CONFIRM_NUMBER);
                    }

                    break;
                case SEND_CONFIRM:
                    socketChannel.write(byteBufferConfirm);

                    if (byteBufferConfirm.position() == byteBufferConfirm.capacity()) {
                        return true;
                    }

                    break;
            }
        } catch (Throwable t) {
            throw new CanNotReceiveFileException("The file can not be received!");
        }

        return false;

    }
}
