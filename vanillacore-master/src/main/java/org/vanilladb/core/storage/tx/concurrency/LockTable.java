package org.vanilladb.core.storage.tx.concurrency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks the compatibility of locking requests on a single item (e.g., file,
 * block, or record). Does <em>not</em> implement a locking protocol to ensure
 * the semantic correctness of locking different items with different
 * granularity.
 * 
 * <p>
 * If a transaction requests to lock an item that causes a conflict with an
 * existing lock on that item, then the transaction is placed into a wait list
 * and will be notified (awaked) to compete for the lock whenever the conflict
 * is resolved. Currently, there is only one wait list for all items.
 * </p>
 */
class LockTable {
	private static Logger logger = Logger.getLogger(LockTable.class.getName());
	
	private static final long MAX_TIME;
	private static final long EPSILON;
	final static int IS_LOCK = 0, IX_LOCK = 1, S_LOCK = 2, SIX_LOCK = 3,
			X_LOCK = 4;

	static {
		String prop = System.getProperty(LockTable.class.getName()
				+ ".MAX_TIME");
		MAX_TIME = (prop == null ? 10000 : Long.parseLong(prop.trim()));
		prop = System.getProperty(LockTable.class.getName() + ".EPSILON");
		EPSILON = (prop == null ? 50 : Long.parseLong(prop.trim()));
	}

	class Lockers {
		Set<Long> sLockers, ixLockers, isLockers;
		// only one tx can hold xLock(sixLock) on single item
		long sixLocker, xLocker;

		Lockers() {
			sLockers = new HashSet<Long>();
			ixLockers = new HashSet<Long>();
			isLockers = new HashSet<Long>();
			sixLocker = -1; // -1 means none
			xLocker = -1;
		}
	}

	private Map<Object, Lockers> lockerMap = new HashMap<Object, Lockers>();
	
	private CopyOnWriteArrayList<Long> T3LockedList = new CopyOnWriteArrayList<Long>();
	
	public void registerT3LockedList(long txNum)
	{
		if(T3LockedList.contains(txNum))
			return;
		T3LockedList.add(txNum);
		//logger.warning(txNum + "in T3LockedList");
//		Iterator<Long> it = T3LockedList.iterator();
//        while (it.hasNext()) {
//        	logger.warning("contain  "+it.next());
//        }
//        logger.warning("registerT3LockedList  ");
	}
	
	public void leaveT3LockedList(long txNum)
	{
//		Iterator<Long> it = T3LockedList.iterator();
//        while (it.hasNext()) {
//        	logger.warning("contain  "+it.next());
//        }

		if(!T3LockedList.contains(txNum))
		{
			logger.warning(txNum + " not in T3LockedList but call leave");
			return ;
		}
		//logger.warning(txNum + "leave T3LockedList");
		T3LockedList.remove(txNum);
	}
	
	public LockTable() {
		if (logger.isLoggable(Level.INFO))
			logger.info("LockTable for assignment 4 is ready");
	}

	/**
	 * Grants an slock on the specified item. If any conflict lock exists when
	 * the method is called, then the calling thread will be placed on a wait
	 * list until the lock is released. If the thread remains on the wait list
	 * for a certain amount of time, then an exception is thrown.
	 * 
	 * @param obj
	 *            a lockable item
	 * @param txNum
	 *            a transaction number
	 * 
	 */
	synchronized void sLock(Object obj, long txNum) {
		if (hasSLock(obj, txNum))
			return;

		try {
			long timestamp = System.currentTimeMillis();
			while (!sLockable(obj, txNum) && !waitingTooLong(timestamp))
				wait(MAX_TIME);
			if (!sLockable(obj, txNum))
				throw new LockAbortException("deadlock detected");
			prepareLockers(obj).sLockers.add(txNum);
		} catch (InterruptedException e) {
			throw new LockAbortException();
		}
	}
	
	synchronized int T3sLock(Object obj, long txNum) {
		//logger.warning(txNum+"in slock for" + obj.hashCode());
		if (hasSLock(obj, txNum))
			return 1;
		//logger.warning(txNum + " T3sLock");
		if(!sLockable(obj, txNum))
		{
			
			long conflictTxNum = lockerMap.get(obj).xLocker; 
			//logger.warning(txNum + " while " +conflictTxNum);
			if(conflictTxNum<txNum)
				return 0;
			if(T3LockedList.contains(conflictTxNum))
				return 2;
		    releaseAll(conflictTxNum,false);  
			
		}
		//logger.info(txNum+" slockable for"+obj.hashCode());
		try {
			prepareLockers(obj).sLockers.add(txNum);
			return 1;
		} catch (Exception e) {
			throw new LockAbortException();	
		}
		
	}

	/**
	 * Grants an xlock on the specified item. If any conflict lock exists when
	 * the method is called, then the calling thread will be placed on a wait
	 * list until the lock is released. If the thread remains on the wait list
	 * for a certain amount of time, then an exception is thrown.
	 * 
	 * @param obj
	 *            a lockable item
	 * @param txNum
	 *            a transaction number
	 * 
	 */
	synchronized void xLock(Object obj, long txNum) {
		if (hasXLock(obj, txNum))
			return;

		try {
			long timestamp = System.currentTimeMillis();
			while (!xLockable(obj, txNum) && !waitingTooLong(timestamp))
				wait(MAX_TIME);
			if (!xLockable(obj, txNum))
				throw new LockAbortException("deadlock detected");
			prepareLockers(obj).xLocker = txNum;
		} catch (InterruptedException e) {
			throw new LockAbortException();
		}
	}
	
	synchronized int T3xLock(Object obj, long txNum) {
		//logger.warning(txNum+"in xlock for" + obj.hashCode());
		if (hasXLock(obj, txNum))
			return 1;

		try {
			if (!xLockable(obj, txNum))
			{
				ArrayList<Long> releaseList = new ArrayList<Long>();
				long xlockConflictTxNum = lockerMap.get(obj).xLocker;
				
				if(xlockConflictTxNum!=-1&&(xlockConflictTxNum<txNum))
					return 0;
				if(xlockConflictTxNum!=-1&&!T3LockedList.contains(xlockConflictTxNum))
					return 2;
					//releaseList.add(xlockConflictTxNum);
				
				
				Iterator<Long> iterator = lockerMap.get(obj).sLockers.iterator();
				while (iterator.hasNext()){
					long slockConflictTxNum =iterator.next();
					if(slockConflictTxNum<txNum)
						return 0;
					if(T3LockedList.contains(slockConflictTxNum))
						return 2;
						//continue;  
					releaseList.add(iterator.next());
				}
				for (Long num : releaseList) {
					releaseAll(num,false);		
				}   
			}
			//logger.info(txNum+" xlockable for"+obj.hashCode());
			prepareLockers(obj).xLocker = txNum;
			return 1;
		} catch (Exception e) {
			throw new LockAbortException();
		}
	}

	/**
	 * Grants an sixlock on the specified item. If any conflict lock exists when
	 * the method is called, then the calling thread will be placed on a wait
	 * list until the lock is released. If the thread remains on the wait list
	 * for a certain amount of time, then an exception is thrown.
	 * 
	 * @param obj
	 *            a lockable item
	 * @param txNum
	 *            a transaction number
	 * 
	 */
	synchronized void sixLock(Object obj, long txNum) {
		if (hasSixLock(obj, txNum))
			return;

		try {
			long timestamp = System.currentTimeMillis();
			while (!sixLockable(obj, txNum) && !waitingTooLong(timestamp))
				wait(MAX_TIME);
			if (!sixLockable(obj, txNum))
				throw new LockAbortException("deadlock detected");
			prepareLockers(obj).sixLocker = txNum;
		} catch (InterruptedException e) {
			throw new LockAbortException();
		}

	}

	/**
	 * Grants an islock on the specified item. If any conflict lock exists when
	 * the method is called, then the calling thread will be placed on a wait
	 * list until the lock is released. If the thread remains on the wait list
	 * for a certain amount of time, then an exception is thrown.
	 * 
	 * @param obj
	 *            a lockable item
	 * @param txNum
	 *            a transaction number
	 */
	synchronized void isLock(Object obj, long txNum) {
		if (hasIsLock(obj, txNum))
			return;
		
		try {
			long timestamp = System.currentTimeMillis();
			while (!isLockable(obj, txNum) && !waitingTooLong(timestamp))
				wait(MAX_TIME);
			if (!isLockable(obj, txNum))
				throw new LockAbortException("deadlock detected");
			prepareLockers(obj).isLockers.add(txNum);
		} catch (InterruptedException e) {
			throw new LockAbortException();
		}
	}

	/**
	 * Grants an ixlock on the specified item. If any conflict lock exists when
	 * the method is called, then the calling thread will be placed on a wait
	 * list until the lock is released. If the thread remains on the wait list
	 * for a certain amount of time, then an exception is thrown.
	 * 
	 * @param obj
	 *            a lockable item
	 * @param txNum
	 *            a transaction number
	 */
	synchronized void ixLock(Object obj, long txNum) {
		if (hasIxLock(obj, txNum))
			return;
		
		try {
			long timestamp = System.currentTimeMillis();
			while (!ixLockable(obj, txNum) && !waitingTooLong(timestamp))
				wait(MAX_TIME);
			if (!ixLockable(obj, txNum))
				throw new LockAbortException("deadlock detected");
			prepareLockers(obj).ixLockers.add(txNum);
		} catch (InterruptedException e) {
			throw new LockAbortException();
		}
	}

	/**
	 * Releases the specified type of lock on an item holding by a transaction.
	 * If a lock is the last lock on that block, then the waiting transactions
	 * are notified.
	 * 
	 * @param obj
	 *            a lockable item
	 * @param txNum
	 *            a transaction number
	 * @param lockType
	 *            the type of lock
	 */
	synchronized void release(Object obj, long txNum, int lockType) {
		Lockers lks = lockerMap.get(obj);
		if (lks == null)
			return;
		switch (lockType) {
		case X_LOCK:
			if (lks.xLocker == txNum) {
				lks.xLocker = -1;
				notifyAll();
			}
			return;
		case SIX_LOCK:
			if (lks.sixLocker == txNum) {
				lks.sixLocker = -1;
				notifyAll();
			}
			return;
		case S_LOCK:
			Set<Long> sl = lks.sLockers;
			if (sl != null && sl.contains(txNum)) {
				sl.remove((Long) txNum);
				if (sl.isEmpty())
					notifyAll();
			}
			return;
		case IS_LOCK:
			Set<Long> isl = lks.isLockers;
			if (isl != null && isl.contains(txNum)) {
				isl.remove((Long) txNum);
				if (isl.isEmpty())
					notifyAll();
			}
			return;
		case IX_LOCK:
			Set<Long> ixl = lks.ixLockers;
			if (ixl != null && ixl.contains(txNum)) {
				ixl.remove((Long) txNum);
				if (ixl.isEmpty())
					notifyAll();
			}
			return;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Releases all locks held by a transaction. If a lock is the last lock on
	 * that block, then the waiting transactions are notified.
	 * 
	 * @param txNum
	 *            a transaction number
	 * 
	 * @param sLockOnly
	 *            release slocks only
	 */
	synchronized void releaseAll(long txNum, boolean sLockOnly) {
		List<Object> released = new LinkedList<Object>();
		for (Object obj : lockerMap.keySet()) {
			if (hasSLock(obj, txNum))
				release(obj, txNum, S_LOCK);

			if (hasXLock(obj, txNum) && !sLockOnly)
				release(obj, txNum, X_LOCK);

			if (hasSixLock(obj, txNum))
				release(obj, txNum, SIX_LOCK);

			while (hasIsLock(obj, txNum))
				release(obj, txNum, IS_LOCK);

			while (hasIxLock(obj, txNum) && !sLockOnly)
				release(obj, txNum, IX_LOCK);

			if (!sLocked(obj) && !xLocked(obj) && !sixLocked(obj)
					&& !isLocked(obj) && !ixLocked(obj))
				released.add(obj);
		}
		for (Object obj : released) {
			lockerMap.remove(obj);
		}
	}

	private Lockers prepareLockers(Object obj) {
		Lockers lockers = lockerMap.get(obj);
		if (lockers == null) {
			lockers = new Lockers();
			lockerMap.put(obj, lockers);
		}
		return lockers;
	}

	private boolean waitingTooLong(long starttime) {
		return System.currentTimeMillis() - starttime + EPSILON > MAX_TIME;
	}

	/*
	 * Verify if an item is locked.
	 */

	private boolean sLocked(Object obj) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.sLockers.size() > 0;
	}

	private boolean xLocked(Object obj) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.xLocker != -1;
	}

	private boolean sixLocked(Object obj) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.sixLocker != -1;
	}

	private boolean isLocked(Object obj) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.isLockers.size() > 0;
	}

	private boolean ixLocked(Object obj) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.ixLockers.size() > 0;
	}

	/*
	 * Verify if an item is held by a tx.
	 */

	private boolean hasSLock(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.sLockers.contains(txNum);
	}

	private boolean hasXLock(Object obj, long txNUm) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.xLocker == txNUm;
	}

	private boolean hasSixLock(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.sixLocker == txNum;
	}

	private boolean hasIsLock(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.isLockers.contains(txNum);
	}

	private boolean hasIxLock(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.ixLockers.contains(txNum);
	}

	private boolean isTheOnlySLocker(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		return lks != null && lks.sLockers.size() == 1
				&& lks.sLockers.contains(txNum);
	}

	private boolean isTheOnlyIsLocker(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		if (lks != null) {
			for (Object o : lks.isLockers)
				if (!o.equals(txNum))
					return false;
			return true;
		}
		return false;
	}

	private boolean isTheOnlyIxLocker(Object obj, long txNum) {
		Lockers lks = lockerMap.get(obj);
		if (lks != null) {
			for (Object o : lks.ixLockers)
				if (!o.equals(txNum))
					return false;
			return true;
		}
		return false;
	}

	/*
	 * Verify if an item is lockable to a tx.
	 */

	private boolean sLockable(Object obj, long txNum) {
		return (!xLocked(obj) || hasXLock(obj, txNum))
				&& (!sixLocked(obj) || hasSixLock(obj, txNum))
				&& (!ixLocked(obj) || isTheOnlyIxLocker(obj, txNum));
	}

	private boolean xLockable(Object obj, long txNum) {
		return (!sLocked(obj) || isTheOnlySLocker(obj, txNum))
				&& (!sixLocked(obj) || hasSixLock(obj, txNum))
				&& (!ixLocked(obj) || isTheOnlyIxLocker(obj, txNum))
				&& (!isLocked(obj) || isTheOnlyIsLocker(obj, txNum))
				&& (!xLocked(obj) || hasXLock(obj, txNum));
	}

	private boolean sixLockable(Object obj, long txNum) {
		return (!sixLocked(obj) || hasSixLock(obj, txNum))
				&& (!ixLocked(obj) || isTheOnlyIxLocker(obj, txNum))
				&& (!sLocked(obj) || isTheOnlySLocker(obj, txNum))
				&& (!xLocked(obj) || hasXLock(obj, txNum));
	}

	private boolean ixLockable(Object obj, long txNum) {
		return (!sLocked(obj) || isTheOnlySLocker(obj, txNum))
				&& (!sixLocked(obj) || hasSixLock(obj, txNum))
				&& (!xLocked(obj) || hasXLock(obj, txNum));
	}

	private boolean isLockable(Object obj, long txNum) {
		return (!xLocked(obj) || hasXLock(obj, txNum));
	}
}