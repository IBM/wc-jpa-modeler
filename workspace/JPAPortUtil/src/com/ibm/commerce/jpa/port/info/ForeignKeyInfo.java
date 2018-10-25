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
import java.util.List;

public class ForeignKeyInfo {
	private TableInfo iTableInfo;
	private TableInfo iParentTableInfo;
	private List<ColumnInfo> iMemberColumns = new ArrayList<ColumnInfo>();
	private List<ColumnInfo> iReferencedColumns = new ArrayList<ColumnInfo>();
	
	public ForeignKeyInfo(TableInfo tableInfo) {
		iTableInfo = tableInfo;
	}
	
	public TableInfo getTableInfo() {
		return iTableInfo;
	}
	
	public void setParentTableInfo(TableInfo parentTableInfo) {
		iParentTableInfo = parentTableInfo;
	}
	
	public TableInfo getParentTableInfo() {
		return iParentTableInfo;
	}
	
	public void addMember(ColumnInfo memberColumn, ColumnInfo referencedColumn) {
		iMemberColumns.add(memberColumn);
		iReferencedColumns.add(referencedColumn);
		memberColumn.setReferencedColumn(referencedColumn);
	}
	
	public List<ColumnInfo> getMemberColumns() {
		return iMemberColumns;
	}
	
	public List<ColumnInfo> getReferencedColumns() {
		return iReferencedColumns;
	}
}
