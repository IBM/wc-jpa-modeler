package com.ibm.commerce.jpa.updaters;

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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class EjbJarXmlUpdater {
	private static final String ENTERPRISE_BEANS = "enterprise-beans";
	private static final String ENTITY = "entity";
	private static final String RELATIONSHIPS = "relationships";
	private static final String DISPLAY_NAME = "display-name";
	private static final String ASSEMBLY_DESCRIPTOR = "assembly-descriptor";
	private static final String SECURITY_ROLE = "security-role";
	private static final String METHOD_PERMISSION = "method-permission";
	private static final String METHOD = "method";
	private static final String EJB_NAME = "ejb-name";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	public EjbJarXmlUpdater(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void update(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("update " + iFile.getName(), 300);
			Document document = iModuleInfo.getXMLUtil().readXml(iFile);
			Element documentElement = document.getDocumentElement();
			boolean deleteFile = updateEjbJarElement(documentElement);
			progressMonitor.worked(100);
			BackupUtil backupUtil = iModuleInfo.getApplicationInfo().getBackupUtil(iModuleInfo.getJavaProject().getProject());
			backupUtil.backupFile3(iFile, new SubProgressMonitor(progressMonitor, 100));
			if (deleteFile) {
				iFile.delete(true, new SubProgressMonitor(progressMonitor, 100));
			}
			else {
				iModuleInfo.getXMLUtil().writeXml(document, iFile, new SubProgressMonitor(progressMonitor, 100));
			}
			iModuleInfo.getApplicationInfo().incrementUpdateCount();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private boolean updateEjbJarElement(Element documentElement) {
		boolean deleteFile = true;
		NodeList childNodes = documentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTERPRISE_BEANS.equals(nodeName)) {
					if (!updateEnterpriseBeansElement(element)) {
						deleteFile = false;
					}
				}
				else if (RELATIONSHIPS.equals(nodeName)) {
					i = XMLUtil.removeElement(element);
				}
				else if (DISPLAY_NAME.equals(nodeName)) {
					// ignore
				}
				else if (ASSEMBLY_DESCRIPTOR.equals(nodeName)) {
					if (updateAssemblyDescriptorElement(element)) {
						i = XMLUtil.removeElement(element);
					}
				}
				else {
					deleteFile = false;
				}
			}
		}
		return deleteFile;
	}
	
	private boolean updateEnterpriseBeansElement(Element enterpriseBeansElement) {
		boolean deleteFile = true;
		NodeList childNodes = enterpriseBeansElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (ENTITY.equals(nodeName)) {
					i = XMLUtil.removeElement(element);
				}
				else {
					deleteFile = false;
				}
			}
		}
		return deleteFile;
	}
	
	private boolean updateAssemblyDescriptorElement(Element assemblyDescriptorElement) {
		boolean deleteAssemblyDescriptor = true;
		NodeList childNodes = assemblyDescriptorElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (SECURITY_ROLE.equals(nodeName)) {
					deleteAssemblyDescriptor = false;
				}
				else if (METHOD_PERMISSION.equals(nodeName)) {
					if (updateMethodPermissionElement(element)) {
						i = XMLUtil.removeElement(element);
					}
					else {
						deleteAssemblyDescriptor = false;
					}
				}
				else {
					deleteAssemblyDescriptor = false;
				}
			}
		}
		return deleteAssemblyDescriptor;
	}
	
	private boolean updateMethodPermissionElement(Element methodPermissionElement) {
		boolean deleteMethodPermissionElement = true;
		NodeList childNodes = methodPermissionElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (METHOD.equals(nodeName)) {
					if (checkMethodElement(element)) {
						i = XMLUtil.removeElement(element);
					}
					else {
						deleteMethodPermissionElement = false;
					}
				}
			}
		}
		return deleteMethodPermissionElement;
	}
	
	private boolean checkMethodElement(Element methodElement) {
		boolean removeMethodElement = false;
		NodeList childNodes = methodElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_NAME.equals(nodeName)) {
					if (iModuleInfo.getEntityInfoByName(XMLUtil.getElementText(element)) != null) {
						removeMethodElement = true;
					}
				}
			}
		}
		return removeMethodElement;
	}
}
