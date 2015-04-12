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

package org.disrupted.rumble.network.protocols;

import android.util.Log;

import org.disrupted.rumble.network.linklayer.LinkLayerConnection;
import org.disrupted.rumble.network.protocols.command.Command;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The GenericProtocol implements a generic protocol where one thread take care
 * of receiving and processing packet from the network while another one take
 * care of receiving and processing command from the upper layer.
 * @author Marlinski
 */
public abstract class ProtocolWorker implements Worker {

    private static final String TAG = "ProtocolWorker";

    private BlockingQueue<Command> commandQueue;

    public ProtocolWorker() {
        commandQueue = new LinkedBlockingQueue<Command>(1);
    }

    protected final void onWorkerConnected() {
        processingCommandFromQueue.start();
        try {
            processingPacketFromNetwork();
        }finally {
            processingCommandFromQueue.interrupt();
        }
    }

    Thread processingCommandFromQueue = new Thread() {
        @Override
        public synchronized void run() {
            try {
                while (true) {
                    Command command = commandQueue.take();
                    onCommandReceived(command);
                }
            }
            catch(InterruptedException e) {
                commandQueue.clear();
            }
        }
    };

    public final boolean execute(Command command){
        try {
            if (isCommandSupported(command.getCommandName())) {
                commandQueue.put(command);
                return true;
            } else {
                Log.d(TAG, "[!] command is not supported: " + command.getCommandName());
                return false;
            }
        } catch (InterruptedException ignore) {
            return false;
        }
    }


    abstract public boolean isCommandSupported(String commandName);

    abstract protected boolean onCommandReceived(Command command);

    abstract public LinkLayerConnection getLinkLayerConnection();

    abstract protected void processingPacketFromNetwork();
}