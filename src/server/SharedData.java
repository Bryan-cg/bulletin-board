package server;

import java.util.*;

public class SharedData {

    private static SharedData instance = null;
    private final Set<String> names;
    private final Map<String, Queue<String>> mailBoxes;

    private SharedData() {
        names = new HashSet<>();
        mailBoxes = new HashMap<>();
    }

    public static SharedData getInstance() {
        if (instance == null)
            instance = new SharedData();

        return instance;
    }

    public Set<String> getNames() {
        return names;
    }

    public Map<String, Queue<String>> getMailBoxes() {
        return mailBoxes;
    }

}
