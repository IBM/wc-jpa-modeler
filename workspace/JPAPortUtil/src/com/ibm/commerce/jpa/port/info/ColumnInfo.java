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

public class ColumnInfo {
	private String iColumnId;
	private TableInfo iTableInfo;
	private String iColumnName;
	private boolean iNullable = true;
	private String iType;
	private String iTypeName;
	private String iPrimitiveType;
	private Integer iLength;
	private String iDefaultValue;
	private ColumnInfo iReferencedColumn;

	public ColumnInfo(TableInfo tableInfo, String columnId) {
		iTableInfo = tableInfo;
		iColumnId = columnId;
	}

	public String getColumnId() {
		return iColumnId;
	}

	public TableInfo getTableInfo() {
		return iTableInfo;
	}
	
	public void setColumnName(String columnName) {
		iColumnName = columnName;
	}

	public String getColumnName() {
		return iColumnName;
	}
	
	public void setNullable(boolean nullable) {
		iNullable = nullable;
	}
	
	public boolean getNullable() {
		return iNullable;
	}
	
	public void setType(String type) {
		iType = type;
	}
	
	public String getType() {
		return iType;
	}
	
	public void setTypeName(String typeName) {
		iTypeName = typeName;
	}
	
	public String getTypeName() {
		return iTypeName;
	}
	
	public void setPrimitiveType(String primitiveType) {
		iPrimitiveType = primitiveType;
	}
	
	public String getPrimitiveType() {
		return iPrimitiveType;
	}
	
	public void setLength(Integer length) {
		iLength = length;
	}
	
	public Integer getLength() {
		return iLength;
	}
	
	public void setDefaultValue(String defaultValue) {
		iDefaultValue = defaultValue;
	}
	
	public String getDefaultValue() {
		return iDefaultValue;
	}
	
	public void setReferencedColumn(ColumnInfo referencedColumn) {
		iReferencedColumn = referencedColumn;
	}
	
	public ColumnInfo getReferencedColumn() {
		return iReferencedColumn;
	}
}
