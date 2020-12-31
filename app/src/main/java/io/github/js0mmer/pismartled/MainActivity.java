package io.github.js0mmer.pismartled;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URI;

public class MainActivity extends AppCompatActivity {

    private Socket socket;

    private Switch mToggle;
    private EditText mServerAddressField;
    private Button mConnectButton;
    private TextView mMessage;

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToggle = findViewById(R.id.toggleSwitch);
        mServerAddressField = findViewById(R.id.serverAddressField);
        mConnectButton = findViewById(R.id.connectButton);
        mMessage = findViewById(R.id.message);

        settings = getSharedPreferences("settings", MODE_PRIVATE);
        mServerAddressField.setText(settings.getString("serverAddress", ""));

        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket != null && socket.connected()) {
                    socket.emit("led", mToggle.isChecked() ? 1 : 0);
                }
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = mServerAddressField.getText().toString();

                if (!address.startsWith("http://") && !address.startsWith("https://")) {
                    mMessage.setText(R.string.include_protocol);
                } else {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("serverAddress", address);
                    editor.commit();

                    if (socket != null && socket.connected()) {
                        socket.disconnect();
                    }

                    initSocket(address);
                }
            }
        });

        String address = settings.getString("serverAddress", null);

        if (address != null) {
            initSocket(address);
        }
    }

    private void initSocket(String address) {
        socket = IO.socket(URI.create(address));

        socket.on("led", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mToggle.setChecked(args[0].equals(1)); // can't run from worker thread; must wrap in runnable and run on ui thread
                    }
                });
            }
        });

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mMessage.setText(R.string.success_connecting);
                socket.emit("status");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mMessage.setText(R.string.error_connecting);
            }
        });

        socket.connect();
    }
}