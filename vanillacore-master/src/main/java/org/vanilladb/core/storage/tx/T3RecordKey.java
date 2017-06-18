package org.vanilladb.core.storage.tx;

import org.vanilladb.core.sql.Constant;

public class T3RecordKey {
	private String tableName;
	private String primaryName;
	private Constant primaryValue;
	
	public T3RecordKey(String tableName, String primaryName,Constant primaryValue)
	{
		this.tableName = tableName;
		this.primaryName = primaryName;
		this.primaryValue = primaryValue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(obj instanceof T3RecordKey 
				&& ((T3RecordKey) obj).tableName.equals(tableName)
				&& ((T3RecordKey) obj).primaryName.equals(primaryName)
				&& ((T3RecordKey) obj).primaryValue.equals(primaryValue))
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((primaryName == null) ? 0 : primaryName.hashCode());
		result = prime * result + ((primaryValue == null) ? 0 : primaryValue.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}
	
}