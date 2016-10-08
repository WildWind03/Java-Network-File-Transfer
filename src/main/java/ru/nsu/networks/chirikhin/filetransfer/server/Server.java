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
                Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();

                    if (selectionKey.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        socketChannel.configureBlocking(false);

                        SelectionKey newSelectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
                        Client client = new Client();
                        long currentId = maxId++;

                        newSelectionKey.attach(currentId);
                    } else {
                        if (selectionKey.isReadable()) {

                        }
                    }
                }
            }

        } catch(Throwable t) {
            logger.error(t.getMessage());
        }
    }
}
