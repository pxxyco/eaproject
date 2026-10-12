package it.unical.ea_project_javafx.util;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleInstanceLock {

    private static final int BASE_PORT = 9999;
    private static final int MAX_PORT_ATTEMPTS = 5;

    private static final String APP_ID = "EA_PROJECT_JAVAFX_INSTANCE";
    private static final String PING_CMD = APP_ID + "_PING";
    private static final String PONG_RESP = APP_ID + "_PONG";
    private static final String FOCUS_CMD = APP_ID + "_FOCUS";

    private static ServerSocket serverSocket;

    /**
     * Tenta di acquisire il lock. Se un'istanza valida è già in esecuzione,
     * le invia il comando di FOCUS e restituisce false.
     */
    public static boolean acquireLock() {

        for (int i = 0; i < MAX_PORT_ATTEMPTS; i++) {
            int port = BASE_PORT + i;
            try {
                serverSocket = new ServerSocket(port, 10, InetAddress.getByName("localhost"));
                return true; // Lock acquisito con successo!
            } catch (IOException e) {
                if (isOurAppRunningOnPort(port)) {
                    sendFocusCommand(port);
                    System.err.print("\n==========================================================" 
                                   + "\n[ERROR] An instance of the application is already running!"
                                   + "\n\tCannot launch a second instance."
                                   + "\n==========================================================\n");
                    return false; // Istanza già presente
                }
            }
        }
        return true;

    }

    /**
     * Avvia il thread daemon in background per ascoltare i messaggi delle nuove istanze.
     */
    public static void startListener(Stage stage) {

        Thread thread = new Thread(() -> {
            while (serverSocket != null && !serverSocket.isClosed()) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    String message = in.readLine();
                    if (PING_CMD.equals(message)) {
                        out.println(PONG_RESP);
                    } else if (FOCUS_CMD.equals(message)) {
                        Platform.runLater(() -> {
                            if (stage != null) {
                                if (stage.isIconified()) stage.setIconified(false);
                                stage.toFront();
                                stage.requestFocus();
                            }
                        });
                    }
                } catch (IOException ignored) {
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

    }

    /**
     * Rilascia il lock al momento della chiusura dell'app.
     */
    public static void release() {

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }

    }

    private static boolean isOurAppRunningOnPort(int port) {

        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(1000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(PING_CMD);
            String response = in.readLine();
            return PONG_RESP.equals(response);
        } catch (IOException e) {
            return false;
        }

    }

    private static void sendFocusCommand(int port) {

        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(FOCUS_CMD);
        } catch (IOException ignored) {
        }

    }

}