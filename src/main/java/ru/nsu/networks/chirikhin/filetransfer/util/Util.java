package ru.nsu.networks.chirikhin.filetransfer.util;

import org.apache.log4j.Logger;

public class Util {
    private static final Logger logger = Logger.getLogger(Util.class.getName());

    private Util() {

    }

    public static int getPort(String port) throws InvalidPortException {
        int portInt;
        try {
            portInt = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new InvalidPortException("A port is not a number. Can not parse", e);
        }

        if (portInt <= 0) {
            throw new InvalidPortException("A port must be positive");
        }

        return portInt;
    }
}
