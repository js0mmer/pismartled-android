package io.github.js0mmer.pismartled;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URI;

public class ToggleTileService extends TileService {

    private Socket socket;

    @Override
    public void onClick() {
        if (getQsTile().getState() == Tile.STATE_ACTIVE) {
            getQsTile().setState(Tile.STATE_INACTIVE);
            socket.emit("led", 0);
        } else if (getQsTile().getState() == Tile.STATE_INACTIVE) {
            getQsTile().setState(Tile.STATE_ACTIVE);
            socket.emit("led", 1);
        }

        getQsTile().updateTile();

        super.onClick();
    }

    @Override
    public void onStartListening() {
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        String address = settings.getString("serverAddress", null);

        if (address == null) {
            getQsTile().setState(Tile.STATE_UNAVAILABLE);
            getQsTile().updateTile();
            return;
        }

        socket = IO.socket(URI.create(address));
        socket.on("led", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args[0].equals(1)) {
                    getQsTile().setState(Tile.STATE_ACTIVE);
                } else if (args[0].equals(0)) {
                    getQsTile().setState(Tile.STATE_INACTIVE);
                }

                getQsTile().updateTile();
            }
        });

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.emit("status");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("Tile", "Error connecting to socket");
                getQsTile().setState(Tile.STATE_UNAVAILABLE);
                getQsTile().updateTile();
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                getQsTile().setState(Tile.STATE_UNAVAILABLE);
                getQsTile().updateTile();
            }
        });

        socket.connect();

        super.onStartListening();
    }

    @Override
    public void onStopListening() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
            socket.off("led");
        }

        super.onStopListening();
    }
}
