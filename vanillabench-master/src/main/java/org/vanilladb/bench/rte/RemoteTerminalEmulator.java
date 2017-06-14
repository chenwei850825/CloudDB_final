package org.vanilladb.bench.rte;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;

import org.vanilladb.bench.StatisticMgr;
import org.vanilladb.bench.TransactionType;
import org.vanilladb.bench.TxnResultSet;
import org.vanilladb.bench.remote.SutConnection;
import org.vanilladb.bench.server.procedure.BasicStoredProcedure;


public abstract class RemoteTerminalEmulator extends Thread {

	private static AtomicInteger rteCount = new AtomicInteger(0);

	private volatile boolean stopBenchmark;
	private volatile boolean isWarmingUp = true;
	private SutConnection conn;
	private StatisticMgr statMgr;
	
	public RemoteTerminalEmulator(SutConnection conn, StatisticMgr statMgr) {
		this.conn = conn;
		this.statMgr = statMgr;
		
		// Set the thread name
		setName("RTE-" + rteCount.getAndIncrement());
	}

	@Override
	public void run() {
		
		this.conn.started();
		while (!stopBenchmark) {
			TxnResultSet rs = executeTxnCycle(conn);
			if (!isWarmingUp)
				statMgr.processTxnResult(rs);
		}
		this.conn.stopped();

	}

	public void startRecordStatistic() {
		isWarmingUp = false;
	}

	public void stopBenchmark() {
		stopBenchmark = true;
	}

	protected abstract TransactionType getNextTxType();
	
	protected abstract TransactionExecutor getTxExeutor(TransactionType type);

	private TxnResultSet executeTxnCycle(SutConnection conn) {
		TransactionType txType = getNextTxType();
		TransactionExecutor executor = getTxExeutor(txType);
		return executor.execute(conn);
	}
}