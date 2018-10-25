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

public class IbmEjbAccessBeanXmiUpdater {
	private static final String EJB_SHADOW = "accessbean:EJBShadow";
	private static final String ENTERPRISE_BEAN = "enterpriseBean";
	private static final String HREF = "href";
	
	private IFile iFile;
	private ModuleInfo iModuleInfo;
	
	public IbmEjbAccessBeanXmiUpdater(IFile file, ModuleInfo moduleInfo) {
		iFile = file;
		iModuleInfo = moduleInfo;
	}
	
	public void update(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("update " + iFile.getName(), 300);
			boolean deleteFile = false;
			Document document = iModuleInfo.getXMLUtil().readXml(iFile);
			Element documentElement = document.getDocumentElement();
			if (EJB_SHADOW.equals(documentElement.getNodeName())) {
				EntityInfo entityInfo = parseEJBShadowElement(documentElement);
				if (entityInfo != null) {
					deleteFile = true;
				}
			}
			else {
				deleteFile = updateXMIElement(documentElement);
			}
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
	
	private boolean updateXMIElement(Element documentElement) {
		boolean deleteFile = true;
		NodeList childNodes = documentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (EJB_SHADOW.equals(nodeName)) {
					EntityInfo entityInfo = parseEJBShadowElement(element);
					if (entityInfo != null) {
						i = XMLUtil.removeElement(element);
					}
					else {
						deleteFile = false;
					}
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
