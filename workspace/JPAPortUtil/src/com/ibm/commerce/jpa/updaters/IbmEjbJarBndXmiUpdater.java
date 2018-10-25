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

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.XMLUtil;

public class IbmEjbJarBndXmiUpdater {
	private static final String EJB_BINDINGS = "ejbBindings";
	private static final String CURRENT_BACKEND_ID = "currentBackendId";
	private static final String ENTERPRISE_BEAN = "enterpriseBean";
	private static final String HREF = "href";
	private static final String DEFAULT_CMP_CONNECTION_FACTORY = "defaultCMPConnectionFactory";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	public IbmEjbJarBndXmiUpdater(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void update(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("update " + iFile.getName(), 300);
			Document document = iModuleInfo.getXMLUtil().readXml(iFile);
			Element documentElement = document.getDocumentElement();
			boolean deleteFile = updateDocumentElement(documentElement);
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
	
	private boolean updateDocumentElement(Element documentElement) {
		boolean deleteFile = true;
		if (documentElement.hasAttribute(CURRENT_BACKEND_ID)) {
			documentElement.removeAttribute(CURRENT_BACKEND_ID);
		}
		NodeList childNodes = documentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_BINDINGS.equals(nodeName)) {
					EntityInfo entityInfo = parseEJBShadowElement(element);
					if (entityInfo != null) {
						i = XMLUtil.removeElement(element);
					}
					else {
						deleteFile = false;
					}
				}
				else if (DEFAULT_CMP_CONNECTION_FACTORY.equals(nodeName)) {
					i = XMLUtil.removeElement(element);
				}
			}
		}
		return deleteFile;
	}
	
	private EntityInfo parseEJBShadowElement(Element ejbShadowElement) {
		EntityInfo entityInfo = null;
		NodeList childNodes = ejbShadowElement.getChildNodes();
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
		return entityInfo;
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
