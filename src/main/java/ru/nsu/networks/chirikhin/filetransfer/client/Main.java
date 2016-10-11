package ru.nsu.networks.chirikhin.filetransfer.client;

import org.apache.log4j.Logger;
import ru.nsu.networks.chirikhin.filetransfer.util.InvalidPortException;
import ru.nsu.networks.chirikhin.filetransfer.util.Util;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final int VALID_COUNT_OF_ARGS = 3;

    public static void main(String[] args) {
        if (VALID_COUNT_OF_ARGS != args.length) {
            logger.error("Invalid count of args");
            return;
        }

        String pathToFile = args[0];
        String ip = args[1];
        String port = args[2];

        try {
            Client client = new Client(pathToFile, ip, Util.getPort(port));
            Thread thread = new Thread(client);
            thread.start();
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }
}
