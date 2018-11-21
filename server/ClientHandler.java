package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author nikalsh
 */
public class ClientHandler {

    private Socket clientSocket;
    private PrintWriter toClient;
    private BufferedReader fromClient;
    private boolean isNowPlaying;
    private int score;

    public ClientHandler(Socket socket) throws IOException {

        clientSocket = socket;
        toClient = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
        fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
    }

    public void println(String input) {
        toClient.println(input);
    }

    public String readLine() throws IOException {
        return fromClient.readLine();
    }

    public boolean isPlaying() {
        return isNowPlaying;
    }

    public void putInGameRoom() {
        isNowPlaying = true;
    }

//    public synchronized void putInLobby() {
//        System.out.println(Thread.currentThread());
//        Thread.currentThread().interrupt();
//        this.notify();
//        isNowPlaying = false;
//    }

    public Socket getSocket() {
        return this.clientSocket;
    }

    public synchronized void pauseThread() {
        while (isNowPlaying) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
        System.out.println("Thread resumed");
    }

}
