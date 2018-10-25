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

import com.ibm.commerce.jpa.port.info.AccessBeanInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class IbmEjbAccessBeanXmiParser {
	private static final String EJB_SHADOW = "accessbean:EJBShadow";
	private static final String ID = "xmi:id";
	private static final String XMI_TYPE = "xmi:type";
	private static final String ACCESSBEAN_DATA_CLASS = "accessbean:DataClass";
	private static final String NAME = "name";
	private static final String ACCESS_BEANS = "accessBeans";
	private static final String PACKAGE = "package";
	private static final String COPY_HELPER_PROPERTIES = "copyHelperProperties";
	private static final String EXCLUDED_PROPERTIES = "excludedProperties";
	private static final String TYPE = "type";
	private static final String GETTER_NAME = "getterName";
	private static final String SETTER_NAME = "setterName";
	private static final String CONVERTER_CLASS_NAME = "converterClassName";
	private static final String NULL_CONSTRUCTOR = "nullConstructor";
	private static final String NULL_CONSTRUCTOR_PARAMETERS = "nullConstructorParameters";
	private static final String PARMS = "parms";
	private static final String ENTERPRISE_BEAN = "enterpriseBean";
	private static final String HREF = "href";
	private static final String IS_FIELD_FROM_KEY = "isFieldFromKey";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	public IbmEjbAccessBeanXmiParser(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void parse() {
		Document document = iModuleInfo.getXMLUtil().readXml(iFile);
		Element documentElement = document.getDocumentElement();
		if (EJB_SHADOW.equals(documentElement.getNodeName())) {
			parseEJBShadowElement(documentElement);
		}
		else {
			parseXMIElement(document.getDocumentElement());
		}
		iModuleInfo.getApplicationInfo().incrementParsedAssetCount();
	}
	
	private void parseXMIElement(Element documentElement) {
		NodeList childNodes = documentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_SHADOW.equals(nodeName)) {
					parseEJBShadowElement(element);
				}
			}
		}
	}
	
	private void parseEJBShadowElement(Element ejbShadowElement) {
		AccessBeanInfo accessBeanInfo = new AccessBeanInfo(ejbShadowElement.getAttribute(NAME));
		NodeList childNodes = ejbShadowElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ACCESS_BEANS.equals(nodeName)) {
					parseAccessBeansElement(element, accessBeanInfo);
				}
				else if (ENTERPRISE_BEAN.equals(nodeName)) {
					EntityInfo entityInfo = parseEnterpriseBeanElement(element);
					if (entityInfo != null) {
						entityInfo.setAccessBeanInfo(accessBeanInfo);
					}
				}
			}
		}
	}
	
	private void parseAccessBeansElement(Element accessBeansElement, AccessBeanInfo accessBeanInfo) {
		if (accessBeansElement.hasAttribute(NAME)) {
			accessBeanInfo.setAccessBeanName(accessBeansElement.getAttribute(NAME));
		}
		if (accessBeansElement.hasAttribute(PACKAGE)) {
			accessBeanInfo.setAccessBeanPackage(accessBeansElement.getAttribute(PACKAGE));
		}
		if (accessBeansElement.hasAttribute(XMI_TYPE)) {
			accessBeanInfo.setDataClassType(ACCESSBEAN_DATA_CLASS.equals(accessBeansElement.getAttribute(XMI_TYPE)));
		}
		NodeList childNodes = accessBeansElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EXCLUDED_PROPERTIES.equals(nodeName)) {
					parseExcludedPropertiesElement(element, accessBeanInfo);
				}
				else if (COPY_HELPER_PROPERTIES.equals(nodeName)) {
					parseCopyHelperPropertiesElement(element, accessBeanInfo);
				}
				else if (NULL_CONSTRUCTOR.equals(nodeName)) {
					parseNullConstructorElement(element, accessBeanInfo);
				}
				else if (NULL_CONSTRUCTOR_PARAMETERS.equals(nodeName)) {
					parseNullConstructorParametersElement(element, accessBeanInfo);
				}
			}
		}
	}
	
	private void parseExcludedPropertiesElement(Element excludedPropertiesElement, AccessBeanInfo accessBeanInfo) {
		AccessBeanInfo.CopyHelperProperty excludedProperty = accessBeanInfo.getExcludedProperty(excludedPropertiesElement.getAttribute(NAME), true);
		if (excludedPropertiesElement.hasAttribute(TYPE)) {
			excludedProperty.setType(excludedPropertiesElement.getAttribute(TYPE));
		}
		if (excludedPropertiesElement.hasAttribute(GETTER_NAME)) {
			excludedProperty.setGetterName(excludedPropertiesElement.getAttribute(GETTER_NAME));
		}
		if (excludedPropertiesElement.hasAttribute(SETTER_NAME)) {
			excludedProperty.setSetterName(excludedPropertiesElement.getAttribute(SETTER_NAME));
		}
		if (excludedPropertiesElement.hasAttribute(CONVERTER_CLASS_NAME)) {
			excludedProperty.setConverterClassName(excludedPropertiesElement.getAttribute(CONVERTER_CLASS_NAME));
		}
	}
	
	private void parseCopyHelperPropertiesElement(Element copyHelperPropertiesElement, AccessBeanInfo accessBeanInfo) {
		AccessBeanInfo.CopyHelperProperty copyHelperProperty = accessBeanInfo.getCopyHelperProperty(copyHelperPropertiesElement.getAttribute(NAME), true);
		if (copyHelperPropertiesElement.hasAttribute(TYPE)) {
			copyHelperProperty.setType(copyHelperPropertiesElement.getAttribute(TYPE));
		}
		if (copyHelperPropertiesElement.hasAttribute(GETTER_NAME)) {
			copyHelperProperty.setGetterName(copyHelperPropertiesElement.getAttribute(GETTER_NAME));
		}
		if (copyHelperPropertiesElement.hasAttribute(SETTER_NAME)) {
			copyHelperProperty.setSetterName(copyHelperPropertiesElement.getAttribute(SETTER_NAME));
		}
		if (copyHelperPropertiesElement.hasAttribute(CONVERTER_CLASS_NAME)) {
			copyHelperProperty.setConverterClassName(copyHelperPropertiesElement.getAttribute(CONVERTER_CLASS_NAME));
		}
	}
	
	private void parseNullConstructorElement(Element nullConstructorElement, AccessBeanInfo accessBeanInfo) {
		AccessBeanInfo.NullConstructor nullConstructor = accessBeanInfo.getNullConstructor(nullConstructorElement.getAttribute(ID), true);
		if (nullConstructorElement.hasAttribute(NAME)) {
			nullConstructor.setName(nullConstructorElement.getAttribute(NAME));
		}
		if (nullConstructorElement.hasAttribute(PARMS)) {
			nullConstructor.setParms(nullConstructorElement.getAttribute(PARMS));
		}
		if (nullConstructorElement.hasAttribute(TYPE)) {
			nullConstructor.setParms(nullConstructorElement.getAttribute(TYPE));
		}
		NodeList childNodes = nullConstructorElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTERPRISE_BEAN.equals(nodeName)) {
					nullConstructor.setEntityInfo(parseEnterpriseBeanElement(element));
				}
			}
		}
	}
	
	private void parseNullConstructorParametersElement(Element nullConstructorParametersElement, AccessBeanInfo accessBeanInfo) {
		AccessBeanInfo.NullConstructorParameter nullConstructorParameter = accessBeanInfo.getNullConstructorParameter(nullConstructorParametersElement.getAttribute(ID), true);
		if (nullConstructorParametersElement.hasAttribute(NAME)) {
			nullConstructorParameter.setName(nullConstructorParametersElement.getAttribute(NAME));
		}
		if (nullConstructorParametersElement.hasAttribute(TYPE)) {
			nullConstructorParameter.setType(nullConstructorParametersElement.getAttribute(TYPE));
		}
		if (nullConstructorParametersElement.hasAttribute(CONVERTER_CLASS_NAME)) {
			nullConstructorParameter.setConverterClassName(nullConstructorParametersElement.getAttribute(CONVERTER_CLASS_NAME));
		}
		nullConstructorParameter.setIsFieldFromKey("true".equals(nullConstructorParametersElement.getAttribute(IS_FIELD_FROM_KEY)));
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
