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
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class EntityInfo {
	private static final String EC_ENTITY_BEAN = "com.ibm.commerce.base.objects.ECEntityBean";
	private static final String EC_ABSTRACT_ENTITY_BEAN = "com.ibm.commerce.base.objects.ECAbstractEntityBean";
	private static final String GIFT_REGISTRY_PROTECTABLE_BEAN_BASE = "com.ibm.commerce.component.giftregistry.objimpl.GiftRegistryProtectableBeanBase";
	private static final String JPA_ENTITY_BASE = "EntityBase";
	private static final String JPA_ENTITY_BASE_PACKAGE = "com.ibm.commerce.base.objects";
	private static final String JPA_GIFT_REGISTRY_PROTECTABLE_ENTITY_BASE = "JPAGiftRegistryProtectableEntityBase";
	private static final String JPA_GIFT_REGISTRY_PROTECTABLE_ENTITY_BASE_PACKAGE = "com.ibm.commerce.component.giftregistry.objimpl";
	private static final String JDBC_FINDER_OBJECT = "com.ibm.commerce.base.objects.JDBCFinderObject";
	private static final String GENERIC_FINDER_OBJECT = "com.ibm.commerce.base.objects.GenericFinderObject";
	private static final String NEGOTIATION_GENERIC_FINDER_OBJECT = "com.ibm.commerce.negotiation.objimpl.GenericFinderObject";
	private static final String UTF_GENERIC_FINDER_OBJECT = "com.ibm.commerce.utf.objimpl.GenericFinderObject";
	private static final String VAP_EJS_JDBC_FINDER_OBJECT = "com.ibm.vap.finders.VapEJSJDBCFinderObject";
	private static final String JPA_QUERY_HELPER = "JPAQueryHelper";
	private static final String JPA_QUERY_HELPER_PACKAGE = "com.ibm.commerce.base.objects";
	private static final Collection<String> FINDER_OBJECT_BASE_CLASSES;
	static {
		FINDER_OBJECT_BASE_CLASSES = new HashSet<String>();
		FINDER_OBJECT_BASE_CLASSES.add(JDBC_FINDER_OBJECT);
		FINDER_OBJECT_BASE_CLASSES.add(GENERIC_FINDER_OBJECT);
		FINDER_OBJECT_BASE_CLASSES.add(NEGOTIATION_GENERIC_FINDER_OBJECT);
		FINDER_OBJECT_BASE_CLASSES.add(UTF_GENERIC_FINDER_OBJECT);
		FINDER_OBJECT_BASE_CLASSES.add(VAP_EJS_JDBC_FINDER_OBJECT);
	}
	private static final String ENTITY_CREATION_DATA = "com.ibm.commerce.context.content.objects.EntityCreationData";
	
	private ModuleInfo iModuleInfo;
	private String iEntityId;
	private String iEjbName;
	private String iHome;
	private String iRemote;
	private String iLocalHome;
	private String iLocal;
	private String iEjbClass;
	private String iPrimaryKeyClass;
	private String iPrimaryKeyField;
	private String iAbstractSchemaName;
	private String iVersion;
	private Map<String, FieldInfo> iFields = new HashMap<String, FieldInfo>();
	private Map<String, FieldInfo> iFieldsByName = new HashMap<String, FieldInfo>();
	private Map<String, FieldInfo> iFieldsByGetterName = new HashMap<String, FieldInfo>();
	private Map<String, FieldInfo> iFieldsBySetterName = new HashMap<String, FieldInfo>();
	private Map<String, FieldInfo> iFieldsBySetterMethodKey = new HashMap<String, FieldInfo>();
	private List<FieldInfo> iKeyFields;
	private Map<String, EjbLocalRefInfo> iEjbLocalRefs = new HashMap<String, EjbLocalRefInfo>();
	private List<RelatedEntityInfo> iRelatedEntities = new ArrayList<RelatedEntityInfo>();
	private List<RelatedEntityInfo> iKeyRelatedEntities;
	private List<EjbRelationshipRoleInfo> iEjbRelationshipRoles = new ArrayList<EjbRelationshipRoleInfo>();
	private Map<String, EjbRelationshipRoleInfo> iEjbRelationshipRolesByName = new HashMap<String, EjbRelationshipRoleInfo>();
	private Map<String, EjbRelationshipRoleInfo> iEjbRelationshipRolesByGetterName = new HashMap<String, EjbRelationshipRoleInfo>();
	private Map<String, EjbRelationshipRoleInfo> iEjbRelationshipRolesBySetterName = new HashMap<String, EjbRelationshipRoleInfo>();
	private AccessBeanInfo iAccessBeanInfo;
	private String iConcurrencyControl;
	private List<FinderInfo> iFinders = new ArrayList<FinderInfo>();
	private List<FinderInfo> iNamedFinders;
	private List<FinderInfo> iUserFinders;
	private Map<String, CreatorInfo> iCreators = new HashMap<String, CreatorInfo>();
	private Set<CreatorInfo> iCreatorsCalledByCreators = new HashSet<CreatorInfo>();
	private Map<String, AccessBeanMethodInfo> iAccessBeanMethods = new HashMap<String, AccessBeanMethodInfo>();
	private EntityInfo iSupertype;
	private Set<EntityInfo> iSubtypes;
	private Map<String,Set<MethodInfo>> iAccessIntents = new HashMap<String,Set<MethodInfo>>();
	private List<TableInfo> iTables = new ArrayList<TableInfo>();
	private String iDiscriminatorValue;
	private ColumnInfo iDiscriminatorColumnInfo;
	private ConstraintInfo iJoinKey;
	private TableInfo iPrimaryTableInfo;
	private TableInfo iSecondaryTableInfo;
	private String iSelectClause;
	private IType iEjbType;
	private IType iHomeType;
	private IType iLocalHomeType;
	private IType iRemoteType;
	private IType iLocalType;
	private IType iPrimaryKeyType;
	private IType iEjbSuperclassType;
	private IType iEjbBaseType;
	private IType iEjbFinderObjectType;
	private IType iEjbFinderObjectSuperclassType;
	private IType iEjbFinderObjectBaseType;
	private IType iEjbAccessHelperType;
	private IType iEjbAccessBeanType;
	private IType iEjbEntityCreationDataType;
	private ClassInfo iEntityClassInfo;
	private ClassInfo iEntityBaseClassInfo;
	private ClassInfo iEntityKeyClassInfo;
	private ClassInfo iEntityQueryHelperClassInfo;
	private ClassInfo iEntityQueryHelperBaseClassInfo;
	private ClassInfo iEntityAccessBeanClassInfo;
	private ClassInfo iEntityAccessHelperClassInfo;
	private ClassInfo iEntityEntityCreationDataClassInfo;
	private String iGeneratePrimaryKeyMethodKey;
	private String iGeneratedPrimaryKeyType;
	private boolean iProtectable;
	private boolean iGroupable;
//	private IType iEntityDataType;
	private IType iFactoryType;
	private List<UserMethodInfo> iUserMethodList = new ArrayList<UserMethodInfo>();
	private Map<String, UserMethodInfo> iUserMethods = new HashMap<String, UserMethodInfo>();
	private Collection<String> iStaticMethods = new HashSet<String>();
	private Collection<String> iPortExemptMethods = new HashSet<String>();
	private Collection<String> iPortExemptFields = new HashSet<String>();
	private Map<String, Collection<String>> iRequiredMethodMap = new HashMap<String, Collection<String>>();
	private Map<String, Collection<String>> iMethodRequiredFieldMap = new HashMap<String, Collection<String>>();
	private Map<String, Collection<String>> iFieldRequiredFieldMap = new HashMap<String, Collection<String>>();
	private Collection<String> iRequiredMethods = new HashSet<String>();
	private Collection<String> iRequiredFields = new HashSet<String>();
	private CompilationUnit iEjbCompilationUnit;
	private CompilationUnit iEjbBaseCompilationUnit;
	private CompilationUnit iEjbEntityCreationDataCompilationUnit;
	private CompilationUnit iEjbKeyClassCompilationUnit;
	private List<String> iEjbHierarchy = new ArrayList<String>();
	private Map<String, StaticFieldInfo> iEjbStaticFields = new HashMap<String, StaticFieldInfo>();
	private Map<String, InstanceVariableInfo> iEjbInstanceVariables = new HashMap<String, InstanceVariableInfo>();
	private List<KeyClassConstructorInfo> iKeyClassConstructors = new ArrayList<KeyClassConstructorInfo>();
	private Map<String, List<MethodDeclaration>> iEjbMethodDeclarations = new HashMap<String, List<MethodDeclaration>>();
	private Map<String, TargetExceptionInfo> iEjbMethodUnhandledTargetExceptions = new HashMap<String, TargetExceptionInfo>();
	private Map<String, FieldInfo> iClobAttributeToFieldMap = new HashMap<String, FieldInfo>();
	private Set<FieldInfo> iEntityCreationDataFields = new HashSet<FieldInfo>();
	private String iJndiName;
	private Collection<String> iEjbAccessBeanMethodKeys;
	
	public EntityInfo(ModuleInfo moduleInfo, String entityId) {
		iModuleInfo = moduleInfo;
		iEntityId = entityId;
	}
	
	public ModuleInfo getModuleInfo() {
		return iModuleInfo;
	}
	
	public String getEntityId() {
		return iEntityId;
	}
	
	public void setEjbName(String ejbName) {
		iEjbName = ejbName;
		if (ejbName != null) {
			iModuleInfo.setEntityName(this, ejbName);
		}
	}
	
	public String getEjbName() {
		return iEjbName;
	}
	
	public void setHome(String home) {
		iHome = home;
	}
	
	public String getHome() {
		return iHome;
	}
	
	public void setRemote(String remote) {
		iRemote = remote;
	}
	
	public String getRemote() {
		return iRemote;
	}
	
	public void setLocalHome(String localHome) {
		iLocalHome = localHome;
	}
	
	public String getLocalHome() {
		return iLocalHome;
	}
	
	public void setLocal(String local) {
		iLocal = local;
	}
	
	public String getLocal() {
		return iLocal;
	}
	
	public void setEjbClass(String ejbClass) {
		iEjbClass = ejbClass;
	}
	
	public String getEjbClass() {
		return iEjbClass;
	}
	
	public void setPrimaryKeyClass(String primaryKeyClass) {
		iPrimaryKeyClass = primaryKeyClass;
	}
	
	public String getPrimaryKeyClass() {
		return iPrimaryKeyClass;
	}
	
	public void setAbstractSchemaName(String abstractSchemaName) {
		iAbstractSchemaName = abstractSchemaName;
	}
	
	public String getAbstractSchemaName() {
		return iAbstractSchemaName;
	}
	
	public void setPrimaryKeyField(String primaryKeyField) {
		iPrimaryKeyField = primaryKeyField;
	}
	
	public String getPrimaryKeyField() {
		return iPrimaryKeyField;
	}
	
	public void setVersion(String version) {
		iVersion = version;
	}
	
	public String getVersion() {
		return iVersion;
	}
	
	public FieldInfo getFieldInfo(String fieldId, boolean create) {
		FieldInfo fieldInfo = iFields.get(fieldId);
		if (fieldInfo == null && create) {
			fieldInfo = new FieldInfo(this, fieldId);
			iFields.put(fieldId, fieldInfo);
		}
		return fieldInfo;
	}
	
	public FieldInfo getFieldInfo(String fieldId) {
		return getFieldInfo(fieldId, false);
	}
	
	public FieldInfo getFieldInfoByName(String fieldName) {
		return iFieldsByName.get(fieldName);
	}
	
	public void setFieldName(FieldInfo fieldInfo, String fieldName) {
		iFieldsByName.put(fieldName, fieldInfo);
	}
	
	public FieldInfo getFieldInfoByGetterName(String getterName) {
		return iFieldsByGetterName.get(getterName);
	}
	
	public void setFieldGetterName(FieldInfo fieldInfo, String getterName) {
		iFieldsByGetterName.put(getterName, fieldInfo);
	}
	
	public FieldInfo getFieldInfoBySetterName(String setterName) {
		return iFieldsBySetterName.get(setterName);
	}
	
	public void setFieldSetterName(FieldInfo fieldInfo, String setterName) {
		iFieldsBySetterName.put(setterName, fieldInfo);
	}
	
	public void setFieldSetterMethodKey(FieldInfo fieldInfo, String setterMethodKey) {
		iFieldsBySetterMethodKey.put(setterMethodKey, fieldInfo);
	}
	
	public FieldInfo getFieldInfoBySetterMethodKey(String setterMethodKey) {
//		String[] methodKeyComponents = setterMethodKey.split("\\+");
//		FieldInfo setterFieldInfo = null;
//		if (methodKeyComponents.length == 2) {
//			FieldInfo fieldInfo = iFieldsBySetterName.get(methodKeyComponents[0]);
//			if (fieldInfo != null && fieldInfo.getTypeName().equals(methodKeyComponents[1])) {
//				setterFieldInfo = fieldInfo;
//			}
//		}
//		return setterFieldInfo;
		return iFieldsBySetterMethodKey.get(setterMethodKey);
	}
	
	public Collection<FieldInfo> getFields() {
		return iFieldsByName.values();
	}
	
	public List<FieldInfo> getKeyFields() {
		if (iKeyFields == null) {
			iKeyFields = new ArrayList<FieldInfo>();
			if ("AlgoPolicy".equals(getEjbName())) {
				iKeyFields.add(getFieldInfoByName("algoPolicyId"));
			}
			else if (getPrimaryKeyField() != null) {
				FieldInfo fieldInfo = getFieldInfoByName(getPrimaryKeyField());
				if (fieldInfo != null) {
					iKeyFields.add(fieldInfo);
				}
			}
			else {
				try {
					IField[] fields = getPrimaryKeyType().getFields();
					if (fields != null) {
						for (IField field : fields) {
							FieldInfo fieldInfo = getFieldInfoByName(field.getElementName());
							if (fieldInfo != null) {
								iKeyFields.add(fieldInfo);
							}
						}
					}
				}
				catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
		return iKeyFields;
	}
	
	public EjbLocalRefInfo getEjbLocalRefInfo(String ejbLocalRefId, boolean create) {
		EjbLocalRefInfo ejbLocalRefInfo = iEjbLocalRefs.get(ejbLocalRefId);
		if (ejbLocalRefInfo == null && create) {
			ejbLocalRefInfo = new EjbLocalRefInfo(ejbLocalRefId);
			iEjbLocalRefs.put(ejbLocalRefId, ejbLocalRefInfo);
		}
		return ejbLocalRefInfo;
	}
	
	public Collection<EjbLocalRefInfo> getEjbLocalRefs() {
		return iEjbLocalRefs.values();
	}
	
	public void addEjbRelationsihpRoleInfo(EjbRelationshipRoleInfo ejbRelationshipRoleInfo) {
		iEjbRelationshipRoles.add(ejbRelationshipRoleInfo);
		iEjbRelationshipRolesByName.put(ejbRelationshipRoleInfo.getFieldName(), ejbRelationshipRoleInfo);
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfoByName(String fieldName) {
		return iEjbRelationshipRolesByName.get(fieldName);
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfoByGetterName(String getterName) {
		return iEjbRelationshipRolesByGetterName.get(getterName);
	}
	
	public void setEjbRelationshipRoleInfoGetterName(EjbRelationshipRoleInfo ejbRelationshipRoleInfo, String getterName) {
		iEjbRelationshipRolesByGetterName.put(getterName, ejbRelationshipRoleInfo);
	}
	
	public EjbRelationshipRoleInfo getEjbRelationshipRoleInfoBySetterName(String setterName) {
		return iEjbRelationshipRolesBySetterName.get(setterName);
	}
	
	public void setEjbRelationshipRoleInfoSetterName(EjbRelationshipRoleInfo ejbRelationshipRoleInfo, String setterName) {
		iEjbRelationshipRolesBySetterName.put(setterName, ejbRelationshipRoleInfo);
	}
	
	public List<EjbRelationshipRoleInfo> getEjbRelationshipRoles() {
		return iEjbRelationshipRoles;
	}
	
	public void setAccessBeanInfo(AccessBeanInfo accessBeanInfo) {
		iAccessBeanInfo = accessBeanInfo;
	}
	
	public AccessBeanInfo getAccessBeanInfo() {
		if (iAccessBeanInfo == null) {
			System.out.println("no access bean info for "+iEjbName);
		}
		return iAccessBeanInfo;
	}

	public void setConcurrencyControl(String concurrencyControl) {
		iConcurrencyControl = concurrencyControl;
	}
	
	public String getConcurrencyControl() {
		return iConcurrencyControl;
	}
	
	public List<FinderInfo> getFinders() {
		return iFinders;
	}
	
	public FinderInfo getFinderInfo(String finderId, boolean create) {
		FinderInfo finderInfo = null;
		for (FinderInfo finder : iFinders) {
			if (finderId.equals(finder.getFinderId())) {
				finderInfo = finder;
				break;
			}
		}
		if (finderInfo == null && create) {
			finderInfo = new FinderInfo(finderId);
			iFinders.add(finderInfo);
		}
		return finderInfo;
	}
	
	public FinderInfo getFinderInfo(String finderMethodName, String[] parameterTypes, boolean create) {
		FinderInfo finderInfo = null;
		for (FinderInfo finder : iFinders) {
			if (finderMethodName.equals(finder.getFinderMethodName())) {
				String[] types = finder.getFinderMethodParameterTypes();
				if (types.length == parameterTypes.length) {
					boolean typesEqual = true;
					for (int i = 0; i < types.length; i++) {
						if (!types[i].equals(parameterTypes[i])) {
							typesEqual = false;
							break;
						}
					}
					if (typesEqual) {
						finderInfo = finder;
						break;
					}
				}
			}
		}
		if (finderInfo == null && create) {
			finderInfo = new FinderInfo(finderMethodName + iFinders.size());
			finderInfo.setFinderMethodName(finderMethodName);
			finderInfo.setFinderMethodParameterTypes(parameterTypes);
			iFinders.add(finderInfo);
		}
		return finderInfo;
	}
	
	public FinderInfo getFinderInfo(String methodKey) {
		FinderInfo finderInfo = null;
		for (FinderInfo finder : iFinders) {
			if (methodKey.equals(finder.getMethodKey())) {
				finderInfo = finder;
				break;
			}
		}
		return finderInfo;
	}
	
	public List<FinderInfo> getQueryFinders() {
		if (iNamedFinders == null) {
			Map<String, List<FinderInfo>> processedFinders = new HashMap<String, List<FinderInfo>>();
			iNamedFinders = new ArrayList<FinderInfo>();
			for (FinderInfo finderInfo : iFinders) {
				if ((finderInfo.getFinderWhereClause() != null && !finderInfo.getFinderWhereClause().equals(finderInfo.getFinderMethodName())) || finderInfo.getFinderSelectStatement() != null || finderInfo.getFinderQuery() != null) {
					iNamedFinders.add(finderInfo);
					String queryName = getEjbName() + "_" + finderInfo.getFinderMethodName();
					List<FinderInfo> duplicateFinders = processedFinders.get(queryName);
					if (duplicateFinders == null) {
						finderInfo.setQueryName(queryName);
						duplicateFinders = new ArrayList<FinderInfo>();
						duplicateFinders.add(finderInfo);
						processedFinders.put(queryName, duplicateFinders);
					}
					else {
						duplicateFinders.add(finderInfo);
						int finderCount = 1;
						for (FinderInfo duplicateFinderInfo : duplicateFinders) {
							duplicateFinderInfo.setQueryName(queryName + "_" + finderCount);
							finderCount++;
						}
					}
				}
			}
		}
		return iNamedFinders;
	}
	
	public List<FinderInfo> getUserFinders() {
		if (iUserFinders == null) {
			iUserFinders = new ArrayList<FinderInfo>();
			for (FinderInfo finderInfo : iFinders) {
				if (finderInfo.getFinderWhereClause() == null && finderInfo.getFinderSelectStatement() == null && finderInfo.getFinderQuery() == null) {
					iUserFinders.add(finderInfo);
				}
			}
		}
		return iUserFinders;
	}
	
	public CreatorInfo getCreatorInfo(String[] parameterTypes, boolean create) {
		StringBuilder sb = new StringBuilder();
		boolean firstParameter = true;
		for (String parameterType : parameterTypes) {
			if (firstParameter) {
				firstParameter = false;
			}
			else {
				sb.append(",");
			}
			sb.append(parameterType);
		}
		String key = sb.toString();
		CreatorInfo creatorInfo = iCreators.get(key);
		if (creatorInfo == null && create) {
			creatorInfo = new CreatorInfo();
			iCreators.put(key, creatorInfo);
		}
		return creatorInfo;
	}
	
	public CreatorInfo getCreatorInfo(String[] parameterTypes) {
		return getCreatorInfo(parameterTypes, false);
	}
	
	public Collection<CreatorInfo> getCreators() {
		return iCreators.values();
	}
	
	public void setAccessBeanMethodInfo(String methodKey, AccessBeanMethodInfo accessBeanMethodInfo) {
		iAccessBeanMethods.put(methodKey, accessBeanMethodInfo);
	}
	
	public AccessBeanMethodInfo getAccessBeanMethodInfo(String methodKey) {
		return iAccessBeanMethods.get(methodKey);
	}
	
	public Collection<AccessBeanMethodInfo> getAccessBeanMethods() {
		return iAccessBeanMethods.values();
	}
	
	public void addCreatorCalledByCreator(CreatorInfo creatorInfo) {
		iCreatorsCalledByCreators.add(creatorInfo);
	}
	
	public Set<CreatorInfo> getCreatorsCalledByCreators() {
		return iCreatorsCalledByCreators;
	}
	
	public void setSuperType(EntityInfo supertype) {
		iSupertype = supertype;
	}
	
	public EntityInfo getSupertype() {
		return iSupertype;
	}
	
	public void addSubtype(EntityInfo subtype) {
		if (iSubtypes == null) {
			iSubtypes = new HashSet<EntityInfo>();
		}
		iSubtypes.add(subtype);
	}
	
	public Set<EntityInfo> getSubtypes() {
		return iSubtypes;
	}
	
	public Set<MethodInfo> getAccessIntentMethods(String intentType) {
		Set<MethodInfo> methods = null;
		if (iAccessIntents != null) {
			methods = iAccessIntents.get(intentType);
		}
		return methods;
	}
	
	public void addAccessIntentMethod(String intentType, MethodInfo methodInfo) {
		if (iAccessIntents == null) {
			iAccessIntents = new HashMap<String, Set<MethodInfo>>();
		}
		Set<MethodInfo> methods = iAccessIntents.get(intentType);
		if (methods == null) {
			methods = new HashSet<MethodInfo>();
			iAccessIntents.put(intentType, methods);
		}
		methods.add(methodInfo);
	}
	
	public void addTable(TableInfo tableInfo) {
		iTables.add(tableInfo);
	}
	
	public List<TableInfo> getTables() {
		return iTables;
	}
	
	public void setPrimaryTableInfo(TableInfo primaryTableInfo) {
		primaryTableInfo.setEntityInfo(this);
		iPrimaryTableInfo = primaryTableInfo;
	}
	
	public TableInfo getPrimaryTableInfo() {
		return iPrimaryTableInfo;
	}
	
	public void setSecondaryTableInfo(TableInfo secondaryTableInfo) {
		secondaryTableInfo.setEntityInfo(this);
		iSecondaryTableInfo = secondaryTableInfo;
	}
	
	public TableInfo getSecondaryTableInfo() {
		return iSecondaryTableInfo;
	}
	
	public TableInfo getTableInfo() {
		TableInfo tableInfo = getSecondaryTableInfo();
		if (tableInfo == null) {
			tableInfo = getPrimaryTableInfo();
			if (tableInfo == null && getSupertype() != null) {
				tableInfo = getSupertype().getPrimaryTableInfo();
			}
		}
		return tableInfo;
	}
	
	public void setDiscriminatorValue(String discriminatorValue) {
		iDiscriminatorValue = discriminatorValue;
	}
	
	public String getDiscriminatorValue() {
		return iDiscriminatorValue;
	}
	
	public void setDiscriminatorColumnInfo(ColumnInfo discriminatorColumnInfo) {
		iDiscriminatorColumnInfo = discriminatorColumnInfo;
	}
	
	public ColumnInfo getDiscriminatorColumnInfo() {
		return iDiscriminatorColumnInfo;
	}
	
	public void setJoinKey(ConstraintInfo constraintInfo) {
		iJoinKey = constraintInfo;
	}
	
	public ConstraintInfo getJoinKey() {
		return iJoinKey;
	}
	
	public String getSelectClause() {
		if (iSelectClause == null) {
			StringBuilder sb = new StringBuilder("SELECT ");
			StringBuilder fromClause = new StringBuilder(" FROM ");
			boolean firstColumn = true;
			int tableCount = 0;
			SortedSet<String> tableNames = new TreeSet<String>();
			Map<String, TableInfo> tableMap = new HashMap<String, TableInfo>();
			if (getSupertype() != null) {
				List<TableInfo> tables = getSupertype().getTables();
				for (TableInfo tableInfo : tables) {
					tableNames.add(tableInfo.getTableName());
					tableMap.put(tableInfo.getTableName(), tableInfo);
				}
			}
			for (TableInfo tableInfo : iTables) {
				if (!tableNames.contains(tableInfo.getTableName())) {
					tableNames.add(tableInfo.getTableName());
					tableMap.put(tableInfo.getTableName(), tableInfo);
				}
			}
			if (tableNames.size() == 0) {
				System.out.println("no tables???");
			}
			Map<String, String> tableAliasMap = new HashMap<String, String>();
			for (String tableName : tableNames) {
				TableInfo tableInfo = tableMap.get(tableName);
				tableCount++;
				String tableAlias = "T" + tableCount;
				tableAliasMap.put(tableName, tableAlias);
				List<ColumnInfo> columns = tableInfo.getColumns();
				for (ColumnInfo columnInfo : columns) {
					if (firstColumn) {
						firstColumn = false;
					}
					else {
						sb.append(", ");
					}
					sb.append(tableAlias);
					sb.append(".");
					sb.append(columnInfo.getColumnName());
				}
				if (tableCount > 1) {
					fromClause.append(", ");
				}
				fromClause.append(tableInfo.getTableName());
				fromClause.append(" ");
				fromClause.append(tableAlias);
				//private static final String genericFindSqlString = " SELECT T1.APRVSTATUS_ID, T1.MBRGRP_ID, T1.APPROVER_ID, T1.ACTIONTIME, T1.COMMENTS, T1.ENTITY_ID, T1.FLOW_ID, T1.FLOWTYPE_ID, T1.FLSTATEDCT_ID, T1.STATUS, T1.SUBMITTER_ID, T1.SUBMITTIME, T1.OPTCOUNTER FROM APRVSTATUS  T1 WHERE ";
				//private static final int[] genericFindInsertPoints = {227};
				//private static final String genericFindSqlString = " SELECT T1.PARTNUMBER, T1.FIELD5, T1.URL, T1.CATENTRY_ID, T1.BASEITEM_ID, T1.FIELD4, T1.OID, T1.ONAUCTION, T1.MFNAME, T1.ONSPECIAL, T1.BUYABLE, T1.FIELD2, T1.MFPARTNUMBER, T1.MEMBER_ID, T1.FIELD1, T1.STATE, T1.FIELD3, T1.ITEMSPC_ID, T1.LASTUPDATE, T1.MARKFORDELETE, T1.STARTDATE, T1.ENDDATE, T1.AVAILABILITYDATE, T1.LASTORDERDATE, T1.ENDOFSERVICEDATE, T1.DISCONTINUEDATE, T1.OPTCOUNTER, T1.CATENTTYPE_ID FROM CATENTRY  T1 WHERE T1.CATENTTYPE_ID = \'ProductBean\' AND ";
				//private static final String genericFindSqlString = " SELECT T1.STATE, T1.MEMBER_ID, T1.OPTCOUNTER, T1.TYPE, T2.ADMINLASTNAME, T2.PREFERREDDELIVERY, T2.FIELD1, T2.ORGENTITYNAME, T2.DESCRIPTION, T2.ADMINFIRSTNAME, T2.FIELD2, T2.ADMINMIDDLENAME, T2.BUSINESSCATEGORY, T2.FIELD3, T2.TAXPAYERID, T2.DN, T2.ORGENTITYTYPE, T2.LEGALID, T2.STATUS FROM MEMBER  T1, ORGENTITY  T2 WHERE T1.TYPE = \'O\' AND T1.MEMBER_ID = T2.ORGENTITY_ID AND ";
			}
			sb.append(fromClause);
			sb.append(" WHERE ");
			if (iDiscriminatorValue != null && iSubtypes == null) {
				ColumnInfo discriminatorColumn = null;
				EntityInfo currentEntity = this;
				while (discriminatorColumn == null && currentEntity != null) {
					discriminatorColumn = currentEntity.getDiscriminatorColumnInfo();
					currentEntity = currentEntity.getSupertype();
				}
				if (discriminatorColumn != null) {
					String tableAlias = tableAliasMap.get(discriminatorColumn.getTableInfo().getTableName());
					sb.append(tableAlias);
					sb.append(".");
					sb.append(discriminatorColumn.getColumnName());
					sb.append(" = ");
					if ("CHAR".equals(discriminatorColumn.getTypeName())) {
						sb.append("'");
						sb.append(iDiscriminatorValue);
						sb.append("'");
					}
					else {
						sb.append(iDiscriminatorValue);
					}
					sb.append(" AND ");
				}
			}
			if (iJoinKey != null) {
				ColumnInfo t1Column = iModuleInfo.getColumnInfo(iJoinKey.getReferencedMembers()[0]);
				ColumnInfo t2Column = iModuleInfo.getColumnInfo(iJoinKey.getMembers()[0]);
				if (t1Column != null && t2Column != null) {
					String table1Alias = tableAliasMap.get(t1Column.getTableInfo().getTableName());
					String table2Alias = tableAliasMap.get(t2Column.getTableInfo().getTableName());
					sb.append(table1Alias);
					sb.append(".");
					sb.append(t1Column.getColumnName());
					sb.append(" = ");
					sb.append(table2Alias);
					sb.append(".");
					sb.append(t2Column.getColumnName());
					sb.append(" AND ");
				}
			}
			iSelectClause = sb.toString();
		}
		return iSelectClause;
	}
	
	public IType getEjbType() {
		if (iEjbType == null) {
			try {
				iEjbType = iModuleInfo.getJavaProject().findType(iEjbClass);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return iEjbType;
	}
	
	public IType getHomeType() {
		if (iHomeType == null && iHome != null) {
			try {
				iHomeType = iModuleInfo.getJavaProject().findType(iHome);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iHomeType;
	}
	
	public IType getLocalHomeType() {
		if (iLocalHomeType == null && iLocalHome != null) {
			try {
				iLocalHomeType = iModuleInfo.getJavaProject().findType(iLocalHome);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iLocalHomeType;
	}
	
	public IType getRemoteType() {
		if (iRemoteType == null && iRemote != null) {
			try {
				iRemoteType = iModuleInfo.getJavaProject().findType(iRemote);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iRemoteType;
	}

	public IType getLocalType() {
		if (iLocalType == null && iLocal != null) {
			try {
				iLocalType = iModuleInfo.getJavaProject().findType(iLocal);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iLocalType;
	}

	private IType getEjbSuperclassType() {
		if (iEjbSuperclassType == null) {
			try {
				String baseName = getEjbType().getSuperclassName();
				if (baseName != null) {
					iEjbSuperclassType = JavaUtil.resolveType(getEjbType(), baseName);
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEjbSuperclassType;
	}
	
	public IType getEjbBaseType() {
		if (iEjbBaseType == null && getEjbSuperclassType() != null) {
			IType superclassType = getEjbSuperclassType();
			if (!superclassType.getFullyQualifiedName('.').equals(EC_ENTITY_BEAN) &&
					!superclassType.getFullyQualifiedName('.').equals(EC_ABSTRACT_ENTITY_BEAN) &&
					!isEntitySupertype(superclassType) &&
					iModuleInfo.getJavaProject().equals(getEjbSuperclassType().getJavaProject())) {
				iEjbBaseType = superclassType;
			}
		}
		return iEjbBaseType;
	}
	
	public IType getPrimaryKeyType() {
		if (iPrimaryKeyType == null) {
			try {
				iPrimaryKeyType = iModuleInfo.getJavaProject().findType(iPrimaryKeyClass);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iPrimaryKeyType;
	}
	
	public IType getEjbFinderObjectType() {
		if (iEjbFinderObjectType == null && getUserFinders().size() > 0) {
			try {
				iEjbFinderObjectType =  iModuleInfo.getJavaProject().findType(iEjbClass + "FinderObject");
				if (iEjbFinderObjectType == null) {
					System.out.println("problem locating finder object for "+iEjbClass);
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEjbFinderObjectType;
	}
	
	public IType getEjbFinderObjectSuperclassType() {
		if (iEjbFinderObjectSuperclassType == null && getEjbFinderObjectType() != null) {
			try {
				String baseName = getEjbFinderObjectType().getSuperclassName();
				if (baseName != null) {
					iEjbFinderObjectSuperclassType = JavaUtil.resolveType(getEjbFinderObjectType(), baseName);
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEjbFinderObjectSuperclassType;
	}
	
	public IType getEjbFinderObjectBaseType() {
		if (iEjbFinderObjectBaseType == null && getEjbFinderObjectSuperclassType() != null) {
			IType ejbFinderObjectSuperclassType = getEjbFinderObjectSuperclassType();
			if (!FINDER_OBJECT_BASE_CLASSES.contains(ejbFinderObjectSuperclassType.getFullyQualifiedName('.')) &&
					!isEntitySupertypeFinderObject(ejbFinderObjectSuperclassType) &&
					iModuleInfo.getJavaProject().equals(ejbFinderObjectSuperclassType.getJavaProject())) {
				iEjbFinderObjectBaseType = ejbFinderObjectSuperclassType;
			}
		}
		return iEjbFinderObjectBaseType;
	}
	
	public IType getEjbAccessHelperType() {
		if (iEjbAccessHelperType == null && (iProtectable || iGroupable)) {
			try {
				int index = iEjbClass.lastIndexOf(".");
				String accessHelperName = iEjbClass.substring(0, index - 4) + "src." + iEjbClass.substring(index + 1) + "AccessHelper";
				iEjbAccessHelperType = iModuleInfo.getJavaProject().findType(accessHelperName);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEjbAccessHelperType;
	}
	
	public IType getEjbAccessBeanType() {
		if (iEjbAccessBeanType == null && iAccessBeanInfo != null) {
			try {
				iEjbAccessBeanType = iModuleInfo.getJavaProject().findType(iAccessBeanInfo.getQualifiedAccessBeanName());
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEjbAccessBeanType;
	}
	
	public IType getEjbEntityCreationDataType() {
		if (iEjbEntityCreationDataType == null && iRemote != null) {
			try {
				IType type = iModuleInfo.getJavaProject().findType(iRemote + "EntityCreationData");
				if (type != null) {
					String[] interfaceNames = type.getSuperInterfaceNames();
					if (interfaceNames != null) {
						for (String interfaceName : interfaceNames) {
							IType interfaceType = JavaUtil.resolveType(type, interfaceName);
							if (interfaceType != null && interfaceType.getFullyQualifiedName('.').equals(ENTITY_CREATION_DATA)) {
								iEjbEntityCreationDataType = type;
								break;
							}
						}
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEjbEntityCreationDataType;
	}
	
	public ClassInfo getEntityClassInfo() {
		if (iEntityClassInfo == null) {
			iEntityClassInfo = new ClassInfo(iEjbName + "JPAEntity", getEjbType().getPackageFragment());
			IType ejbSuperclassType = getEjbSuperclassType();
			if (ejbSuperclassType != null) {
				try {
					if (ejbSuperclassType.getFullyQualifiedName('.').equals(EC_ENTITY_BEAN) || ejbSuperclassType.getFullyQualifiedName('.').equals(EC_ABSTRACT_ENTITY_BEAN)) {
						iEntityClassInfo.setSuperclassName(JPA_ENTITY_BASE);
						iEntityClassInfo.setSuperclassPackage(JPA_ENTITY_BASE_PACKAGE);
					}
					else if (isEntitySupertype(ejbSuperclassType)) {
						iEntityClassInfo.setSuperclassName(iSupertype.getEntityClassInfo().getClassName());
						iEntityClassInfo.setSuperclassPackage(iSupertype.getEntityClassInfo().getPackageFragment().getElementName());
					}
					else if (iModuleInfo.getJavaProject().equals(ejbSuperclassType.getJavaProject())) {
						String baseName = getEjbType().getSuperclassName();
						int index = baseName.lastIndexOf(".");
						if (index != -1) {
							baseName = baseName.substring(index + 1);
						}
						index = baseName.lastIndexOf("BeanBase");
						if (index != -1) {
							baseName = baseName.substring(0, index);
						}
						baseName += "JPAEntityBase";
						iEntityClassInfo.setSuperclassName(baseName);
						iEntityClassInfo.setSuperclassPackage(ejbSuperclassType.getPackageFragment().getElementName());
					}
					else {
						System.out.println("unexpected entity bean superclass "+ejbSuperclassType);
					}
				}
				catch (JavaModelException e) {
					e.printStackTrace();
				}
			}
		}
		return iEntityClassInfo;
	}
	
	public ClassInfo getEntityBaseClassInfo() {
		if (iEntityBaseClassInfo == null && getEjbBaseType() != null) {
			ClassInfo entityClassInfo = getEntityClassInfo();
			String baseName = entityClassInfo.getSuperclassName();
			iEntityBaseClassInfo = new ClassInfo(baseName, getEjbBaseType().getPackageFragment());
			try {
				baseName = getEjbBaseType().getSuperclassName();
				if (baseName != null) {
					IType baseBaseType = JavaUtil.resolveType(iEjbBaseType, baseName);
					if (baseBaseType != null) {
						if (baseBaseType.equals(iModuleInfo.getJavaProject().findType(EC_ENTITY_BEAN)) || baseBaseType.equals(iModuleInfo.getJavaProject().findType(EC_ABSTRACT_ENTITY_BEAN))) {
							iEntityBaseClassInfo.setSuperclassName(JPA_ENTITY_BASE);
							iEntityBaseClassInfo.setSuperclassPackage(JPA_ENTITY_BASE_PACKAGE);
						}
						else if (isEntitySupertype(baseBaseType)) {
							iEntityBaseClassInfo.setSuperclassName(iSupertype.getEntityClassInfo().getClassName());
							iEntityBaseClassInfo.setSuperclassPackage(iSupertype.getEntityClassInfo().getPackageFragment().getElementName());
						}
						else if (baseBaseType.equals(iModuleInfo.getJavaProject().findType(GIFT_REGISTRY_PROTECTABLE_BEAN_BASE))) {
							iEntityBaseClassInfo.setSuperclassName(JPA_GIFT_REGISTRY_PROTECTABLE_ENTITY_BASE);
							iEntityBaseClassInfo.setSuperclassPackage(JPA_GIFT_REGISTRY_PROTECTABLE_ENTITY_BASE_PACKAGE);
						}
						else {
							System.out.println("unexpected entity bean base superclass "+baseBaseType);
						}
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEntityBaseClassInfo;
	}
	
	public ClassInfo getEntityKeyClassInfo() {
		if (iEntityKeyClassInfo == null && getKeyFields().size() > 1) {
			String idClass = getPrimaryKeyClass();
			int index = idClass.lastIndexOf(".");
			if (index > -1) {
				idClass = idClass.substring(index + 1);
			}
			index = idClass.lastIndexOf("Key");
			if (index != -1) {
				idClass = idClass.substring(0, index);
			}
			iEntityKeyClassInfo = new ClassInfo(idClass + "JPAKey", getPrimaryKeyType().getPackageFragment());
		}
		return iEntityKeyClassInfo;
	}
	
	public ClassInfo getEntityQueryHelperClassInfo() {
		if (iEntityQueryHelperClassInfo == null && getEjbFinderObjectType() != null) {
			String baseName = iRemote;
			int index = baseName.lastIndexOf(".");
			if (index != -1) {
				baseName = baseName.substring(index + 1);
			}
			iEntityQueryHelperClassInfo = new ClassInfo(baseName + "JPAQueryHelper", getEjbFinderObjectType().getPackageFragment());
			IType ejbFinderObjectSuperclassType = getEjbFinderObjectSuperclassType();
			if (ejbFinderObjectSuperclassType != null) {
				if (FINDER_OBJECT_BASE_CLASSES.contains(ejbFinderObjectSuperclassType.getFullyQualifiedName('.'))) {
					iEntityQueryHelperClassInfo.setSuperclassName(JPA_QUERY_HELPER);
					iEntityQueryHelperClassInfo.setSuperclassPackage(JPA_QUERY_HELPER_PACKAGE);
				}
				else if (isEntitySupertypeFinderObject(ejbFinderObjectSuperclassType)) {
					iEntityQueryHelperClassInfo.setSuperclassName(iSupertype.getEntityQueryHelperClassInfo().getClassName());
					iEntityQueryHelperClassInfo.setSuperclassPackage(iSupertype.getEntityQueryHelperClassInfo().getPackageFragment().getElementName());
				}
				else if (iModuleInfo.getJavaProject().equals(ejbFinderObjectSuperclassType.getJavaProject())) {
					try {
						baseName = getEjbFinderObjectType().getSuperclassName();
						index = baseName.lastIndexOf(".");
						if (index != -1) {
							baseName = baseName.substring(index + 1);
						}
						index = baseName.lastIndexOf("BeanFinderObjectBase");
						if (index != -1) {
							baseName = baseName.substring(0, index);
						}
						baseName += "JPAQueryHelperBase";
						iEntityQueryHelperClassInfo.setSuperclassName(baseName);
						iEntityQueryHelperClassInfo.setSuperclassPackage(ejbFinderObjectSuperclassType.getPackageFragment().getElementName());
					}
					catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("unexpected finder object superclass "+ejbFinderObjectSuperclassType);
				}
			}
		}
		return iEntityQueryHelperClassInfo;
	}
	
	public ClassInfo getEntityQueryHelperBaseClassInfo() {
		if (iEntityQueryHelperBaseClassInfo == null && getEntityQueryHelperClassInfo() != null && getEjbFinderObjectBaseType() != null) {
			String baseName = getEntityQueryHelperClassInfo().getSuperclassName();
			iEntityQueryHelperBaseClassInfo = new ClassInfo(baseName, getEjbFinderObjectBaseType().getPackageFragment());
			try {
				baseName = getEjbFinderObjectBaseType().getSuperclassName();
				if (baseName != null) {
					IType baseBaseType = JavaUtil.resolveType(getEjbFinderObjectBaseType(), baseName);
					if (baseBaseType != null) {
						if (FINDER_OBJECT_BASE_CLASSES.contains(baseBaseType.getFullyQualifiedName('.'))) {
							iEntityQueryHelperBaseClassInfo.setSuperclassName(JPA_QUERY_HELPER);
							iEntityQueryHelperBaseClassInfo.setSuperclassPackage(JPA_QUERY_HELPER_PACKAGE);
						}
						else if (isEntitySupertypeFinderObject(baseBaseType)) {
							iEntityQueryHelperBaseClassInfo.setSuperclassName(iSupertype.getEntityQueryHelperClassInfo().getClassName());
							iEntityQueryHelperBaseClassInfo.setSuperclassPackage(iSupertype.getEntityQueryHelperClassInfo().getPackageFragment().getElementName());
						}
						else {
							System.out.println("unexpected finder object base superclass "+baseBaseType);
						}
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iEntityQueryHelperBaseClassInfo;
	}
	
	public ClassInfo getEntityAccessBeanClassInfo() {
		if (iEntityAccessBeanClassInfo == null) {
			IPackageFragment packageFragment = getEjbAccessBeanType() == null ? (getRemoteType() != null ? getRemoteType().getPackageFragment() : getLocalType().getPackageFragment()) : getEjbAccessBeanType().getPackageFragment();
			String entityAccessBeanName = iEjbName + "JPAAccessBean";
			if (iAccessBeanInfo != null && iAccessBeanInfo.getDataClassType()) {
				entityAccessBeanName = iEjbName + "JPAData";
			}
			iEntityAccessBeanClassInfo = new ClassInfo(entityAccessBeanName, packageFragment);
		}
		return iEntityAccessBeanClassInfo;
	}
	
	public ClassInfo getEntityAccessHelperClassInfo() {
		if (iEntityAccessHelperClassInfo == null && getEjbAccessHelperType() != null) {
			iEntityAccessHelperClassInfo = new ClassInfo(iEjbName + "JPAAccessHelper", getEjbAccessHelperType().getPackageFragment());
		}
		return iEntityAccessHelperClassInfo;
	}
	
	public ClassInfo getEntityEntityCreationDataClassInfo() {
		if (iEntityEntityCreationDataClassInfo == null && getEjbEntityCreationDataType() != null) {
			iEntityEntityCreationDataClassInfo = new ClassInfo("JPA" + getEjbEntityCreationDataType().getTypeQualifiedName(), getEjbEntityCreationDataType().getPackageFragment());
		}
		return iEntityEntityCreationDataClassInfo;
	}

	public void setGeneratePrimaryKeyMethodKey(String generatePrimaryKeyMethodKey) {
		iGeneratePrimaryKeyMethodKey = generatePrimaryKeyMethodKey;
		TargetExceptionInfo targetExceptionInfo = new TargetExceptionInfo();
		targetExceptionInfo.addTargetException(TargetExceptionUtil.PERSISTENCE_EXCEPTION);
		setEjbMethodUnhandledTargetExceptions(generatePrimaryKeyMethodKey, targetExceptionInfo);
	}
	
	public String getGeneratePrimaryKeyMethodKey() {
		return iGeneratePrimaryKeyMethodKey;
	}
	
	public void setGeneratedPrimaryKeyType(String generatedPrimaryKeyType) {
		iGeneratedPrimaryKeyType = generatedPrimaryKeyType;
	}
	
	public String getGeneratedPrimaryKeyType() {
		return iGeneratedPrimaryKeyType;
	}

	public void addRelatedEntityInfo(RelatedEntityInfo relatedEntityInfo) {
		iRelatedEntities.add(relatedEntityInfo);
	}
	
	public List<RelatedEntityInfo> getRelatedEntities() {
		return iRelatedEntities;
	}
	
	public List<RelatedEntityInfo> getKeyRelatedEntities() {
		if (iKeyRelatedEntities == null) {
			iKeyRelatedEntities = new ArrayList<RelatedEntityInfo>();
			for (RelatedEntityInfo relatedEntityInfo : iRelatedEntities) {
				if (relatedEntityInfo.getIsKeyField()) {
					iKeyRelatedEntities.add(relatedEntityInfo);
				}
			}
		}
		return iKeyRelatedEntities;
	}
	
	public void setProtectable(boolean protectable) {
		iProtectable = protectable;
	}
	
	public boolean getProtectable() {
		return iProtectable;
	}

	public void setGroupable(boolean groupable) {
		iGroupable = groupable;
	}
	
	public boolean getGroupable() {
		return iGroupable;
	}
	
//	public IType getEntityDataType() {
//		if (iEntityDataType == null && iLocal != null) {
//			try {
//				iEntityDataType = iModuleInfo.getJavaProject().findType(iLocal + "Data");
//			}
//			catch (JavaModelException e) {
//				e.printStackTrace();
//			}
//		}
//		return iEntityDataType;
//	}
	
	public IType getFactoryType() {
		if (iFactoryType == null) {
			try {
				if (iRemote != null) {
					String factoryName = iRemote + "Factory";
					iFactoryType = iModuleInfo.getJavaProject().findType(factoryName);
				}
				else if (iLocal != null) {
					if (iLocal.endsWith("Local")) {
						String factoryName = iLocal.substring(0, iLocal.length() - 5) + "Factory";
						iFactoryType = iModuleInfo.getJavaProject().findType(factoryName);
					}
					else {
						String factoryName = iLocal + "Factory";
						iFactoryType = iModuleInfo.getJavaProject().findType(factoryName);
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return iFactoryType;
	}
	
	public List<UserMethodInfo> getUserMethods() {
		return iUserMethodList;
	}
	
	public void addUserMethod(UserMethodInfo userMethodInfo) {
		if (!iUserMethods.containsKey(userMethodInfo.getKey())) {
			iUserMethodList.add(userMethodInfo);
			iUserMethods.put(userMethodInfo.getKey(), userMethodInfo);
		}
	}
	
	public UserMethodInfo getUserMethodInfo(String methodKey) {
		return iUserMethods.get(methodKey);
	}
	
	public void addStaticMethod(String staticMethodKey) {
		iStaticMethods.add(staticMethodKey);
	}
	
	public Collection<String> getStaticMethods() {
		return iStaticMethods;
	}
	
	public void addRequiredMethod(String requiredMethodKey) {
		if (!iPortExemptMethods.contains(requiredMethodKey)) {
			iRequiredMethods.add(requiredMethodKey);
		}
	}
	
	public boolean isRequiredMethod(String methodKey) {
		return iRequiredMethods.contains(methodKey);
	}
	
	public void addRequiredField(String requiredFieldName) {
		if (!iPortExemptFields.contains(requiredFieldName)) {
			iRequiredFields.add(requiredFieldName);
		}
	}
	
	public boolean isRequiredField(String fieldName) {
		return iRequiredFields.contains(fieldName);
	}
	
	public Collection<String> getRequiredMethods(String methodKey) {
		return iRequiredMethodMap.get(methodKey);
	}
	
	public void addRequiredMethod(String methodKey, String requiredMethodKey) {
		Collection<String> requiredMethods = iRequiredMethodMap.get(methodKey);
		if (requiredMethods == null) {
			requiredMethods = new HashSet<String>();
			iRequiredMethodMap.put(methodKey, requiredMethods);
		}
		requiredMethods.add(requiredMethodKey);
	}
	
	public Collection<String> getMethodRequiredFields(String methodKey) {
		return iMethodRequiredFieldMap.get(methodKey);
	}
	
	public void addMethodRequiredField(String methodKey, String requiredFieldName) {
		Collection<String> requiredFields = iMethodRequiredFieldMap.get(methodKey);
		if (requiredFields == null) {
			requiredFields = new HashSet<String>();
			iMethodRequiredFieldMap.put(methodKey, requiredFields);
		}
		requiredFields.add(requiredFieldName);
	}
	
	public Collection<String> getFieldRequiredFields(String fieldName) {
		return iFieldRequiredFieldMap.get(fieldName);
	}
	
	public void addFieldRequiredField(String fieldName, String requiredFieldName) {
		Collection<String> requiredFields = iFieldRequiredFieldMap.get(fieldName);
		if (requiredFields == null) {
			requiredFields = new HashSet<String>();
			iFieldRequiredFieldMap.put(fieldName, requiredFields);
		}
		requiredFields.add(requiredFieldName);
	}
	
	public void addPortExemptMethod(String methodKey) {
		iPortExemptMethods.add(methodKey);
	}
	
	public boolean isPortExemptMethod(String methodKey) {
		return iPortExemptMethods.contains(methodKey);
	}
	
	public void addPortExemptField(String fieldName) {
		iPortExemptFields.add(fieldName);
	}
	
	public boolean isPortExemptField(String fieldName) {
		return iPortExemptFields.contains(fieldName);
	}
	
	public void setEjbCompilationUnit(CompilationUnit ejbCompilationUnit) {
		iEjbCompilationUnit = ejbCompilationUnit;
	}
	
	public CompilationUnit getEjbCompilationUnit() {
		return iEjbCompilationUnit;
	}
	
	public void setEjbBaseCompilationUnit(CompilationUnit ejbBaseCompilationUnit) {
		iEjbBaseCompilationUnit = ejbBaseCompilationUnit;
	}
	
	public CompilationUnit getEjbBaseCompilationUnit() {
		return iEjbBaseCompilationUnit;
	}

	public void setEjbEntityCreationDataCompilationUnit(CompilationUnit ejbEntityCreationDataCompilationUnit) {
		iEjbEntityCreationDataCompilationUnit = ejbEntityCreationDataCompilationUnit;
	}
	
	public CompilationUnit getEjbEntityCreationDataCompilationUnit() {
		return iEjbEntityCreationDataCompilationUnit;
	}
	
	public void setEjbKeyClassCompilationUnit(CompilationUnit ejbKeyClassCompilationUnit) {
		iEjbKeyClassCompilationUnit = ejbKeyClassCompilationUnit;
	}
	
	public CompilationUnit getEjbKeyClassCompilationUnit() {
		return iEjbKeyClassCompilationUnit;
	}
	
	public void addClassToEjbHierarchy(String className) {
		iEjbHierarchy.add(className);
	}
	
	public boolean isClassInEjbHierarchy(String className) {
		return iEjbHierarchy.contains(className);
	}
	
	public void addEjbStaticFieldInfo(StaticFieldInfo staticFieldInfo) {
		iEjbStaticFields.put(staticFieldInfo.getVariableName(), staticFieldInfo);
	}
	
	public StaticFieldInfo getEjbStaticFieldInfo(String fieldName) {
		return iEjbStaticFields.get(fieldName);
	}
	
	public void addEjbInstanceVariableInfo(InstanceVariableInfo instanceVariableInfo) {
		iEjbInstanceVariables.put(instanceVariableInfo.getVariableName(), instanceVariableInfo);
	}
	
	public InstanceVariableInfo getEjbInstanceVariableInfo(String variableName) {
		return iEjbInstanceVariables.get(variableName);
	}
	
	public void addKeyClassConstructor(KeyClassConstructorInfo keyClassConstructorInfo) {
		iKeyClassConstructors.add(keyClassConstructorInfo);
	}
	
	public List<KeyClassConstructorInfo> getKeyClassConstructors() {
		return iKeyClassConstructors;
	}
	
	public void addEjbMethodDeclaration(MethodDeclaration ejbMethodDeclaration) {
		String methodKey = JavaUtil.getMethodKey(ejbMethodDeclaration);
		List<MethodDeclaration> methodDeclarations = iEjbMethodDeclarations.get(methodKey);
		if (methodDeclarations == null) {
			methodDeclarations = new ArrayList<MethodDeclaration>();
			iEjbMethodDeclarations.put(methodKey, methodDeclarations);
		}
		methodDeclarations.add(ejbMethodDeclaration);
	}
	
	public List<MethodDeclaration> getEjbMethodDeclarations(String methodKey) {
		return iEjbMethodDeclarations.get(methodKey);
	}
	
	public void setEjbMethodUnhandledTargetExceptions(String methodKey, TargetExceptionInfo unhandledExceptions) {
		iEjbMethodUnhandledTargetExceptions.put(methodKey, unhandledExceptions);
	}
	
	public TargetExceptionInfo getEjbMethodUnhandledTargetExceptions(String methodKey) {
		return iEjbMethodUnhandledTargetExceptions.get(methodKey);
	}
	
	public void setClobAttributeFieldInfo(String clobAttributeName, FieldInfo fieldInfo) {
		iClobAttributeToFieldMap.put(clobAttributeName, fieldInfo);
	}
	
	public FieldInfo getClobAttributeFieldInfo(String clobAttributeName) {
		return iClobAttributeToFieldMap.get(clobAttributeName);
	}

	public void addEntityCreationDataField(FieldInfo fieldInfo) {
		iEntityCreationDataFields.add(fieldInfo);
	}
	
	public Set<FieldInfo> getEntityCreationDataFields() {
		return iEntityCreationDataFields;
	}
	
	public void setJndiName(String jndiName) {
		iJndiName = jndiName;
	}
	
	public String getJndiName() {
		return iJndiName;
	}
	
	public boolean isEjbAccessBeanMethod(String methodKey) {
		boolean result = false;
		synchronized (this) {
			if (iEjbAccessBeanMethodKeys == null) {
				iEjbAccessBeanMethodKeys = new HashSet<String>();
				IType type = getEjbAccessBeanType();
				if (type != null) {
					try {
						IMethod[] methods = type.getMethods();
						for (IMethod method : methods) {
							String key = JavaUtil.getMethodKey(type, method);
							if (key != null) {
								iEjbAccessBeanMethodKeys.add(key);
							}
						}
					}
					catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
			iEjbAccessBeanMethodKeys.contains(methodKey);
		}
		return result;
	}
	
	private boolean isEntitySupertype(IType superClass) {
		boolean isSupertype = false;
		if (getSupertype() != null && getSupertype().getEjbType().equals(superClass)) {
			isSupertype = true;
		}
		return isSupertype;
	}
	
	private boolean isEntitySupertypeFinderObject(IType superClass) {
		boolean isSupertype = false;
		if (getSupertype() != null && (superClass.equals(getSupertype().getEjbFinderObjectType()) || superClass.equals(getSupertype().getEjbFinderObjectBaseType()))) {
			isSupertype = true;
		}
		return isSupertype;
	}
	
	public void releaseParseResources() {
		iRequiredMethodMap = null;
		iMethodRequiredFieldMap = null;
		iFieldRequiredFieldMap = null;
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
