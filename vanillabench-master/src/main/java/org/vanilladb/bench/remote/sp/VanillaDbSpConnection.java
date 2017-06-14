package org.vanilladb.bench.remote.sp;

import java.rmi.RemoteException;
import java.sql.SQLException;

import org.vanilladb.bench.remote.SutConnection;
import org.vanilladb.bench.remote.SutResultSet;
import org.vanilladb.core.remote.storedprocedure.SpConnection;
import org.vanilladb.core.remote.storedprocedure.SpResultSet;

public class VanillaDbSpConnection implements SutConnection {
	private SpConnection conn;

	public VanillaDbSpConnection(SpConnection conn) {
		this.conn = conn;
	}

	@Override
	public SutResultSet callStoredProc(int pid, Object... pars) throws SQLException {
		SpResultSet r = conn.callStoredProc(pid, pars);
		return new VanillaDbSpResultSet(r);
	}

	@Override
	public void stopped(){
		try {
			conn.stopped();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void started(){
		try {
			conn.started();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
