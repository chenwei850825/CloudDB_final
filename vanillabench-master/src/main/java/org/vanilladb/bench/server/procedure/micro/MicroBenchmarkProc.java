package org.vanilladb.bench.server.procedure.micro;

import org.vanilladb.bench.server.param.micro.MicroBenchmarkProcParamHelper;
import org.vanilladb.bench.server.procedure.BasicStoredProcedure;
import org.vanilladb.core.query.algebra.Plan;
import org.vanilladb.core.query.algebra.Scan;
import org.vanilladb.core.server.VanillaDb;

public class MicroBenchmarkProc extends BasicStoredProcedure<MicroBenchmarkProcParamHelper> {

	public MicroBenchmarkProc() {
		super(new MicroBenchmarkProcParamHelper());
	}

	@Override
	protected void executeSql() {

		for (int idx = 0; idx < paramHelper.getReadCount(); idx++) {
			int iid = paramHelper.getReadItemId(idx);

			String sql = "SELECT i_name, i_price FROM item WHERE i_id = " + iid;
			Plan p = VanillaDb.newPlanner().createQueryPlan(sql, tx);
			Scan s = p.open();
			s.beforeFirst();
			if (s.next()) {
				String name = (String) s.getVal("i_name").asJavaVal();
				double price = (Double) s.getVal("i_price").asJavaVal();

				paramHelper.setItemName(name, idx);
				paramHelper.setItemPrice(price, idx);
			} else
				throw new RuntimeException("Cloud not find item record with i_id = " + iid);

			s.close();
		}
		
		for (int idx = 0; idx < paramHelper.getReadCount(); idx++) {
			int iid = paramHelper.getReadItemId(idx);
			
			if(ids[iid] != 0){

				String sql = "UPDATE item SET i_price = " + ids[iid] + " WHERE i_id =" + iid;
				VanillaDb.newPlanner().executeUpdate(sql, tx);
				ids[iid] = 0;

			}

			String sql = "SELECT i_name, i_price FROM item WHERE i_id = " + iid;
			Plan p = VanillaDb.newPlanner().createQueryPlan(sql, tx);
			Scan s = p.open();
			s.beforeFirst();
			if (s.next()) {
				String name = (String) s.getVal("i_name").asJavaVal();
				double price = (Double) s.getVal("i_price").asJavaVal();

				paramHelper.setItemName(name, idx);
				paramHelper.setItemPrice(price, idx);
			} else
				throw new RuntimeException("Cloud not find item record with i_id = " + iid);

			s.close();
		}

		for (int idx = 0; idx < paramHelper.getWriteCount(); idx++) {
			int iid = paramHelper.getWriteItemId(idx);
			double newPrice = paramHelper.getNewItemPrice(idx);
			
			ids[iid]=newPrice;
		}
	
	}
}
