package com.ibm.commerce.jpa.port;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import com.ibm.commerce.jpa.port.info.AccessBeanSubclassInfo;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.parsers.ModuleInfoXmlParser;
import com.ibm.commerce.jpa.port.parsers.ProjectInfoXmlParser;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.JPASeederUtilBase;
import com.ibm.commerce.jpa.port.util.JavaUtil;
import com.ibm.commerce.jpa.updaters.ModuleUpdater;

public class ReplaceEjbsJob extends Job {
	private static final int PARALLEL_JOBS = 4;
	private static final Collection<String> EXTRA_DELETE_INTENDED_TYPES;
	static {
		EXTRA_DELETE_INTENDED_TYPES = new HashSet<String>();
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.negotiation.helpers._AuctionJDBCHelper_Stub");
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.negotiation.helpers._EJSRemoteStatelessAuctionJDBCHelper_7ccf3f64_Tie");
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.rfq.helpers._EJSRemoteStatelessRFQJdbcHelper_1fd90e60_Tie");
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.rfq.helpers._RFQJdbcHelper_Stub");
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.context.objects.util.EJBLocalHomeFactory");
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.negotiation.util.SortingAttribute");
		EXTRA_DELETE_INTENDED_TYPES.add("com.ibm.commerce.utf.helper.SortingAttribute");
	}
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private IProgressMonitor iProgressGroup;
	private Map<String, String> iNameMap = new HashMap<String, String>();
	private Collection<IProject> iBuildPendingProjects = Collections.synchronizedCollection(new HashSet<IProject>());
	
	public ReplaceEjbsJob() {
		super("Entity Reference Port");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 82000);
	}
	
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			iProgressGroup.beginTask("Replace EJBs", 82000);
			progressMonitor.beginTask("replace EJBs", 82000);
			//creates an empty ApplicationInfo object
			initializeApplicationInfo();					// 0 ticks
			
			//this restores only the files in the .jpaGeneratedFileList3 metafile
			restoreProjects(progressMonitor);				// 1000 ticks
			
			//compile all projects in the build pending projects list
			buildProjects(progressMonitor);				// 5000 ticks
			
			//parses the jpa entity info out of the .jpaModuleInfo.xml metafile from every project in the workspace
			//this avoids having to parse the deployment descriptors and source, but it means you must 
			//run the Generate JPA Entities before you can run this job or it will fail
			parseModules(progressMonitor);					// 5000 ticks
			
			//parses these metafiles into the ProjectInfo objects, that are contained in the ApplicationInfo
			//.jpaAccessBeanSubclassInfo.xml, .jpaEntityReferences.xml, .jpaEntityReferenceSubclasses.xml
			parseProjects(progressMonitor);					// 5000 ticks
			
			//parses the lists of generated files from the .jpaGeneratedFileList* files into maps
			//It removes the "JPA" junk from the names as well
			parseGeneratedFileLists(progressMonitor);		// 5000 ticks
			
			//deletes all files are flagged as "deleted intended" in the .jpaModuleInfo meta file
			//it also deletes all ejb stubs
			//it stores a backup in the .jpaBackup3 folder
			deleteEjbFiles(progressMonitor);				// 10000 ticks
			
			//deletes the following files from each module
			//	projectInfo.getEntityReferencingTypes();
			//  projectInfo.getDeleteIntendedTypes() -- this comes from .jpaModuleInfo - seems we just did this in previous step
			//  projectInfo.getApplicationInfo().getEjbStubTypes() -- seems we just did this in previous step
			//  ReplaceEjbsJob.EXTRA_DELETE_INTENDED_TYPES - most of these do not seem necessary for WCE
			//  projectInfo.getAccessBeanSubclasses()
			deleteEntityReferencingFiles(progressMonitor);	// 10000 ticks
			
			//loops through all of the files in the .jpaGeneratedFileList* metafiles and removes "JPA", "JPAEntity", etc from the name
			//this should not collide with any EJB artifacts because they should have been deleted by previous steps
			renameGeneratedFiles(progressMonitor);			// 20000 ticks
			
			//deletes EJB deployment descriptors, extensions, bindings xml, map xmls, etc from all modules
			updateModules(progressMonitor);					// 1000 ticks

			//this copies in a ContractCache object - was something to specifically fix an OOB EJB - not needed by WCE
			//seedNewClasses(progressMonitor, "SeedClasses4/projects"); // 100 ticks
			
			//this was to solve specific compile problems with generated artifacts for OOB entities - not needed by WCE
			//seedNewClasses(progressMonitor, "SeedClassesForRuntimeErrors/projects"); // 100 ticks
			
			iApplicationInfo.printSummary();
			iApplicationInfo = null;
			Collection<IProject> buildPendingProjects = new HashSet<IProject>();
			buildPendingProjects.addAll(iBuildPendingProjects);
			buildProjects(progressMonitor);					// 10000 ticks
			iBuildPendingProjects = buildPendingProjects;
			buildProjects(progressMonitor);					// 10000 ticks			
		}
		catch (InterruptedException e) {
			status = Status.CANCEL_STATUS;
		}
		finally {
			progressMonitor.done();
			iProgressGroup.done();
		}
		return status;
	}

	private void initializeApplicationInfo() {
		iApplicationInfo = new ApplicationInfo();
	}

	private void restoreProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		IWorkspaceRoot root = iWorkspace.getRoot();
		IProject[] projects = root.getProjects();
		Set<IProject> projectSet = new HashSet<IProject>();
		for (IProject project : projects) {
			projectSet.add(project);
		}
		Set<RestoreProjectJob> restoreProjectJobs = new HashSet<RestoreProjectJob>();
		for (int i = 0; i < PARALLEL_JOBS; i++) {
			RestoreProjectJob restoreProjectJob = new RestoreProjectJob(iApplicationInfo, iBuildPendingProjects, projectSet);
			restoreProjectJob.setProgressGroup(iProgressGroup, 1000 / PARALLEL_JOBS);
			restoreProjectJob.schedule();
			restoreProjectJobs.add(restoreProjectJob);
		}
		for (RestoreProjectJob restoreProjectJob : restoreProjectJobs) {
			restoreProjectJob.join();
			progressMonitor.worked(1000 / PARALLEL_JOBS);
		}
	}
	
	private void parseModules(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			IWorkspaceRoot root = iWorkspace.getRoot();
			IProject[] projects = root.getProjects();
			Set<ModuleInfo> modules = new HashSet<ModuleInfo>();
			for (IProject project : projects) {
				IFile moduleInfoXmlFile = project.getFile(".jpaModuleInfo.xml");
				if (moduleInfoXmlFile.exists()) {
					IJavaProject javaProject = JavaCore.create(project);
					ModuleInfo moduleInfo = new ModuleInfo(iApplicationInfo, javaProject);
					iApplicationInfo.addModule(moduleInfo);
					modules.add(moduleInfo);
				}
			}
			Set<ParseModuleJob> parseModuleJobs = new HashSet<ParseModuleJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				ParseModuleJob parseModuleJob = new ParseModuleJob(modules);
				parseModuleJob.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				parseModuleJob.schedule();
				parseModuleJobs.add(parseModuleJob);
			}
			for (ParseModuleJob parseModuleJob : parseModuleJobs) {
				parseModuleJob.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void parseProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			IWorkspaceRoot root = iWorkspace.getRoot();
			IProject[] projects = root.getProjects();
			Set<ProjectInfo> projectInfos = new HashSet<ProjectInfo>();
			for (IProject project : projects) {
				IFile accessBeanSubclassInfoXmlFile = project.getFile(".jpaAccessBeanSubclassInfo.xml");
				IFile entityReferencesXmlFile = project.getFile(".jpaEntityReferences.xml");
				IFile entityReferenceSubclassesXmlFile = project.getFile(".jpaEntityReferenceSubclasses.xml");
				if (accessBeanSubclassInfoXmlFile.exists() || entityReferencesXmlFile.exists() || entityReferenceSubclassesXmlFile.exists()) {
					ProjectInfo projectInfo = iApplicationInfo.getProjectInfo(project);
					projectInfos.add(projectInfo);
				}
			}
			Set<ParseProjectJob> parseProjectJobs = new HashSet<ParseProjectJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				ParseProjectJob parseProjectJob = new ParseProjectJob(projectInfos);
				parseProjectJob.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				parseProjectJob.schedule();
				parseProjectJobs.add(parseProjectJob);
			}
			for (ParseProjectJob parseProjectJob : parseProjectJobs) {
				parseProjectJob.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void parseGeneratedFileLists(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			IWorkspaceRoot root = iWorkspace.getRoot();
			IProject[] projects = root.getProjects();
			Set<IFile> fileListFiles = new HashSet<IFile>();
			for (IProject project : projects) {
				IFile generatedFileList = project.getFile(".jpaGeneratedFileList");
				if (generatedFileList.exists()) {
					fileListFiles.add(generatedFileList);
				}
				IFile generatedFileList2 = project.getFile(".jpaGeneratedFileList2");
				if (generatedFileList2.exists()) {
					fileListFiles.add(generatedFileList2);
				}
			}
			Set<ParseGeneratedFileListFileJob> jobs = new HashSet<ParseGeneratedFileListFileJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				ParseGeneratedFileListFileJob job = new ParseGeneratedFileListFileJob(iApplicationInfo, fileListFiles, iNameMap);
				job.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				job.schedule();
				jobs.add(job);
			}
			for (ParseGeneratedFileListFileJob job : jobs) {
				job.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void deleteEjbFiles(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ModuleInfo> modules = new HashSet<ModuleInfo>();
			modules.addAll(iApplicationInfo.getModules());
			Set<DeleteEjbFilesJob> jobs = new HashSet<DeleteEjbFilesJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				DeleteEjbFilesJob job = new DeleteEjbFilesJob(modules, iBuildPendingProjects);
				job.setProgressGroup(iProgressGroup, 10000 / PARALLEL_JOBS);
				job.schedule();
				jobs.add(job);
			}
			for (DeleteEjbFilesJob job : jobs) {
				job.join();
				progressMonitor.worked(10000 / PARALLEL_JOBS);
			}
		}
	}

	private void deleteEntityReferencingFiles(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ProjectInfo> projects = new HashSet<ProjectInfo>();
			projects.addAll(iApplicationInfo.getProjects());
			Set<DeleteEntityReferencingFilesJob> jobs = new HashSet<DeleteEntityReferencingFilesJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				DeleteEntityReferencingFilesJob job = new DeleteEntityReferencingFilesJob(projects, iBuildPendingProjects);
				job.setProgressGroup(iProgressGroup, 10000 / PARALLEL_JOBS);
				job.schedule();
				jobs.add(job);
			}
			for (DeleteEntityReferencingFilesJob job : jobs) {
				job.join();
				progressMonitor.worked(10000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void renameGeneratedFiles(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			IWorkspaceRoot root = iWorkspace.getRoot();
			IProject[] projects = root.getProjects();
			Set<IFile> fileListFiles = new HashSet<IFile>();
			for (IProject project : projects) {
				IFile generatedFileList = project.getFile(".jpaGeneratedFileList");
				if (generatedFileList.exists()) {
					fileListFiles.add(generatedFileList);
				}
				IFile generatedFileList2 = project.getFile(".jpaGeneratedFileList2");
				if (generatedFileList2.exists()) {
					fileListFiles.add(generatedFileList2);
				}
			}
			Set<RenameGeneratedFilesJob> jobs = new HashSet<RenameGeneratedFilesJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				RenameGeneratedFilesJob job = new RenameGeneratedFilesJob(fileListFiles, iNameMap, iApplicationInfo, iBuildPendingProjects);
				job.setProgressGroup(iProgressGroup, 20000 / PARALLEL_JOBS);
				job.schedule();
				jobs.add(job);
			}
			for (RenameGeneratedFilesJob job : jobs) {
				job.join();
				progressMonitor.worked(20000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void updateModules(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ModuleInfo> modules = new HashSet<ModuleInfo>();
			modules.addAll(iApplicationInfo.getModules());
			Set<UpdateModuleJob> jobs = new HashSet<UpdateModuleJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				UpdateModuleJob job = new UpdateModuleJob(iWorkspace, modules);
				job.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				job.schedule();
				jobs.add(job);
			}
			for (UpdateModuleJob job : jobs) {
				job.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void buildProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<BuildProjectJob> buildProjectJobs = new HashSet<BuildProjectJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				BuildProjectJob buidProjectJob = new BuildProjectJob(iBuildPendingProjects);
				buidProjectJob.setProgressGroup(iProgressGroup, 10000 / PARALLEL_JOBS);
				buidProjectJob.schedule();
				buildProjectJobs.add(buidProjectJob);
			}
			for (BuildProjectJob buidProjectJob : buildProjectJobs) {
				buidProjectJob.join();
				progressMonitor.worked(10000 / PARALLEL_JOBS);
			}
		}
	}
	
	private static class RestoreProjectJob extends Job {
		private ApplicationInfo iApplicationInfo;
		private Collection<IProject> iBuildPendingProjects;
		private Set<IProject> iProjects;

		public RestoreProjectJob(ApplicationInfo applicationInfo, Collection<IProject> buildPendingProjects, Set<IProject> projects) {
			super("Restore projects");
			iApplicationInfo = applicationInfo;
			iBuildPendingProjects = buildPendingProjects;
			iProjects = projects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Restore projects", IProgressMonitor.UNKNOWN);
				IProject project = getProject();
				while (project != null) {
					boolean restored = false;
					if (!progressMonitor.isCanceled()) {
						BackupUtil backupUtil = iApplicationInfo.getBackupUtil(project);
						try {
							restored = backupUtil.restore3(new SubProgressMonitor(progressMonitor, 1000));
							if (restored) {
								iBuildPendingProjects.add(project);
							}
						}
						catch (CoreException e) {
							e.printStackTrace();
							status = Status.CANCEL_STATUS;
						}
					}
					else {
						status = Status.CANCEL_STATUS;
					}
					if (status != Status.CANCEL_STATUS && !progressMonitor.isCanceled()) {
						project = getProject();
					}
					else {
						project = null;
						status = Status.CANCEL_STATUS;
					}
				}
			}
			finally {
				progressMonitor.done();
				iApplicationInfo = null;
				iProjects = null;
			}
			return status;
		}
		
		private IProject getProject() {
			IProject project = null;
			synchronized(iProjects) {
				for (IProject currentProject : iProjects) {
					project = currentProject;
					break;
				}
				if (project != null) {
					iProjects.remove(project);
				}
			}
			return project;
		}
	}
	
	private static class ParseModuleJob extends Job {
		private Set<ModuleInfo> iModules;

		public ParseModuleJob(Set<ModuleInfo> modules) {
			super("Parse modules");
			iModules = modules;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Parse modules", IProgressMonitor.UNKNOWN);
				ModuleInfo moduleInfo = getModule();
				while (moduleInfo != null) {
					if (!progressMonitor.isCanceled()) {
						ModuleInfoXmlParser moduleInfoXmlParser = new ModuleInfoXmlParser(moduleInfo);
						moduleInfoXmlParser.parse(new SubProgressMonitor(progressMonitor, 1000));
					}
					else {
						status = Status.CANCEL_STATUS;
					}
					moduleInfo = getModule();
				}
			}
			finally {
				progressMonitor.done();
				iModules = null;
			}
			return status;
		}
		
		private ModuleInfo getModule() {
			ModuleInfo module = null;
			synchronized(iModules) {
				for (ModuleInfo currentModule : iModules) {
					module = currentModule;
					break;
				}
				if (module != null) {
					iModules.remove(module);
				}
			}
			return module;
		}
	}
	
	private static class ParseProjectJob extends Job {
		private Set<ProjectInfo> iProjects;

		public ParseProjectJob(Set<ProjectInfo> projects) {
			super("Parse projects");
			iProjects = projects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Parse projects", IProgressMonitor.UNKNOWN);
				ProjectInfo projectInfo = getProjectInfo();
				while (projectInfo != null) {
					if (!progressMonitor.isCanceled()) {
						ProjectInfoXmlParser projectInfoXmlParser = new ProjectInfoXmlParser(projectInfo);
						projectInfoXmlParser.parse(new SubProgressMonitor(progressMonitor, 1000));
						projectInfo = getProjectInfo();
					}
					else {
						status = Status.CANCEL_STATUS;
						projectInfo = null;
					}
				}
			}
			finally {
				progressMonitor.done();
				iProjects = null;
			}
			return status;
		}
		
		private ProjectInfo getProjectInfo() {
			ProjectInfo projectInfo = null;
			synchronized(iProjects) {
				for (ProjectInfo currentProject : iProjects) {
					projectInfo = currentProject;
					break;
				}
				if (projectInfo != null) {
					iProjects.remove(projectInfo);
				}
			}
			return projectInfo;
		}
	}
	
	private static class ParseGeneratedFileListFileJob extends Job {
		private ApplicationInfo iApplicationInfo;
		private Set<IFile> iFiles;
		private Map<String, String> iNameMap;
		
		public ParseGeneratedFileListFileJob(ApplicationInfo applicationInfo, Set<IFile> files, Map<String, String> nameMap) {
			super("Parse Generated File List Files");
			iApplicationInfo = applicationInfo;
			iFiles = files;
			iNameMap = nameMap;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Parse generated file list files", IProgressMonitor.UNKNOWN);
				IFile file = getFile();
				while (file != null) {
					if (!progressMonitor.isCanceled()) {
						try {
							InputStream inputStream = file.getContents(true);
							try {
								InputStreamReader reader = new InputStreamReader(inputStream);
								BufferedReader bufferedReader = new BufferedReader(reader);
								String portableString = bufferedReader.readLine();
								while (portableString != null && !progressMonitor.isCanceled()) {
									String name = portableString.substring(portableString.lastIndexOf('/') + 1, portableString.lastIndexOf('.'));
									if (name.contains("$JPA")) {
										synchronized(iNameMap) {
											iNameMap.put(name, name.replace("$JPA", ""));
										}
									}
									else if (name.endsWith("JPAEntity")) {
										synchronized(iNameMap) {
											iNameMap.put(name, name.replace("JPAEntity", ""));
										}
									}
									else if (name.contains("JPA")) {
										synchronized(iNameMap) {
											iNameMap.put(name, name.replace("JPA", ""));
										}
									}
									portableString = bufferedReader.readLine();
								}
								iApplicationInfo.incrementParsedAssetCount();
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							finally {
								try {
									inputStream.close();
								}
								catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						catch (CoreException e) {
							e.printStackTrace();
						}
					}
					else {
						status = Status.CANCEL_STATUS;
						file = null;
					}
					if (!progressMonitor.isCanceled()) {
						file = getFile();
					}
					else {
						status = Status.CANCEL_STATUS;
						file = null;
					}
				}
			}
			finally {
				progressMonitor.done();
				iApplicationInfo = null;
				iFiles = null;
				iNameMap = null;
			}
			return status;
		}
		
		private IFile getFile() {
			IFile file = null;
			synchronized(iFiles) {
				for (IFile currentFile : iFiles) {
					file = currentFile;
					break;
				}
				if (file != null) {
					iFiles.remove(file);
				}
			}
			return file;
		}
	}
	
	private static class DeleteEjbFilesJob extends Job {
		private Set<ModuleInfo> iModules;
		private Collection<IProject> iBuildPendingProjects;
		
		public DeleteEjbFilesJob(Set<ModuleInfo> modules, Collection<IProject> buildPendingProjects) {
			super("delete EJB files");
			iModules = modules;
			iBuildPendingProjects = buildPendingProjects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("delete EJB files", IProgressMonitor.UNKNOWN);
				ModuleInfo moduleInfo = getModuleInfo();
				while (moduleInfo != null) {
					IJavaProject javaProject = moduleInfo.getJavaProject();
					BackupUtil backupUtil = moduleInfo.getApplicationInfo().getBackupUtil(javaProject.getProject());
					if (!progressMonitor.isCanceled()) {
						Set<String> deleteIntendedTypes = new HashSet<String>();
						deleteIntendedTypes.addAll(moduleInfo.getDeleteIntendedTypes());
						deleteIntendedTypes.addAll(moduleInfo.getApplicationInfo().getEjbStubTypes());
						for (String deleteIntendedType : deleteIntendedTypes) {
							try {
								IType type = javaProject.findType(deleteIntendedType, new SubProgressMonitor(progressMonitor, 100));
								if (type != null && type.getCompilationUnit().getResource().getProject().equals(javaProject.getProject())) {
									IFile file = (IFile) type.getCompilationUnit().getResource();
									backupUtil.backupFile3(file, new SubProgressMonitor(progressMonitor, 100));
									file.delete(true, new SubProgressMonitor(progressMonitor, 100));
									moduleInfo.getApplicationInfo().incrementDeleteCount();
								}
								else if (type == null && !moduleInfo.getApplicationInfo().getEjbStubTypes().contains(deleteIntendedType)) {
									System.out.println("unable to find deleteIntendedType: "+deleteIntendedType+" in "+javaProject.getProject().getName());
								}
							}
							catch (JavaModelException e) {
								e.printStackTrace();
							}
							catch (CoreException e) {
								e.printStackTrace();
							}
						}
						iBuildPendingProjects.add(javaProject.getProject());
					}
					else {
						status = Status.CANCEL_STATUS;
						moduleInfo = null;
					}
					if (!progressMonitor.isCanceled()) {
						moduleInfo = getModuleInfo();
					}
					else {
						status = Status.CANCEL_STATUS;
						moduleInfo = null;
					}
				}
			}
			finally {
				progressMonitor.done();
				iModules = null;
				iBuildPendingProjects = null;
			}
			return status;
		}
		
		private ModuleInfo getModuleInfo() {
			ModuleInfo moduleInfo = null;
			synchronized(iModules) {
				for (ModuleInfo currentModuleInfo : iModules) {
					moduleInfo = currentModuleInfo;
					break;
				}
				if (moduleInfo != null) {
					iModules.remove(moduleInfo);
				}
			}
			return moduleInfo;
		}
	}

	private static class DeleteEntityReferencingFilesJob extends Job {
		private Set<ProjectInfo> iProjects;
		private Collection<IProject> iBuildPendingProjects;
		
		public DeleteEntityReferencingFilesJob(Set<ProjectInfo> projects, Collection<IProject> buildPendingProjects) {
			super("delete entity referencing files");
			iProjects = projects;
			iBuildPendingProjects = buildPendingProjects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("delete entity referencing files", IProgressMonitor.UNKNOWN);
				ProjectInfo projectInfo = getProjectInfo();
				while (projectInfo != null) {
					BackupUtil backupUtil = projectInfo.getApplicationInfo().getBackupUtil(projectInfo.getProject());
					IJavaProject javaProject = projectInfo.getJavaProject();
					if (!progressMonitor.isCanceled()) {
						if (javaProject.getJavaModel() != null) {
							Set<String> deleteIntendedTypes = new HashSet<String>();
							deleteIntendedTypes.addAll(projectInfo.getEntityReferencingTypes());
							deleteIntendedTypes.addAll(projectInfo.getDeleteIntendedTypes());
							deleteIntendedTypes.addAll(projectInfo.getApplicationInfo().getEjbStubTypes());
							deleteIntendedTypes.addAll(ReplaceEjbsJob.EXTRA_DELETE_INTENDED_TYPES);
	//						deleteIntendedTypes.addAll(projectInfo.getEntityReferenceSubclasses());
							Collection<AccessBeanSubclassInfo> accessBeanSubclasses = projectInfo.getAccessBeanSubclasses();
							for (AccessBeanSubclassInfo accessBeanSubclassInfo : accessBeanSubclasses) {
								deleteIntendedTypes.add(accessBeanSubclassInfo.getName());
							}
							for (String deleteIntendedType : deleteIntendedTypes) {
								try {
									IType type = javaProject.findType(deleteIntendedType, new SubProgressMonitor(progressMonitor, 100));
									if (type != null && type.getCompilationUnit() != null && type.getCompilationUnit().getResource().getProject().equals(javaProject.getProject())) {
										IFile file = (IFile) type.getCompilationUnit().getResource();
										backupUtil.backupFile3(file, new SubProgressMonitor(progressMonitor, 100));
										file.delete(true, new SubProgressMonitor(progressMonitor, 100));
										projectInfo.getApplicationInfo().incrementDeleteCount();
									}
									else if (type == null && !projectInfo.getApplicationInfo().getEjbStubTypes().contains(deleteIntendedType) &&
											!ReplaceEjbsJob.EXTRA_DELETE_INTENDED_TYPES.contains(deleteIntendedType)) {
										System.out.println("unable to find type "+deleteIntendedType+" in "+javaProject.getProject());
									}
								}
								catch (JavaModelException e) {
//									e.printStackTrace();
								}
								catch (CoreException e) {
									e.printStackTrace();
								}
							}
							iBuildPendingProjects.add(javaProject.getProject());
						}
					}
					else {
						status = Status.CANCEL_STATUS;
						projectInfo = null;
					}
					if (!progressMonitor.isCanceled()) {
						projectInfo = getProjectInfo();
					}
					else {
						status = Status.CANCEL_STATUS;
						projectInfo = null;
					}
				}
			}
			finally {
				progressMonitor.done();
				iProjects = null;
				iBuildPendingProjects = null;
			}
			return status;
		}
		
		private ProjectInfo getProjectInfo() {
			ProjectInfo projectInfo = null;
			synchronized(iProjects) {
				for (ProjectInfo currentProjectInfo : iProjects) {
					projectInfo = currentProjectInfo;
					break;
				}
				if (projectInfo != null) {
					iProjects.remove(projectInfo);
				}
			}
			return projectInfo;
		}
	}
	
	private static class RenameGeneratedFilesJob extends Job {
		private Set<IFile> iFiles;
		private Map<String, String> iNameMap;
		private ApplicationInfo iApplicationInfo;
		private Collection<IProject> iBuildPendingProjects;
		private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);
		
		public RenameGeneratedFilesJob(Set<IFile> files, Map<String, String> nameMap, ApplicationInfo applicationInfo, Collection<IProject> buildPendingProjects) {
			super("Rename generated files job");
			iFiles = files;
			iNameMap = nameMap;
			iApplicationInfo = applicationInfo;
			iBuildPendingProjects = buildPendingProjects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Rename generated files job", IProgressMonitor.UNKNOWN);
				IFile file = getFile();
				while (file != null) {
					if (!progressMonitor.isCanceled()) {
						try {
							InputStream inputStream = file.getContents(true);
							try {
								InputStreamReader reader = new InputStreamReader(inputStream);
								BufferedReader bufferedReader = new BufferedReader(reader);
								String portableString = bufferedReader.readLine();
								while (portableString != null && !progressMonitor.isCanceled()) {
									IPath path = Path.fromPortableString(portableString);
									IFile generatedFile = file.getProject().getFile(path);
									if (generatedFile.exists()) {
										BackupUtil backupUtil = iApplicationInfo.getBackupUtil(generatedFile.getProject());
										String name = portableString.substring(portableString.lastIndexOf('/') + 1, portableString.lastIndexOf('.'));
										if (name.contains("$JPA")) {
											backupUtil.backupFile3(generatedFile, new SubProgressMonitor(progressMonitor, 100));
											generatedFile.delete(true, new SubProgressMonitor(progressMonitor, 100));
											iApplicationInfo.incrementDeleteCount();
										}
										else if (name.contains("JPA")) {
											String targetName = name.replace("JPA", "");
											if (name.endsWith("JPAEntity")) {
												targetName = name.replace("JPAEntity", "");
											}
											ICompilationUnit compilationUnit = (ICompilationUnit) JavaCore.create(generatedFile);
											iBuildPendingProjects.add(compilationUnit.getResource().getProject());
											JavaUtil.renameJavaClass(backupUtil, iASTParser, compilationUnit, targetName, iNameMap, new SubProgressMonitor(progressMonitor, 1000));
											iApplicationInfo.incrementGeneratedAssetCount();
										}
									}									
									portableString = bufferedReader.readLine();
								}
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							finally {
								try {
									inputStream.close();
								}
								catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						catch (CoreException e) {
							e.printStackTrace();
						}
					}
					else {
						status = Status.CANCEL_STATUS;
						file = null;
					}
					if (!progressMonitor.isCanceled()) {
						file = getFile();
					}
					else {
						status = Status.CANCEL_STATUS;
						file = null;
					}
				}
			}
			finally {
				progressMonitor.done();
				iFiles = null;
				iNameMap = null;
				iApplicationInfo = null;
				iBuildPendingProjects = null;
				iASTParser = null;
			}
			return status;
		}
		
		private IFile getFile() {
			IFile file = null;
			synchronized(iFiles) {
				for (IFile currentFile : iFiles) {
					file = currentFile;
					break;
				}
				if (file != null) {
					iFiles.remove(file);
				}
			}
			return file;
		}
	}
	
	private static class UpdateModuleJob extends Job {
		private IWorkspace iWorkspace;
		private Set<ModuleInfo> iModules;

		public UpdateModuleJob(IWorkspace workspace, Set<ModuleInfo> modules) {
			super("Update modules");
			iWorkspace = workspace;
			iModules = modules;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Update modules", IProgressMonitor.UNKNOWN);
				ModuleInfo moduleInfo = getModule();
				while (moduleInfo != null) {
					if (!progressMonitor.isCanceled()) {
						ModuleUpdater moduleUpdater = new ModuleUpdater(iWorkspace, moduleInfo);
						moduleUpdater.update(new SubProgressMonitor(progressMonitor, 1000));
					}
					else {
						status = Status.CANCEL_STATUS;
					}
					moduleInfo = getModule();
				}
			}
			finally {
				progressMonitor.done();
				iWorkspace = null;
				iModules = null;
			}
			return status;
		}
		
		private ModuleInfo getModule() {
			ModuleInfo module = null;
			synchronized(iModules) {
				for (ModuleInfo currentModule : iModules) {
					module = currentModule;
					break;
				}
				if (module != null) {
					iModules.remove(module);
				}
			}
			return module;
		}
	}
	
	private static class BuildProjectJob extends Job {
		private Collection<IProject> iBuildPendingProjects;
		
		public BuildProjectJob(Collection<IProject> buildPendingProjects) {
			super("Build projects");
			iBuildPendingProjects = buildPendingProjects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Build projects", IProgressMonitor.UNKNOWN);
				IProject project = getProject();
				while (project != null) {
					if (!progressMonitor.isCanceled() && status != Status.CANCEL_STATUS) {
						try {
							System.out.println("building "+project.getName());
							project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(progressMonitor, 1000));
						}
						catch (CoreException e) {
							e.printStackTrace();
							status = Status.CANCEL_STATUS;
						}
					}
					else {
						status = Status.CANCEL_STATUS;
					}
					if (!progressMonitor.isCanceled() && status != Status.CANCEL_STATUS) {
						project = getProject();
					}
					else {
						project = null;
					}
				}
			}
			finally {
				progressMonitor.done();
				iBuildPendingProjects = null;
			}
			return status;
		}
		
		private IProject getProject() {
			IProject project = null;
			synchronized(iBuildPendingProjects) {
				for (IProject currentProject : iBuildPendingProjects) {
					project = currentProject;
					break;
				}
				if (project != null) {
					iBuildPendingProjects.remove(project);
				}
			}
			return project;
		}
	}
	
	private void seedNewClasses(IProgressMonitor progressMonitor, String sourceFolderName) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			SeedNewClassesJob seedNewClassesJob = new SeedNewClassesJob(iApplicationInfo, iBuildPendingProjects, sourceFolderName);
			seedNewClassesJob.setProgressGroup(iProgressGroup, 100);
			seedNewClassesJob.schedule();
			seedNewClassesJob.join();
			progressMonitor.worked(100);
		}
	}
	
	private static class SeedNewClassesJob extends Job {
		private ApplicationInfo iApplicationInfo;
		private Collection<IProject> iBuildPendingProjects;
		private String iSourceFolderName;
		
		public SeedNewClassesJob(ApplicationInfo applicationInfo, Collection<IProject> buildPendingProjects, String sourceFolderName) {
			super("Seed new classes");
			iApplicationInfo = applicationInfo;
			iBuildPendingProjects = buildPendingProjects;
			iSourceFolderName = sourceFolderName;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			System.out.println("Seed new classes");
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Seed new classes", IProgressMonitor.UNKNOWN);
				if (!progressMonitor.isCanceled()) {
					JPASeederUtilBase seederUtil = new JPASeederUtilBase(iApplicationInfo, iBuildPendingProjects, iSourceFolderName, false);
					seederUtil.seedNewClasses(progressMonitor);
				}
				else {
					status = Status.CANCEL_STATUS;
				}
			}
			finally {
				progressMonitor.done();
				iApplicationInfo = null;
				iBuildPendingProjects = null;
			}
			return status;
		}
	}
}
