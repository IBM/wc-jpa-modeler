package com.ibm.commerce.jpa.port.generators;

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

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.CopyHelperProperty;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;
import com.ibm.commerce.jpa.port.util.TargetExceptionUtil;

public class ModuleInfoXmlGenerator {
	private ModuleInfo iModuleInfo;
	private IProject iProject;
	
	public ModuleInfoXmlGenerator(ModuleInfo moduleInfo) {
		iModuleInfo = moduleInfo;
		iProject = moduleInfo.getJavaProject().getProject();
	}
	
	public void generate(IProgressMonitor progressMonitor) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
		sb.append("<ModuleInfo>\r\n");
		Collection<EntityInfo> entities = iModuleInfo.getEntities();
		for (EntityInfo entityInfo : entities) {
			appendEntityInfo(sb, entityInfo);
		}
		Collection<String> deleteIntendedTypes = iModuleInfo.getDeleteIntendedTypes();
		for (String deleteIntendedType : deleteIntendedTypes) {
			sb.append("\t<DeleteIntendedType>");
			sb.append(deleteIntendedType);
			sb.append("</DeleteIntendedType>\r\n");
		}
		sb.append("</ModuleInfo>");
		IFile moduleInfoXmlFile = iProject.getFile(".jpaModuleInfo.xml");
		ByteArrayInputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes());
		try {
			if (moduleInfoXmlFile.exists()) {
				moduleInfoXmlFile.setContents(inputStream, true, false, new SubProgressMonitor(progressMonitor, 100));
			}
			else {
				moduleInfoXmlFile.create(inputStream, true, new SubProgressMonitor(progressMonitor, 100));
			}
			iModuleInfo.getApplicationInfo().incrementGeneratedAssetCount();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private void appendEntityInfo(StringBuilder sb, EntityInfo entityInfo) {
		sb.append("\t<EntityInfo entityId=\"");
		sb.append(entityInfo.getEntityId());
		sb.append("\" ejbName=\"");
		sb.append(entityInfo.getEjbName());
		sb.append("\"");
		if (entityInfo.getSupertype() != null) {
			sb.append(" superEntityId=\"");
			sb.append(entityInfo.getSupertype().getEntityId());
			sb.append("\"");
		}
		sb.append(">\r\n");
		if (entityInfo.getHome() != null) {
			sb.append("\t\t<Home>");
			sb.append(entityInfo.getHome());
			sb.append("</Home>\r\n");
		}
		if (entityInfo.getRemote() != null) {
			sb.append("\t\t<Remote>");
			sb.append(entityInfo.getRemote());
			sb.append("</Remote>\r\n");
		}
		if (entityInfo.getLocalHome() != null) {
			sb.append("\t\t<LocalHome>");
			sb.append(entityInfo.getLocalHome());
			sb.append("</LocalHome>\r\n");
		}
		if (entityInfo.getLocal() != null) {
			sb.append("\t\t<Local>");
			sb.append(entityInfo.getLocal());
			sb.append("</Local>\r\n");
		}
		if (entityInfo.getEjbClass() != null) {
			sb.append("\t\t<EjbClass>");
			sb.append(entityInfo.getEjbClass());
			sb.append("</EjbClass>\r\n");
		}
		if (entityInfo.getPrimaryKeyClass() != null) {
			sb.append("\t\t<PrimaryKeyClass>");
			sb.append(entityInfo.getPrimaryKeyClass());
			sb.append("</PrimaryKeyClass>\r\n");
		}
		if (entityInfo.getPrimaryKeyField() != null) {
			sb.append("\t\t<PrimaryKeyField>");
			sb.append(entityInfo.getPrimaryKeyField());
			sb.append("</PrimaryKeyField>\r\n");
		}
		if (entityInfo.getProtectable()) {
			sb.append("\t\t<Protectable>true</Protectable>\r\n");
		}
		if (entityInfo.getGroupable()) {
			sb.append("\t\t<Groupable>true</Groupable>\r\n");
		}
		if (entityInfo.getJndiName() != null) {
			sb.append("\t\t<JndiName>");
			sb.append(entityInfo.getJndiName());
			sb.append("</JndiName>\r\n");
		}
		List<RelatedEntityInfo> relatedEntities = entityInfo.getRelatedEntities();
		if (relatedEntities != null) {
			for (RelatedEntityInfo relatedEntityInfo : relatedEntities) {
				appendRelatedEntityInfo(sb, relatedEntityInfo);
			}
		}
		Collection<FieldInfo> fields = entityInfo.getFields();
		if (fields != null) {
			for (FieldInfo fieldInfo : fields) {
				appendFieldInfo(sb, fieldInfo);
			}
		}
		if (entityInfo.getAccessBeanInfo() != null) {
			appendAccessBeanInfo(sb, entityInfo.getAccessBeanInfo());
		}
		List<FinderInfo> finders = entityInfo.getFinders();
		if (finders != null) {
			for (FinderInfo finderInfo : finders) {
				appendFinderInfo(sb, finderInfo);
			}
		}
		List<UserMethodInfo> userMethods = entityInfo.getUserMethods();
		if (userMethods != null) {
			for (UserMethodInfo userMethodInfo : userMethods) {
				appendUserMethodInfo(sb, entityInfo, userMethodInfo);
			}
		}
		Collection<String> staticMethods = entityInfo.getStaticMethods();
		if (staticMethods != null) {
			for (String staticMethod : staticMethods) {
				appendStaticMethodInfo(sb, entityInfo, staticMethod);
			}
		}
		if (entityInfo.getAccessBeanInfo() != null) {
			Collection<CopyHelperProperty> copyHelperProperties = entityInfo.getAccessBeanInfo().getCopyHelperProperties();
			if (copyHelperProperties != null) {
				for (CopyHelperProperty copyHelperProperty : copyHelperProperties) {
					appendCopyHelperProperty(sb, entityInfo, copyHelperProperty);
				}
			}
		}
		List<KeyClassConstructorInfo> keyClassConstructors = entityInfo.getKeyClassConstructors();
		if (keyClassConstructors != null) {
			for (KeyClassConstructorInfo keyClassConstructorInfo : keyClassConstructors) {
				appendKeyClassConstructorInfo(sb, keyClassConstructorInfo);
			}
		}
		sb.append("\t</EntityInfo>\r\n");
	}
	
	private void appendRelatedEntityInfo(StringBuilder sb, RelatedEntityInfo relatedEntityInfo) {
		sb.append("\t\t<RelatedEntityInfo parentEntityId=\"");
		sb.append(relatedEntityInfo.getParentEntityInfo().getEntityId());
		sb.append("\" fieldName=\"");
		sb.append(relatedEntityInfo.getFieldName());
		sb.append("\">\r\n");
		if (relatedEntityInfo.getGetterName() != null) {
			sb.append("\t\t\t<GetterName>");
			sb.append(relatedEntityInfo.getGetterName());
			sb.append("</GetterName>\r\n");
		}
		if (relatedEntityInfo.getSetterName() != null) {
			sb.append("\t\t\t<SetterName>");
			sb.append(relatedEntityInfo.getSetterName());
			sb.append("</SetterName>\r\n");
		}
		List<FieldInfo> memberFields = relatedEntityInfo.getMemberFields();
		if (memberFields != null) {
			for (FieldInfo fieldInfo : memberFields) {
				sb.append("\t\t\t<MemberField fieldId=\"");
				sb.append(fieldInfo.getFieldId());
				sb.append("\" referencedFieldId=\"");
				sb.append(fieldInfo.getReferencedFieldInfo().getFieldId());
				sb.append("\"/>\r\n");
			}
		}
		sb.append("\t\t</RelatedEntityInfo>\r\n");
	}
	
	private void appendFieldInfo(StringBuilder sb, FieldInfo fieldInfo) {
		sb.append("\t\t<FieldInfo fieldId=\"");
		sb.append(fieldInfo.getFieldId());
		sb.append("\" fieldName=\"");
		sb.append(fieldInfo.getFieldName());
		sb.append("\" targetFieldName=\"");
		sb.append(fieldInfo.getTargetFieldName());
		sb.append("\">\r\n");
		if (fieldInfo.getTypeName() != null) {
			sb.append("\t\t\t<TypeName>");
			sb.append(fieldInfo.getTypeName());
			sb.append("</TypeName>\r\n");
		}
		Collection<String> setterNames = fieldInfo.getSetterNames();
		if (setterNames != null) {
			for (String setterName : setterNames) {
				sb.append("\t\t\t<SetterName>");
				sb.append(setterName);
				sb.append("</SetterName>\r\n");
			}
		}
		Collection<String> getterNames = fieldInfo.getGetterNames();
		if (getterNames != null) {
			for (String getterName : getterNames) {
				sb.append("\t\t\t<GetterName>");
				sb.append(getterName);
				sb.append("</GetterName>\r\n");
			}
		}
		if (fieldInfo.getTargetSetterName() != null) {
			sb.append("\t\t\t<TargetSetterName>");
			sb.append(fieldInfo.getTargetSetterName());
			sb.append("</TargetSetterName>\r\n");
		}
		if (fieldInfo.getTargetGetterName() != null) {
			sb.append("\t\t\t<TargetGetterName>");
			sb.append(fieldInfo.getTargetGetterName());
			sb.append("</TargetGetterName>\r\n");
		}
		if (fieldInfo.getHasStringConversionAccessMethod()) {
			sb.append("\t\t\t<HasStringConversionAccessMethod>true</HasStringConversionAccessMethod>\r\n");
		}
		sb.append("\t\t</FieldInfo>\r\n");
	}
	
	private void appendAccessBeanInfo(StringBuilder sb, AccessBeanInfo accessBeanInfo) {
		sb.append("\t\t<AccessBeanInfo>\r\n");
		if (accessBeanInfo.getAccessBeanName() != null) {
			sb.append("\t\t\t<AccessBeanName>");
			sb.append(accessBeanInfo.getAccessBeanName());
			sb.append("</AccessBeanName>\r\n");
		}
		if (accessBeanInfo.getAccessBeanPackage() != null) {
			sb.append("\t\t\t<AccessBeanPackage>");
			sb.append(accessBeanInfo.getAccessBeanPackage());
			sb.append("</AccessBeanPackage>\r\n");
		}
		Collection<String> accessBeanInterfaces = accessBeanInfo.getAccessBeanInterfaces();
		for (String accessBeanInterface : accessBeanInterfaces) {
			sb.append("\t\t\t<AccessBeanInterface>");
			sb.append(accessBeanInterface);
			sb.append("</AccessBeanInterface>\r\n");
		}
		if (accessBeanInfo.getDataClassType()) {
			sb.append("\t\t\t<DataClassType>true</DataClassType>\r\n");
		}
		Collection<NullConstructorParameter> nullConstructorParameters = accessBeanInfo.getNullConstructorParameters();
		if (nullConstructorParameters != null) {
			for (NullConstructorParameter nullConstructorParameter : nullConstructorParameters) {
				sb.append("\t\t\t<NullConstructorParameter");
				if (nullConstructorParameter.getName() != null) {
					sb.append(" name=\"");
					sb.append(nullConstructorParameter.getName());
					sb.append("\"");
				}
				if (nullConstructorParameter.getType() != null) {
					sb.append(" type=\"");
					sb.append(nullConstructorParameter.getType());
					sb.append("\"");
				}
				if (nullConstructorParameter.getConverterClassName() != null) {
					sb.append(" converterClassName=\"");
					sb.append(nullConstructorParameter.getConverterClassName());
					sb.append("\"");
				}
				sb.append("/>\r\n");
			}
		}
		sb.append("\t\t</AccessBeanInfo>\r\n");
	}
	
	private void appendFinderInfo(StringBuilder sb, FinderInfo finderInfo) {
		sb.append("\t\t<FinderInfo finderId=\"");
		sb.append(finderInfo.getFinderId());
		sb.append("\">\r\n");
		if (finderInfo.getFinderWhereClause() != null) {
			sb.append("\t\t\t<FinderWhereClause><![CDATA[");
			sb.append(finderInfo.getFinderWhereClause());
			sb.append("]]></FinderWhereClause>\r\n");
		}
		if (finderInfo.getFinderSelectStatement() != null) {
			sb.append("\t\t\t<FinderSelectStatement><![CDATA[");
			sb.append(finderInfo.getFinderSelectStatement());
			sb.append("]]></FinderSelectStatement>\r\n");
		}
		if (finderInfo.getFinderQuery() != null) {
			sb.append("\t\t\t<FinderQuery><![CDATA[");
			sb.append(finderInfo.getFinderQuery());
			sb.append("]]></FinderQuery>\r\n");
		}
		if (finderInfo.getFinderMethodName() != null) {
			sb.append("\t\t\t<FinderMethod name=\"");
			sb.append(finderInfo.getFinderMethodName());
			sb.append("\"");
			if (finderInfo.getFinderMethodReturnType() != null) {
				sb.append(" returnType=\"");
				sb.append(finderInfo.getFinderMethodReturnType());
				sb.append("\"");
			}
			sb.append(">\r\n");
			String[] finderMethodParameterTypes = finderInfo.getFinderMethodParameterTypes();
			if (finderMethodParameterTypes != null) {
				for (int i = 0; i < finderMethodParameterTypes.length; i++) {
					sb.append("\t\t\t\t<FinderMethodParameter");
					if (finderInfo.getFinderMethodParameterName(i) != null) {
						sb.append(" name=\"");
						sb.append(finderInfo.getFinderMethodParameterName(i));
						sb.append("\"");
					}
					sb.append(" type=\"");
					sb.append(finderMethodParameterTypes[i]);
					sb.append("\"/>\r\n");
				}
			}
			sb.append("\t\t\t</FinderMethod>\r\n");
		}
		sb.append("\t\t</FinderInfo>\r\n");
	}
	
	private void appendUserMethodInfo(StringBuilder sb, EntityInfo entityInfo, UserMethodInfo userMethodInfo) {
		sb.append("\t\t<UserMethodInfo methodName=\"");
		sb.append(userMethodInfo.getMethodName());
		sb.append("\"");
		if (userMethodInfo.getReturnType() != null) {
			sb.append(" returnType=\"");
			sb.append(userMethodInfo.getReturnType());
			sb.append("\"");
		}
		if (userMethodInfo.getFieldInfo() != null) {
			sb.append(" fieldId=\"");
			sb.append(userMethodInfo.getFieldInfo().getFieldId());
			sb.append("\"");
		}
		sb.append(">\r\n");
		List<String> parameterNames = userMethodInfo.getParameterNames();
		List<String> parameterTypes = userMethodInfo.getParameterTypes();
		if (parameterNames != null && parameterTypes != null) {
			for (int i = 0; i < parameterNames.size(); i++) {
				sb.append("\t\t\t<UserMethodParameter name=\"");
				sb.append(parameterNames.get(i));
				sb.append("\" type=\"");
				sb.append(parameterTypes.get(i));
				sb.append("\"/>\r\n");
			}
		}
		TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getEjbMethodUnhandledTargetExceptions(entityInfo, userMethodInfo.getKey());
		if (targetExceptionInfo != null) {
			Collection<String> targetExceptions = targetExceptionInfo.getTargetExceptions();
			for (String exception : targetExceptions) {
				sb.append("\t\t\t<TargetException name=\"");
				sb.append(exception);
				sb.append("\"/>\r\n");
			}
			Collection<String> sourceExceptions = targetExceptionInfo.getSourceExceptions();
			for (String exception : sourceExceptions) {
				sb.append("\t\t\t<SourceException name=\"");
				sb.append(exception);
				sb.append("\"/>\r\n");
			}
		}
		sb.append("\t\t</UserMethodInfo>\r\n");
	}

	private void appendStaticMethodInfo(StringBuilder sb, EntityInfo entityInfo, String staticMethodKey) {
		sb.append("\t\t<StaticMethodInfo methodKey=\"");
		sb.append(staticMethodKey);
		sb.append("\">\r\n");
		TargetExceptionInfo targetExceptionInfo = TargetExceptionUtil.getEjbMethodUnhandledTargetExceptions(entityInfo, staticMethodKey);
		if (targetExceptionInfo != null) {
			Collection<String> targetExceptions = targetExceptionInfo.getTargetExceptions();
			for (String exception : targetExceptions) {
				sb.append("\t\t\t<TargetException name=\"");
				sb.append(exception);
				sb.append("\"/>\r\n");
			}
			Collection<String> sourceExceptions = targetExceptionInfo.getSourceExceptions();
			for (String exception : sourceExceptions) {
				sb.append("\t\t\t<SourceException name=\"");
				sb.append(exception);
				sb.append("\"/>\r\n");
			}
		}
		sb.append("\t\t</StaticMethodInfo>\r\n");
	}
	
	private void appendCopyHelperProperty(StringBuilder sb, EntityInfo entityInfo, CopyHelperProperty copyHelperProperty) {
		if (copyHelperProperty.getFieldInfo() == null) {
			if (copyHelperProperty.getSetterName() != null) {
				sb.append("\t\t<UserMethodInfo methodName=\"");
				sb.append(copyHelperProperty.getSetterName());
				sb.append("\">\r\n");
				sb.append("\t\t\t<UserMethodParameter name=\"");
				sb.append(copyHelperProperty.getName());
				sb.append("\" type=\"");
				sb.append(copyHelperProperty.getType());
				sb.append("\"/>\r\n");
				sb.append("\t\t</UserMethodInfo>\r\n");
			}
			if (copyHelperProperty.getGetterName() != null) {
				sb.append("\t\t<UserMethodInfo methodName=\"");
				sb.append(copyHelperProperty.getGetterName());
				sb.append("\" returnType=\"");
				sb.append(copyHelperProperty.getType());
				sb.append("\">\r\n");
				sb.append("\t\t</UserMethodInfo>\r\n");
			}
		}
	}
	
	private void appendKeyClassConstructorInfo(StringBuilder sb, KeyClassConstructorInfo keyClassConstructorInfo) {
		sb.append("\t\t<KeyClassConstructorInfo>\r\n");
		List<FieldInfo> fields = keyClassConstructorInfo.getFields();
		if (fields != null) {
			for (FieldInfo fieldInfo : fields) {
				sb.append("\t\t\t<FieldInfo fieldId=\"");
				sb.append(fieldInfo.getFieldId());
				sb.append("\"/>\r\n");
			}
		}
		sb.append("\t\t</KeyClassConstructorInfo>\r\n");
	}
}
