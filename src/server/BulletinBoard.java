package server;

import shared.Chat;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BulletinBoard extends UnicastRemoteObject implements Chat {
    private static final String SHA2_ALGORITHM = "SHA-256";
    private final int BB_SIZE = 20;
    private ArrayList<HashMap<ByteBuffer, byte[]>> cells; //hashmap = row in bulletin-board, element in row: <t, v> (t = hashing from tag)

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
    public synchronized void write(byte[] idx, byte[] v, byte[] tag) throws NoSuchAlgorithmException {
        byte[] hashedTag = hashing(tag);
        int i = new BigInteger(idx).intValue();
        cells.get(i).put(ByteBuffer.wrap(hashedTag), v);
    }

    @Override
    public synchronized byte[] get(byte[] idx, byte[] tag) throws NoSuchAlgorithmException {
        int i = new BigInteger(idx).intValue();
        byte[] hashedTag = hashing(tag);
        return cells.get(i).get(ByteBuffer.wrap(hashedTag));
    }

    private byte[] hashing(byte[] tag) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(SHA2_ALGORITHM);
        return messageDigest.digest(tag);
    }

}
