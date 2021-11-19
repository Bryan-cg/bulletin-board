package server;

import shared.Chat;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Set;

public class ChatImpl extends UnicastRemoteObject implements Chat {

    private static final long serialVersionUID = 2144719768445459230L;

    private final SharedData data;

    protected ChatImpl() throws RemoteException {
        super();
        data = SharedData.getInstance();
    }

    @Override
    public synchronized String getMessage(String name) throws RemoteException {
        String message;
        while ((message = data.getMailBoxes().get(name).poll()) == null) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    @Override
    public synchronized void sendMessage(String name, String message) throws RemoteException {
        synchronized (data) {
            if (data.getNames().contains(name)) {
                data.getMailBoxes().forEach((key, value) -> value.add(name + ": " + message));
            }
            this.notifyAll();
        }
    }

    @Override
    public synchronized boolean register(String name) throws RemoteException {
        synchronized (data) {
            if (!data.getNames().contains(name)) {
                data.getNames().add(name);
                data.getMailBoxes().put(name, new LinkedList<>());
                return true;
            }
            return false;
        }
    }

    @Override
    public synchronized boolean unRegister(String name) throws RemoteException {
        synchronized (data) {
            if (name != null) {
                if (data.getNames().contains(name)) {
                    data.getNames().remove(name);
                    data.getMailBoxes().remove(name);
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public Set<String> getUsers() throws RemoteException {
        synchronized (data) {
            return data.getNames();
        }
    }

    @Override
    public synchronized void sendMessagePrivate(String username, String message, String target) throws RemoteException {
        synchronized (data) {
            if (data.getNames().contains(target) && data.getNames().contains(username)) {
                data.getMailBoxes().get(target).add("PRIVATE " + username + ": " + message);
            }
            this.notifyAll();
        }
    }

}
