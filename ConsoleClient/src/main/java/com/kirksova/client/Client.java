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
import java.util.Base64;
import java.util.Scanner;

/**
 * Отправляем сообщения серверу
 */

public class Client {

    private static final Logger log = Logger.getLogger(Client.class);
    private static final String register = "Hello!\nYou can:\ninput '/register agent your_name' to register as agent, "
        + "'/register client your_name' to register as client\ninput '/sign in agent your_name' to login as agent, "
        + "'/sign in client your_name' to login as client\ninput /leave if you client to complete the dialog\ninput "
        + "/exit to exit the program";
    private static final int PORT = 8189;
    private static final String EXIT = "/exit";

    public static void clientStart() {
        try (Socket socket = new Socket(InetAddress.getLocalHost(), PORT); Scanner scannerServerMessage =
            new Scanner(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            Scanner scannerUserMessageText = new Scanner(System.in); PrintWriter sendingMessagesToServer =
            new PrintWriter(socket.getOutputStream(), true)) {
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

    private static void processMessages(Scanner scannerUserMessageText, PrintWriter sendingMessagesToServer,
        MessageReader messageReader)
        throws JsonProcessingException {
        String strOut = "";
        while (!EXIT.equals(strOut)) {
            Message message;
            strOut = scannerUserMessageText.nextLine();
            Message messageServer = messageReader.getMessage();
            if (messageServer != null) {
                if (messageServer.getTypeOfMessage() == MessageType.CORRECT_LOGIN_NAME ||
                    messageServer.getTypeOfMessage() == MessageType.CORRECT_REGISTRATION ||
                    messageServer.getTypeOfMessage() == MessageType.INCORRECT_LOGIN_PASSWORD){
                    Base64.Encoder encoder = Base64.getEncoder();
                    String encodedString = encoder.encodeToString(strOut.getBytes());
                    message = new Message(messageServer.getSenderId(), encodedString, MessageType.MESSAGE_CHAT);
                }else {
                    message = new Message(messageServer.getSenderId(), strOut, MessageType.MESSAGE_CHAT,
                        messageServer.getTo(), messageServer.getSenderName());
                }
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



