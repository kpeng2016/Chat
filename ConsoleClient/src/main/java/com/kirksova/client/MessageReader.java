package com.kirksova.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Запускается поток для чтения сообщений, который передает нам сервер.
 */

public class MessageReader implements Runnable {

    private static final Logger log = Logger.getLogger(MessageReader.class);
    private static final String STRING = ":";
    private Scanner scannerServerMessage;
    private PrintWriter sendingMessagesToServer;
    private Message message;

    MessageReader(Scanner in, PrintWriter out) {
        this.sendingMessagesToServer = out;
        this.scannerServerMessage = in;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public void run() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String serverMessage;
            while (scannerServerMessage.hasNextLine()) {
                serverMessage = scannerServerMessage.nextLine();
                message = mapper.readValue(serverMessage, new TypeReference<Message>() {
                });
                switch (message.getTypeOfMessage()) {
                    case NO_CLIENT_IN_QUEUE:
                        break;
                    case MESSAGE_CHAT:
                        System.out.println(message.getNameTo() + STRING + message.getText());
                        break;
                    case CONNECTED_AGENT:
                    case CONNECTED_CLIENT:
                    case DISCONNECTION_OF_THE_AGENT:
                    case DISCONNECTION_OF_THE_CLIENT:
                        sendingMessagesToServer.println(mapper.writeValueAsString(message));
                    default:
                        System.out.println(message.getText());
                }
            }
        } catch (IOException e) {
            log.debug("Convert Json exception", e);
        }
    }
}

