package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Chat extends Remote {

	void write(byte[] idx, byte[] v, byte[] hashedTag);

	byte[] get(byte[] idx, byte[] tag);
}
