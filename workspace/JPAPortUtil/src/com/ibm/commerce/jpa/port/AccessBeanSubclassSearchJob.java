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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.ibm.commerce.jpa.port.generators.AccessBeanSubclassInfoXmlGenerator;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.ProjectInfo;
import com.ibm.commerce.jpa.port.parsers.ModuleInfoXmlParser;
import com.ibm.commerce.jpa.port.parsers.ProjectParser;
import com.ibm.commerce.jpa.port.search.AccessBeanSubclassSearchUtil;

/**
* Searches for any subclasses of the pre-located access beans using JDT.  These subclasses are then parsed and stored in a metafile.
* 
* The Generate JPA Entities step must be run before this job to create the .jpaModuleInfo.xml
*/
public class AccessBeanSubclassSearchJob extends Job {
	private static final int PARALLEL_JOBS = 6;
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private IProgressMonitor iProgressGroup;
	
	public AccessBeanSubclassSearchJob() {
		super("Entity Search");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 31000);
	}
	
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			iProgressGroup.beginTask("Entity search jobs", 31000);
			progressMonitor.beginTask("Entity Search", 31000);
			//creates an empty ApplicationInfo object
			initializeApplicationInfo();					// 0 ticks
			
			//parses the .jpaModuleInfo.xml metadata file for each project from the project root
			//this means that the Generate JPA Entities job must be run first to generate that file
			//the resulting parsed data is store in the ModuleInfo collection within the ApplicationInfo
			parseModules(progressMonitor);					// 5000 ticks
			
			//uses JDT and parallel processing to search for subclass types of all access bean types
			//Any located AB subclass types are stored in ApplicationInfo.getProject().getAccessBeanSubclassInfo(subtypeName, true);
			searchForAccessBeanSubclasses(progressMonitor);	// 20000 ticks
			
			//parses the AB subclass info from each project and stores it in the ApplicationInfo.ProjectInfo
			parseProjects(progressMonitor);					// 5000 ticks
			
			//writes all of the collected AB subclass info to a .jpaAccessBeanSubclassInfo.xml metadata file at the root of each project
			generateProjects(progressMonitor); 				// 1000 ticks
			
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
	
	private void searchForAccessBeanSubclasses(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ModuleInfo> modules = new HashSet<ModuleInfo>();
			modules.addAll(iApplicationInfo.getModules());
			Set<SearchJob> searchJobs = new HashSet<SearchJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				SearchJob searchJob = new SearchJob(modules);
				searchJob.setProgressGroup(iProgressGroup, 20000 / PARALLEL_JOBS);
				searchJob.schedule();
				searchJobs.add(searchJob);
			}
			for (SearchJob searchJob : searchJobs) {
				searchJob.join();
				progressMonitor.worked(20000 / PARALLEL_JOBS);
			}
		}
	}
	
	private void parseProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<ProjectInfo> projects = new HashSet<ProjectInfo>();
			projects.addAll(iApplicationInfo.getProjects());
			Set<ParseProjectJob> parseProjectJobs = new HashSet<ParseProjectJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				ParseProjectJob parseProjectJob = new ParseProjectJob(projects);
				parseProjectJob.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				parseProjectJob.schedule();
				parseProjectJobs.add(parseProjectJob);
			}
			for (Job job : parseProjectJobs) {
				job.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
		}
	}
	
	
	private void generateProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Collection<ProjectInfo> projects = iApplicationInfo.getProjects();
			for (ProjectInfo projectInfo : projects) {
				if (!progressMonitor.isCanceled()) {
					AccessBeanSubclassInfoXmlGenerator accessBeanSubclassInfoXmlGenerator = new AccessBeanSubclassInfoXmlGenerator(projectInfo);
					accessBeanSubclassInfoXmlGenerator.generate(new SubProgressMonitor(progressMonitor, 1000 / projects.size()));
				}
			}
//			Collection<ProjectInfo> projects = new HashSet<ProjectInfo>();
//			projects.addAll(iApplicationInfo.getProjects());
//			int projectCount = projects.size();
//			Collection<GenerateProjectJob> generateProjectJobs = new HashSet<GenerateProjectJob>();
//			GenerateProjectJobChangeListener listener = new GenerateProjectJobChangeListener(projects);
//			for (int i = 0; i < PARALLEL_JOBS; i++) {
//				GenerateProjectJob generateProjectJob = new GenerateProjectJob();
//				generateProjectJob.addJobChangeListener(listener);
//				synchronized (projects) {
//					for (ProjectInfo projectInfo : projects) {
//						projects.remove(projectInfo);
//						generateProjectJob.setProjectInfo(projectInfo);
//						generateProjectJob.setProgressGroup(iProgressGroup, 1000 / projectCount);
//						generateProjectJob.schedule();
//						generateProjectJobs.add(generateProjectJob);
//						break;
//					}
//				}
//			}
//			while (listener.hasActiveJob()) {
//				for (Job job : generateProjectJobs) {
//					job.join();
//				}
//			}
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
	
	private static class SearchJob extends Job {
		private Set<ModuleInfo> iModules;
		
		public SearchJob(Set<ModuleInfo> modules) {
			super("Search for access bean subclasses");
			iModules = modules;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Search for access bean subclasses", IProgressMonitor.UNKNOWN);
				ModuleInfo moduleInfo = getModuleInfo();
				while (moduleInfo != null) {
					if (!progressMonitor.isCanceled()) {
						AccessBeanSubclassSearchUtil searchUtil = new AccessBeanSubclassSearchUtil(moduleInfo);
						searchUtil.search(new SubProgressMonitor(progressMonitor, 1000));
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
			}
			finally {
				progressMonitor.done();
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
	
	private static class ParseProjectJob extends Job {
		private Collection<ProjectInfo> iProjects;
		
		public ParseProjectJob(Collection<ProjectInfo> projects) {
			super("parse project info");
			iProjects = projects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("parse project info", IProgressMonitor.UNKNOWN);
				ProjectInfo projectInfo = getProjectInfo();
				while (projectInfo != null) {
					ProjectParser projectParser = new ProjectParser(projectInfo);
					projectParser.parse(new SubProgressMonitor(progressMonitor, 1000));
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
	
//	private static class GenerateProjectJob extends Job {
//		private ProjectInfo iProjectInfo;
//		
//		public GenerateProjectJob() {
//			super("generate project info");
//		}
//		
//		public void setProjectInfo(ProjectInfo projectInfo) {
//			iProjectInfo = projectInfo;
//		}
//		
//		public IStatus run(IProgressMonitor progressMonitor) {
//			IStatus status = Status.OK_STATUS;
//			try {
//				progressMonitor.beginTask("generate project info " + iProjectInfo.getProject().getName(), 1000);
//				AccessBeanSubclassInfoXmlGenerator accessBeanSubclassInfoXmlGenerator = new AccessBeanSubclassInfoXmlGenerator(iProjectInfo);
//				accessBeanSubclassInfoXmlGenerator.generate(new SubProgressMonitor(progressMonitor, 1000));
//				if (progressMonitor.isCanceled()) {
//					status = Status.CANCEL_STATUS;
//				}
//			}
//			finally {
//				progressMonitor.worked(1000);
//				progressMonitor.done();
//				iProjectInfo = null;
//			}
//			return status;
//		}
//	}
//	
//	private class GenerateProjectJobChangeListener extends JobChangeAdapter {
//		private Collection<ProjectInfo> iProjects;
//		private int iProjectCount;
//		private int iActiveJobCount;
//		
//		public GenerateProjectJobChangeListener(Collection<ProjectInfo> projects) {
//			iProjects = projects;
//			iProjectCount = projects.size();
//		}
//		
//		public boolean hasActiveJob() {
//			return iActiveJobCount > 0;
//		}
//		
//		public void scheduled(IJobChangeEvent event) {
//			synchronized (this) {
//				iActiveJobCount++;
//			}
//		}
//		
//		public void done(IJobChangeEvent event) {
//			if (event.getResult() == Status.OK_STATUS) {
//				synchronized (iProjects) {
//					if (!iProjects.isEmpty()) {
//						GenerateProjectJob job = (GenerateProjectJob) event.getJob();
//						for (ProjectInfo projectInfo : iProjects) {
//							job.setProjectInfo(projectInfo);
//							job.setProgressGroup(iProgressGroup, iProjectCount / 1000);
//							job.schedule();
//							iProjects.remove(projectInfo);
//							break;
//						}
//					}
//				}
//			}
//			synchronized (this) {
//				iActiveJobCount--;
//			}
//		}
//	}
}
