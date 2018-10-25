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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;

public class ModuleUpdater {
	private static final String META_INF = "META-INF";
	private static final String EJB_JAR_XML = "ejb-jar.xml";
	private static final String IBM_EJB_ACCESS_BEAN_XMI = "ibm-ejb-access-bean.xmi";
	private static final String IBM_EJB_JAR_EXT_XMI = "ibm-ejb-jar-ext.xmi";
	private static final String IBM_EJB_JAR_BND_XMI = "ibm-ejb-jar-bnd.xmi";
	private static final String USER_DEFINED_CONVERTERS_CMI = "UserDefinedConverters.xmi";
	private static final String TABLE_DDL = "Table.ddl";
	private static final String INDEX_DDL = "Index.ddl";
	private static final String DROP_TABLES_DDL = "dropTables.ddl";
	private static final String BACKENDS = "backends";

	private IWorkspace iWorkspace;
	private ModuleInfo iModuleInfo;
	private BackupUtil iBackupUtil;
	private IJavaProject iJavaProject;
	
	public ModuleUpdater(IWorkspace workspace, ModuleInfo moduleInfo) {
		iWorkspace = workspace;
		iModuleInfo = moduleInfo;
		iJavaProject = moduleInfo.getJavaProject();
		iBackupUtil = moduleInfo.getApplicationInfo().getBackupUtil(iJavaProject.getProject());
	}
	
	public IStatus update(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			progressMonitor.beginTask("update " + iJavaProject.getProject().getName(), 1400);
			IClasspathEntry[] classpathEntries = iJavaProject.getResolvedClasspath(true);
			for (IClasspathEntry entry : classpathEntries) {
				if (progressMonitor.isCanceled()) {
					status = Status.CANCEL_STATUS;
					break;
				}
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath path = entry.getPath();
					IFile ejbJarXmlFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(EJB_JAR_XML));
					if (ejbJarXmlFile.exists()) {
						EjbJarXmlUpdater updater = new EjbJarXmlUpdater(ejbJarXmlFile, iModuleInfo);
						updater.update(new SubProgressMonitor(progressMonitor, 100));
					}
					IFile ibmEjbJarExtXmiFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(IBM_EJB_JAR_EXT_XMI));
					if (ibmEjbJarExtXmiFile.exists()) {
						IbmEjbJarExtXmiUpdater updater = new IbmEjbJarExtXmiUpdater(ibmEjbJarExtXmiFile, iModuleInfo);
						updater.update(new SubProgressMonitor(progressMonitor, 100));
					}
					IFile ibmEjbAccessBeanXmiFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(IBM_EJB_ACCESS_BEAN_XMI));
					if (ibmEjbAccessBeanXmiFile.exists()) {
						IbmEjbAccessBeanXmiUpdater updater = new IbmEjbAccessBeanXmiUpdater(ibmEjbAccessBeanXmiFile, iModuleInfo); 
						updater.update(new SubProgressMonitor(progressMonitor, 100));
					}
					IFile ibmEjbJarBndXmiFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(IBM_EJB_JAR_BND_XMI));
					if (ibmEjbJarBndXmiFile.exists()) {
						IbmEjbJarBndXmiUpdater updater = new IbmEjbJarBndXmiUpdater(ibmEjbJarBndXmiFile, iModuleInfo);
						updater.update(new SubProgressMonitor(progressMonitor, 100));
					}
					IFile userDefinedCreatorsFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(USER_DEFINED_CONVERTERS_CMI));
					if (userDefinedCreatorsFile.exists()) {
						iBackupUtil.backupFile3(userDefinedCreatorsFile, new SubProgressMonitor(progressMonitor, 100));
						userDefinedCreatorsFile.delete(true, new SubProgressMonitor(progressMonitor, 100));
						iModuleInfo.getApplicationInfo().incrementDeleteCount();
					}
					IFile tableDdlFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(TABLE_DDL));
					if (tableDdlFile.exists()) {
						iBackupUtil.backupFile3(tableDdlFile, new SubProgressMonitor(progressMonitor, 100));
						tableDdlFile.delete(true, new SubProgressMonitor(progressMonitor, 100));
						iModuleInfo.getApplicationInfo().incrementDeleteCount();
					}
					IFile indexDdlFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(INDEX_DDL));
					if (indexDdlFile.exists()) {
						iBackupUtil.backupFile3(indexDdlFile, new SubProgressMonitor(progressMonitor, 100));
						indexDdlFile.delete(true, new SubProgressMonitor(progressMonitor, 100));
						iModuleInfo.getApplicationInfo().incrementDeleteCount();
					}
					IFile dropTablesDdlFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(DROP_TABLES_DDL));
					if (dropTablesDdlFile.exists()) {
						iBackupUtil.backupFile3(dropTablesDdlFile, new SubProgressMonitor(progressMonitor, 100));
						dropTablesDdlFile.delete(true, new SubProgressMonitor(progressMonitor, 100));
						iModuleInfo.getApplicationInfo().incrementDeleteCount();
					}
					IFolder backends = iWorkspace.getRoot().getFolder(path.append(META_INF).append(BACKENDS));
					if (backends.exists()) {
						iBackupUtil.backupFolder3(backends, new SubProgressMonitor(progressMonitor, 100));
						backends.delete(true, new SubProgressMonitor(progressMonitor, 100));
						iModuleInfo.getApplicationInfo().incrementDeleteCount();
					}
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
		return status;
	}
}
