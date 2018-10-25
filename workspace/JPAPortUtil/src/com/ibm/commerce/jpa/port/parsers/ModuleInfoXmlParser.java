package com.ibm.commerce.jpa.port.parsers;

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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.AccessBeanInfo.NullConstructorParameter;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.KeyClassConstructorInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.RelatedEntityInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.info.UserMethodInfo;
import com.ibm.commerce.jpa.port.util.ApplicationInfoUtil;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class ModuleInfoXmlParser {
	private static final String ENTITY_INFO = "EntityInfo";
	private static final String ENTITY_ID = "entityId";
	private static final String SUPER_ENTITY_ID = "superEntityId";
	private static final String EJB_NAME = "ejbName";
	private static final String HOME = "Home";
	private static final String REMOTE = "Remote";
	private static final String LOCAL_HOME = "LocalHome";
	private static final String LOCAL = "Local";
	private static final String EJB_CLASS = "EjbClass";
	private static final String PRIMARY_KEY_CLASS = "PrimaryKeyClass";
	private static final String PRIMARY_KEY_FIELD = "PrimaryKeyField";
	private static final String PROTECTABLE = "Protectable";
	private static final String GROUPABLE = "Groupable";
	private static final String JNDI_NAME = "JndiName";
	private static final String RELATED_ENTITY_INFO = "RelatedEntityInfo";
	private static final String PARENT_ENTITY_ID = "parentEntityId";
	private static final String MEMBER_FIELD = "MemberField";
	private static final String REFERENCED_FIELD_ID = "referencedFieldId";
	private static final String FIELD_INFO = "FieldInfo";
	private static final String FIELD_ID = "fieldId";
	private static final String FIELD_NAME = "fieldName";
	private static final String TYPE_NAME = "TypeName";
	private static final String SETTER_NAME = "SetterName";
	private static final String GETTER_NAME = "GetterName";
	private static final String TARGET_SETTER_NAME = "TargetSetterName";
	private static final String TARGET_GETTER_NAME = "TargetGetterName";
	private static final String HAS_STRING_CONVERSION_ACCESS_METHOD = "HasStringConversionAccessMethod";
	private static final String TARGET_FIELD_NAME = "targetFieldName";
	private static final String ACCESS_BEAN_INFO = "AccessBeanInfo";
	private static final String ACCESS_BEAN_ID = "accessBeanId";
	private static final String ACCESS_BEAN_NAME = "AccessBeanName";
	private static final String ACCESS_BEAN_PACKAGE = "AccessBeanPackage";
	private static final String ACCESS_BEAN_INTERFACE = "AccessBeanInterface";
	private static final String DATA_CLASS_TYPE = "DataClassType";
	private static final String NULL_CONSTRUCTOR_PARAMETER = "NullConstructorParameter";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String TYPE = "type";
	private static final String CONVERTER_CLASS_NAME = "converterClassName";
	private static final String FINDER_INFO = "FinderInfo";
	private static final String FINDER_ID = "finderId";
	private static final String RETURN_TYPE = "returnType";
	private static final String FINDER_WHERE_CLAUSE = "FinderWhereClause";
	private static final String FINDER_SELECT_STATEMENT = "FinderSelectStatement";
	private static final String FINDER_QUERY = "FinderQuery";
	private static final String FINDER_METHOD = "FinderMethod";
	private static final String FINDER_METHOD_PARAMETER = "FinderMethodParameter";
	private static final String USER_METHOD_INFO = "UserMethodInfo";
	private static final String STATIC_METHOD_INFO = "StaticMethodInfo";
	private static final String METHOD_NAME = "methodName";
	private static final String METHOD_KEY = "methodKey";
	private static final String USER_METHOD_PARAMETER = "UserMethodParameter";
	private static final String TARGET_EXCEPTION = "TargetException";
	private static final String SOURCE_EXCEPTION = "SourceException";
	private static final String KEY_CLASS_CONSTRUCTOR_INFO = "KeyClassConstructorInfo";
	private static final String DELETE_INTENDED_TYPE = "DeleteIntendedType";
	
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private IProject iProject;
	
	public ModuleInfoXmlParser(ModuleInfo moduleInfo) {
		iModuleInfo = moduleInfo;
		iApplicationInfo = moduleInfo.getApplicationInfo();
		iProject = moduleInfo.getJavaProject().getProject();
	}
	
	public void parse(IProgressMonitor progressMonitor) {
		IFile moduleInfoXmlFile = iProject.getFile(".jpaModuleInfo.xml");
		if (moduleInfoXmlFile.exists()) {
			Document document = iModuleInfo.getXMLUtil().readXml(moduleInfoXmlFile);
			parseModuleInfo(document.getDocumentElement());
			iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
		}
	}
	
	private void parseModuleInfo(Element moduleInfoElement) {
		NodeList childNodes = moduleInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTITY_INFO.equals(nodeName)) {
					parseEntityInfoElement(element);
				}
				else if (DELETE_INTENDED_TYPE.equals(nodeName)) {
					String deleteIntendedType = XMLUtil.getElementText(element);
					iModuleInfo.addDeleteIntendedType(deleteIntendedType);
					if (deleteIntendedType.endsWith("_Stub")) {
						iApplicationInfo.addEjbStubType(deleteIntendedType);
					}
				}
			}
		}
		Collection<EntityInfo> entities = iModuleInfo.getEntities();
		for (EntityInfo entityInfo : entities) {
			if (entityInfo.getSupertype() == null) {
				ApplicationInfoUtil.resolveTypeMappings(iApplicationInfo, entityInfo);
			}
		}
		for (EntityInfo entityInfo : entities) {
			if (entityInfo.getSupertype() != null) {
				ApplicationInfoUtil.resolveTypeMappings(iApplicationInfo, entityInfo);
			}
		}
	}
	
	private void parseEntityInfoElement(Element entityInfoElement) {
		EntityInfo entityInfo = iModuleInfo.getEntityInfo(entityInfoElement.getAttribute(ENTITY_ID), true);
		entityInfo.setEjbName(entityInfoElement.getAttribute(EJB_NAME));
		if (entityInfoElement.hasAttribute(SUPER_ENTITY_ID)) {
			EntityInfo superEntityInfo = iModuleInfo.getEntityInfo(entityInfoElement.getAttribute(SUPER_ENTITY_ID), true);
			entityInfo.setSuperType(superEntityInfo);
			superEntityInfo.addSubtype(entityInfo);
		}
		NodeList childNodes = entityInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (HOME.equals(nodeName)) {
					parseHomeElement(element, entityInfo);
				}
				else if (REMOTE.equals(nodeName)) {
					parseRemoteElement(element, entityInfo);
				}
				else if (LOCAL_HOME.equals(nodeName)) {
					parseLocalHomeElement(element, entityInfo);
				}
				else if (LOCAL.equals(nodeName)) {
					parseLocalElement(element, entityInfo);
				}
				else if (EJB_CLASS.equals(nodeName)) {
					parseEjbClassElement(element, entityInfo);
				}
				else if (PRIMARY_KEY_CLASS.equals(nodeName)) {
					parsePrimaryKeyClassElement(element, entityInfo);
				}
				else if (PRIMARY_KEY_FIELD.equals(nodeName)) {
					parsePrimaryKeyFieldElement(element, entityInfo);
				}
				else if (PROTECTABLE.equals(nodeName)) {
					parseProtectableElement(element, entityInfo);
				}
				else if (GROUPABLE.equals(nodeName)) {
					parseGroupableElement(element, entityInfo);
				}
				else if (JNDI_NAME.equals(nodeName)) {
					parseJndiNameElement(element, entityInfo);
				}
				else if (RELATED_ENTITY_INFO.equals(nodeName)) {
					parseRelatedEntityInfoElement(element, entityInfo);
				}
				else if (FIELD_INFO.equals(nodeName)) {
					parseFieldInfoElement(element, entityInfo);
				}
				else if (ACCESS_BEAN_INFO.equals(nodeName)) {
					parseAccessBeanInfoElement(element, entityInfo);
				}
				else if (FINDER_INFO.equals(nodeName)) {
					parseFinderInfoElement(element, entityInfo);
				}
				else if (USER_METHOD_INFO.equals(nodeName)) {
					parseUserMethodInfoElement(element, entityInfo);
				}
				else if (STATIC_METHOD_INFO.equals(nodeName)) {
					parseStaticMethodInfoElement(element, entityInfo);
				}
				else if (KEY_CLASS_CONSTRUCTOR_INFO.equals(nodeName)) {
					parseKeyClassConstructorInfoElement(element, entityInfo);
				}
			}
		}
	}
	
	private void parseHomeElement(Element homeElement, EntityInfo entityInfo) {
		entityInfo.setHome(XMLUtil.getElementText(homeElement));
	}
	
	private void parseRemoteElement(Element remoteElement, EntityInfo entityInfo) {
		entityInfo.setRemote(XMLUtil.getElementText(remoteElement));
	}
	
	private void parseLocalHomeElement(Element localHomeElement, EntityInfo entityInfo) {
		entityInfo.setLocalHome(XMLUtil.getElementText(localHomeElement));
	}
	
	private void parseLocalElement(Element localElement, EntityInfo entityInfo) {
		entityInfo.setLocal(XMLUtil.getElementText(localElement));
	}
	
	private void parseEjbClassElement(Element ejbClassElement, EntityInfo entityInfo) {
		entityInfo.setEjbClass(XMLUtil.getElementText(ejbClassElement));
	}
	
	private void parsePrimaryKeyClassElement(Element primaryKeyClassElement, EntityInfo entityInfo) {
		entityInfo.setPrimaryKeyClass(XMLUtil.getElementText(primaryKeyClassElement));
	}
	
	private void parsePrimaryKeyFieldElement(Element primaryKeyFieldElement, EntityInfo entityInfo) {
		entityInfo.setPrimaryKeyField(XMLUtil.getElementText(primaryKeyFieldElement));
	}
	
	private void parseProtectableElement(Element protectableElement, EntityInfo entityInfo) {
		entityInfo.setProtectable(Boolean.valueOf(XMLUtil.getElementText(protectableElement)));
	}

	private void parseGroupableElement(Element groupableElement, EntityInfo entityInfo) {
		entityInfo.setGroupable(Boolean.valueOf(XMLUtil.getElementText(groupableElement)));
	}
	
	private void parseJndiNameElement(Element jndiNameElement, EntityInfo entityInfo) {
		entityInfo.setJndiName(XMLUtil.getElementText(jndiNameElement));
	}
	
	private void parseRelatedEntityInfoElement(Element relatedEntityInfoElement, EntityInfo entityInfo) {
		RelatedEntityInfo relatedEntityInfo = new RelatedEntityInfo(entityInfo, iModuleInfo.getEntityInfo(relatedEntityInfoElement.getAttribute(PARENT_ENTITY_ID), true));
		relatedEntityInfo.setFieldName(relatedEntityInfoElement.getAttribute(FIELD_NAME));
		NodeList childNodes = relatedEntityInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (GETTER_NAME.equals(nodeName)) {
					relatedEntityInfo.setGetterName(XMLUtil.getElementText(element));
				}
				else if (SETTER_NAME.equals(nodeName)) {
					relatedEntityInfo.setSetterName(XMLUtil.getElementText(element));
				}
				else if (MEMBER_FIELD.equals(nodeName)) {
					parseMemberFieldElement(element, relatedEntityInfo);
				}
			}
		}
	}
	
	private void parseMemberFieldElement(Element memberFieldElement, RelatedEntityInfo relatedEntityInfo) {
		FieldInfo memberFieldInfo = relatedEntityInfo.getEntityInfo().getFieldInfo(memberFieldElement.getAttribute(FIELD_ID), true);
		FieldInfo referencedFieldInfo = relatedEntityInfo.getParentEntityInfo().getFieldInfo(memberFieldElement.getAttribute(REFERENCED_FIELD_ID), true);
		relatedEntityInfo.addMemberField(memberFieldInfo, referencedFieldInfo);
	}
	
	private void parseFieldInfoElement(Element fieldInfoElement, EntityInfo entityInfo) {
		FieldInfo fieldInfo = entityInfo.getFieldInfo(fieldInfoElement.getAttribute(FIELD_ID), true);
		fieldInfo.setFieldName(fieldInfoElement.getAttribute(FIELD_NAME));
		fieldInfo.setTargetFieldName(fieldInfoElement.getAttribute(TARGET_FIELD_NAME));
		NodeList childNodes = fieldInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (TYPE_NAME.equals(nodeName)) {
					fieldInfo.setTypeName(XMLUtil.getElementText(element));
				}
				else if (SETTER_NAME.equals(nodeName)) {
					fieldInfo.setSetterName(XMLUtil.getElementText(element));
				}
				else if (GETTER_NAME.equals(nodeName)) {
					fieldInfo.setGetterName(XMLUtil.getElementText(element));
				}
				else if (TARGET_GETTER_NAME.equals(nodeName)) {
					fieldInfo.setTargetGetterName(XMLUtil.getElementText(element));
				}
				else if (TARGET_SETTER_NAME.equals(nodeName)) {
					fieldInfo.setTargetSetterName(XMLUtil.getElementText(element));
				}
				else if (HAS_STRING_CONVERSION_ACCESS_METHOD.equals(nodeName)) {
					fieldInfo.setHasStringConversionAccessMethod(Boolean.parseBoolean(XMLUtil.getElementText(element)));
				}
			}
		}
	}
	
	private void parseAccessBeanInfoElement(Element accessBeanInfoElement, EntityInfo entityInfo) {
		AccessBeanInfo accessBeanInfo = new AccessBeanInfo(accessBeanInfoElement.getAttribute(ACCESS_BEAN_ID));
		entityInfo.setAccessBeanInfo(accessBeanInfo);
		NodeList childNodes = accessBeanInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ACCESS_BEAN_NAME.equals(nodeName)) {
					accessBeanInfo.setAccessBeanName(XMLUtil.getElementText(element));
				}
				else if (ACCESS_BEAN_PACKAGE.equals(nodeName)) {
					accessBeanInfo.setAccessBeanPackage(XMLUtil.getElementText(element));
				}
				else if (ACCESS_BEAN_INTERFACE.equals(nodeName)) {
					accessBeanInfo.addAccessBeanInterface(XMLUtil.getElementText(element));
				}
				else if (DATA_CLASS_TYPE.equals(nodeName)) {
					accessBeanInfo.setDataClassType(Boolean.parseBoolean(XMLUtil.getElementText(element)));
				}
				else if (NULL_CONSTRUCTOR_PARAMETER.equals(nodeName)) {
					parseNullConstructorParameterElement(element, accessBeanInfo);
				}
			}
		}
	}
	
	private void parseNullConstructorParameterElement(Element nullConstructorParameterElement, AccessBeanInfo accessBeanInfo) {
		NullConstructorParameter nullConstructorParameter = accessBeanInfo.getNullConstructorParameter(nullConstructorParameterElement.getAttribute(ID), true);
		if (nullConstructorParameterElement.hasAttribute(NAME)) {
			nullConstructorParameter.setName(nullConstructorParameterElement.getAttribute(NAME));
		}
		if (nullConstructorParameterElement.hasAttribute(TYPE)) {
			nullConstructorParameter.setType(nullConstructorParameterElement.getAttribute(TYPE));
		}
		if (nullConstructorParameterElement.hasAttribute(CONVERTER_CLASS_NAME)) {
			nullConstructorParameter.setConverterClassName(nullConstructorParameterElement.getAttribute(CONVERTER_CLASS_NAME));
		}
	}
	
	private void parseFinderInfoElement(Element finderInfoElement, EntityInfo entityInfo) {
		FinderInfo finderInfo = entityInfo.getFinderInfo(finderInfoElement.getAttribute(FINDER_ID), true);
		NodeList childNodes = finderInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (FINDER_WHERE_CLAUSE.equals(nodeName)) {
					finderInfo.setFinderWhereClause(XMLUtil.getElementText(element));
				}
				else if (FINDER_SELECT_STATEMENT.equals(nodeName)) {
					finderInfo.setFinderSelectStatement(XMLUtil.getElementText(element));
				}
				else if (FINDER_QUERY.equals(nodeName)) {
					finderInfo.setFinderQuery(XMLUtil.getElementText(element));
				}
				else if (FINDER_METHOD.equals(nodeName)) {
					parseFinderMethodElement(element, finderInfo);
				}
			}
		}
	}
	
	private void parseFinderMethodElement(Element finderMethodElement, FinderInfo finderInfo) {
		finderInfo.setFinderMethodName(finderMethodElement.getAttribute(NAME));
		finderInfo.setFinderMethodReturnType(finderMethodElement.getAttribute(RETURN_TYPE));
		NodeList childNodes = finderMethodElement.getChildNodes();
		List<String> parameterNames = new ArrayList<String>();
		List<String> parameterTypes = new ArrayList<String>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (FINDER_METHOD_PARAMETER.equals(nodeName)) {
					parameterNames.add(element.getAttribute(NAME));
					parameterTypes.add(element.getAttribute(TYPE));
				}
			}
		}
		finderInfo.setFinderMethodParameterTypes(parameterTypes.toArray(new String[parameterTypes.size()]));
		for (int i = 0; i < parameterNames.size(); i++) {
			finderInfo.setFinderMethodParameterName(i, parameterNames.get(i));
		}
	}
	
	private void parseUserMethodInfoElement(Element userMethodInfoElement, EntityInfo entityInfo) {
		UserMethodInfo userMethodInfo = new UserMethodInfo(userMethodInfoElement.getAttribute(METHOD_NAME));
		if (userMethodInfoElement.hasAttribute(RETURN_TYPE)) {
			userMethodInfo.setReturnType(userMethodInfoElement.getAttribute(RETURN_TYPE));
		}
		if (userMethodInfoElement.hasAttribute(FIELD_ID)) {
			userMethodInfo.setFieldInfo(entityInfo.getFieldInfo(userMethodInfoElement.getAttribute(FIELD_ID), true));
		}
		TargetExceptionInfo targetExceptionInfo = new TargetExceptionInfo();
		NodeList childNodes = userMethodInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (USER_METHOD_PARAMETER.equals(nodeName)) {
					userMethodInfo.addParameter(element.getAttribute(NAME), element.getAttribute(TYPE));
				}
				else if (TARGET_EXCEPTION.equals(nodeName)) {
					targetExceptionInfo.addTargetException(element.getAttribute(NAME));
				}
				else if (SOURCE_EXCEPTION.equals(nodeName)) {
					targetExceptionInfo.addSourceException(element.getAttribute(NAME));
				}
			}
		}
		entityInfo.addUserMethod(userMethodInfo);
		entityInfo.setEjbMethodUnhandledTargetExceptions(userMethodInfo.getKey(), targetExceptionInfo);
	}

	private void parseStaticMethodInfoElement(Element staticMethodInfoElement, EntityInfo entityInfo) {
		String staticMethodKey = staticMethodInfoElement.getAttribute(METHOD_KEY);
		TargetExceptionInfo targetExceptionInfo = new TargetExceptionInfo();
		NodeList childNodes = staticMethodInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (TARGET_EXCEPTION.equals(nodeName)) {
					targetExceptionInfo.addTargetException(element.getAttribute(NAME));
				}
				else if (SOURCE_EXCEPTION.equals(nodeName)) {
					targetExceptionInfo.addSourceException(element.getAttribute(NAME));
				}
			}
		}
		entityInfo.setEjbMethodUnhandledTargetExceptions(staticMethodKey, targetExceptionInfo);
	}
	
	private void parseKeyClassConstructorInfoElement(Element keyClassConstructorInfoElement, EntityInfo entityInfo) {
		KeyClassConstructorInfo keyClassConstructorInfo = new KeyClassConstructorInfo();
		entityInfo.addKeyClassConstructor(keyClassConstructorInfo);
		NodeList childNodes = keyClassConstructorInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (FIELD_INFO.equals(nodeName)) {
					keyClassConstructorInfo.getFields().add(entityInfo.getFieldInfo(element.getAttribute(FIELD_ID), true));
				}
			}
		}
	}
}
