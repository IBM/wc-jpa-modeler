package com.ibm.commerce.jpa.port.info;

/*
 *-----------------------------------------------------------------
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableInfo {
	private ModuleInfo iModuleInfo;
	private String iTableId;
	private String iTableName;
	private Map<String, ColumnInfo> iColumns = new HashMap<String, ColumnInfo>();
	private List<ColumnInfo> iColumnList = new ArrayList<ColumnInfo>();
	private Map<String, ColumnInfo> iColumnsByName = new HashMap<String, ColumnInfo>();
	private Map<String, ConstraintInfo> iConstraints = new HashMap<String, ConstraintInfo>();
	private Set<ForeignKeyInfo> iForeignKeys = new HashSet<ForeignKeyInfo>();
	private EntityInfo iEntityInfo;
	private List<ColumnInfo> iPrimaryKeyColumns = new ArrayList<ColumnInfo>();

	public TableInfo(ModuleInfo moduleInfo, String tableId) {
		iModuleInfo = moduleInfo;
		iTableId = tableId;
	}
	
	public ModuleInfo getModuleInfo() {
		return iModuleInfo;
	}

	public String getTableId() {
		return iTableId;
	}

	public void setTableName(String tableName) {
		iTableName = tableName;
	}

	public String getTableName() {
		return iTableName;
	}
	
	public ColumnInfo getColumnInfo(String columnId, boolean create) {
		ColumnInfo columnInfo = iColumns.get(columnId);
		if (columnInfo == null && create) {
			columnInfo = new ColumnInfo(this, columnId);
			iColumns.put(columnId, columnInfo);
			iColumnList.add(columnInfo);
		}
		return columnInfo;
	}
	
	public ColumnInfo getColumnInfo(String columnId) {
		return getColumnInfo(columnId, false);
	}
	
	public List<ColumnInfo> getColumns() {
		return iColumnList;
	}
	
	public void setColumnName(ColumnInfo columnInfo, String columnName) {
		columnInfo.setColumnName(columnName);
		iColumnsByName.put(columnName, columnInfo);
	}
	
	public ColumnInfo getColumnInfoByName(String columnName) {
		return iColumnsByName.get(columnName);
	}
	
	public ConstraintInfo getConstraintInfo(String constraintId, boolean create) {
		ConstraintInfo constraintInfo = iConstraints.get(constraintId);
		if (constraintInfo == null && create) {
			constraintInfo = new ConstraintInfo(this, constraintId);
			iConstraints.put(constraintId, constraintInfo);
		}
		return constraintInfo;
	}
	
	public ConstraintInfo getConstraintInfo(String constraintId) {
		return getConstraintInfo(constraintId, false);
	}
	
	public Collection<ConstraintInfo> getConstraints() {
		return iConstraints.values();
	}
	
	public void addForeignKey(ForeignKeyInfo foreignKeyInfo) {
		iForeignKeys.add(foreignKeyInfo);
	}
	
	public Set<ForeignKeyInfo> getForeignKeys() {
		return iForeignKeys;
	}
	
	public void setEntityInfo(EntityInfo entityInfo) {
		iEntityInfo = entityInfo;
	}
	
	public EntityInfo getEntityInfo() {
		return iEntityInfo;
	}
	
	public void addPrimaryKeyColumnInfo(ColumnInfo primaryKeyColumnInfo) {
		if (!iPrimaryKeyColumns.contains(primaryKeyColumnInfo)) {
			iPrimaryKeyColumns.add(primaryKeyColumnInfo);
		}
	}
	
	public List<ColumnInfo> getPrimaryKeyColumns() {
		return iPrimaryKeyColumns;
	}

	StringBuffer errors = null;
	
	public void addError(String error) {
		if(errors == null) {
			errors = new StringBuffer("");
		}
		errors.append(error + System.lineSeparator());
	}
	public String getErrors() {
		
		return (errors==null)?null:errors.toString();
	}
}
