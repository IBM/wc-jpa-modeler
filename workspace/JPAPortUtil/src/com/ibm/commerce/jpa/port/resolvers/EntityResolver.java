package com.ibm.commerce.jpa.port.resolvers;

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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.CopyHelperProperty;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanIfStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanMethodInvocationStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanParameterInitializedFieldStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanReturnStatement;
import com.ibm.commerce.jpa.port.info.AccessBeanMethodInfo.AccessBeanStatement;
import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.CreatorInfo;
import com.ibm.commerce.jpa.port.info.EjbRelationshipRoleInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.ForeignKeyInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;

public class EntityResolver {
	private static final Map<String, String> RELATED_ENTITY_FIELD_NAME_MAP;
	static {
		RELATED_ENTITY_FIELD_NAME_MAP = new HashMap<String, String>();
		RELATED_ENTITY_FIELD_NAME_MAP.put("packageReferenceNumber", "packageEntity");
		RELATED_ENTITY_FIELD_NAME_MAP.put("catalogGroupIdParent", "parentCatalogGroup");
		RELATED_ENTITY_FIELD_NAME_MAP.put("catalogGroupIdChild", "childCatalogGroup");
		RELATED_ENTITY_FIELD_NAME_MAP.put("catalogEntryIdChild", "childCatalogEntry");
		RELATED_ENTITY_FIELD_NAME_MAP.put("catalogEntryIdParent", "parentCatalogEntry");
		RELATED_ENTITY_FIELD_NAME_MAP.put("catalogIdLink", "linkedCatalog");
		RELATED_ENTITY_FIELD_NAME_MAP.put("attachmentIdValue", "attachment");
	}
	private static final String GET = "get";
	private static final String SET = "set";
	
	private EntityInfo iEntityInfo;
	private AccessBeanInfo iAccessBeanInfo;
	private Collection<String> iProcessedMethodKeys = new HashSet<String>();
	private Collection<String> iProcessedFieldNames = new HashSet<String>();
	
	public EntityResolver(EntityInfo entityInfo) {
		iEntityInfo = entityInfo;
		iAccessBeanInfo = iEntityInfo.getAccessBeanInfo();
	}
	
	public void resolve() {
		resolveParentEntities();
		resolveFields();
		resolveUserMethods();
		resolveCopyHelpers();
		resolveKeyFields();
		resolveAccessBeanMethods();
		iEntityInfo.releaseParseResources();
	}
	
	private void resolveParentEntities() {
		List<TableInfo> tables = new ArrayList<TableInfo>();
		if (iEntityInfo.getSecondaryTableInfo() != null) {
			tables.add(iEntityInfo.getSecondaryTableInfo());
		}
		if (iEntityInfo.getPrimaryTableInfo() != null) {
			tables.add(iEntityInfo.getPrimaryTableInfo());
		}
		if (iEntityInfo.getSupertype() != null && iEntityInfo.getSupertype().getPrimaryTableInfo() != null) {
			TableInfo tableInfo = iEntityInfo.getSupertype().getPrimaryTableInfo();
			if (!tables.contains(tableInfo)) {
				tables.add(tableInfo);
			}
		}
//		TableInfo tableInfo = iEntityInfo.getSecondaryTableInfo();
//		if (tableInfo == null) {
//			tableInfo = iEntityInfo.getPrimaryTableInfo();
//		}
//		if (tableInfo == null && iEntityInfo.getSupertype() != null) {
//			tableInfo = iEntityInfo.getSupertype().getPrimaryTableInfo();
//		}
		for (TableInfo tableInfo : tables) {
			Collection<FieldInfo> fields = iEntityInfo.getFields();
			Set<ForeignKeyInfo> foreignKeys = tableInfo.getForeignKeys();
			for (ForeignKeyInfo foreignKey : foreignKeys) {
				EntityInfo parentEntityInfo = foreignKey.getParentTableInfo().getEntityInfo();
				if (parentEntityInfo != null) {
					RelatedEntityInfo relatedEntityInfo = new RelatedEntityInfo(iEntityInfo, parentEntityInfo);
					Collection<FieldInfo> relatedEntityFields = parentEntityInfo.getFields();
					List<ColumnInfo> memberColumns = foreignKey.getMemberColumns();
					List<ColumnInfo> referencedColumns = foreignKey.getReferencedColumns();
					String relatedEntityFieldName = null;
					for (int i = 0; i < memberColumns.size(); i++) {
						ColumnInfo memberColumn = memberColumns.get(i);
						FieldInfo memberFieldInfo = null;
						for (FieldInfo fieldInfo : fields) {
							if (fieldInfo.getColumnInfo() == memberColumn) {
								memberFieldInfo = fieldInfo;
								if (relatedEntityFieldName == null || !relatedEntityFieldName.toLowerCase().contains(parentEntityInfo.getEjbName().toLowerCase())) {
									relatedEntityFieldName = memberFieldInfo.getFieldName();
								}
								break;
							}
						}
						ColumnInfo referencedColumn = referencedColumns.get(i);
						FieldInfo referencedFieldInfo = null;
						for (FieldInfo fieldInfo : relatedEntityFields) {
							if (fieldInfo.getColumnInfo() == referencedColumn) {
								referencedFieldInfo = fieldInfo;
								break;
							}
						}
						if (memberFieldInfo != null && referencedFieldInfo != null) {
							relatedEntityInfo.addMemberField(memberFieldInfo, referencedFieldInfo);
						}
					}
					for (EjbRelationshipRoleInfo ejbRelationshipRoleInfo : iEntityInfo.getEjbRelationshipRoles()) {
						if (ejbRelationshipRoleInfo.getEjbRelationInfo().getConstraintInfo().getForeignKeyInfo() == foreignKey) {
							relatedEntityInfo.setEjbRelationshipRoleInfo(ejbRelationshipRoleInfo);
							relatedEntityFieldName = ejbRelationshipRoleInfo.getFieldName();
							relatedEntityInfo.setFieldName(relatedEntityFieldName);
							relatedEntityInfo.setGetterName(ejbRelationshipRoleInfo.getGetterName());
							relatedEntityInfo.setSetterName(ejbRelationshipRoleInfo.getSetterName());
							for (FieldInfo fieldInfo : fields) {
								if (fieldInfo.getFieldName().startsWith(relatedEntityFieldName + "_")) {
									String referencedFieldName = fieldInfo.getFieldName().substring(relatedEntityFieldName.length() + 1);
									for (FieldInfo relatedFieldInfo : relatedEntityFields) {
										if (relatedFieldInfo.getFieldName().equals(referencedFieldName)) {
											relatedEntityInfo.addMemberField(fieldInfo, relatedFieldInfo);
											if (fieldInfo.getIsKeyField()) {
												ejbRelationshipRoleInfo.setIsKeyField(true);
											}
											break;
										}
									}
								}
							}
						}
					}
					if (relatedEntityInfo.getMemberFields().size() > 0) {
						if (RELATED_ENTITY_FIELD_NAME_MAP.get(relatedEntityFieldName) != null) {
							relatedEntityFieldName = RELATED_ENTITY_FIELD_NAME_MAP.get(relatedEntityFieldName);
						}
						else {
							relatedEntityFieldName = Introspector.decapitalize(relatedEntityFieldName);
							String oldSuffix = null;
							if (relatedEntityFieldName.endsWith("_id")) {
								oldSuffix = "_id";
							}
							else if (relatedEntityFieldName.endsWith("ReferenceNumber")) {
								oldSuffix = "ReferenceNumber";
							}
							else if (relatedEntityFieldName.endsWith("Number")) {
								oldSuffix = "Number";
							}
							else if (relatedEntityFieldName.endsWith("ID") || relatedEntityFieldName.endsWith("Id")) {
								oldSuffix = "ID";
							}
							if (oldSuffix != null) {
								relatedEntityFieldName = relatedEntityFieldName.substring(0, relatedEntityFieldName.length() - oldSuffix.length());
							}
							if (relatedEntityFieldName.equals("class")) {
								relatedEntityFieldName = Introspector.decapitalize(relatedEntityInfo.getParentEntityInfo().getEjbName());
							}
							if (relatedEntityFieldName.startsWith("str") && Character.isUpperCase(relatedEntityFieldName.charAt(3))) {
								relatedEntityFieldName = Introspector.decapitalize(relatedEntityFieldName.substring(3));
							}
						}
						relatedEntityInfo.setFieldName(relatedEntityFieldName);
						iEntityInfo.addRelatedEntityInfo(relatedEntityInfo);
						UserMethodInfo userMethodInfo = iEntityInfo.getUserMethodInfo(relatedEntityInfo.getGetterName());
						if (userMethodInfo != null) {
							userMethodInfo.setRelatedEntityInfo(relatedEntityInfo);
						}
					}
				}
			}
		}
	}

	private void resolveFields() {
		Collection<FieldInfo> fields = iEntityInfo.getFields();
		for (FieldInfo fieldInfo : fields) {
			String targetGetterName = fieldInfo.getTargetGetterName();
			String targetSetterName = fieldInfo.getTargetSetterName();
			if (targetGetterName.startsWith("is")) {
				if (!targetGetterName.substring(2).equals(targetSetterName.substring(3))) {
					String fieldName = fieldInfo.getTargetFieldName();
					if (fieldName.startsWith("i") && Character.toUpperCase(fieldName.charAt(1)) == fieldName.charAt(1)) {
						fieldInfo.setTargetGetterName("get" + fieldName.substring(1));
						fieldInfo.setTargetSetterName("set" + fieldName.substring(1));
					}
					else {
						fieldInfo.setTargetGetterName("get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
						fieldInfo.setTargetSetterName("set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
					}
				}
			}
			else {
				if (!targetGetterName.substring(3).equals(targetSetterName.substring(3))) {
					String fieldName = fieldInfo.getTargetFieldName();
					if (fieldName.startsWith("i") && Character.toUpperCase(fieldName.charAt(1)) == fieldName.charAt(1)) {
						fieldInfo.setTargetGetterName("get" + fieldName.substring(1));
						fieldInfo.setTargetSetterName("set" + fieldName.substring(1));
					}
					else {
						fieldInfo.setTargetGetterName("get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
						fieldInfo.setTargetSetterName("set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
					}
				}
			}
		}
	}
	
	private void resolveUserMethods() {
		List<UserMethodInfo> userMethods = iEntityInfo.getUserMethods();
		for (UserMethodInfo userMethod : userMethods) {
			FieldInfo fieldInfo = null;
			EjbRelationshipRoleInfo ejbRelationshipRoleInfo = null;
			String methodName = userMethod.getMethodName();
			int numberOfParameters = userMethod.getParameterTypes().size();
			if (numberOfParameters == 0 && userMethod.getReturnType() != null) {
				fieldInfo = iEntityInfo.getFieldInfoByGetterName(methodName);
				ejbRelationshipRoleInfo = iEntityInfo.getEjbRelationshipRoleInfoByGetterName(methodName);
			}
			else if (numberOfParameters == 1 && userMethod.getReturnType() == null) {
				fieldInfo = iEntityInfo.getFieldInfoBySetterName(methodName);
				if (fieldInfo != null && !userMethod.getParameterTypes().get(0).equals(fieldInfo.getTypeName())) {
					fieldInfo = null;
				}
				ejbRelationshipRoleInfo = iEntityInfo.getEjbRelationshipRoleInfoBySetterName(methodName);
				if (ejbRelationshipRoleInfo != null && !userMethod.getParameterTypes().get(0).equals(ejbRelationshipRoleInfo.getFieldType())) {
					ejbRelationshipRoleInfo = null;
				}
			}
			if (fieldInfo != null) {
				userMethod.setFieldInfo(fieldInfo);
			}
			if (ejbRelationshipRoleInfo != null) {
				userMethod.setEjbRelationshipRoleInfo(ejbRelationshipRoleInfo);
			}
		}
		for (UserMethodInfo userMethod : userMethods) {
			if (userMethod.getRelatedEntityInfo() == null && userMethod.getFieldInfo() == null && userMethod.getEjbRelationshipRoleInfo() == null) {
				resolveMethodRequirements(userMethod.getKey());
			}
		}
	}
	
	private void resolveCopyHelpers() {
		if(iEntityInfo != null && iEntityInfo.getAccessBeanInfo() != null) {
			Collection<CopyHelperProperty> copyHelperProperties = iEntityInfo.getAccessBeanInfo().getCopyHelperProperties();
			for (CopyHelperProperty copyHelperProperty : copyHelperProperties) {
				if (copyHelperProperty.getFieldInfo() == null) {
					FieldInfo fieldInfo = iEntityInfo.getFieldInfoByName(copyHelperProperty.getName());
					if (fieldInfo == null && copyHelperProperty.getGetterName() != null) {
						fieldInfo = iEntityInfo.getFieldInfoByGetterName(copyHelperProperty.getGetterName());
					}
					if (fieldInfo == null && copyHelperProperty.getSetterName() != null) {
						fieldInfo = iEntityInfo.getFieldInfoBySetterName(copyHelperProperty.getSetterName());
					}
					if (fieldInfo != null) {
						copyHelperProperty.setFieldInfo(fieldInfo);
						fieldInfo.setCopyHelperProperty(copyHelperProperty);
					}
				}
				if (copyHelperProperty.getFieldInfo() == null) {
					if (copyHelperProperty.getGetterName() != null && iEntityInfo.getUserMethodInfo(copyHelperProperty.getGetterName()) == null) {
						String methodKey = copyHelperProperty.getGetterName();
						iEntityInfo.addRequiredMethod(methodKey);
						resolveMethodRequirements(methodKey);
					}
					if (copyHelperProperty.getSetterName() != null) {
						String methodKey = copyHelperProperty.getSetterName() + "+" + copyHelperProperty.getType();
						if (iEntityInfo.getUserMethodInfo(methodKey) == null) {
							iEntityInfo.addRequiredMethod(methodKey);
							resolveMethodRequirements(methodKey);
						}
					}
				}
			}
		}
	}
	
	private void resolveKeyFields() {
		List<FieldInfo> keyFields = iEntityInfo.getKeyFields();
		for (FieldInfo keyFieldInfo : keyFields) {
			if (keyFieldInfo.getCopyHelperProperty() == null && iAccessBeanInfo != null) {
				CopyHelperProperty copyHelperProperty = iAccessBeanInfo.getCopyHelperProperty(keyFieldInfo.getFieldName(), true);
				copyHelperProperty.setType(keyFieldInfo.getTypeName());
				copyHelperProperty.setGetterName(keyFieldInfo.getTargetGetterName());
				copyHelperProperty.setFieldInfo(keyFieldInfo);
				keyFieldInfo.setCopyHelperProperty(copyHelperProperty);
			}
		}
	}
	
	private void resolveMethodRequirements(String methodKey) {
		if (!iProcessedMethodKeys.contains(methodKey)) {
			iProcessedMethodKeys.add(methodKey);
			Collection<String> methods = new HashSet<String>();
			methods.add(methodKey);
			while (!methods.isEmpty()) {
				Collection<String> newMethods = new HashSet<String>();
				for (String currentMethodKey : methods) {
					Collection<String> requiredMethods = iEntityInfo.getRequiredMethods(currentMethodKey);
					if (requiredMethods != null) {
						for (String requiredMethodKey : requiredMethods) {
							if (!iProcessedMethodKeys.contains(requiredMethodKey) && iEntityInfo.getUserMethodInfo(requiredMethodKey) == null && iEntityInfo.getFieldInfoByGetterName(requiredMethodKey) == null && iEntityInfo.getFieldInfoBySetterMethodKey(requiredMethodKey) == null) {
								iProcessedMethodKeys.add(requiredMethodKey);
								iEntityInfo.addRequiredMethod(requiredMethodKey);
								if (iEntityInfo.getSupertype() != null) {
									iEntityInfo.getSupertype().addRequiredMethod(requiredMethodKey);
								}
								newMethods.add(requiredMethodKey);
							}
						}
					}
					resolveMethodRequiredFields(currentMethodKey);
				}
				methods = newMethods;
			}
		}
	}
	
	private void resolveMethodRequiredFields(String methodKey) {
		Collection<String> requiredFields = iEntityInfo.getMethodRequiredFields(methodKey);
		if (requiredFields != null) {
			for (String requiredFieldName : requiredFields) {
				iEntityInfo.addRequiredField(requiredFieldName);
				if (iEntityInfo.getSupertype() != null) {
					iEntityInfo.getSupertype().addRequiredField(requiredFieldName);
				}
				resolveFieldRequirements(requiredFieldName);
			}
		}
	}
	
	private void resolveFieldRequirements(String fieldName) {
		if (!iProcessedFieldNames.contains(fieldName)) {
			iProcessedFieldNames.add(fieldName);
			Collection<String> fields = new HashSet<String>();
			fields.add(fieldName);
			while (!fields.isEmpty()) {
				Collection<String> newFields = new HashSet<String>();
				for (String currentFieldName : fields) {
					Collection<String> requiredFields = iEntityInfo.getFieldRequiredFields(currentFieldName);
					if (requiredFields != null) {
						for (String requiredFieldName : requiredFields) {
							if (!iProcessedFieldNames.contains(requiredFieldName)) {
								iProcessedFieldNames.add(requiredFieldName);
								iEntityInfo.addRequiredField(requiredFieldName);
								if (iEntityInfo.getSupertype() != null) {
									iEntityInfo.getSupertype().addRequiredField(requiredFieldName);
								}
								newFields.add(requiredFieldName);
							}
						}
					}
				}
				fields = newFields;
			}
		}
	}
	
	private void resolveAccessBeanMethods() {
		Collection<CreatorInfo> creators = iEntityInfo.getCreators();
		for (CreatorInfo creatorInfo : creators) {
			if (!creatorInfo.isInvalid()) {
				creatorInfo.getAccessBeanMethodInfo().resolve();
			}
		}
		Collection<AccessBeanMethodInfo> accessBeanMethods = iEntityInfo.getAccessBeanMethods();
		for (AccessBeanMethodInfo accessBeanMethodInfo : accessBeanMethods) {
			if (!accessBeanMethodInfo.isInvalid()) {
				accessBeanMethodInfo.resolve();
			}
		}
		Set<CreatorInfo> creatorsCalledByCreators = iEntityInfo.getCreatorsCalledByCreators();
		for (CreatorInfo creatorInfo : creatorsCalledByCreators) {
			AccessBeanMethodInfo accessBeanMethodInfo = creatorInfo.getAccessBeanMethodInfo();
			String methodKey = accessBeanMethodInfo.getMethodKey();
			iEntityInfo.setAccessBeanMethodInfo(methodKey, accessBeanMethodInfo);
			String[] parameterTypes = accessBeanMethodInfo.getParameterTypes();
			AccessBeanMethodInfo newAccessBeanMethodInfo = new AccessBeanMethodInfo(iEntityInfo, "generated" + methodKey, accessBeanMethodInfo.getMethodName(), parameterTypes, null);
			AccessBeanMethodInvocationStatement statement = new AccessBeanMethodInvocationStatement(methodKey);
			for (int i = 0; i < parameterTypes.length; i++) {
				String parameterName = accessBeanMethodInfo.getTargetParameterName(i);
				newAccessBeanMethodInfo.setParameterName(i, parameterName);
				statement.addArgument(iEntityInfo.getEjbCompilationUnit().getAST().newName(parameterName));
			}
			newAccessBeanMethodInfo.getStatements().add(statement);
			newAccessBeanMethodInfo.addReferencedAccessBeanMethod(methodKey);
			creatorInfo.setAccessBeanMethodInfo(newAccessBeanMethodInfo);
		}
		if(iAccessBeanInfo != null) {
			Collection<CopyHelperProperty> copyHelperProperties = iAccessBeanInfo.getCopyHelperProperties();
			for (CopyHelperProperty copyHelperProperty : copyHelperProperties) {
				if (!iAccessBeanInfo.isExcludedPropertyName(copyHelperProperty.getName())) {
					FieldInfo fieldInfo = copyHelperProperty.getFieldInfo();
					if (fieldInfo != null) {
						AccessBeanMethodInfo getterAccessBeanMethodInfo = null;
						Collection<String> getterNames = fieldInfo.getGetterNames();
						for (String getterName : getterNames) {
							if (getterAccessBeanMethodInfo == null || getterName.equals(fieldInfo.getTargetGetterName())) {
								getterAccessBeanMethodInfo = iEntityInfo.getAccessBeanMethodInfo(getterName);
							}
						}
						if (getterAccessBeanMethodInfo != null && containsAdditionalGetterLogic(fieldInfo, getterAccessBeanMethodInfo)) {
							fieldInfo.setGetterAccessBeanMethodInfo(getterAccessBeanMethodInfo);
						}
						AccessBeanMethodInfo setterAccessBeanMethodInfo = null;
						Collection<String> setterNames = fieldInfo.getSetterNames();
						for (String setterName : setterNames) {
							if (setterAccessBeanMethodInfo == null || setterName.equals(fieldInfo.getTargetSetterName())) {
								String setterMethodKey = setterName + "+" + fieldInfo.getTypeName();
								setterAccessBeanMethodInfo = iEntityInfo.getAccessBeanMethodInfo(setterMethodKey);
								if (setterAccessBeanMethodInfo == null && copyHelperProperty.getType() != null) {
									setterMethodKey = setterName + "+" + copyHelperProperty.getType();
									setterAccessBeanMethodInfo = iEntityInfo.getAccessBeanMethodInfo(setterMethodKey);
								}
							}
						}
						if (setterAccessBeanMethodInfo != null && containsAdditionalSetterLogic(fieldInfo, setterAccessBeanMethodInfo)) {
							fieldInfo.setSetterAccessBeanMethodInfo(setterAccessBeanMethodInfo);
						}
					}
				}
			}
		}
		List<UserMethodInfo> userMethods = iEntityInfo.getUserMethods();
		for (UserMethodInfo userMethodInfo : userMethods) {
			if (userMethodInfo.getFieldInfo() != null && userMethodInfo.getFieldInfo().getCopyHelperProperty() == null) {
				if (userMethodInfo.getMethodName().startsWith(GET)) {
					AccessBeanMethodInfo accessBeanMethodInfo = iEntityInfo.getAccessBeanMethodInfo(userMethodInfo.getMethodName());
					if (accessBeanMethodInfo != null && containsAdditionalGetterLogic(userMethodInfo.getFieldInfo(), accessBeanMethodInfo)) {
						userMethodInfo.setAccessBeanMethodInfo(accessBeanMethodInfo);
					}
				}
				else if (userMethodInfo.getMethodName().startsWith(SET)) {
					String setterMethodKey = userMethodInfo.getMethodName() + "+" + userMethodInfo.getParameterTypes().get(0);
					AccessBeanMethodInfo accessBeanMethodInfo = iEntityInfo.getAccessBeanMethodInfo(setterMethodKey);
					if (accessBeanMethodInfo != null && containsAdditionalSetterLogic(userMethodInfo.getFieldInfo(), accessBeanMethodInfo)) {
						userMethodInfo.setAccessBeanMethodInfo(accessBeanMethodInfo);
					}
				}
			}
		}
	}
	
	private boolean containsAdditionalGetterLogic(FieldInfo fieldInfo, AccessBeanMethodInfo accessBeanMethodInfo) {
		boolean additionalLogic = false;
		List<AccessBeanStatement> statements = accessBeanMethodInfo.getStatements();
		if (statements.size() > 1) {
			additionalLogic = true;
		}
		else if (statements.size() == 1 && statements.get(0) instanceof AccessBeanIfStatement) {
			additionalLogic = true;
		}
		else if (statements.size() == 1 && statements.get(0) instanceof AccessBeanReturnStatement) {
			Expression returnExpression = ((AccessBeanReturnStatement) statements.get(0)).getReturnExpression();
			if (returnExpression != null) {
				additionalLogic = true;
				if (fieldInfo.getRelatedEntityInfo() != null) {
					if (returnExpression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
						additionalLogic = false;
					}
				}
				else if (returnExpression.getNodeType() == ASTNode.METHOD_INVOCATION) {
					MethodInvocation methodInvocation = (MethodInvocation) returnExpression;
					if (methodInvocation.getExpression() != null &&
						methodInvocation.getExpression().getNodeType() == ASTNode.SIMPLE_NAME &&
						methodInvocation.getName().getIdentifier().equals(fieldInfo.getTargetGetterName()) &&
						((SimpleName)methodInvocation.getExpression()).getIdentifier().equals("iTypedEntity")) {
						additionalLogic = false;
					}
				}
			}
		}
		return additionalLogic;
	}
	
	private boolean containsAdditionalSetterLogic(FieldInfo fieldInfo, AccessBeanMethodInfo accessBeanMethodInfo) {
		boolean additionalLogic = false;
		List<AccessBeanStatement> statements = accessBeanMethodInfo.getStatements();
		if (statements.size() > 1) {
			additionalLogic = true;
		}
		else if (statements.size() == 1) {
			if (statements.get(0) instanceof AccessBeanParameterInitializedFieldStatement) {
				additionalLogic = false;
			}
			else if (statements.get(0) instanceof AccessBeanIfStatement) {
				AccessBeanIfStatement ifStatement = (AccessBeanIfStatement) statements.get(0);
				if (ifStatement.getElseStatements().size() > 0) {
					additionalLogic = true;
				}
				else if (ifStatement.getThenStatements().size() == 1 && ifStatement.getThenStatements().get(0) instanceof AccessBeanParameterInitializedFieldStatement) {
					additionalLogic = false;
				}
			}
			else {
				additionalLogic = true;
			}
		}
		return additionalLogic;
	}
}
