package org.vanilladb.bench.server.procedure.micro;

import java.awt.List;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.vanilladb.bench.server.param.micro.MicroBenchmarkProcParamHelper;
import org.vanilladb.bench.server.procedure.BasicStoredProcedure;
import org.vanilladb.core.query.algebra.Plan;
import org.vanilladb.core.query.algebra.Scan;
import org.vanilladb.core.server.VanillaDb;

public class MicroBenchmarkProc extends BasicStoredProcedure<MicroBenchmarkProcParamHelper> {

	public MicroBenchmarkProc() {
		super(new MicroBenchmarkProcParamHelper());
	}

	public static void stop(){
		System.out.println("stop");
		try {
			PrintWriter writer = new PrintWriter("stickies.txt", "UTF-8");

		for(int i = 0 ; i < 100000 ; i++){		
			if(BasicStoredProcedure.ids[i] != 0){
				    writer.println(i +  " " + BasicStoredProcedure.ids[i]);
			}
		}
	    writer.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}
	
	
	public static void start(){
	    BufferedReader br = null;
	    try{
	      br = new BufferedReader(new FileReader("stickies.txt"));
	      String in = null;
	      while((in = br.readLine()) != null){
	    	  String[] lastData = in.split(" ");
	    	  BasicStoredProcedure.ids[Integer.valueOf(lastData[0])] = Double.valueOf(lastData[1]);
	      }
	    }catch(IOException ioe){
	      
	    }finally{
	     
	    }    
	    
	    
	}
	
	@Override
	protected void executeSql() {

		if(paramHelper.isStopped() == true){
			System.out.println("stopped");
		}

		for (int idx = 0; idx < paramHelper.getReadCount(); idx++) {
			int iid = paramHelper.getReadItemId(idx);
			
			if(BasicStoredProcedure.ids[iid] != 0){
				String sql = "UPDATE item SET i_price = " + BasicStoredProcedure.ids[iid] + " WHERE i_id =" + iid;
				VanillaDb.newPlanner().executeUpdate(sql, tx);
				BasicStoredProcedure.ids[iid] = 0;
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
			
			BasicStoredProcedure.ids[iid]=newPrice;
			//System.out.println(BasicStoredProcedure.ids.toString() );
			

		}
	
	}
}
