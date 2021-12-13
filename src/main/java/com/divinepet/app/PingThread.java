package com.divinepet.app;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PingThread implements Runnable{
    @Override
    public void run() {
        while (true){
            try {
                URL url = new URL("https://www.google.com/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                connection.disconnect();
                Thread.sleep(300000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
