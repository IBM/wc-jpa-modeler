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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.ibm.commerce.jpa.port.generators.EntityReferenceSubclassesXmlGenerator;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.parsers.ModuleInfoXmlParser;
import com.ibm.commerce.jpa.port.parsers.ProjectInfoXmlParser;
import com.ibm.commerce.jpa.port.search.EntityReferenceSubclassSearchUtil;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.EntityReferenceUtil;
import com.ibm.commerce.jpa.port.util.JPASeederUtilBase;

/**
 * Searches all known entity referencing types for subclasses of those types and writes the list to a meta file in each project.
 * 
 * The Generate JPA Entities job must be run first to build the JPA module info meta file
 *
 */
public class EntityReferenceSubclassSearchJob extends Job {
	private static final int PARALLEL_JOBS = 4;
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private Collection<IProject> iBuildPendingProjects = Collections.synchronizedCollection(new HashSet<IProject>());
	private IProgressMonitor iProgressGroup;
	
	public EntityReferenceSubclassSearchJob() {
		super("Entity Reference Subclass Search");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 37100);
	}
	
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			iProgressGroup.beginTask("Entity Reference Subclass Search Jobs", 37100);
			progressMonitor.beginTask("JPA Port", 37100);
			
			//creates an emtpy ApplicationInfo object
			initializeApplicationInfo();					// 0 ticks
			
			//deletes all generated files that are in the .jpaGeneratedFileList2/3 meta files
			//restores all files from the .jpaBackup2/3 directories
			//deletes the .jpaEntityReferenceSubclasses.xml metafile, if it exists
			restoreProjects(progressMonitor);				// 1000 ticks
			
			//WCE doesn't need this - we put the v9 OOB jars on the CP
			//seedNewClasses(progressMonitor, "SeedClasses2/projects"); // 100 ticks
			
			//invokes an incremental build on all projects that were restored
			buildProjects(progressMonitor);				// 5000 ticks
			
			//parses project metadata information from the .jpaModuleInfo.xml metafile at the root of each project
			//this info is stored in the ApplicationInfo.ModuleInfo collection
			//the Generate JPA Entities job generates this meta file and it must be run first
			parseModules(progressMonitor);				// 5000 ticks
			
			//creates a collection of all projects that contain a .jpaAccessBeanSubclassInfo.xml or .jpaEntityReferences.xml metafile
			//the .jpaAccessBeanSubclassInfo.xml, .jpaEntityReferences.xml and .jpaEntityReferenceSubclasses.xml meta files for each 
			//of those projects are then parsed into the ApplicationInfo.ProjectInfo collection
			parseProjects(progressMonitor);				// 5000 ticks
			
			//searches all types that references the EJB entities (as listed in .jpaAccessBeanSubclassInfo.xml and .jpaEntityReferences.xml)
			//for any subclasses.  Those subclasses are added to ApplicationInfo.ProjectInfo.addEntityReferenceSubclass()
			searchForReferenceSubclasses(progressMonitor);	// 20000 ticks
			
			
			//writes the entity references subclasses to the .jpaEntityReferenceSubclasses.xml meta file.
			//The list of subclasses are contained in ApplicationInfo.ProjectInfo.addEntityReferenceSubclass()
			generateProjects(progressMonitor); 				// 1000 ticks
			
			iApplicationInfo.printSummary();
			iApplicationInfo = null;
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
	
	private void seedNewClasses(IProgressMonitor progressMonitor, String sourceFolderName) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			SeedNewClassesJob seedNewClassesJob = new SeedNewClassesJob(iApplicationInfo, iBuildPendingProjects, sourceFolderName);
			seedNewClassesJob.setProgressGroup(iProgressGroup, 100);
			seedNewClassesJob.schedule();
			seedNewClassesJob.join();
			progressMonitor.worked(100);
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
			EntityReferenceUtil.loadPredefinedEntityReferences(iApplicationInfo, root);
			IProject[] projects = root.getProjects();
			Set<ProjectInfo> projectInfos = new HashSet<ProjectInfo>();
			for (IProject project : projects) {
				IFile accessBeanSubclassInfoXmlFile = project.getFile(".jpaAccessBeanSubclassInfo.xml");
				IFile entityReferencesXmlFile = project.getFile(".jpaEntityReferences.xml");
				if (accessBeanSubclassInfoXmlFile.exists() || entityReferencesXmlFile.exists()) {
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

	private void searchForReferenceSubclasses(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ProjectInfo> projects = new HashSet<ProjectInfo>();
			projects.addAll(iApplicationInfo.getProjects());
			Set<SearchJob> searchJobs = new HashSet<SearchJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				SearchJob searchJob = new SearchJob(projects);
				searchJob.setProgressGroup(iProgressGroup, 20000 / PARALLEL_JOBS);
				searchJob.schedule();
				searchJobs.add(searchJob);
			}
			for (Job searchJob : searchJobs) {
				searchJob.join();
				progressMonitor.worked(20000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void generateProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<GenerateProjectJob> generateProjectJobs = new HashSet<GenerateProjectJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				GenerateProjectJob generateProjectJob = new GenerateProjectJob(iApplicationInfo.getProjects());
				generateProjectJob.setProgressGroup(iProgressGroup, 1000 / PARALLEL_JOBS);
				generateProjectJob.schedule();
				generateProjectJobs.add(generateProjectJob);
			}
			for (GenerateProjectJob generateProjectJob : generateProjectJobs) {
				generateProjectJob.join();
				progressMonitor.worked(1000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void buildProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<BuildProjectJob> buildProjectJobs = new HashSet<BuildProjectJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				BuildProjectJob buildProjectJob = new BuildProjectJob(iBuildPendingProjects);
				buildProjectJob.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				buildProjectJob.schedule();
				buildProjectJobs.add(buildProjectJob);
			}
			for (BuildProjectJob buidProjectJob : buildProjectJobs) {
				buidProjectJob.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
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
							restored = backupUtil.restore2(new SubProgressMonitor(progressMonitor, 1000));
							if (restored) {
								iBuildPendingProjects.add(project);
							}
							IFile entityReferenceSubclassesXmlFile = project.getFile(".jpaEntityReferenceSubclasses.xml");
							if (entityReferenceSubclassesXmlFile.exists()) {
								entityReferenceSubclassesXmlFile.delete(true, new SubProgressMonitor(progressMonitor, 10));
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
						status = Status.CANCEL_STATUS;
						project = null;
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
			}
			return status;
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
						moduleInfo = getModule();
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
	
	private static class SearchJob extends Job {
		private Collection<ProjectInfo> iProjects;
		
		public SearchJob(Set<ProjectInfo> projects) {
			super("Search for entity reference subclasses");
			iProjects = projects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Search for entity reference subclasses", IProgressMonitor.UNKNOWN);
				ProjectInfo projectInfo = getProjectInfo();
				while (projectInfo != null) {
					if (!progressMonitor.isCanceled()) {
						EntityReferenceSubclassSearchUtil searchUtil = new EntityReferenceSubclassSearchUtil(projectInfo);
						searchUtil.search(new SubProgressMonitor(progressMonitor, 1000));
					}
					else {
						status = Status.CANCEL_STATUS;
						projectInfo = null;
						break;
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
	
	private static class GenerateProjectJob extends Job {
		private Collection<ProjectInfo> iProjects;
		
		public GenerateProjectJob(Collection<ProjectInfo> projects) {
			super("generate project info");
			iProjects = projects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("generate project info", IProgressMonitor.UNKNOWN);
				ProjectInfo projectInfo = getProjectInfo();
				while (projectInfo != null) {
					EntityReferenceSubclassesXmlGenerator entityReferencesSubclassesXmlGenerator = new EntityReferenceSubclassesXmlGenerator(projectInfo);
					entityReferencesSubclassesXmlGenerator.generate(new SubProgressMonitor(progressMonitor, 1000));
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
}
