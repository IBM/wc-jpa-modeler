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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import com.ibm.commerce.jpa.port.generators.EntityReferencesXmlGenerator;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.parsers.ModuleInfoXmlParser;
import com.ibm.commerce.jpa.port.search.EntitySearchUtil;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.JPASeederUtilBase;

public class EntityReferenceSearchJob extends Job {
	private static final int PARALLEL_JOBS = 6;
	private static final Map<String, Collection<String>> PREDEFINED_ENTITY_REFERENCING_TYPES;
	static {
		PREDEFINED_ENTITY_REFERENCING_TYPES = new HashMap<String, Collection<String>>();
		Collection<String> entityReferencingTypes = new HashSet<String>();
	//	entityReferencingTypes.add("com.ibm.commerce.member.dataobjects.DOBase");
	//	PREDEFINED_ENTITY_REFERENCING_TYPES.put("Member-MemberManagementLogic", entityReferencingTypes);
	}
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private IProgressMonitor iProgressGroup;
	private Set<String> iBinaryReferences = new TreeSet<String>();
	private Set<IType> iTypeReferences = new HashSet<IType>();
	private Collection<IProject> iBuildPendingProjects = Collections.synchronizedCollection(new HashSet<IProject>());
	
	public EntityReferenceSearchJob() {
		super("Entity Search");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 32000);
	}
	
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			iProgressGroup.beginTask("Entity search jobs", 32000);
			progressMonitor.beginTask("Entity Search", 32000);
			initializeApplicationInfo();					// 0 ticks
			restoreProjects(progressMonitor);				// 1000 ticks
			seedNewClasses(progressMonitor, "SeedClasses2/projects"); // 100 ticks
			buildProjects(progressMonitor);				// 5000 ticks
			parseModules(progressMonitor);					// 5000 ticks
			searchForReferences(progressMonitor);			// 20000 ticks
			generateProjects(progressMonitor); 				// 1000 ticks
			System.out.println("binary references:");
			for (String binaryReference : iBinaryReferences) {
				System.out.println(binaryReference);
			}
			iApplicationInfo.printSummary();
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
			for (ParseModuleJob parseEjbModuleJob : parseModuleJobs) {
				parseEjbModuleJob.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void searchForReferences(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ModuleInfo> modules = new HashSet<ModuleInfo>();
			modules.addAll(iApplicationInfo.getModules());
			Set<EntityReferencesSearchJob> searchJobs = new HashSet<EntityReferencesSearchJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				EntityReferencesSearchJob searchJob = new EntityReferencesSearchJob(iApplicationInfo, modules, iBinaryReferences, iTypeReferences);
				searchJob.setProgressGroup(iProgressGroup, 20000 / PARALLEL_JOBS);
				searchJob.schedule();
				searchJobs.add(searchJob);
			}
			for (EntityReferencesSearchJob searchJob : searchJobs) {
				searchJob.join();
				progressMonitor.worked(20000 / PARALLEL_JOBS);
			}
			for (IType typeReference : iTypeReferences) {
				String typeName = typeReference.getFullyQualifiedName('.');
				if (!iApplicationInfo.isAccessBeanSubclass(typeName)) {
					iApplicationInfo.getProjectInfo(typeReference.getJavaProject().getProject()).addEntityReferencingType(typeName);
				}
			}
			iTypeReferences = null;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			Collection<String> projectNames = PREDEFINED_ENTITY_REFERENCING_TYPES.keySet();
			for (String projectName : projectNames) {
				IProject project = root.getProject(projectName);
				Collection<String> typeNames = PREDEFINED_ENTITY_REFERENCING_TYPES.get(projectName);
				for (String typeName : typeNames) {
					iApplicationInfo.getProjectInfo(project).addEntityReferencingType(typeName);
				}
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
							IFile entityReferencesXmlFile = project.getFile(".jpaEntityReferences.xml");
							if (entityReferencesXmlFile.exists()) {
								entityReferencesXmlFile.delete(true, new SubProgressMonitor(progressMonitor, 10));
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

	private static class ParseModuleJob extends Job {
		private Set<ModuleInfo> iModules;

		public ParseModuleJob(Set<ModuleInfo> modules) {
			super("Parse Ejb Module");
			iModules = modules;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Parse projects", IProgressMonitor.UNKNOWN);
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
	
	private static class EntityReferencesSearchJob extends Job {
		private ApplicationInfo iApplicationInfo;
		private Set<ModuleInfo> iModules;
		private Set<String> iBinaryReferences;
		private Set<IType> iTypeReferences;
		
		public EntityReferencesSearchJob(ApplicationInfo applicationInfo, Set<ModuleInfo> modules, Set<String> binaryReferences, Set<IType> typeReferences) {
			super("Search for entity references");
			iApplicationInfo = applicationInfo;
			iModules = modules;
			iBinaryReferences = binaryReferences;
			iTypeReferences = typeReferences;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Search for entity references", IProgressMonitor.UNKNOWN);
				ModuleInfo moduleInfo = getModuleInfo();
				Collection<EntityInfo> entities = new HashSet<EntityInfo>();
				while (moduleInfo != null) {
					entities.addAll(moduleInfo.getEntities());
					if (!progressMonitor.isCanceled()) {
						if (entities.size() > 50) {
							EntitySearchUtil entitySearchUtil = new EntitySearchUtil(iApplicationInfo, entities, iBinaryReferences, iTypeReferences);
							entitySearchUtil.search(new SubProgressMonitor(progressMonitor, 1000));
							entities = new HashSet<EntityInfo>();
						}
					}
					else {
						status = Status.CANCEL_STATUS;
						moduleInfo = null;
						break;
					}
					if (!progressMonitor.isCanceled()) {
						moduleInfo = getModuleInfo();
					}
					else {
						status = Status.CANCEL_STATUS;
						moduleInfo = null;
					}
				}
				if (entities.size() > 0 && status != Status.CANCEL_STATUS && !progressMonitor.isCanceled()) {
					EntitySearchUtil entitySearchUtil = new EntitySearchUtil(iApplicationInfo, entities, iBinaryReferences, iTypeReferences);
					entitySearchUtil.search(new SubProgressMonitor(progressMonitor, 1000));
				}
			}
			finally {
				progressMonitor.done();
				iApplicationInfo = null;
				iModules = null;
				iBinaryReferences = null;
				iTypeReferences = null;
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
					EntityReferencesXmlGenerator projectInfoXmlGenerator = new EntityReferencesXmlGenerator(projectInfo);
					projectInfoXmlGenerator.generate(new SubProgressMonitor(progressMonitor, 1000));
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
