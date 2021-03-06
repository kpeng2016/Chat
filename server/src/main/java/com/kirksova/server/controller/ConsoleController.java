package com.kirksova.server.controller;

import com.kirksova.server.socket.ThreadSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

import java.net.ServerSocket;
import java.net.Socket;

@Controller
public class ConsoleController implements CommandLineRunner {

    @Value("${host.socketPort}")
    private Integer socketPort;

    @Autowired
    ThreadSocket threadSocket;

    @Override
    public void run(String... args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(socketPort);
        //TODO Implement thread pool
        while (true) {
            Socket incoming = serverSocket.accept();
            threadSocket.setUserSocket(incoming);
            new Thread(threadSocket).start();
        }
    }
}

