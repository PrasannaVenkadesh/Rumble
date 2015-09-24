/*
 * Copyright (C) 2014 Disrupted Systems
 * This file is part of Rumble.
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
 * You should have received a copy of the GNU General Public License along
 * with Rumble.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.disrupted.rumble.network.services.chat;

import android.util.Log;

import org.disrupted.rumble.network.NetworkCoordinator;
import org.disrupted.rumble.network.protocols.ProtocolChannel;
import org.disrupted.rumble.network.protocols.command.CommandSendChatMessage;
import org.disrupted.rumble.network.protocols.events.ChatMessageReceived;
import org.disrupted.rumble.network.protocols.events.NeighbourConnected;
import org.disrupted.rumble.network.protocols.events.NeighbourDisconnected;
import org.disrupted.rumble.network.services.ServiceLayer;
import org.disrupted.rumble.userinterface.events.UserComposeChatMessage;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * @author Marlinski
 */
public class ChatService implements ServiceLayer {

    private static final String TAG = "ChatService";

    private static final Object lock = new Object();
    private static ChatService instance;

    private static NetworkCoordinator networkCoordinator;

    private static Map<String, ChatMessageDispatcher> workerIdentifierTodispatcher;

    public static ChatService getInstance(NetworkCoordinator networkCoordinator) {
        synchronized (lock) {
            if(instance == null)
                instance = new ChatService(networkCoordinator);

            return instance;
        }
    }
    private ChatService(NetworkCoordinator networkCoordinator) {
        this.networkCoordinator = networkCoordinator;
    }

    @Override
    public String getServiceIdentifier() {
        return TAG;
    }

    public void startService() {
        synchronized (lock) {
            Log.d(TAG, "[+] Starting ChatService");
            workerIdentifierTodispatcher = new HashMap<String, ChatMessageDispatcher>();
            EventBus.getDefault().register(this);
        }
    }
    public void stopService() {
        synchronized (lock) {
            Log.d(TAG, "[-] Stopping ChatService");
            if(EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);

            for(Map.Entry<String, ChatMessageDispatcher> entry : workerIdentifierTodispatcher.entrySet()) {
                ChatMessageDispatcher dispatcher = entry.getValue();
                dispatcher.stopDispatcher();
            }
            workerIdentifierTodispatcher.clear();
        }
    }

    public void onEvent(NeighbourConnected event) {
        synchronized (lock) {
            ChatMessageDispatcher dispatcher = workerIdentifierTodispatcher.get(event.worker.getWorkerIdentifier());
            if (dispatcher != null) {
                Log.e(TAG, "worker already binded ?!");
                return;
            }
            dispatcher = new ChatMessageDispatcher(event.worker);
            workerIdentifierTodispatcher.put(event.worker.getWorkerIdentifier(), dispatcher);
            dispatcher.startDispatcher();
        }
    }
    public void onEvent(NeighbourDisconnected event) {
        synchronized (lock) {
            ChatMessageDispatcher dispatcher = workerIdentifierTodispatcher.get(event.worker.getWorkerIdentifier());
            if (dispatcher == null)
                return;
            dispatcher.stopDispatcher();
            workerIdentifierTodispatcher.remove(event.worker.getWorkerIdentifier());
        }
    }

    /*
     * this one service is very stupid, it only sends chat message from user to connected neighbour
     */
    private static class ChatMessageDispatcher {

        private ProtocolChannel channel;

        public ChatMessageDispatcher(ProtocolChannel channel) {
            this.channel = channel;
        }

        public void startDispatcher() {
            EventBus.getDefault().register(this);
        }

        public void stopDispatcher() {
            if(EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
            this.channel = null;
        }

        public void onEvent(UserComposeChatMessage event) {
            channel.execute(new CommandSendChatMessage(event.chatMessage));
        }

        public void onEvent(ChatMessageReceived event) {
            if(!event.channel.equals(this.channel))
                channel.execute(new CommandSendChatMessage(event.chatMessage));
        }

    }
}
