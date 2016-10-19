package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Server implements Runnable{
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;

    private final HashMap<Long, Client> clientHashMap = new HashMap<>();

    private long maxId = 0;

    public Server (int port) throws ServerInitException {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);


        } catch (IOException e) {
            throw new ServerInitException(e.getMessage(), e);
        }

    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();

                Set<SelectionKey> selectionKeySet = selector.selectedKeys();

                Iterator<SelectionKey> iterator = selectionKeySet.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    if (selectionKey.isAcceptable()) {
                        logger.info("New client is accepted");
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        socketChannel.configureBlocking(false);

                        SelectionKey newSelectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
                        Client client = new Client(socketChannel);
                        long currentId = maxId++;


                        clientHashMap.put(currentId, client);
                        newSelectionKey.attach(currentId);
                    } else {
                        if (selectionKey.isReadable()) {
                            logger.info("Attempt to read");
                            long clientId = (long) selectionKey.attachment();
                            Client client = clientHashMap.get(clientId);

                            try {
                                if (client.letWork()) {
                                    clientHashMap.remove(clientId);
                                    selectionKey.cancel();
                                }
                            } catch (Throwable t) {
                                if (t instanceof CanNotReceiveFileException) {
                                    logger.info ("The client will be deleted. Server can not receive the file");
                                    clientHashMap.remove(clientId);
                                    selectionKey.cancel();
                                }

                                logger.error(t.getMessage());
                            }
                        }
                    }

                    iterator.remove();
                }
            }

        } catch(Throwable t) {
            logger.error(t.getMessage());
        }
    }
}
