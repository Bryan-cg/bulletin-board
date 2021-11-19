package server;

import java.util.ArrayList;
import java.util.HashMap;

public class BulletinBoard {
    ArrayList<HashMap<Integer, String>> cells;

    public BulletinBoard(int size) {
        cells = new ArrayList<HashMap<Integer, String>>(size);
        for (int i = 0; i < size; i++) {
            cells.add(new HashMap<Integer, String>());
        }
    }

    public void add(int i, String v, String t) {
        cells.get(i).put(Integer.parseInt(t), v);
    }

    public String get(int i, String b) {
        return cells.get(i).get(Integer.parseInt(b));
    }

}
