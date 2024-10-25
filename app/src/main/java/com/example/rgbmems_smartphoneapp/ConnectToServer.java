package com.example.rgbmems_smartphoneapp;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

public class ConnectToServer {
    public static Socket client;
    public String serverIp = "10.0.2.2"; // Change to your server's IP address
    public int serverPort = 8000; // Port on which the server is listening
    private PendingMessage pendingMessage = null; // Store the pending message to be sent
    private Handler handler = new Handler(); // Initialize the Handler
    private TextView responseTextView; // Declare TextView

    //private static final String TAG = "SocketClient";   //最終的には"ConnectServer"に統一する
    private OutputStream outputStream; // Output stream for sending data
    private InputStream inputStream; // Input stream for receiving data
    private boolean isReconnecting = false; // Flag to check if a reconnection attempt is in progress

    public void setResponseTextView(TextView responseTextView) {
        this.responseTextView = responseTextView; // Assign TextView from MainActivity
    }

    public void setPendingMessage(String messageType, int messageValue) {
        this.pendingMessage = new PendingMessage(messageType, messageValue);
        Log.d("ConnectServer", "Pending message stored: " + messageType + " - " + messageValue);

    }

    public void connectToServer(Context context) {
        new Thread(() -> {
            try {
                if (client == null || client.isClosed()) {
                    client = new Socket(serverIp, serverPort);
                    // Set timeout for the socket
                    client.setSoTimeout(30000); // 30 seconds timeout for receiving data
                    Log.d("ConnectServer", "Connected to server");
                }

                if (client != null && client.isConnected()) {
                    // Connection successful
                    Log.d("ConnectServer", "Connection successful");

                    // Check if there is a pending message, send it immediately upon successful connection
                    if (pendingMessage != null) {
                        Log.d("ConnectServer", "Pending message found. Sending message...");
                        sendMessageToServer(pendingMessage.getName(), pendingMessage.getCheckNumber());
                        pendingMessage = null; // Clear the pending message after sending
                    }

                    // Read the response from the server
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(),"UTF-8"));
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        Log.d("ConnectServer", "Response from server: " + serverResponse);

                        // Update the user interface with the response from the server
                        String finalResponse = serverResponse;
                        ((MainActivity) context).runOnUiThread(() -> {
                            // Display the response from the server on the screen using TextView
                            if (responseTextView != null) {
                                updateResponseText(finalResponse); // Call the method to update and hide the TextView
                            }
                        });
                    }
                } else {
                    // Connection failed
                    Log.d("ClientThread", "Not connected to server!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ConnectServer", "Error connecting to server", e);
            }
        }).start();
    }

    // Send message to the server
    public void sendMessageToServer(String type, int value) {
        new Thread(() -> {
            try {
                //if (client != null && !client.isClosed()) {
                if (isConnected()) {
                    // Create a JSON object from type and value
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("type", type);
                    jsonMessage.put("value", value);

                    // Call getOutputStream and check
                    if (client != null && !client.isClosed()) {
                        // Initialize outputStream from the client's output stream
                        outputStream = client.getOutputStream();
                    } else {
                        Log.e("ClientThread", "Client is null or closed.");
                        return;
                    }
                    // Send message to the server
                    // メッセージを送信する前にoutputStreamがnullでないことを確認する
                    if (outputStream != null) {
                        synchronized (outputStream) {
                            //outputStream.flush();
                            outputStream.write((jsonMessage.toString() + "\n").getBytes());
                            outputStream.flush();
                        }
                        Log.d("ClientThread", "Message sent to server: " + jsonMessage.toString());
                    } else {
                        Log.e("ClientThread", "OutputStream is null. Cannot send message.");
                    }
                }
                else {
                    Log.e("ClientThread", "Connection not established. Message not sent.");
                }
            } catch (IOException | JSONException e) {
                Log.e("ConnectServer", "Error sending message to server", e);
            }
        }).start();
    }

    // Check if the client is connected to the server
    public boolean isConnected() {
        return client != null && client.isConnected() && !client.isClosed();
    }

    // Update the content of the TextView and hide it after a certain period of time
    public void updateResponseText(String response) {
        if (responseTextView != null) {
            responseTextView.setText(response);
            responseTextView.setVisibility(View.VISIBLE); // Show the TextView

            // Set a timer to hide the TextView
            handler.postDelayed(() -> responseTextView.setVisibility(View.GONE), 3000); // Hide after 3 seconds
        }
    }

    public void connect() {
        new Thread(() -> {
            try {
                // Connect to socket
                if (client == null || client.isClosed()) {
                    client = new Socket(serverIp, serverPort);
                }
                // Set timeout for the socket
                client.setSoTimeout(30000); // 30 seconds timeout for receiving data
                outputStream = client.getOutputStream();
                inputStream = client.getInputStream();
                Log.d("ConnectServer", "Connected to server");
            } catch (IOException e) {
                Log.e("ConnectServer", "Error connecting to server: " + e.getMessage(), e);
            }
        }).start();
    }

    // Continuously check connection; if not connected, attempt to reconnect
    private void ensureConnected() {
        if (!isConnected() && !isReconnecting) {
            isReconnecting = true; // Set flag to indicate reconnection attempt
            connect(); // Attempt to connect
            isReconnecting = false; // Reset flag after connection attempt
        }
    }

    // Update sendImage method to send only the image without the image sequence number
    public void sendImage(byte[] imageData) {
        new Thread(() -> {
            ensureConnected(); // Ensure connection before sending image
            try {
                outputStream = client.getOutputStream();
                inputStream = client.getInputStream();
                if (isConnected()) {
                    // Check image data
                    if (imageData == null || imageData.length == 0) {
                        Log.e("ConnectServer", "Image data is null or empty");
                        return; // Return early if there is no image data
                    }

                    JSONObject jsonMessage_img = new JSONObject();
                    jsonMessage_img.put("type", "sendimage");              //HOME画面の画像番号選択と同じでいい？
                    jsonMessage_img.put("value", SecondFragment.currentNumber);   //画像番号
                    //jsonMessage_img.put("ImageDataByteArray", imageData);         //画像のbyte配列

                    //Send data through the socket
                    synchronized (outputStream) {
                        //outputStream.flush(); // Clear old data if any
                        outputStream.write((jsonMessage_img.toString() + "\n").getBytes());
                        //メモjsonでデータを送るので再度送る必要ない？
                        //outputStream.write(imageData); // Write the image data to output stream
                        outputStream.flush(); // Ensure all data is sent
                    }

                    //Introduce a small delay to allow server to process JSON message
                    Thread.sleep(100);  // Delay of 100ms (can be adjusted)

                    //Send the length of image data
                    int imageLength = imageData.length;
                    synchronized (outputStream) {
                        outputStream.write((imageLength + "\n").getBytes()); // Send the length as a new line
                        outputStream.flush(); // Ensure length is sent
                    }

                    //Introduce another small delay before sending the image data
                    Thread.sleep(100);  // Delay of 100ms (can be adjusted)

                    //Send the actual image data
                    synchronized (outputStream) {
                        outputStream.write(imageData); // Write the image data to output stream
                        outputStream.flush(); // Ensure all data is sent
                    }

                    Log.d("ConnectServer", "Image sent to server");

                    // Read response from server
                    byte[] responseBuffer = new byte[4096];
                    int bytesRead = inputStream.read(responseBuffer); // Read response
                    if (bytesRead > 0) {
                        String response = new String(responseBuffer, 0, bytesRead);
                        Log.d("ConnectServer", "Server response: " + response);
                    } else {
                        Log.e("ConnectServer", "No response from server");
                    }
                } else {
                    Log.e("ConnectServer", "Socket is not connected");
                }
            } catch (SocketException e) {
                Log.e("ConnectServer", "Socket error: " + e.getMessage(), e);
                reconnect(); // Reconnect if there is a socket issue
            } catch (SocketTimeoutException e) {
                Log.e("ConnectServer", "Socket timeout: " + e.getMessage(), e);
                reconnect(); // Reconnect if there is a timeout
            } catch (IOException | JSONException e) {
                Log.e("ConnectServer", "Error sending image: " + e.getMessage(), e);
                reconnect(); // Try reconnecting after an error
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    // Attempt to reconnect in case of issues
    private void reconnect() {
        disconnect(); // Disconnect current socket
        connect(); // Attempt to reconnect
    }

    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close(); // Close input stream
            }
            if (outputStream != null) {
                outputStream.close(); // Close output stream
            }
            if (client != null && !client.isClosed()) {
                client.close(); // Close socket if it's not already closed
                Log.d("ConnectServer", "Socket closed");
            }
        } catch (IOException e) {
            Log.e("ConnectServer", "Error closing socket", e);
        }
    }
}

