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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ColumnInfo;
import com.ibm.commerce.jpa.port.info.ConstraintInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ForeignKeyInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.TableInfo;
import com.ibm.commerce.jpa.port.util.ApplicationInfoUtil;

public class ModuleParser {
	private static final String META_INF = "META-INF";
	private static final String EJB_JAR_XML = "ejb-jar.xml";
	private static final String IBM_EJB_ACCESS_BEAN_XMI = "ibm-ejb-access-bean.xmi";
	private static final String IBM_EJB_JAR_EXT_XMI = "ibm-ejb-jar-ext.xmi";
	private static final String IBM_EJB_JAR_BND_XMI = "ibm-ejb-jar-bnd.xmi";
	private static final String BACKENDS = "backends";
	private static final String MAP_XMI = "Map.mapxmi";
	private static final String DBM = "dbm";
	private static final String EJB_DEPLOY = "ejbdeploy";
	private static final String WEBSPHERE_DEPLOY = "websphere_deploy";
	
	private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);
	private ModuleInfo iModuleInfo;
	private ApplicationInfo iApplicationInfo;
	private IJavaProject iJavaProject;
	private IWorkspace iWorkspace;
	
	public ModuleParser(IWorkspace workspace, ModuleInfo moduleInfo) {
		System.out.println("Parsing module " + moduleInfo.getJavaProject().getElementName());
		iModuleInfo = moduleInfo;
		iApplicationInfo = iModuleInfo.getApplicationInfo();
		iJavaProject = moduleInfo.getJavaProject();
		iWorkspace = workspace;
	}
	
	public IStatus parse(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			progressMonitor.beginTask("parse " + iJavaProject.getProject().getName(), 1000 + iModuleInfo.getEntities().size() * 1400 + 1000);
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
						EjbJarXmlParser parser = new EjbJarXmlParser(ejbJarXmlFile, iModuleInfo);
						parser.parse();
					}
					IFile ibmEjbJarExtXmiFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(IBM_EJB_JAR_EXT_XMI));
					if (ibmEjbJarExtXmiFile.exists()) {
						IbmEjbJarExtXmiParser parser = new IbmEjbJarExtXmiParser(ibmEjbJarExtXmiFile, iModuleInfo);
						parser.parse();
					}
					IFile ibmEjbJarBndXmiFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(IBM_EJB_JAR_BND_XMI));
					if (ibmEjbJarBndXmiFile.exists()) {
						IbmEjbJarBndXmiParser parser = new IbmEjbJarBndXmiParser(ibmEjbJarBndXmiFile, iModuleInfo); 
						parser.parse();
					}
					IFile ibmEjbAccessBeanXmiFile = iWorkspace.getRoot().getFile(path.append(META_INF).append(IBM_EJB_ACCESS_BEAN_XMI));
					if (ibmEjbAccessBeanXmiFile.exists()) {
						IbmEjbAccessBeanXmiParser parser = new IbmEjbAccessBeanXmiParser(ibmEjbAccessBeanXmiFile, iModuleInfo); 
						parser.parse();
					}
					IFolder backends = iWorkspace.getRoot().getFolder(path.append(META_INF).append(BACKENDS));
					if (backends.exists()) {
						IResource[] members = backends.members();
						if (members != null) {
							for (IResource member : members) {
								if (member instanceof IFolder) {
									IFolder dbFolder = (IFolder) member;
									IResource[] dbFolderMembers = dbFolder.members();
									for (IResource dbFolderMember : dbFolderMembers) {
										if (dbFolderMember instanceof IFile) {
											IFile dbFolderFile = (IFile) dbFolderMember;
											if (DBM.equals(dbFolderFile.getFileExtension())) {
												SchemaDbmParser parser = new SchemaDbmParser(dbFolderFile, iModuleInfo);
												parser.parse();
											}
										}
									}
									IFile mapXmiFile = dbFolder.getFile(MAP_XMI);
									if (mapXmiFile.exists()) {
										MapXmiParser parser = new MapXmiParser(mapXmiFile, iModuleInfo);
										parser.parse();
									}
								}
							}
						}
					}
					IFolder folder = iWorkspace.getRoot().getFolder(path);
					parseFolder(folder);
				}
			}
			progressMonitor.worked(1000);
			Collection<EntityInfo> entities = iModuleInfo.getEntities();
			for (EntityInfo entityInfo : entities) {
				if (progressMonitor.isCanceled()) {
					status = Status.CANCEL_STATUS;
					break;
				}
				if (entityInfo.getSupertype() == null) {
					parseEntity(progressMonitor, entityInfo);
				}
			}
			for (EntityInfo entityInfo : entities) {
				if (progressMonitor.isCanceled()) {
					status = Status.CANCEL_STATUS;
					break;
				}
				if (entityInfo.getSupertype() != null) {
					parseEntity(progressMonitor, entityInfo);
				}
			}
			if (!progressMonitor.isCanceled()) {
				resolveForeignKeys();
			}
			else {
				status = Status.CANCEL_STATUS;
			}
			progressMonitor.worked(1000);
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
	
	private void parseFolder(IFolder folder) throws CoreException {
		IResource[] members = folder.members();
		for (IResource member : members) {
			if (member.getType() == IResource.FOLDER) {
				if (WEBSPHERE_DEPLOY.equals(member.getName()) || EJB_DEPLOY.equals(member.getName())) {
					parseDeleteIntendedFolder((IFolder) member);
				}
				else {
					parseFolder((IFolder) member);
				}
			}
		}
	}
	
	private void parseDeleteIntendedFolder(IFolder folder) throws CoreException {
		IResource[] members = folder.members();
		for (IResource member : members) {
			if (member.getType() == IResource.FOLDER) {
				parseDeleteIntendedFolder((IFolder) member);
			}
			else if (member.getType() == IResource.FILE) {
				IFile file = (IFile) member;
				if (file.getName().endsWith(".java")) {
					IJavaElement javaElement = JavaCore.create(file);
					iModuleInfo.addDeleteIntendedType(((ICompilationUnit) javaElement).getTypes()[0].getFullyQualifiedName('.'));
				}
			}
		}
	}
	
	private void parseEntity(IProgressMonitor progressMonitor, EntityInfo entityInfo) {
		HomeInterfaceParser homeInterfaceParser = new HomeInterfaceParser(iASTParser, entityInfo);
		homeInterfaceParser.parse(new SubProgressMonitor(progressMonitor, 200));
		LocalHomeInterfaceParser localHomeInterfaceParser = new LocalHomeInterfaceParser(iASTParser, entityInfo);
		localHomeInterfaceParser.parse(new SubProgressMonitor(progressMonitor, 200));
		RemoteInterfaceParser remoteInterfaceParser = new RemoteInterfaceParser(iASTParser, entityInfo);
		remoteInterfaceParser.parse(new SubProgressMonitor(progressMonitor, 200));
		LocalInterfaceParser localInterfaceParser = new LocalInterfaceParser(iASTParser, entityInfo);
		localInterfaceParser.parse(new SubProgressMonitor(progressMonitor, 200));
		EjbClassParser ejbClassParser = new EjbClassParser(iASTParser, entityInfo);
		ejbClassParser.parse(new SubProgressMonitor(progressMonitor, 200));
		EjbKeyClassParser ejbKeyClassParser = new EjbKeyClassParser(iASTParser, entityInfo);
		ejbKeyClassParser.parse(new SubProgressMonitor(progressMonitor, 200));
		if (entityInfo.getAccessBeanInfo() != null && !entityInfo.getAccessBeanInfo().getDataClassType()) {
			EjbAccessBeanClassParser ejbAccessBeanClassParser = new EjbAccessBeanClassParser(iASTParser, entityInfo);
			ejbAccessBeanClassParser.parse(new SubProgressMonitor(progressMonitor, 200));
		}
		else if (entityInfo.getEjbAccessBeanType() != null) {
			iModuleInfo.addDeleteIntendedType(entityInfo.getEjbAccessBeanType().getFullyQualifiedName('.'));
		}
		if(entityInfo.getAccessBeanInfo() != null) {
			Collection<String> accessBeanInterfaces = entityInfo.getAccessBeanInfo().getAccessBeanInterfaces();
			for (String accessBeanInterfaceName : accessBeanInterfaces) {
				iModuleInfo.addDeleteIntendedType(accessBeanInterfaceName);
			}
		}
		EjbEntityCreationDataClassParser ejbEntityCreationDataClassParser = new EjbEntityCreationDataClassParser(iASTParser, entityInfo);
		ejbEntityCreationDataClassParser.parse(new SubProgressMonitor(progressMonitor, 200));
		if (entityInfo.getEjbFinderObjectType() != null) {
			iModuleInfo.addDeleteIntendedType(entityInfo.getEjbFinderObjectType().getFullyQualifiedName('.'));
			if (entityInfo.getEjbFinderObjectBaseType() != null) {
				iModuleInfo.addDeleteIntendedType(entityInfo.getEjbFinderObjectBaseType().getFullyQualifiedName('.'));
			}
		}
//		if (entityInfo.getEntityDataType() != null) {
//			iModuleInfo.addDeleteIntendedType(entityInfo.getEntityDataType().getFullyQualifiedName('.'));
//		}
		if (entityInfo.getFactoryType() != null) {
			iModuleInfo.addDeleteIntendedType(entityInfo.getFactoryType().getFullyQualifiedName('.'));
		}
		if (entityInfo.getEjbAccessHelperType() != null) {
			iModuleInfo.addDeleteIntendedType(entityInfo.getEjbAccessHelperType().getFullyQualifiedName('.'));
		}
		ApplicationInfoUtil.resolveTypeMappings(iApplicationInfo, entityInfo);
	}
	
	private void resolveForeignKeys() {
		Collection<TableInfo> tables = iModuleInfo.getTables();
		for (TableInfo table : tables) {
			Collection<ConstraintInfo> constraints = table.getConstraints();
			for (ConstraintInfo constraintInfo : constraints) {
				if (constraintInfo.getType().equals(ConstraintInfo.FOREIGN_KEY_CONSTRAINT)) {
					ForeignKeyInfo foreignKeyInfo = new ForeignKeyInfo(table);
					String[] members = constraintInfo.getMembers();
					String[] referencedMembers = constraintInfo.getReferencedMembers();
					List<ColumnInfo> memberColumns = new ArrayList<ColumnInfo>();
					List<ColumnInfo> referencedColumns = new ArrayList<ColumnInfo>();
					TableInfo parentTable = null;
					String referencedTable = constraintInfo.getReferencedTable();
					if (referencedTable != null && referencedMembers == null) {
						parentTable = iModuleInfo.getTableInfo(referencedTable);
						referencedColumns.addAll(parentTable.getPrimaryKeyColumns());
					}
					for (int i = 0; i < members.length; i++) {
						memberColumns.add(iModuleInfo.getColumnInfo(members[i]));
						if (referencedMembers != null) {
							referencedColumns.add(iModuleInfo.getColumnInfo(referencedMembers[i]));
						}
					}
					while (memberColumns.size() > 0) {
						ColumnInfo memberColumn = memberColumns.remove(0);
						ColumnInfo referencedColumn = null;
						for (int i = 0; i < referencedColumns.size(); i++) {
							if (memberColumn.getColumnName().equals(referencedColumns.get(i).getColumnName())) {
								referencedColumn = referencedColumns.remove(i);
								break;
							}
						}
						if (referencedColumn == null) {
							referencedColumn = referencedColumns.remove(0);
						}
						parentTable = referencedColumn.getTableInfo();
						foreignKeyInfo.addMember(memberColumn, referencedColumn);
					}
					foreignKeyInfo.setParentTableInfo(parentTable);
					table.addForeignKey(foreignKeyInfo);
					constraintInfo.setForeignKeyInfo(foreignKeyInfo);
				}
			}
		}
	}
}
