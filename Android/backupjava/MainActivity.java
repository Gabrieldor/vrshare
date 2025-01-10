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
                Socket socket = new Socket(ip, 5000);
                byte[] size_size = new byte[2];
                byte[] pixels = new byte[1000];
                byte[] consize = new byte[4];
                byte[] byteArray = new byte[1000];
                BufferedInputStream in;
                ByteArrayOutputStream baos;
                while(true) {
                    in = new BufferedInputStream(socket.getInputStream());
                    baos = new ByteArrayOutputStream();
                    in.read(size_size);

                    if(size_size[0] > 5 || size_size[0] < 0){
                        socket.close();
                        cancel(true);
                    }

                    ByteBuffer wrapped = ByteBuffer.wrap(size_size); // big-endian by default
                    short num = wrapped.getShort();
                    Log.d("BYTEOUTPUT", Arrays.toString(size_size));
                    //Log.d("BYTEOUTPUT", String.valueOf(num));



                    in.read(consize);
                    //Log.d("BYTEOUTPUT", Arrays.toString(consize));
                    ByteBuffer wrapped_size = ByteBuffer.wrap(consize); // big-endian by default
                    int size_num = wrapped_size.getInt();


                    long read = 0;
                    while (read < size_num) {
                        int reada = in.read(pixels);
                        baos.write(pixels, 0, reada);
                        read += reada;
                    }
                    if(read == 0 && size_num != 0){
                        socket.close();
                        cancel(true);
                    }

                    byteArray = baos.toByteArray();
                    //Log.d("BYTEOUTPUT", Arrays.toString(pixels));
                    //Log.d("BYTEOUTPUT", String.valueOf(read));
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


