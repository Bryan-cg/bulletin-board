package server;

import shared.Chat;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class BulletinBoard extends UnicastRemoteObject implements Chat {
    private final int BB_SIZE = 20;
    private ArrayList<HashMap<byte[], byte[]>> cells; //hashmap = row in bulletin-board, element in row: <v, t> (t = hashing from tag)

    public BulletinBoard() throws RemoteException {
        super();
        initializeBB();
    }

    private void initializeBB() {
        cells = new ArrayList<>(BB_SIZE);
        for (int i = 0; i < BB_SIZE; i++) {
            cells.add(new HashMap<>());
        }
    }

    @Override
    public void write(int idx, byte[] v, byte[] hashedTag) {
        cells.get(idx).put(hashedTag, v);
    }

    @Override
    public byte[] get(int i, byte[] tag) {
        byte[] hashedTag = hashing(tag);
        return cells.get(i).get(hashedTag);
    }

    private byte[] hashing(byte[] tag) {
        //TODO hashing tag
        return null;
    }

}
