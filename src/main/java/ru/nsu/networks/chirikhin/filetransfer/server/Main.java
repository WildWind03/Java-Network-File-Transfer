package ru.nsu.networks.chirikhin.filetransfer.server;

import org.apache.log4j.Logger;
import ru.nsu.networks.chirikhin.filetransfer.util.InvalidPortException;
import ru.nsu.networks.chirikhin.filetransfer.util.Util;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (1 != args.length) {
            logger.error("Invalid count of args");
            return;
        }

        String port = args[0];
        int portInt;
        try {
            portInt = Util.getPort(port);
        } catch (InvalidPortException e) {
            logger.error(e.getMessage());
            return;
        }


        Server server = new Server(portInt);
        Thread serverThread = new Thread(server);
        serverThread.start();

        try {
            serverThread.join();
        } catch (InterruptedException e) {
            logger.error("The main thread has been interrupted!");
        }
    }
}
