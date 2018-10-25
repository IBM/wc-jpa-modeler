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

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class ApplicationInfo {
	private static final String ENTITY_CREATION_DATA = "com.ibm.commerce.context.content.objects.EntityCreationData";
	private static final String ENTITY_BEAN_CREATION_DATA = "com.ibm.commerce.context.content.resources.EntityBeanCreationData";
	
	private XMLUtil iXMLUtil = new XMLUtil();
	private Map<String, XMLUtil> iXMLUtils = new HashMap<String, XMLUtil>();
	private PersistenceUnitInfo iPersistenceUnitInfo;
	private Set<ModuleInfo> iModules = new HashSet<ModuleInfo>();
	private Map<String, String> iTypeMap = new HashMap<String, String>();
	private Set<String> iEntityTypes = new HashSet<String>();
	private Set<String> iEntityInterfaceTypes = new HashSet<String>();
	private Set<String> iEntityKeyTypes = new HashSet<String>();
	private Set<String> iAccessBeanTypes = new HashSet<String>();
	private Set<String> iAccessBeanInterfaceTypes = new HashSet<String>();
	private Set<String> iDataClassTypes = new HashSet<String>();
	private Map<String, EntityInfo> iEntitiesByType = new HashMap<String, EntityInfo>();
	private Set<String> iHomeInterfaceTypes = new HashSet<String>();
	private Map<String, BackupUtil> iBackupUtils = new HashMap<String, BackupUtil>();
	private Set<String> iDeleteIntendedTypes = new HashSet<String>();
	private Set<String> iEjbStubTypes = new HashSet<String>();
	private Set<String> iFactoryTypes = new HashSet<String>();
	private Map<String, ProjectInfo> iProjects = new HashMap<String, ProjectInfo>();
	private Set<String> iAccessBeanSubclasses = new HashSet<String>();
	private Map<String, AccessBeanSubclassInfo> iAccessBeanSubclassesByType = new HashMap<String, AccessBeanSubclassInfo>();
	private Set<String> iEntityJndiNames = new HashSet<String>();
	private Set<String> iStubTypes = new HashSet<String>();
	private int iParsedAssetCount = 0;
	private int iGeneratedAssetCount = 0;
	private int iSearchResultCount = 0;
	private int iUpdateCount = 0;
	private int iDeleteCount = 0;
	
	public ApplicationInfo() {
		iTypeMap.put(ENTITY_CREATION_DATA, ENTITY_BEAN_CREATION_DATA);
		addTypeMapping("com.ibm.commerce.registry.StoreRegistry", "com.ibm.commerce.registry.StoreRegistry");
		addTypeMapping("com.ibm.commerce.contract.objects.ContractJDBCHelperAccessBean", "com.ibm.commerce.contract.objects.ContractJDBCHelperAccessBean");
		addTypeMapping("com.ibm.commerce.utf.helper.SortingAttribute", "com.ibm.commerce.base.util.SortingAttribute");
		addDeleteIntendedType("com.ibm.commerce.utf.helper.SortingAttribute");
		addTypeMapping("com.ibm.commerce.negotiation.util.SortingAttribute", "com.ibm.commerce.base.util.SortingAttribute");
		addDeleteIntendedType("com.ibm.commerce.negotiation.util.SortingAttribute");
		iTypeMap.put("com.ibm.ivj.ejb.runtime.AbstractEntityAccessBean", "com.ibm.commerce.persistence.AbstractJpaEntityAccessBean");
		iTypeMap.put("com.ibm.ivj.ejb.runtime.AbstractAccessBean", "com.ibm.commerce.persistence.AbstractJpaEntityAccessBean");
		addDeleteIntendedType("com.ibm.commerce.context.objects.util.EJBLocalHomeFactory");
		addTypeMapping("com.ibm.commerce.datatype.RefreshOnceAccessBean", "com.ibm.commerce.datatype.InstantiateOnceAccessBean");
		addTypeMapping("com.ibm.commerce.datatype.RefreshOnceAccessBeanHelper", "com.ibm.commerce.datatype.InstantiateOnceAccessBeanHelper");
	}
	
	public XMLUtil getXMLUtil() {
		return iXMLUtil;
	}
	
	public void setPersistenceUnitInfo(PersistenceUnitInfo persistenceUnitInfo) {
		iPersistenceUnitInfo = persistenceUnitInfo;
	}
	
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return iPersistenceUnitInfo;
	}
	
	public void addModule(ModuleInfo moduleInfo) {
		iModules.add(moduleInfo);
	}
	
	public Set<ModuleInfo> getModules() {
		return iModules;
	}
	
	public TableInfo getTableInfo(String tableName) {
		TableInfo tableInfo = null;
		for (ModuleInfo moduleInfo : iModules) {
			tableInfo = moduleInfo.getTableInfoByName(tableName);
			if (tableInfo != null) {
				break;
			}
		}
		return tableInfo;
	}
	
	public void addTypeMapping(String oldType, String newType) {
		synchronized(iTypeMap) {
			iTypeMap.put(oldType, newType);
//			addDeleteIntendedType(newType);
		}
	}
	
	public String getTypeMapping(String oldType) {
		synchronized(iTypeMap) {
			return iTypeMap.get(oldType);
		}
	}
	
	public void addEntityType(String entityType) {
		synchronized(iEntityTypes) {
			iEntityTypes.add(entityType);
		}
	}
	
	public boolean isEntityType(String entityType) {
		synchronized(iEntityTypes) {
			return iEntityTypes.contains(entityType);
		}
	}

	public void addEntityInterfaceType(String entityInterfaceType) {
		synchronized(iEntityInterfaceTypes) {
			iEntityInterfaceTypes.add(entityInterfaceType);
		}
	}
	
	public boolean isEntityInterfaceType(String entityInterfaceType) {
		synchronized(iEntityInterfaceTypes) {
			return iEntityInterfaceTypes.contains(entityInterfaceType);
		}
	}
	
	public void addEntityKeyType(String entityKeyType) {
		synchronized(iEntityKeyTypes) {
			iEntityKeyTypes.add(entityKeyType);
		}
	}
	
	public boolean isEntityKeyType(String entityKeyType) {
		synchronized(iEntityKeyTypes) {
			return iEntityKeyTypes.contains(entityKeyType);
		}
	}
	
	public void addAccessBeanType(String accessBeanType) {
		synchronized(iAccessBeanTypes) {
			iAccessBeanTypes.add(accessBeanType);
		}
	}
	
	public boolean isAccessBeanType(String type) {
		synchronized(iAccessBeanTypes) {
			return iAccessBeanTypes.contains(type);
		}
	}

	public void addAccessBeanInterfaceType(String accessBeanInterfaceType) {
		synchronized(iAccessBeanInterfaceTypes) {
			iAccessBeanInterfaceTypes.add(accessBeanInterfaceType);
		}
	}
	
	public boolean isAccessBeanInterfaceType(String type) {
		synchronized(iAccessBeanInterfaceTypes) {
			return iAccessBeanInterfaceTypes.contains(type);
		}
	}
	
	public void addDataClassType(String dataClassType) {
		synchronized(iDataClassTypes) {
			iDataClassTypes.add(dataClassType);
		}
	}
	
	public boolean isDataClassType(String type) {
		synchronized(iDataClassTypes) {
			return iDataClassTypes.contains(type);
		}
	}
	
	public void setEntityInfoForType(String type, EntityInfo entityInfo) {
		synchronized(iEntitiesByType) {
			iEntitiesByType.put(type, entityInfo);
		}
	}
	
	public EntityInfo getEntityInfoForType(String type) {
		synchronized(iEntitiesByType) {
			return iEntitiesByType.get(type);
		}
	}
	
	public void addHomeInterfaceType(String homeInterfaceType) {
		synchronized(iHomeInterfaceTypes) {
			iHomeInterfaceTypes.add(homeInterfaceType);
		}
	}
	
	public boolean isHomeInterfaceType(String type) {
		synchronized(iHomeInterfaceTypes) {
			return iHomeInterfaceTypes.contains(type);
		}
	}
	
	public BackupUtil getBackupUtil(IProject project) {
		synchronized (iBackupUtils) {
			BackupUtil backupUtil = iBackupUtils.get(project.getName());
			if (backupUtil == null) {
				backupUtil = new BackupUtil(project);
				iBackupUtils.put(project.getName(), backupUtil);
			}
			return backupUtil;
		}
	}
	
	public ProjectInfo getProjectInfo(IProject project) {
		synchronized (iProjects) {
			ProjectInfo projectInfo = iProjects.get(project.getName());
			if (projectInfo == null) {
				projectInfo = new ProjectInfo(this, project);
				iProjects.put(project.getName(), projectInfo);
			}
			return projectInfo;
		}
	}
	
	public Collection<ProjectInfo> getProjects() {
		return iProjects.values();
	}
	
	public XMLUtil getXMLUtil(IProject project) {
		synchronized (iXMLUtils) {
			XMLUtil xmlUtil = iXMLUtils.get(project.getName());
			if (xmlUtil == null) {
				xmlUtil = new XMLUtil();
				iXMLUtils.put(project.getName(), xmlUtil);
			}
			return xmlUtil;
		}
	}
	
	public ModuleInfo getModuleInfo(IJavaProject project) {
		ModuleInfo moduleInfo = null;
		for (ModuleInfo currentModuleInfo : iModules) {
			if (currentModuleInfo.getJavaProject().getElementName().equals(project.getElementName())) {
				moduleInfo = currentModuleInfo;
				break;
			}
		}
		return moduleInfo;
	}
	
	public void addDeleteIntendedType(String typeName) {
		if ("java.lang.String".equals(typeName)) {
			System.out.println("wrong!");
		}
		synchronized (iDeleteIntendedTypes) {
			iDeleteIntendedTypes.add(typeName);
		}
	}
	
	public boolean isDeleteIntendedType(String typeName) {
		synchronized (iDeleteIntendedTypes) {
			return iDeleteIntendedTypes.contains(typeName);
		}
	}
	
	public void addEjbStubType(String typeName) {
		synchronized (iEjbStubTypes) {
			iEjbStubTypes.add(typeName);
		}
	}
	
	public Collection<String> getEjbStubTypes() {
		return iEjbStubTypes;
	}
	
	public void addFactoryType(String factoryType) {
		synchronized (iFactoryTypes) {
			iFactoryTypes.add(factoryType);
		}
	}
	
	public boolean isFactoryType(String type) {
		synchronized (iFactoryTypes) {
			return iFactoryTypes.contains(type);
		}
	}
	
	public void addAccessBeanSubclass(String accessBeanSubclass) {
		synchronized (iAccessBeanSubclasses) {
			iAccessBeanSubclasses.add(accessBeanSubclass);
		}
	}
	
	public boolean isAccessBeanSubclass(String type) {
		synchronized (iAccessBeanSubclasses) {
			return iAccessBeanSubclasses.contains(type);
		}
	}
	
	public void setAccessBeanSubclassInfoForType(String type, AccessBeanSubclassInfo accessBeanSubclassInfo) {
		synchronized (iAccessBeanSubclassesByType) {
			iAccessBeanSubclassesByType.put(type, accessBeanSubclassInfo);
		}
	}
	
	public AccessBeanSubclassInfo getAccessBeanSubclassInfoForType(String type) {
		synchronized (iAccessBeanSubclassesByType) {
			return iAccessBeanSubclassesByType.get(type);
		}
	}

	public void addEntityJndiName(String jndiName) {
		synchronized (iEntityJndiNames) {
			iEntityJndiNames.add(jndiName);
		}
	}
	
	public boolean isEntityJndiName(String string) {
		return iEntityJndiNames.contains(string);
	}
	
	public void addStubType(String stubType) {
		synchronized (iStubTypes) {
			iStubTypes.add(stubType);
		}
	}
	
	public void removeStubType(String stubType) {
		synchronized (iStubTypes) {
			iStubTypes.remove(stubType);
		}
	}
	
	public boolean isStubType(String type) {
		return iStubTypes.contains(type);
	}

	public void incrementParsedAssetCount() {
		synchronized (this) {
			iParsedAssetCount++;
		}
	}
	
	public void incrementGeneratedAssetCount() {
		synchronized (this) {
			iGeneratedAssetCount++;
		}
	}
	
	public void incrementSearchResultCount() {
		synchronized (this) {
			iSearchResultCount++;
		}
	}
	
	public void incrementUpdateCount() {
		synchronized (this) {
			iUpdateCount++;
		}
	}
	
	public void incrementDeleteCount() {
		synchronized (this) {
			iDeleteCount++;
		}
	}
	
	public void printSummary() {
		System.out.println("parsed " + iParsedAssetCount + " assets");
		System.out.println("generated " + iGeneratedAssetCount + " assets");
		System.out.println("search found " + iSearchResultCount + " assets");
		System.out.println("updated " + iUpdateCount + " assets");
		System.out.println("deleted " + iDeleteCount + " assets");
	}
}
