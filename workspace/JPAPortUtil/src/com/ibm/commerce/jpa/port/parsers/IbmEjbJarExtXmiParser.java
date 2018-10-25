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

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FinderInfo;
import com.ibm.commerce.jpa.port.info.MethodInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class IbmEjbJarExtXmiParser {
	private static final String EJB_EXTENSIONS = "ejbExtensions";
	private static final String ID = "xmi:id";
	private static final String NAME = "name";
	private static final String CONCURRENCY_CONTROL = "concurrencyControl";
	private static final String TYPE = "type";
	private static final String PARMS = "parms";
	private static final String ENTERPRISE_BEAN = "enterpriseBean";
	private static final String HREF = "href";
	private static final String FINDER_DESCRIPTORS = "finderDescriptors";
	private static final String XMI_TYPE = "xmi:type";
	private static final String WHERE_CLAUSE = "whereClause";
	private static final String SELECT_STATEMENT = "selectStatement";
	private static final String EJB_QL_QUERY_STRING = "ejbqlQueryString";
	private static final String FINDER_METHOD_ELEMENTS = "finderMethodElements";
	private static final String ACCESS_INTENTS = "accessIntents";
	private static final String INTENT_TYPE = "intentType";
	private static final String METHOD_ELEMENTS = "methodElements";
	private static final String GENERALIZATIONS = "generalizations";
	private static final String SUBTYPE = "subtype";
	private static final String SUPERTYPE = "supertype";
	private static final String FOR_UPDATE_WITH_RS = "FOR UPDATE WITH RS";
	private static final String FOR_UPDATE = "FOR UPDATE";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	/**
	 * This will parse the EJB deployment descriptor extension xml file.  It adds this information into the entityInfo objects
	 * which are contained in the ModuleInfo.  The entityInfo objects are originally created by the EjbJarXmlParser.
	 * @param file
	 * @param moduleInfo
	 */
	public IbmEjbJarExtXmiParser(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void parse() {
		Document document = iModuleInfo.getXMLUtil().readXml(iFile);
		parseEJBJarExtensionElement(document.getDocumentElement());
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseEJBJarExtensionElement(Element ejbJarExtensionElement) {
		NodeList childNodes = ejbJarExtensionElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_EXTENSIONS.equals(nodeName)) {
					parseEjbExtensionsElement(element);
				}
				else if (GENERALIZATIONS.equals(nodeName)) {
					// ARVERA: Skipping generalization
					//parseGeneralizationsElement(element);
				}
			}
		}
	}
	
	private void parseEjbExtensionsElement(Element ejbExtensionsElement) {
		EntityInfo entityInfo = null;
		NodeList childNodes = ejbExtensionsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTERPRISE_BEAN.equals(nodeName)) {
					entityInfo = parseEnterpriseBeanElement(element);
				}
			}
		}
		if (entityInfo != null && ejbExtensionsElement.hasAttribute(CONCURRENCY_CONTROL)) {
			entityInfo.setConcurrencyControl(ejbExtensionsElement.getAttribute(CONCURRENCY_CONTROL));
		}
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (FINDER_DESCRIPTORS.equals(nodeName) && entityInfo != null) {
					parseFinderDescriptorsElement(element, entityInfo);
				}
				else if (ACCESS_INTENTS.equals(nodeName) && entityInfo != null) {
					parseAccessIntentsElement(element, entityInfo);
				}
			}
		}
	}
	
	private EntityInfo parseEnterpriseBeanElement(Element enterpriseBeanElement) {
		EntityInfo entityInfo = null;
		String href = enterpriseBeanElement.getAttribute(HREF);
		int index = href.indexOf('#');
		if (index > -1) {
			String entityId = href.substring(index + 1);
			entityInfo = iModuleInfo.getEntityInfo(entityId);
		}
		return entityInfo;
	}
	
	private void parseFinderDescriptorsElement(Element finderDescriptorsElement, EntityInfo entityInfo) {
		FinderInfo finderInfo = entityInfo.getFinderInfo(finderDescriptorsElement.getAttribute(ID), true);
		if (finderDescriptorsElement.hasAttribute(XMI_TYPE)) {
			finderInfo.setFinderType(finderDescriptorsElement.getAttribute(XMI_TYPE));
		}
		if (finderDescriptorsElement.hasAttribute(WHERE_CLAUSE)) {
			String finderWhereClause = finderDescriptorsElement.getAttribute(WHERE_CLAUSE);
			finderInfo.setFinderWhereClause(finderWhereClause);
			if (finderWhereClause.contains(FOR_UPDATE_WITH_RS)) {
				finderInfo.setOracleFinderWhereClause(finderWhereClause.replaceAll(FOR_UPDATE_WITH_RS, FOR_UPDATE));
			}
		}
		if (finderDescriptorsElement.hasAttribute(SELECT_STATEMENT)) {
			finderInfo.setFinderSelectStatement(finderDescriptorsElement.getAttribute(SELECT_STATEMENT));
		}
		if (finderDescriptorsElement.hasAttribute(EJB_QL_QUERY_STRING)) {
			finderInfo.setFinderQuery(finderDescriptorsElement.getAttribute(EJB_QL_QUERY_STRING));
		}
		NodeList childNodes = finderDescriptorsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (FINDER_METHOD_ELEMENTS.equals(nodeName)) {
					parseFinderMethodElementsElement(element, finderInfo);
				}
			}
		}
	}
	
	private void parseFinderMethodElementsElement(Element finderMethodElementsElement, FinderInfo finderInfo) {
		if (finderMethodElementsElement.hasAttribute(NAME)) {
			finderInfo.setFinderMethodName(finderMethodElementsElement.getAttribute(NAME));
		}
		if (finderMethodElementsElement.hasAttribute(PARMS)) {
			finderInfo.setFinderMethodParameterTypes(finderMethodElementsElement.getAttribute(PARMS));
		}
		if (finderMethodElementsElement.hasAttribute(TYPE)) {
			finderInfo.setFinderMethodType(finderMethodElementsElement.getAttribute(TYPE));
		}
	}
	
	private void parseAccessIntentsElement(Element accessIntentsElement, EntityInfo entityInfo) {
		String intentType = accessIntentsElement.getAttribute(INTENT_TYPE);
		NodeList childNodes = accessIntentsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (METHOD_ELEMENTS.equals(nodeName)) {
					MethodInfo methodInfo = new MethodInfo(element.getAttribute(NAME), element.getAttribute(PARMS));
					if (element.hasAttribute(TYPE)) {
						methodInfo.setMethodType(element.getAttribute(TYPE));
					}
					entityInfo.addAccessIntentMethod(intentType, methodInfo);
				}
			}
		}
	}
	
	private void parseGeneralizationsElement(Element generalizationsElement) {
		EntityInfo subtype = null;
		EntityInfo supertype = null;
		NodeList childNodes = generalizationsElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (SUBTYPE.equals(nodeName)) {
					subtype = parseEnterpriseBeanElement(element);
				}
				else if (SUPERTYPE.equals(nodeName)) {
					supertype = parseEnterpriseBeanElement(element);
				}
			}
		}
		subtype.setSuperType(supertype);
		supertype.addSubtype(subtype);
	}
}
