package server;

import java.util.ArrayList;
import java.util.HashMap;

public class BulletinBoard {
    private final int BB_SIZE;
    private ArrayList<HashMap<byte[], byte[]>> cells; //hashmap = row in bulletin-board, element in row: <v, t> (t = hashing from tag)

    public BulletinBoard(int size) {
        this.BB_SIZE = size;
        initializeBB();
    }

    private void initializeBB() {
        cells = new ArrayList<>(BB_SIZE);
        for (int i = 0; i < BB_SIZE; i++) {
            cells.add(new HashMap<>());
        }
    }

    public void add(int i, byte[] v, byte[] t) {
        cells.get(i).put(t, v);
    }

    public byte[] get(int i, byte[] b) {
        return cells.get(i).get(b);
    }

}
