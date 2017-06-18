package org.vanilladb.core.storage.tx.concurrency;

import java.util.logging.Logger;

import org.vanilladb.core.storage.file.BlockId;
import org.vanilladb.core.storage.record.RecordId;
import org.vanilladb.core.storage.tx.T3RecordKey;
import org.vanilladb.core.storage.tx.Transaction;



public class T3ConservativeConcurrencyMgr extends ConcurrencyMgr {

	public T3ConservativeConcurrencyMgr(long txNumber) {
		txNum = txNumber;
	}

	@Override
	public void onTxCommit(Transaction tx) {
		lockTbl.releaseAll(txNum, false);
		leaveT3LockedList();
		//Logger logger = Logger.getLogger("XD");
		//logger.warning(tx.getTransactionNumber()+" onTxCommit");
	}

	@Override
	public void onTxRollback(Transaction tx) {
		lockTbl.releaseAll(txNum, false);
		leaveT3LockedList();
		//Logger logger = Logger.getLogger("XD");
		//logger.warning(tx.getTransactionNumber()+" onTxRollback");
	}

	@Override
	public void onTxEndStatement(Transaction tx) {
		// do nothing
	}

	@Override
	public void modifyFile(String fileName) {
//		lockTbl.xLock(fileName, txNum);
	}

	@Override
	public void readFile(String fileName) {
//		lockTbl.isLock(fileName, txNum);
	}

	@Override
	public void insertBlock(BlockId blk) {
//		lockTbl.xLock(blk.fileName(), txNum);
//		lockTbl.xLock(blk, txNum);
	}

	@Override
	public void modifyBlock(BlockId blk) {
//		lockTbl.ixLock(blk.fileName(), txNum);
//		lockTbl.xLock(blk, txNum);
	}

	@Override
	public void readBlock(BlockId blk) {
//		lockTbl.isLock(blk.fileName(), txNum);
//		lockTbl.sLock(blk, txNum);
	}
	
	@Override
	public void modifyRecord(RecordId recId) {
//		lockTbl.ixLock(recId.block().fileName(), txNum);
//		lockTbl.ixLock(recId.block(), txNum);
//		lockTbl.xLock(recId, txNum);
	}

	@Override
	public void readRecord(RecordId recId) {
//		lockTbl.isLock(recId.block().fileName(), txNum);
//		lockTbl.isLock(recId.block(), txNum);
//		lockTbl.sLock(recId, txNum);
	}

	@Override
	public void modifyIndex(String dataFileName) {
		//lockTbl.ixLock(dataFileName, txNum);
	}

	@Override
	public void readIndex(String dataFileName) {
		//lockTbl.isLock(dataFileName, txNum);
	}
	
	public boolean readT3RecordKey(T3RecordKey record)
	{
		
		int res = lockTbl.T3sLock(record, txNum);
		while(res==2)
		{
			res = lockTbl.T3sLock(record, txNum);
		}
		if(res==1)
			return true;
		return false;
		
	}
	
	public boolean modifyT3RecordKey(T3RecordKey record)
	{
		int res = lockTbl.T3xLock(record, txNum);
		while(res==2)
		{
			res = lockTbl.T3xLock(record, txNum);
		}
		if(res==1)
			return true;
		return false;
		
	}
	
	public void releaseLocks()
	{
		lockTbl.releaseAll(txNum, false);
	}
	
	public void registerT3LockedList()
	{
		lockTbl.registerT3LockedList(txNum);
	}
	
	public void leaveT3LockedList()
	{
		lockTbl.leaveT3LockedList(txNum);
	}
	
}