package com.example.vrshare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CLIENT";
    private ImageView image;
    private ImageView image2;
    private EditText input;
    private Button button;

    public static String ip;
    private static SharedPreferences settings;
    private static SharedPreferences.Editor editor;

    private ReceiveScreen receiveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide title bar and go full screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        settings = MainActivity.this.getPreferences(Context.MODE_PRIVATE);

        // UI references
        input = findViewById(R.id.editText2);
        input.setText(settings.getString("ip", "Enter the IP"), TextView.BufferType.EDITABLE);

        button = findViewById(R.id.button);
        image = findViewById(R.id.imageView);
        image2 = findViewById(R.id.imageView2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Save IP to SharedPreferences
                ip = input.getText().toString();
                editor = settings.edit();
                editor.putString("ip", ip);
                editor.apply();

                // Start receiving screenshots
                receiveTask = new ReceiveScreen();
                receiveTask.execute();

                // Hide the button & input
                v.setVisibility(View.GONE);
                input.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * AsyncTask that continuously reads frames from the server.
     * If a frame is too large or partially read, it RECONNECTS (no break).
     */
    public class ReceiveScreen extends AsyncTask<Void, Bitmap, Void> {

        private Socket socket;
        private BufferedInputStream in;

        private static final int MAX_FRAME_SIZE = 50_000_000; // 50 MB limit
        private static final int BUFFER_SIZE = 8192;

        @Override
        protected Void doInBackground(Void... params) {
            // Keep running unless cancelled
            while (!isCancelled()) {
                try {
                    // Ensure we are connected
                    if (socket == null || socket.isClosed()) {
                        reconnect();
                        if (socket == null) {
                            // If we failed to reconnect, wait and retry
                            Thread.sleep(1000);
                            continue;
                        }
                    }

                    // 1) Read 4 bytes for size
                    byte[] sizeBytes = new byte[4];
                    if (!readFully(sizeBytes, 4)) {
                        // End of stream, or partial read => reconnect
                        Log.d(TAG, "Unable to read frame size (EOF?), reconnecting...");
                        closeSocket();
                        continue;
                    }

                    ByteBuffer wrapped = ByteBuffer.wrap(sizeBytes);
                    int frameSize = wrapped.getInt();
                    Log.d(TAG, "Incoming frame size = " + frameSize);

                    // If suspicious frame, reconnect and skip
                    if (frameSize <= 0 || frameSize > MAX_FRAME_SIZE) {
                        Log.d(TAG, "Suspicious frame size => reconnecting...");
                        closeSocket();
                        continue;
                    }

                    // 2) Read exactly frameSize bytes
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int totalRead = 0;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while (totalRead < frameSize) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            // EOF mid-frame => reconnect
                            Log.d(TAG, "EOF in the middle of a frame => reconnecting...");
                            closeSocket();
                            continue;  // go back to main while loop
                        }
                        totalRead += bytesRead;
                        baos.write(buffer, 0, bytesRead);
                    }

                    // 3) Decode the frame
                    byte[] frameData = baos.toByteArray();
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    // Downsample if needed to prevent OOM
                    // opts.inSampleSize = 2;  // Example: downsample by 2

                    Bitmap bmp = BitmapFactory.decodeByteArray(frameData, 0, frameData.length, opts);
                    if (bmp == null) {
                        // Decoding failed => reconnect
                        Log.d(TAG, "Bitmap decode failed => reconnecting...");
                        closeSocket();
                        continue;
                    }

                    // 4) Display the bitmap
                    publishProgress(bmp);

                } catch (IOException e) {
                    // If we had an IO error, we attempt reconnect
                    Log.e(TAG, "IOException: " + e.getMessage());
                    closeSocket();
                } catch (InterruptedException e) {
                    // If interrupted while sleeping => just end
                    Log.e(TAG, "InterruptedException in doInBackground");
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
            if (values != null && values.length > 0) {
                Bitmap bmp = values[0];
                image.setImageBitmap(bmp);
                image2.setImageBitmap(bmp);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "AsyncTask cancelled => closing socket...");
            closeSocket();
        }

        /**
         * Connect to server, store in 'socket' & 'in'.
         */
        private void reconnect() {
            closeSocket(); // ensure old socket is closed
            try {
                Log.d(TAG, "Connecting to " + ip + ":5000 ...");
                socket = new Socket(ip, 5000);
                in = new BufferedInputStream(socket.getInputStream());
                Log.d(TAG, "Connected to server at " + ip + ":5000");
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect: " + e.getMessage());
                socket = null;
            }
        }

        /**
         * Safely close the current socket.
         */
        private void closeSocket() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error closing socket: " + ex.getMessage());
            }
            socket = null;
            in = null;
        }

        /**
         * Reads exactly 'length' bytes into 'buf'.
         * Returns false if we can't read that many (EOF).
         */
        private boolean readFully(byte[] buf, int length) throws IOException {
            int offset = 0;
            while (offset < length) {
                int bytesRead = in.read(buf, offset, length - offset);
                if (bytesRead == -1) {
                    return false; // EOF
                }
                offset += bytesRead;
            }
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiveTask != null && !receiveTask.isCancelled()) {
            receiveTask.cancel(true);
        }
    }
}
