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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.info.TargetExceptionInfo;
import com.ibm.commerce.jpa.port.util.AccessBeanUtil;
import com.ibm.commerce.jpa.port.util.ApplicationInfoUtil;
import com.ibm.commerce.jpa.port.util.EntityReferenceUtil;
import com.ibm.commerce.jpa.port.util.FinderResultCacheUtil;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class ProjectInfoXmlParser {
	private static final String ENTITY_REFERENCING_TYPE = "EntityReferencingType";
	private static final String INDIRECT_ENTITY_REFERENCING_TYPE = "IndirectEntityReferencingType";
	private static final String ENTITY_REFERENCE_SUBCLASS = "EntityReferenceSubclass";
	private static final String ACCESS_BEAN_SUBCLASS_INFO = "AccessBeanSubclassInfo";
	private static final String NAME = "name";
	private static final String SUPERCLASS_NAME = "superclassName";
	private static final String METHOD_INFO = "MethodInfo";
	private static final String METHOD_KEY = "MethodKey";
	private static final String TARGET_EXCEPTION = "TargetException";
	private static final String SOURCE_EXCEPTION = "SourceException";
	
	private ProjectInfo iProjectInfo;
	private ApplicationInfo iApplicationInfo;
	private IProject iProject;
	private IJavaProject iJavaProject;
	
	public ProjectInfoXmlParser(ProjectInfo projectInfo) {
		iProjectInfo = projectInfo;
		iApplicationInfo = projectInfo.getApplicationInfo();
		iProject = projectInfo.getProject();
		iJavaProject = projectInfo.getJavaProject();
	}
	
	public void parse(IProgressMonitor progressMonitor) {
		parseProjectInfoFile(".jpaAccessBeanSubclassInfo.xml");
		parseProjectInfoFile(".jpaEntityReferences.xml");
		parseProjectInfoFile(".jpaEntityReferenceSubclasses.xml");
	}
	
	public void parseProjectInfoFile(String fileName) {
		IFile projectInfoXmlFile = iProject.getFile(fileName);
		if (projectInfoXmlFile.exists()) {
			Document document = iApplicationInfo.getXMLUtil(iProject).readXml(projectInfoXmlFile);
			parseModuleInfo(document.getDocumentElement());
			iApplicationInfo.incrementParsedAssetCount();
		}
	}
	
	private void parseModuleInfo(Element moduleInfoElement) {
		NodeList childNodes = moduleInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTITY_REFERENCING_TYPE.equals(nodeName)) {
					String typeName = XMLUtil.getElementText(element);
					iApplicationInfo.addDeleteIntendedType(typeName);
					try {
						IType type = iJavaProject.findType(typeName);
						if (type == null) {
							System.out.println("unable to find "+typeName);
						}
						if (!FinderResultCacheUtil.isFinderResultCacheUtil(type) && !EntityReferenceUtil.isPortExemptEntityReferencingType(type)) {
							String simpleTypeName = type.getTypeQualifiedName('.');
							if (simpleTypeName.startsWith("_")) {
								iProjectInfo.addIndirectEntityReferencingType(typeName);
								ApplicationInfoUtil.addJpaStubTypeMapping(iApplicationInfo, type);
							}
							else {
								iProjectInfo.addEntityReferencingType(typeName);
								ApplicationInfoUtil.addJpaTypeMapping(iApplicationInfo, type);
							}
						}
						else {
							iProjectInfo.addDeleteIntendedType(typeName);
						}
					}
					catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				else if (INDIRECT_ENTITY_REFERENCING_TYPE.equals(nodeName)) {
					String typeName = XMLUtil.getElementText(element);
					try {
						IType type = iJavaProject.findType(typeName);
						iProjectInfo.addIndirectEntityReferencingType(typeName);
						ApplicationInfoUtil.addJpaStubTypeMapping(iApplicationInfo, type);
					}
					catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				else if (ENTITY_REFERENCE_SUBCLASS.equals(nodeName)) {
					String typeName = XMLUtil.getElementText(element);
					iApplicationInfo.addDeleteIntendedType(typeName);
					iProjectInfo.addEntityReferenceSubclass(typeName);
					try {
						IType type = iJavaProject.findType(typeName);
						iProjectInfo.addIndirectEntityReferencingType(type.getFullyQualifiedName('.'));
						ApplicationInfoUtil.addJpaStubTypeMapping(iApplicationInfo, type);
					}
					catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
				else if (ACCESS_BEAN_SUBCLASS_INFO.equals(nodeName)) {
					AccessBeanSubclassInfo accessBeanSubclassInfo = parseAccessBeanSubclassInfo(element);
					EntityInfo entityInfo = accessBeanSubclassInfo.getEntityInfo();
					String typeName = accessBeanSubclassInfo.getName();
					if (entityInfo != null) {
						iApplicationInfo.setEntityInfoForType(typeName, entityInfo);
					}
					try {
						IType type = iJavaProject.findType(typeName);
						if (type == null) {
							System.out.println("unable to find type " + typeName);
						}
						while (type.getDeclaringType() != null) {
							type = type.getDeclaringType();
						}
						iApplicationInfo.addDeleteIntendedType(type.getFullyQualifiedName('.'));
						if (!AccessBeanUtil.isPortExemptAccessBeanSubclass(type)) {
							ApplicationInfoUtil.addJpaTypeMapping(iApplicationInfo, type);
							iProjectInfo.addEntityReferencingType(type.getFullyQualifiedName('.'));
						}
						else {
							iApplicationInfo.addTypeMapping(typeName, entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
						}
					}
					catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private AccessBeanSubclassInfo parseAccessBeanSubclassInfo(Element accessBeanSubclassInfoElement) {
		AccessBeanSubclassInfo accessBeanSubclassInfo = iProjectInfo.getAccessBeanSubclassInfo(accessBeanSubclassInfoElement.getAttribute(NAME), true);
		if (accessBeanSubclassInfoElement.hasAttribute(SUPERCLASS_NAME)) {
			String superclassName = accessBeanSubclassInfoElement.getAttribute(SUPERCLASS_NAME);
			try {
				IType superclassType = iProjectInfo.getJavaProject().findType(superclassName);
				if (superclassType == null) {
					System.out.println("unable to find type "+superclassType);
				}
				ProjectInfo superclassProjectInfo = iApplicationInfo.getProjectInfo(superclassType.getJavaProject().getProject());
				AccessBeanSubclassInfo superclass = superclassProjectInfo.getAccessBeanSubclassInfo(superclassType.getFullyQualifiedName('.'), true);
				accessBeanSubclassInfo.setSuperclass(superclass);
				superclass.addSubclass(accessBeanSubclassInfo);
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		NodeList childNodes = accessBeanSubclassInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (METHOD_INFO.equals(nodeName)) {
					parseMethodInfo(accessBeanSubclassInfo, element);
				}
			}
		}
		return accessBeanSubclassInfo;
	}
	
	private void parseMethodInfo(AccessBeanSubclassInfo accessBeanSubclassInfo, Element methodInfoElement) {
		String methodKey = null;
		TargetExceptionInfo targetExceptionInfo = new TargetExceptionInfo();
		NodeList childNodes = methodInfoElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (METHOD_KEY.equals(nodeName)) {
					methodKey = XMLUtil.getElementText(element);
				}
				else if (TARGET_EXCEPTION.equals(nodeName)) {
					String exception = element.getAttribute(NAME);
					targetExceptionInfo.addTargetException(exception);
				}
				else if (SOURCE_EXCEPTION.equals(nodeName)) {
					String exception = element.getAttribute(NAME);
					targetExceptionInfo.addSourceException(exception);
				}
			}
		}
		accessBeanSubclassInfo.setMethodUnhandledTargetExceptions(methodKey, targetExceptionInfo);
	}
}
