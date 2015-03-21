package org.disrupted.rumble.network.protocols.rumble;

import android.util.Log;

/**
 * @author Marlinski
 */
public class RumbleBTState {

    private static final String TAG = "RumbleBTState";

    public static enum RumbleBluetoothState {
        NOT_CONNECTED, CONNECTION_INITIATED, CONNECTION_ACCEPTED, CONNECTED;
    }

    private final Object lockRumbleBTState = new Object();
    private RumbleBluetoothState state;
    private String threadID;

    public RumbleBTState() {
        this.state = RumbleBluetoothState.NOT_CONNECTED;
        this.threadID = null;
    }

    /*
     * goTo the CONNECTION INITIATED state which happens when we initiate a connection
     * (most probably from the BluetoothLinkLayer.connectTo method).
     * The threadID is then stored as it may be cancelled under certain circumstances.
     * It throws a StateException if previous state is different than NOT_CONNECTED
     */
    public void connectionInitiated(String threadID) throws StateException {
        synchronized (lockRumbleBTState) {
            switch (state) {
                case CONNECTED:
                case CONNECTION_INITIATED:
                case CONNECTION_ACCEPTED:
                    throw new StateException();
                case NOT_CONNECTED:
                default:
                    this.threadID = threadID;
                    state = RumbleBluetoothState.CONNECTION_INITIATED;
            }
        }
    }

    /*
     * goTo the CONNECTION ACCEPTED state which happens when RumbleBTServer receive
     * a connection (accept() returns). The threadID is then stored as it may be
     * cancelled under certain circumstances.
     * It can only happen from the NOT_CONNECTED or CONNECTION_INITIATED state.
     * It throws a StateException otherwise
     */
    public void connectionAccepted(String threadID) throws StateException {
        synchronized (lockRumbleBTState) {
            switch (state) {
                case CONNECTED:
                case CONNECTION_ACCEPTED:
                    throw new StateException();
                case CONNECTION_INITIATED:
                case NOT_CONNECTED:
                default:
                    this.threadID = threadID;
                    state = RumbleBluetoothState.CONNECTION_ACCEPTED;
            }
        }
    }

    /*
     * goTo the CONNECTED state which happens when a BluetoothConnection connect()
     * (wether it is as a BluetoothClient or as a BluetoothServerConnection)
     * It can only happen from an intermediary state like CONNECTION_INITIATED or
     * CONNECTED_ACCEPTED. It throws a StateException otherwise
     */
    public void connected(String threadID) throws StateException {
        synchronized (lockRumbleBTState) {
            switch (state) {
                case NOT_CONNECTED:
                case CONNECTED:
                    throw new StateException();
                case CONNECTION_INITIATED:
                case CONNECTION_ACCEPTED:
                default:
                    state = RumbleBluetoothState.CONNECTED;
                    this.threadID = threadID;
            }
        }
    }

    /*
     * goTo the NOT_CONNECTED state which can happen from any state
     *    - when a BluetoothConnection disconnect()
     *    - when the intermediary state has been cancelled
     */
    public void notConnected() {
        synchronized (lockRumbleBTState) {
            switch (state) {
                default:
                    state = RumbleBluetoothState.NOT_CONNECTED;
                    this.threadID = null;
            }
        }
    }

    public RumbleBluetoothState getState() {
        return state;
    }
    public String getConnectionInitiatedThreadID()  throws StateException{
        if(state == RumbleBluetoothState.CONNECTION_INITIATED)
            return this.threadID;
        throw new StateException();
    }
    public String getConnectionAcceptedThreadID() throws StateException{
        if(state == RumbleBluetoothState.CONNECTION_ACCEPTED)
            return this.threadID;
        throw new StateException();
    }

    public class StateException extends Exception {
    }
}