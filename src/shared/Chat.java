package shared;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public interface Chat extends Remote {

	void write(byte[] idx, byte[] v, byte[] hashedTag) throws RemoteException, NoSuchAlgorithmException;

	byte[] get(byte[] idx, byte[] tag) throws IOException, NoSuchAlgorithmException;
}
