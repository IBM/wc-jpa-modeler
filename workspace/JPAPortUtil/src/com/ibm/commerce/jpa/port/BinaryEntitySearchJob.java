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
import java.util.TreeSet;

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

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.parsers.ModuleInfoXmlParser;
import com.ibm.commerce.jpa.port.search.BinaryEntitySearchUtil;

/**
 * This job contains the controller logic that is invoked when the "Binary Entity Search" menu option is selected.
 * 
 * 
 *
 */
public class BinaryEntitySearchJob extends Job {
	private static final int PARALLEL_JOBS = 6;
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private IProgressMonitor iProgressGroup;
	private Set<String> iBinaryReferences = new TreeSet<String>();
	
	public BinaryEntitySearchJob() {
		super("Entity Search");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 2500);
	}
	
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			iProgressGroup.beginTask("Entity search jobs", 25000);
			progressMonitor.beginTask("Entity Search", 25000);
			
			//Create an empty ApplicationInfo object
			initializeApplicationInfo();					// 0 ticks
			
			//parses project metadata into a .jpaModuleInfo.xml file that is created
			//at the root of each project - this metadata is held in memory in the 
			//ModuleInfo collection within the ApplicationInfo
			parseModules(progressMonitor);					// 5000 ticks
			
			//runs the EntityReferencesSearchJob against all modules
			//the results are stored in the iBinaryReferences collection, and not in a meta-file anywhere - seems this would make this an information style job only
			searchForReferences(progressMonitor);			// 20000 ticks
			
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
				EntityReferencesSearchJob searchJob = new EntityReferencesSearchJob(iApplicationInfo, modules, iBinaryReferences);
				searchJob.setProgressGroup(iProgressGroup, 20000 / PARALLEL_JOBS);
				searchJob.schedule();
				searchJobs.add(searchJob);
			}
			for (EntityReferencesSearchJob searchJob : searchJobs) {
				searchJob.join();
				progressMonitor.worked(20000 / PARALLEL_JOBS);
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
	
	/**
	 * Uses JDT to find all references to the Entity (remote, home, local, key, etc) and AB classes.  It stores these references
	 * in the binaryReferences collection
	 * 
	 *
	 */
	private static class EntityReferencesSearchJob extends Job {
		private ApplicationInfo iApplicationInfo;
		private Set<ModuleInfo> iModules;
		private Set<String> iBinaryReferences;
		
		public EntityReferencesSearchJob(ApplicationInfo applicationInfo, Set<ModuleInfo> modules, Set<String> binaryReferences) {
			super("Search for entity references");
			iApplicationInfo = applicationInfo;
			iModules = modules;
			iBinaryReferences = binaryReferences;
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
							BinaryEntitySearchUtil entitySearchUtil = new BinaryEntitySearchUtil(iApplicationInfo, entities, iBinaryReferences);
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
					BinaryEntitySearchUtil entitySearchUtil = new BinaryEntitySearchUtil(iApplicationInfo, entities, iBinaryReferences);
					entitySearchUtil.search(new SubProgressMonitor(progressMonitor, 1000));
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
}
