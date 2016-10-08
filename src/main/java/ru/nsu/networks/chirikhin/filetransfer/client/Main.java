package ru.nsu.networks.chirikhin.filetransfer.client;

import org.apache.log4j.Logger;

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
    }
}
