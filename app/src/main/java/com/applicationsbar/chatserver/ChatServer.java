package com.applicationsbar.chatserver;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class ChatServer {




    /*
     * A chat server that delivers public and private messages.
     */


    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static void main(String args[]) {

        // The default port number.
        int portNumber = 2222;
        if (args.length < 1) {
            System.out.println("Usage: java MultiThreadChatServerSync <portNumber>\n"
                    + "Now using port number=" + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

        /*
         * Open a server socket on the portNumber (default 2222). Note that we can
         * not choose a port less than 1023 if we are not privileged users (root).
         */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

        /*
         * Create a client socket for each connection and pass it to a new client
         * thread.
         */
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                int i = 0;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class clientThread extends Thread {

    private String clientName = null;
    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket tclientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.tclientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {
            /*
             * Create input and output streams for this client.
             */
            is = new DataInputStream(tclientSocket.getInputStream());
            os = new PrintStream(tclientSocket.getOutputStream());
            String loginString = is.readLine().trim();

            String username= loginString.split("&")[1].split("=")[1];
            String password=loginString.split("&")[0].split("=")[1];
            User u=User.getUser(username,password);

            if (u!=null) {
                String name=u.firstName+" "+u.lastName;
                /* Welcome the new the client. */
                sendStringMessage(os, "Welcome " + name
                        + " to our chat room.");
                synchronized (this) {
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && threads[i] == this) {
                            clientName = "@" + name;
                            break;
                        }
                    }
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && threads[i] != this) {
                            threads[i].os.println("*** A new user " + name
                                    + " entered the chat room !!! ***");
                        }
                    }
                }
                /* Start the conversation. */
                while (true) {


                    int messageType = is.read();
                    if (messageType == -1)
                        break;
                    int messageLength = 0;
                    for (int i = 0; i < 4; i++) {
                        int j = is.read();
                        messageLength = messageLength * 256 + j;
                    }
                    byte[] message = new byte[messageLength];
                    for (int i = 0; i < messageLength; i++) {
                        message[i] = (byte) is.read();
                    }

                    if (messageType == 1) {
                        String line = new String(message);
                        if (line.indexOf("*** Bye") != -1)
                            break;
                        if (line.startsWith("/quit")) {
                            break;
                        }
                        /* If the message is private sent it to the given client. */
                        if (line.startsWith("@")) {
                            String[] words = line.split("\\s", 2);
                            if (words.length > 1 && words[1] != null) {
                                words[1] = words[1].trim();
                                if (!words[1].isEmpty()) {
                                    synchronized (this) {
                                        for (int i = 0; i < maxClientsCount; i++) {
                                            if (threads[i] != null && threads[i] != this
                                                    && threads[i].clientName != null
                                                    && threads[i].clientName.equals(words[0])) {

                                                //threads[i].os.println("<" + name + "> " + words[1]);
                                                sendStringMessage(threads[i].os, ">" + name + "> " + words[1]);
                                                /*
                                                 * Echo this message to let the client know the private
                                                 * message was sent.
                                                 */
                                                sendStringMessage(os, ">" + name + "> " + words[1]);
                                                //this.os.println(">" + name + "> " + words[1]);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            /* The message is public, broadcast it to all other clients. */
                            synchronized (this) {
                                for (int i = 0; i < maxClientsCount; i++) {
                                    if (threads[i] != null && threads[i].clientName != null) {
                                        sendStringMessage(threads[i].os, ">" + name + "> " + line);

                                        // threads[i].os.println("<" + name + "> " + line);
                                    }
                                }
                            }
                        }
                    } else {
                        synchronized (this) {
                            for (int i = 0; i < maxClientsCount; i++) {
                                if (threads[i] != null && threads[i].clientName != null) {
                                    sendByteArrayMessage(threads[i].os, message);

                                }
                            }
                        }
                    }

                }
                synchronized (this) {
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && threads[i] != this
                                && threads[i].clientName != null) {
                            threads[i].os.println("*** The user " + name
                                    + " is leaving the chat room !!! ***");
                        }
                    }
                }
                //  os.println("*** Bye " + name + " ***");

                /*
                 * Clean up. Set the current thread variable to null so that a new client
                 * could be accepted by the server.
                 */
            }
            else {
                sendLoginFaileMessage(this.os,"Login failed");
            }

            /*
             * Close the output stream, close the input stream, close the socket.
             */
            this.is.close();
            this.os.close();
            this.tclientSocket.close();
            System.out.println("Socket Closed"+this.tclientSocket.isClosed());
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }
        } catch (Exception e) {
            try {
                if (is!=null) is.close();
                if (os!=null) os.close();
                if (tclientSocket!=null) tclientSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
    }


    private void sendByteArrayMessage(PrintStream os, byte[] ba) {


        try {

            byte currentByte = 2;
            os.write(currentByte);
            int len = ba.length;

            for (int i = 3; i >= 0; i--) {
                currentByte = (byte) ((len >> (i * 8)) % 256);
                os.write(currentByte);
            }

            os.write(ba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendStringMessage(PrintStream os, String s) {

        byte[] ba = s.getBytes();
        try {

            byte currentByte = 1;
            os.write(currentByte);
            int len = ba.length;

            for (int i = 3; i >= 0; i--) {
                currentByte = (byte) ((len >> (i * 8)) % 256);
                os.write(currentByte);
            }

            os.write(ba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLoginFaileMessage(PrintStream os, String s) {

        byte[] ba = s.getBytes();
        try {

            byte currentByte = 3;
            os.write(currentByte);
            int len = ba.length;

            for (int i = 3; i >= 0; i--) {
                currentByte = (byte) ((len >> (i * 8)) % 256);
                os.write(currentByte);
            }

            os.write(ba);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



