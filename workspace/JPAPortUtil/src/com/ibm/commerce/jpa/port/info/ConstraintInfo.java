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

public class ConstraintInfo {
	public static final String PRIMARY_KEY_CONSTRAINT = "SQLConstraints:PrimaryKey";
	public static final String FOREIGN_KEY_CONSTRAINT = "SQLConstraints:ForeignKey";
	
	private TableInfo iTableInfo;
	private String iConstraintId;
	private String iConstraintName;
	private String iType;
	private String[] iMembers;
	private String iForeignKey;
	private String iUniqueConstraint;
	private String[] iReferencedMembers;
	private String iReferencedTable;
	private ForeignKeyInfo iForeignKeyInfo;

	public ConstraintInfo(TableInfo tableInfo, String constraintId) {
		iTableInfo = tableInfo;
		iConstraintId = constraintId;
	}
	
	public TableInfo getTableInfo() {
		return iTableInfo;
	}

	public String getConstraintId() {
		return iConstraintId;
	}

	public void setConstraintName(String constraintName) {
		iConstraintName = constraintName;
	}

	public String getConstraintName() {
		return iConstraintName;
	}
	
	public void setType(String type) {
		iType = type;
	}
	
	public String getType() {
		return iType;
	}
	
	public void setMembers(String members) {
		members = members.trim();
		iMembers = members.length() > 0 ? members.split(" ") : new String[0];
	}
	
	public String[] getMembers() {
		return iMembers;
	}
	
	public void setForeignKey(String foreignKey) {
		iForeignKey = foreignKey;
	}
	
	public String getForeignKey() {
		return iForeignKey;
	}
	
	public void setUniqueConstraint(String uniqueConstraint) {
		iUniqueConstraint = uniqueConstraint;
	}
	
	public String getUniqueConstraint() {
		return iUniqueConstraint;
	}
	
	public void setReferencedMembers(String referencedMembers) {
		referencedMembers = referencedMembers.trim();
		iReferencedMembers = referencedMembers.length() > 0 ? referencedMembers.split(" ") : new String[0];
	}
	
	public String[] getReferencedMembers() {
		return iReferencedMembers;
	}
	
	public void setReferencedTable(String referencedTable) {
		iReferencedTable = referencedTable;
	}
	
	public String getReferencedTable() {
		return iReferencedTable;
	}
	
	public void setForeignKeyInfo(ForeignKeyInfo foreignKeyInfo) {
		iForeignKeyInfo = foreignKeyInfo;
	}
	
	public ForeignKeyInfo getForeignKeyInfo() {
		return iForeignKeyInfo;
	}
}
