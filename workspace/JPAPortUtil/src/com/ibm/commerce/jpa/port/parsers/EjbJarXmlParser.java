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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.GenerationProperties;
import com.ibm.commerce.jpa.port.info.EjbLocalRefInfo;
import com.ibm.commerce.jpa.port.info.EjbRelationInfo;
import com.ibm.commerce.jpa.port.info.EjbRelationshipRoleInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class EjbJarXmlParser {
	private static final String ENTERPRISE_BEANS = "enterprise-beans";
	private static final String ENTITY = "entity";
	private static final String ID = "id";
	private static final String EJB_NAME = "ejb-name";
	private static final String HOME = "home";
	private static final String REMOTE = "remote";
	private static final String EJB_CLASS = "ejb-class";
	private static final String PRIM_KEY_CLASS = "prim-key-class";
	private static final String PRIM_KEY_FIELD = "primkey-field";
	private static final String LOCAL_HOME = "local-home";
	private static final String LOCAL = "local";
	private static final String CMP_VERSION = "cmp-version";
	private static final String CMP_FIELD = "cmp-field";
	private static final String FIELD_NAME = "field-name";
	private static final String ABSTRACT_SCHEMA_NAME = "abstract-schema-name";
	private static final String EJB_LOCAL_REF = "ejb-local-ref";
	private static final String EJB_REF_NAME = "ejb-ref-name";
	private static final String EJB_REF_TYPE = "ejb-ref-type";
	private static final String EJB_LINK = "ejb-link";
	private static final String QUERY = "query";
	private static final String QUERY_METHOD = "query-method";
	private static final String METHOD_NAME = "method-name";
	private static final String METHOD_PARAMS = "method-params";
	private static final String METHOD_PARAM = "method-param";
	private static final String EJB_QL = "ejb-ql";
	private static final String RELATIONSHIPS = "relationships";
	private static final String EJB_RELATION = "ejb-relation";
	private static final String EJB_RELATION_NAME = "ejb-relation-name";
	private static final String EJB_RELATIONSHIP_ROLE = "ejb-relationship-role";
	private static final String EJB_RELATIONSHIP_ROLE_NAME = "ejb-relationship-role-name";
	private static final String MULTIPLICITY = "multiplicity";
	private static final String CASCADE_DELETE = "cascade-delete";
	private static final String RELATIONSHIP_ROLE_SOURCE = "relationship-role-source";
	private static final String CMR_FIELD = "cmr-field";
	private static final String CMR_FIELD_NAME = "cmr-field-name";
	private static final String CMR_FIELD_TYPE = "cmr-field-type";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	/**
	 * This class parses the ejb-jar.xml deployment descriptor for an EJB project.
	 * It specifically parses out the beans in the deployment description and their associated child info and puts
	 * them into the entityInfo objects within the ModuleInfo
	 * 
	 * @param file
	 * @param moduleInfo
	 */
	public EjbJarXmlParser(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void parse() {
		Document document = iModuleInfo.getXMLUtil().readXml(iFile);
		parseEjbJarElement(document.getDocumentElement());
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseEjbJarElement(Element documentElement) {
		NodeList childNodes = documentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTERPRISE_BEANS.equals(nodeName)) {
					parseEnterpriseBeansElement(element);
				}
				else if (RELATIONSHIPS.equals(nodeName)) {
					parseRelationshipsElement(element);
				}
			}
		}
	}
	
	private void parseEnterpriseBeansElement(Element enterpriseBeansElement) {
		NodeList childNodes = enterpriseBeansElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTITY.equals(nodeName)) {
					parseEntityElement(element);
				}
			}
		}
	}
	
	private void parseEntityElement(Element entityElement) {
		EntityInfo entityInfo = iModuleInfo.getEntityInfo(entityElement.getAttribute(ID), true);
		NodeList childNodes = entityElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_NAME.equals(nodeName)) {
					entityInfo.setEjbName(XMLUtil.getElementText(element));
				}
				else if (HOME.equals(nodeName)) {
					entityInfo.setHome(XMLUtil.getElementText(element));
				}
				else if (REMOTE.equals(nodeName)) {
					entityInfo.setRemote(XMLUtil.getElementText(element));
				}
				else if (EJB_CLASS.equals(nodeName)) {
					entityInfo.setEjbClass(XMLUtil.getElementText(element));
				}
				else if (PRIM_KEY_CLASS.equals(nodeName)) {
					entityInfo.setPrimaryKeyClass(XMLUtil.getElementText(element));
				}
				else if (CMP_VERSION.equals(nodeName)) {
					entityInfo.setVersion(XMLUtil.getElementText(element));
				}
				else if (CMP_FIELD.equals(nodeName)) {
					parseFieldElement(element, entityInfo);
				}
				else if (PRIM_KEY_FIELD.equals(nodeName)) {
					entityInfo.setPrimaryKeyField(XMLUtil.getElementText(element));
				}
				else if (LOCAL_HOME.equals(nodeName)) {
					entityInfo.setLocalHome(XMLUtil.getElementText(element));
				}
				else if (LOCAL.equals(nodeName)) {
					entityInfo.setLocal(XMLUtil.getElementText(element));
				}
				else if (ABSTRACT_SCHEMA_NAME.equals(nodeName)) {
					entityInfo.setAbstractSchemaName(XMLUtil.getElementText(element));
				}
				else if (EJB_LOCAL_REF.equals(nodeName)) {
					parseEjbLocalRef(element, entityInfo);
				}
				else if (QUERY.equals(nodeName)) {
					parseQuery(element, entityInfo);
				}
			}
		}
		if(entityInfo != null && !entityInfo.getEjbClass().matches(GenerationProperties.ARTIFACT_CLASS_QUALIFIER)) {
			iModuleInfo.getEntities().remove(entityInfo);
		}
	}
	
	private void parseFieldElement(Element fieldElement, EntityInfo entityInfo) {
		String fieldName = null;
		String fieldId = null;
		if (fieldElement.hasAttribute(ID)) {
			fieldId = fieldElement.getAttribute(ID);
		}
		NodeList childNodes = fieldElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (FIELD_NAME.equals(nodeName)) {
					fieldName = XMLUtil.getElementText(element);
				}
			}
		}
		if (fieldId == null) {
			fieldId = fieldName;
		}
		if (fieldId != null) {
			FieldInfo fieldInfo = entityInfo.getFieldInfo(fieldId, true);
			fieldInfo.setFieldName(fieldName);
		}
	}
	
	private void parseEjbLocalRef(Element ejbLocalRefElement, EntityInfo entityInfo) {
		EjbLocalRefInfo ejbLocalRefInfo = entityInfo.getEjbLocalRefInfo(ejbLocalRefElement.getAttribute(ID), true);
		NodeList childNodes = ejbLocalRefElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_REF_NAME.equals(nodeName)) {
					ejbLocalRefInfo.setEjbRefName(XMLUtil.getElementText(element));
				}
				else if (EJB_REF_TYPE.equals(nodeName)) {
					ejbLocalRefInfo.setEjbRefType(XMLUtil.getElementText(element));
				}
				else if (LOCAL_HOME.equals(nodeName)) {
					ejbLocalRefInfo.setLocalHome(XMLUtil.getElementText(element));
				}
				else if (LOCAL.equals(nodeName)) {
					ejbLocalRefInfo.setLocal(XMLUtil.getElementText(element));
				}
				else if (EJB_LINK.equals(nodeName)) {
					ejbLocalRefInfo.setEjbLink(XMLUtil.getElementText(element));
				}
			}
		}
	}
	
	private void parseQuery(Element queryElement, EntityInfo entityInfo) {
//	<query>
//		<description></description>
//		<query-method>
//			<method-name>findPasswordHistoryByUserIdOrderByPasswordCreation</method-name>
//			<method-params>
//				<method-param>java.lang.Long</method-param>
//			</method-params>
//		</query-method>
//		<ejb-ql>select object(o) from UserPasswordHistory o where o.userId = ?1 order by o.passwordCreation</ejb-ql>
//	</query>
		String finderQuery = null;
		FinderInfo finderInfo = null;
		NodeList childNodes = queryElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (QUERY_METHOD.equals(nodeName)) {
					finderInfo = parseQueryMethod(element, entityInfo);
				}
				else if (EJB_QL.equals(nodeName)) {
					finderQuery = XMLUtil.getElementText(element);
				}
			}
		}
		if (finderQuery == null) {
			System.out.println("null finderQuery "+finderInfo.getFinderMethodName());
		}
		finderInfo.setFinderQuery(finderQuery);
	}
	
	private FinderInfo parseQueryMethod(Element queryMethodElement, EntityInfo entityInfo) {
		String finderMethodName = null;
		String[] parameterTypes = null;
		NodeList childNodes = queryMethodElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (METHOD_NAME.equals(nodeName)) {
					finderMethodName = XMLUtil.getElementText(element);
				}
				else if (METHOD_PARAMS.equals(nodeName)) {
					parameterTypes = parseMethodParams(element);
				}
			}
		}
		return entityInfo.getFinderInfo(finderMethodName, parameterTypes, true);
	}
	
	private String[] parseMethodParams(Element methodParamsElement) {
		List<String> parameterTypes = new ArrayList<String>();
		NodeList childNodes = methodParamsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (METHOD_PARAM.equals(nodeName)) {
					parameterTypes.add(XMLUtil.getElementText(element));
				}
			}
		}
		return (String[]) parameterTypes.toArray(new String[parameterTypes.size()]);
	}
	
	private void parseRelationshipsElement(Element relationshipsElement) {
		NodeList childNodes = relationshipsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_RELATION.equals(nodeName)) {
					parseEjbRelationElement(element);
				}
			}
		}
	}

	private void parseEjbRelationElement(Element ejbRelationElement) {
		EjbRelationInfo ejbRelationInfo = new EjbRelationInfo();
		NodeList childNodes = ejbRelationElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_RELATION_NAME.equals(nodeName)) {
					ejbRelationInfo.setEjbRelationName(XMLUtil.getElementText(element));
				}
				else if (EJB_RELATIONSHIP_ROLE.equals(nodeName)) {
					parseEjbRelationshipRoleElement(element, ejbRelationInfo);
				}
			}
		}
		EjbRelationshipRoleInfo ejbRelationshipRoleInfo1 = ejbRelationInfo.getEjbRelationshipRoles().get(0);
		EjbRelationshipRoleInfo ejbRelationshipRoleInfo2 = ejbRelationInfo.getEjbRelationshipRoles().get(1);
		if (ejbRelationshipRoleInfo1.getFieldType() == null) {
			ejbRelationshipRoleInfo1.setFieldType(ejbRelationshipRoleInfo2.getEntityInfo().getLocal());
		}
		if (ejbRelationshipRoleInfo2.getFieldType() == null) {
			ejbRelationshipRoleInfo2.setFieldType(ejbRelationshipRoleInfo1.getEntityInfo().getLocal());
		}
		ejbRelationshipRoleInfo1.setRelatedEntityInfo(ejbRelationshipRoleInfo2.getEntityInfo());
		ejbRelationshipRoleInfo2.setRelatedEntityInfo(ejbRelationshipRoleInfo1.getEntityInfo());
		ejbRelationshipRoleInfo1.setRelatedMultiplicity(ejbRelationshipRoleInfo2.getMultiplicity());
		ejbRelationshipRoleInfo2.setRelatedMultiplicity(ejbRelationshipRoleInfo1.getMultiplicity());
		ejbRelationshipRoleInfo1.setRelatedFieldName(ejbRelationshipRoleInfo2.getFieldName());
		ejbRelationshipRoleInfo2.setRelatedFieldName(ejbRelationshipRoleInfo1.getFieldName());
	}
	
	private void parseEjbRelationshipRoleElement(Element ejbRelationshipRoleElement, EjbRelationInfo ejbRelationInfo) {
		EjbRelationshipRoleInfo ejbRelationshipRoleInfo = iModuleInfo.getEjbRelationshipRoleInfo(ejbRelationshipRoleElement.getAttribute(ID), true);
		ejbRelationshipRoleInfo.setEjbRelationInfo(ejbRelationInfo);
		ejbRelationInfo.addEjbRelationshipRole(ejbRelationshipRoleInfo);
		NodeList childNodes = ejbRelationshipRoleElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_RELATIONSHIP_ROLE_NAME.equals(nodeName)) {
					ejbRelationshipRoleInfo.setEjbRelationshipRoleName(XMLUtil.getElementText(element));
				}
				else if (MULTIPLICITY.equals(nodeName)) {
					ejbRelationshipRoleInfo.setMultiplicity(XMLUtil.getElementText(element));
				}
				else if (CASCADE_DELETE.equals(nodeName)) {
					ejbRelationshipRoleInfo.setCascadeDelete(true);
				}
				else if (RELATIONSHIP_ROLE_SOURCE.equals(nodeName)) {
					parseRelationshipRoleSourceElement(element, ejbRelationshipRoleInfo);
				}
				else if (CMR_FIELD.equals(nodeName)) {
					parseCmrFieldElement(element, ejbRelationshipRoleInfo);					
				}
			}
		}
		EntityInfo entityInfo = iModuleInfo.getEntityInfoByName(ejbRelationshipRoleInfo.getEjbName());
		entityInfo.addEjbRelationsihpRoleInfo(ejbRelationshipRoleInfo);
		ejbRelationshipRoleInfo.setEntityInfo(entityInfo);
	}
	
	private void parseRelationshipRoleSourceElement(Element relationshipRoleSourceElement, EjbRelationshipRoleInfo ejbRelationshipRoleInfo) {
		NodeList childNodes = relationshipRoleSourceElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_NAME.equals(nodeName)) {
					ejbRelationshipRoleInfo.setEjbName(XMLUtil.getElementText(element));
				}
			}
		}
	}
	
	private void parseCmrFieldElement(Element cmrFieldElement, EjbRelationshipRoleInfo ejbRelationshipRoleInfo) {
		NodeList childNodes = cmrFieldElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (CMR_FIELD_NAME.equals(nodeName)) {
					ejbRelationshipRoleInfo.setFieldName(XMLUtil.getElementText(element));
				}
				else if (CMR_FIELD_TYPE.equals(nodeName)) {
					ejbRelationshipRoleInfo.setFieldType(XMLUtil.getElementText(element));
				}
			}
		}
	}
}
