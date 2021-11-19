package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Chat extends Remote {
	Set<String> getUsers() throws RemoteException;

	String getMessage(String name) throws RemoteException;

	void sendMessage(String username, String message) throws RemoteException;

	boolean register(String name) throws RemoteException;

	boolean unRegister(String name) throws RemoteException;

	void sendMessagePrivate(String username, String message, String target) throws RemoteException;

}
