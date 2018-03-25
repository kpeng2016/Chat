package com.kirksova.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirksova.client.Message.MessageType;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Отправляем сообщения серверу
 */

public class Client {

    private static final Logger log = Logger.getLogger(Client.class);
    private static final String register = "Зарегистрируйтесь.\nДля регистрации введите /register agent ВашеИмя, если вы агент, или /register client ВашеИмя, если вы клиент";
    private static final int PORT = 8189;
    private static final String EXIT = "/exit";

    public static void clientStart() {
        try (Socket socket = new Socket(InetAddress.getLocalHost(), PORT); Scanner scannerServerMessage = new Scanner(
            new InputStreamReader(socket.getInputStream(), "UTF-8"));
            Scanner scannerUserMessageText = new Scanner(System.in); PrintWriter sendingMessagesToServer = new PrintWriter(
            socket.getOutputStream(), true)) {
            System.out.println(register);
            MessageReader messageReader = startReaderThread(scannerServerMessage, sendingMessagesToServer);
            processMessages(scannerUserMessageText, sendingMessagesToServer, messageReader);
        } catch (UnknownHostException e) {
            // Update messages
            log.debug("UnknownHostException", e);
        } catch (IOException e) {
            log.debug("IOException", e);
        }
    }

    private static void processMessages(Scanner scannerUserMessageText, PrintWriter sendingMessagesToServer, MessageReader messageReader)
        throws JsonProcessingException {
        String strOut = "";
        /**String str = "Highlight";
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedString = encoder.encodeToString(str.getBytes());*/
        while (!EXIT.equals(strOut)) {
            Message message;
            strOut = scannerUserMessageText.nextLine();
            Message messageServer = messageReader.getMessage();
            if (messageServer != null) {
                message = new Message(messageServer.getSenderId(), strOut, MessageType.MESSAGE_CHAT,
                    messageServer.getTo(), messageServer.getNameTo());
            } else {
                message = new Message(null, strOut, MessageType.MESSAGE_CHAT);
            }
            sendingMessagesToServer.println(new ObjectMapper().writeValueAsString(message));

        }
    }

    private static MessageReader startReaderThread(Scanner in, PrintWriter out) {
        MessageReader messageReader = new MessageReader(in, out);
        new Thread(messageReader).start();
        return messageReader;
    }

}



