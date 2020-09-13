package com.android.priya.client;

import android.support.v7.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int SERVERPORT = 12345;
    private ClientThread clientThread;
    public static final String SERVER_IP="192.168.43.223";
    private Thread thread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;
    private EditText edMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.cl);
        clientTextColor = ContextCompat.getColor(this, R.color.colorPrimaryDark);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
    }

    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() + "]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.connect_server) {
            msgList.removeAllViews();
            showMessage("Connecting to Server...", R.color.colorAccent);
            clientThread = new ClientThread();
            thread = new Thread(clientThread);
            thread.start();
            showMessage("Connected to Server...", clientTextColor);
            return;
        }

        if (view.getId() == R.id.send_data) {
            String clientMessage = edMessage.getText().toString().trim();
            showMessage(clientMessage, Color.BLUE);
            if (null != clientThread) {
                clientMessage=encrypt(clientMessage,"ANDROIDAPPLICATION");
                clientThread.sendMessage(clientMessage);

            }
        }

    }
    public static String encrypt(String text, final String key)

    {

        String res = "";

        text = text.toUpperCase();

        for (int i = 0, j = 0; i < text.length(); i++) {

            char c = text.charAt(i);
            if (Character.isWhitespace(c))
                res += " ";
            else {
                if (c < 'A' || c > 'Z')

                    continue;

                res += (char) ((c + key.charAt(j) - 2 * 'A') % 26 + 'A');

                j = ++j % key.length();

            }
        }
        return res;

    }
    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);

                while (!Thread.currentThread().isInterrupted()) {

                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        showMessage(message, Color.RED);
                        break;
                    }
                    showMessage("Server: (ENCRYPTED)  " + message, clientTextColor);
                    message=decrypt(message,"ANDROIDAPPLICATION");
                    showMessage("Server: (DECRYPTED)  " + message, clientTextColor);
                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
        public  String decrypt(String text, final String key)

        {

            String res = "";

            text = text.toUpperCase();

            for (int i = 0, j = 0; i < text.length(); i++) {

                char c = text.charAt(i);
                if (Character.isWhitespace(c))
                    res += " ";
                else {
                    if (c < 'A' || c > 'Z') {
                        continue;
                    }

                    res += (char) ((c - key.charAt(j) + 26) % 26 + 'A');

                    j = ++j % key.length();

                }
            }
            return res;

        }
        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }
}