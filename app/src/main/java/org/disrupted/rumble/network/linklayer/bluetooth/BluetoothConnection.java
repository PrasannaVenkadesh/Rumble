/*
 * Copyright (C) 2014 Disrupted Systems
 *
 * This file is part of Rumble.
 *
 * Rumble is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rumble is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rumble.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.disrupted.rumble.network.linklayer.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.disrupted.rumble.app.RumbleApplication;
import org.disrupted.rumble.network.linklayer.LinkLayerConnection;
import org.disrupted.rumble.network.linklayer.exception.InputOutputStreamException;
import org.disrupted.rumble.network.linklayer.exception.LinkLayerConnectionException;
import org.disrupted.rumble.network.linklayer.exception.SocketAlreadyClosedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * BluetoothConnection is a generic implementation of a Bluetooth Connection wether it is to manage
 * a client or to connect to a server. It is abstract as the connection part is specific and should
 * implements connect()  as requested per LinkLayerConnection interface.
 *
 * @author Marlinski
 */
public abstract class BluetoothConnection implements LinkLayerConnection {

    private static final String TAG = "BluetoothConnection";

    protected String remoteMacAddress;
    protected boolean secureSocket;
    protected BluetoothDevice mmBluetoothDevice;
    protected BluetoothSocket mmConnectedSocket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected boolean registered;

    public BluetoothConnection(String remoteMacAddress) {
        this.remoteMacAddress = remoteMacAddress;
        this.mmConnectedSocket = null;
        this.secureSocket = false;
        this.inputStream = null;
        this.outputStream = null;
        this.registered = false;
    }

    @Override
    public String getLinkLayerIdentifier() {
        return BluetoothLinkLayerAdapter.LinkLayerIdentifier;
    }

    public InputStream getInputStream() throws IOException, InputOutputStreamException{
        InputStream input = mmConnectedSocket.getInputStream();
        if(input == null)
            throw new InputOutputStreamException();
        return input;
    }

    public OutputStream getOutputStream() throws IOException, InputOutputStreamException {
        OutputStream output = mmConnectedSocket.getOutputStream();
        if(output == null)
            throw new InputOutputStreamException();
        return output;
    }

    public String getRemoteMacAddress() {
        return remoteMacAddress;
    }


    @Override
    public void disconnect() throws LinkLayerConnectionException {
        try {
            mmConnectedSocket.close();
            Log.d(TAG, "[-] Socket Closed Manually");
        } catch (IOException e) {
            throw new SocketAlreadyClosedException();
        } finally {
            if(registered)
                RumbleApplication.getContext().unregisterReceiver(mReceiver);
            registered = false;
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                if(device.getAddress().equals(mmBluetoothDevice.getAddress())) {
                    try {
                        Log.d(TAG, "[+] ACTION_ACL_DISCONNECTED");
                        disconnect();
                    } catch(LinkLayerConnectionException ignore) {
                    }
                }
            }
        }
    };
}