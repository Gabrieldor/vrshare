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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    protected ImageView image;
    protected ImageView image2;
    protected EditText input;
    public static String ip;
    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        input = (EditText) findViewById(R.id.editText2);
        input.setText(settings.getString("ip", "Enter the IP"), TextView.BufferType.EDITABLE);
        Button button = (Button) findViewById(R.id.button);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        image = (ImageView) findViewById(R.id.imageView);
        image2 = (ImageView) findViewById(R.id.imageView2);
        button.setOnClickListener(new View.OnClickListener (){

            @Override
            public void onClick(View v){
                try {
                    ip = input.getText().toString();
                    new ReceiveScreen().execute(MainActivity.this);
                    v.setVisibility(View.GONE);

                    input.setVisibility(View.INVISIBLE);

                    editor = settings.edit();
                    editor.putString("ip", ip);
                    editor.apply();
                }catch(Exception e){
                    //e.printStackTrace();
                }

            }
        });

    }
    public class ReceiveScreen extends AsyncTask<Context, Bitmap, Void> {
        @Override
        protected Void doInBackground(Context... params){
            try{
                DatagramSocket socket = new DatagramSocket();
                ByteArrayOutputStream baos;
                byte[] pixels = new byte[65565];
                byte[] byteArray;
                DatagramPacket in;

                InetAddress host = InetAddress.getByName(ip);
                while(true) {
                    String s = "a";
                    byte[] b = s.getBytes();

                    DatagramPacket dp = new DatagramPacket(b , b.length , host , 5000);
                    socket.send(dp);

                    baos = new ByteArrayOutputStream();
                    in = new DatagramPacket(pixels, pixels.length);
                    socket.receive(in);
                    baos.write(pixels, 0, pixels.length);
                    socket.receive(in);
                    baos.write(pixels, 0, pixels.length);

                    byteArray = baos.toByteArray();

                    Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    publishProgress(bmp);
                    //freeMemory();
                }
            } catch (IOException e){
                //e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
            image.setImageBitmap(values[0]);
            image2.setImageBitmap(values[0]);
        }

        protected void onCancelled (){
            new ReceiveScreen().execute(MainActivity.this);
        }

        public void freeMemory(){
            System.runFinalization();
            Runtime.getRuntime().gc();
            System.gc();
        }

    }
}


