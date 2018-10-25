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
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class IbmEjbJarBndXmiParser {
	private static final String EJB_BINDINGS = "ejbBindings";
	private static final String JNDI_NAME = "jndiName";
	private static final String ENTERPRISE_BEAN = "enterpriseBean";
	private static final String HREF = "href";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	/**
	 * This will parse the EJB deployment descriptor bindings xml file.  It adds this information into the entityInfo objects
	 * which are contained in the ModuleInfo.  The entityInfo objects are originally created by the EjbJarXmlParser.
	 * 
	 * This basically only parses the JNDI name for the beans.
	 * 
	 * @param file
	 * @param moduleInfo
	 */
	public IbmEjbJarBndXmiParser(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void parse() {
		Document document = iModuleInfo.getXMLUtil().readXml(iFile);
		parseEJBJarBindingElement(document.getDocumentElement());
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseEJBJarBindingElement(Element ejbJarExtensionElement) {
		NodeList childNodes = ejbJarExtensionElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_BINDINGS.equals(nodeName)) {
					parseEjbBindingsElement(element);
				}
			}
		}
	}
	
	private void parseEjbBindingsElement(Element ejbBindingsElement) {
		String jndiName = null;
		if (ejbBindingsElement.hasAttribute(JNDI_NAME)) {
			jndiName = ejbBindingsElement.getAttribute(JNDI_NAME);
		}
		EntityInfo entityInfo = null;
		NodeList childNodes = ejbBindingsElement.getChildNodes();
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
		if (jndiName != null && entityInfo != null) {
			entityInfo.setJndiName(jndiName);
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
}
