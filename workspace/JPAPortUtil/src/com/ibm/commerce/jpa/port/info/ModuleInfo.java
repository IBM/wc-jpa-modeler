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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.commerce.jpa.port.util.XMLUtil;

public class ModuleInfo {
	private ApplicationInfo iApplicationInfo;
	private IJavaProject iJavaProject;
	private Map<String, EntityInfo> iEntities = new HashMap<String, EntityInfo>();
	private Map<String, EntityInfo> iEntitiesByName = new HashMap<String, EntityInfo>();
	private Map<String, EjbRelationshipRoleInfo> iEjbRelationshipRoles = new HashMap<String, EjbRelationshipRoleInfo>();
	private Map<String, TableInfo> iTables = new HashMap<String, TableInfo>();
	private Map<String, TableInfo> iTablesByName = new HashMap<String, TableInfo>();
	private Map<String, ColumnInfo> iColumns = new HashMap<String, ColumnInfo>();
	private Map<String, ConstraintInfo> iConstraints = new HashMap<String, ConstraintInfo>();
	private Map<String, CompilationUnit> iCompilationUnits = new HashMap<String, CompilationUnit>();
	private Set<String> iDeleteIntendedTypes = new HashSet<String>();
	
	public ModuleInfo(ApplicationInfo applicationInfo, IJavaProject javaProject) {
		iApplicationInfo = applicationInfo;
		iJavaProject = javaProject;
	}

	public ApplicationInfo getApplicationInfo() {
		return iApplicationInfo;
	}
	
	public IJavaProject getJavaProject() {
		return iJavaProject;
	}
	
	public XMLUtil getXMLUtil() {
		return iApplicationInfo.getXMLUtil(iJavaProject.getProject());
	}
	
	public EntityInfo getEntityInfo(String entityId) {
		return getEntityInfo(entityId, false);
	}
	
	public EntityInfo getEntityInfo(String entityId, boolean create) {
		EntityInfo entityInfo = iEntities.get(entityId);
		if (entityInfo == null && create) {
			entityInfo = new EntityInfo(this, entityId);
			iEntities.put(entityId, entityInfo);
		}
		return entityInfo;
	}
	
	public Collection<EntityInfo> getEntities() {
		return iEntities.values();
	}
	
	public EntityInfo getEntityInfoByName(String entityName) {
		return iEntitiesByName.get(entityName);
	}
	
	public void setEntityName(EntityInfo entityInfo, String entityName) {
		iEntitiesByName.put(entityName, entityInfo);
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfo(String ejbRelationshipRoleId) {
		return getEjbRelationshipRoleInfo(ejbRelationshipRoleId, false);
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfo(String ejbRelationshipRoleId, boolean create) {
		EjbRelationshipRoleInfo ejbRelationshipRoleInfo = iEjbRelationshipRoles.get(ejbRelationshipRoleId);
		if (ejbRelationshipRoleInfo == null && create) {
			ejbRelationshipRoleInfo = new EjbRelationshipRoleInfo(ejbRelationshipRoleId);
			iEjbRelationshipRoles.put(ejbRelationshipRoleId, ejbRelationshipRoleInfo);
		}
		return ejbRelationshipRoleInfo;
	}
	
	public Collection<EjbRelationshipRoleInfo> getEjbRelationshipRoles() {
		return iEjbRelationshipRoles.values();
	}
	
	public TableInfo getTableInfo(String tableId, boolean create) {
		TableInfo tableInfo = iTables.get(tableId);
		if (tableInfo == null && create) {
			tableInfo = new TableInfo(this, tableId);
			iTables.put(tableId, tableInfo);
		}
		return tableInfo;
	}
	
	public TableInfo getTableInfo(String tableId) {
		return getTableInfo(tableId, false);
	}
	
	public void setTableName(TableInfo tableInfo, String tableName) {
		tableInfo.setTableName(tableName);
		iTablesByName.put(tableName, tableInfo);
	}
	
	public TableInfo getTableInfoByName(String tableName) {
		return iTablesByName.get(tableName);
	}
	
	public Collection<TableInfo> getTables() {
		return iTables.values();
	}
	
	public void addColumnInfo(ColumnInfo columnInfo) {
		if (iColumns.containsKey(columnInfo.getColumnId())) {
			System.out.println("duplicate column ID "+columnInfo.getColumnId()+" in "+iJavaProject.getElementName());
		}
		iColumns.put(columnInfo.getColumnId(), columnInfo);
	}
	
	public ColumnInfo getColumnInfo(String columnId) {
		return iColumns.get(columnId);
	}
	
	public void addConstraintInfo(ConstraintInfo constraintInfo) {
		iConstraints.put(constraintInfo.getConstraintId(), constraintInfo);
	}
	
	public ConstraintInfo getConstraintInfo(String constraintId) {
		return iConstraints.get(constraintId);
	}
	
	public void setCompilationUnit(String type, CompilationUnit compilationUnit) {
		iCompilationUnits.put(type, compilationUnit);
	}
	
	public CompilationUnit getCompilationUnit(String type) {
		return iCompilationUnits.get(type);
	}
	
	public void addDeleteIntendedType(String deleteIntendedType) {
		iDeleteIntendedTypes.add(deleteIntendedType);
		iApplicationInfo.addDeleteIntendedType(deleteIntendedType);
	}
	
	public Set<String> getDeleteIntendedTypes() {
		return iDeleteIntendedTypes;
	}
}
