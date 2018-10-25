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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.ibm.commerce.jpa.port.generators.ModuleGenerator;
import com.ibm.commerce.jpa.port.generators.PersistenceXmlGenerator;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;
import com.ibm.commerce.jpa.port.info.PersistenceUnitInfo;
import com.ibm.commerce.jpa.port.parsers.ModuleParser;
import com.ibm.commerce.jpa.port.parsers.PortConfigurationParser;
import com.ibm.commerce.jpa.port.resolvers.ApplicationResolver;
import com.ibm.commerce.jpa.port.util.BackupUtil;
import com.ibm.commerce.jpa.port.util.JPASeederUtilBase;
import com.ibm.commerce.jpa.port.util.ResolveImportsUtil;

public class JPAGenerateJob extends Job {
	//2018-05-01 - bsteinba@us.ibm.com - shouldn't need this as WCSEData project should already have a persistence.xml in v9
//	private static final String PERSISTENCE_UNIT_ROOT_PROJECT = "WCPersistenceUnitRoot";
	private static final int PARALLEL_JOBS = 4;
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private IProgressMonitor iProgressGroup;
	private Collection<IProject> iBuildPendingProjects = Collections.synchronizedCollection(new HashSet<IProject>());
	
	public JPAGenerateJob() {
		super("Generate JPA Entities and Access Beans");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 43700);
	}
	
	/**
	 * This method is invoked by the eclipse framework when the JPAGenerateWizard calls the JPAGenerateJob.schedule() method.  It is the 
	 * entry point for the entire JPA entity and access bean generation flow.
	 */
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			//the stats of this progress monitor will be visible to the user in the Progress tab of eclipse
			iProgressGroup.beginTask("JPA port jobs", 43700);
			progressMonitor.beginTask("JPA Port", 43700);
			
			//this loads the metadata info for the WCPersistentUnitRoot project
			initializeApplicationInfo();					// 0 ticks
			
			//this will delete all previously generated files and restore any changed files for all projects in the workspace, using the metadata this utility creates and manages
			restoreProjects(progressMonitor);				// 1000 ticks
			
			//this copies some v9 OOB source code into the project so that the new JPA artifacts will compile
			//we can't do this as we would be distributing v9 source code, so we will have consumers of this utility
			//add the v9 compiled jars to their classpath instead, or bundle them with this utility
			//seedNewClasses(progressMonitor, "SeedClasses/projects"); // 100 ticks
			
			//this invokes a build (compile) on all projects in the workspace
			buildProjects(progressMonitor);				// 5000 ticks
			
			//parses the access beans and entity EJB's in all projects in the workspace
			parseEJBProjects(progressMonitor);				// 5000 ticks
			
			
			//this reads the xml files in the <PLUGIN_ROOT>/configuration directory and stores the foreignKey relationships for OOB tables in their 
			//associated TableInfo objects - not necessary for WCE
			//parseConfiguration(progressMonitor);			// 500 ticks
			
			//resolves entity relationship references
			resolveApplication(progressMonitor);			// 1000 ticks
			
			//this runs the actual JPA entity and access bean code generation
			portEJBProjects(progressMonitor);				// 20000 ticks
			
			//this creates/updates the persistence.xml in the WCPersistentUnitRoot project
			//the persistence.xml that it generates only contains a jar-file entry to the jar of the module, which already exists for WCSEData
			//this is useless for WCE purposes
			//generatePersistenceUnitXml(progressMonitor); 	// 100 ticks
			
			//this copies some v9 OOB source code into the project so that the new JPA artifacts will compile
			//we can't do this as we would be distributing v9 source code, so we will have consumers of this utility
			//add the v9 compiled jars to their classpath instead, or bundle them with this utility
			//seedNewClasses(progressMonitor, "SeedClasses2/projects"); // 100 ticks
			
			//uses System.out to log a summary of the results of this run
			iApplicationInfo.printSummary();
			iApplicationInfo = null;
			
			//runs a build (compile) on all projects in the workspace
			buildProjects(progressMonitor);				// 5000 ticks
			
			//adds/cleans up the imports of all generated files
			resolveImports(progressMonitor);				// 1000 ticks
			
			//runs a build (compile) on all projects in the workspace
			buildProjects(progressMonitor);				// 5000 ticks
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
		//2018-05-01 - bsteinba@us.ibm.com - shouldn't need this as WCSEData project should already have a persistence.xml in v9
//		IWorkspaceRoot root = iWorkspace.getRoot();
//		PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfo(JavaCore.create(root.getProject(PERSISTENCE_UNIT_ROOT_PROJECT)));
//		iApplicationInfo.setPersistenceUnitInfo(persistenceUnitInfo);
	}
	
	private void restoreProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		IWorkspaceRoot root = iWorkspace.getRoot();
		IProject[] projects = root.getProjects();
		Set<IProject> projectSet = new HashSet<IProject>();
		for (IProject project : projects) {
			projectSet.add(project);
		}
		Set<RestoreProjectJob> restoreProjectJobs = new HashSet<RestoreProjectJob>();
		//loop through all projects in workspace and run a RestoreProjectJob against them
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
	
	private void parseEJBProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			IWorkspaceRoot root = iWorkspace.getRoot();
			IProject[] projects = root.getProjects();
			Set<ModuleInfo> modules = new HashSet<ModuleInfo>();
			for (IProject project : projects) {
				if (isEJBProject(project)) {
					IJavaProject javaProject = JavaCore.create(project);
					ModuleInfo moduleInfo = new ModuleInfo(iApplicationInfo, javaProject);
					iApplicationInfo.addModule(moduleInfo);
					modules.add(moduleInfo);
				}
			}
			Set<ParseModuleJob> parseModuleJobs = new HashSet<ParseModuleJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				ParseModuleJob parseModuleJob = new ParseModuleJob(iWorkspace, modules);
				parseModuleJob.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
				parseModuleJob.schedule();
				parseModuleJobs.add(parseModuleJob);
			}
			for (ParseModuleJob parseModuleJob : parseModuleJobs) {
				parseModuleJob.join();
				progressMonitor.worked(5000 / PARALLEL_JOBS);
			}
			StringBuffer errors = new StringBuffer("");			
			Iterator<ModuleInfo> i = iApplicationInfo.getModules().iterator();
			while(i.hasNext()) {
				ModuleInfo moduleInfo = i.next();
				for(EntityInfo entity : moduleInfo.getEntities()) {  
					
					if(entity.getErrors() != null) {
						errors.append(entity.getErrors() + System.lineSeparator());
					}
					if(entity.getAccessBeanInfo() != null && entity.getAccessBeanInfo().getErrors() != null) {
						errors.append(entity.getAccessBeanInfo().getErrors() + System.lineSeparator());
					}
					if(entity.getTableInfo() != null && entity.getTableInfo().getErrors() != null) {
						errors.append(entity.getTableInfo().getErrors() + System.lineSeparator());
					}					
				}
			}
			//show errors
			final String errorString = errors.toString();
			if(!errorString.isEmpty()) {
				Display.getDefault().asyncExec(new Runnable() {
				    public void run() {
				    	MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Warning", errorString + System.lineSeparator() + "Job will still run to completion");
				    }
				});
			}
		}
	}
	
	private void parseConfiguration(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			ParseConfigurationJob parseConfigurationJob = new ParseConfigurationJob(iApplicationInfo);
			parseConfigurationJob.setProgressGroup(iProgressGroup, 500);
			parseConfigurationJob.schedule();
			parseConfigurationJob.join();
			progressMonitor.worked(500);
		}
	}
	
	private void resolveApplication(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			ResolveApplicationJob resolveApplicationJob = new ResolveApplicationJob(iApplicationInfo);
			resolveApplicationJob.setProgressGroup(iProgressGroup, 1000);
			resolveApplicationJob.schedule();
			resolveApplicationJob.join();
			progressMonitor.worked(1000);
		}
	}

	private void portEJBProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			Set<PortModuleJob> portModuleJobs = new HashSet<PortModuleJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				PortModuleJob portModuleJob = new PortModuleJob(iWorkspace, iApplicationInfo, iBuildPendingProjects);
				portModuleJob.setProgressGroup(iProgressGroup, 20000 / PARALLEL_JOBS);
				portModuleJob.schedule();
				portModuleJobs.add(portModuleJob);
			}
			for (PortModuleJob portModuleJob : portModuleJobs) {
				portModuleJob.join();
				progressMonitor.worked(20000 / PARALLEL_JOBS);
			}
			Set<ModuleInfo> modules = iApplicationInfo.getModules();
		
			for(ModuleInfo moduleInfo: modules) {
				moduleInfo.getEntities().clear();
			}
		}
	}
	
	private void resolveImports(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			IWorkspaceRoot root = iWorkspace.getRoot();
			IProject[] projects = root.getProjects();
			Set<IProject> projectSet = new HashSet<IProject>();
			for (IProject project : projects) {
				projectSet.add(project);
			}
			Set<ResolveImportsJob> resolveImportsJobs = new HashSet<ResolveImportsJob>();
			for (int i = 0; i < PARALLEL_JOBS; i++) {
				ResolveImportsJob resolveImportsJob = new ResolveImportsJob(projectSet, iBuildPendingProjects);
				resolveImportsJob.setProgressGroup(iProgressGroup, 1000 / PARALLEL_JOBS);
				resolveImportsJob.schedule();
				resolveImportsJobs.add(resolveImportsJob);
			}
			for (ResolveImportsJob resolveImportsJob : resolveImportsJobs) {
				resolveImportsJob.join();
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
	
	private void generatePersistenceUnitXml(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			GeneratePersistenceXmlJob generatePersistenceXmlJob = new GeneratePersistenceXmlJob(iApplicationInfo.getPersistenceUnitInfo());
			generatePersistenceXmlJob.setProgressGroup(iProgressGroup, 100);
			generatePersistenceXmlJob.schedule();
			progressMonitor.worked(100);
			generatePersistenceXmlJob.join();
			iApplicationInfo.incrementGeneratedAssetCount();
		}
	}

	private boolean isEJBProject(IProject project) {
		boolean ejbProject = false;
		try {
			if(project.isOpen()) {
				if (project.isNatureEnabled("org.eclipse.jdt.core.javanature") && project.isNatureEnabled("org.eclipse.wst.common.project.facet.core.nature")) {
					IFacetedProject facetedProject = ProjectFacetsManager.create(project);
					Set<IProjectFacet> facets = facetedProject.getFixedProjectFacets();
					for (IProjectFacet facet : facets) {
						if (facet.getId().equals("jst.ejb")) {
							ejbProject = true;
						}
					}
				}
				if (!ejbProject) {
					IFile jpaModuleInfo = project.getFile(".jpaModuleInfo.xml");
					if (jpaModuleInfo.exists()) {
						ejbProject = true;
					}
				}
			}
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		return ejbProject;
	}
	
	/**
	 * This job loops through all of the projects in the workspace in a thread-safe mannner.
	 * 
	 * It deletes all files in the .jpaGeneratedFileList* metadata files within each project.
	 * It then restores all the files in the .jpaBackup2 backup metadata directories.
	 * It then deletes the generated meta file and backup meta directory.
	 * 
	 * @author ADMINIBM
	 *
	 */
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
							restored = backupUtil.restore(new SubProgressMonitor(progressMonitor, 1000));
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
					JPASeederUtilBase seederUtil = new JPASeederUtilBase(iApplicationInfo, iBuildPendingProjects, iSourceFolderName, true);
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
	
	/**
	 * Parses all of the EJB deployment descriptors to build up a list of EntityInfo objects, which is sets in the ModuleInfo object.
	 * Then uses AST to look up the home/remote/access bean/etc compilation units to augment the data in the entity info with method parameters and response times.
	 * Also builds a list of everything that needs deleted when replacements are generated.
	 * 
	 * @author ADMINIBM
	 *
	 */
	private static class ParseModuleJob extends Job {
		private IWorkspace iWorkspace;
		private Set<ModuleInfo> iModules;

		public ParseModuleJob(IWorkspace workspace, Set<ModuleInfo> modules) {
			super("Parse project");
			iWorkspace = workspace;
			iModules = modules;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Parse projects", IProgressMonitor.UNKNOWN);
				ModuleInfo moduleInfo = getModule();
				while (moduleInfo != null) {
					if (!progressMonitor.isCanceled()) {
						ModuleParser moduleParser = new ModuleParser(iWorkspace, moduleInfo);
						moduleParser.parse(new SubProgressMonitor(progressMonitor, 1000));
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
	
	private static class ParseConfigurationJob extends Job {
		private ApplicationInfo iApplicationInfo;
		
		public ParseConfigurationJob(ApplicationInfo applicationInfo) {
			super("Parse configuration");
			iApplicationInfo = applicationInfo;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Parse configuration", IProgressMonitor.UNKNOWN);
				if (!progressMonitor.isCanceled()) {
					try {
						Activator plugin = Activator.getDefault();
						File pluginLocation = FileLocator.getBundleFile(plugin.getBundle());
						if (pluginLocation.isDirectory()) {
//							File allForeignKeysFile = null;
							File configurationDirectory = new File(pluginLocation, "configuration");
							File[] configurationFiles = configurationDirectory.listFiles();
							if (configurationFiles != null) {
								for (File configurationFile : configurationFiles) {
									if (!progressMonitor.isCanceled()) {
										if (configurationFile.isFile() && configurationFile.getName().endsWith(".xml")) {
//											if (configurationFile.getName().endsWith("allForeignKeys.xml")) {
//												allForeignKeysFile = configurationFile;
//											}
//											else {
											PortConfigurationParser portConfigurationParser = new PortConfigurationParser(iApplicationInfo, configurationFile);
											portConfigurationParser.parse();
											progressMonitor.worked(100);
//											}
										}
									}
									else {
										status = Status.CANCEL_STATUS;
										break;
									}
								}
//								if (allForeignKeysFile != null) {
//									PortConfigurationParser portConfigurationParser = new PortConfigurationParser(iApplicationInfo, allForeignKeysFile);
//									portConfigurationParser.parse();
//									progressMonitor.worked(100);
//								}
							}
						}
					}
					catch (IOException e) {
						e.printStackTrace();
					}
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
	
	private static class ResolveApplicationJob extends Job {
		private ApplicationInfo iApplicationInfo;
		
		public ResolveApplicationJob(ApplicationInfo applicationInfo) {
			super("Resolve application");
			iApplicationInfo = applicationInfo;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Resolve application", IProgressMonitor.UNKNOWN);
				if (!progressMonitor.isCanceled()) {
					ApplicationResolver applicationResolver = new ApplicationResolver(iApplicationInfo);
					applicationResolver.resolve();
					progressMonitor.worked(1000);
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
	
	/**
	 * Creates a ModuleGenerator for each ModuleInfo.  The ModuleGenerator loops through each entity in the module and uses the
	 * Entity*Generator classes to generate the source code for each relevant entity.
	 *
	 */
	private static class PortModuleJob extends Job {
		private IWorkspace iWorkspace;
		private ApplicationInfo iApplicationInfo;
		private Collection<IProject> iBuildPendingProjects;
		
		public PortModuleJob(IWorkspace workspace, ApplicationInfo applicationInfo, Collection<IProject> buildPendingProjects) {
			super("Port modules");
			iWorkspace = workspace;
			iApplicationInfo = applicationInfo;
			iBuildPendingProjects = buildPendingProjects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Port modules", IProgressMonitor.UNKNOWN);
				ModuleInfo module = getModule();
				while (module != null) {
					if (module.getEntities().size() > 0) {
						if (!progressMonitor.isCanceled()) {
							ModuleGenerator moduleGenerator = new ModuleGenerator(iWorkspace, module);
							status = moduleGenerator.generate(new SubProgressMonitor(progressMonitor, 1000));
							//iApplicationInfo.getPersistenceUnitInfo().addEntityJarProject(module.getJavaProject());
							iBuildPendingProjects.add(module.getJavaProject().getProject());
						}
						else {
							status = Status.CANCEL_STATUS;
						}
					}
					module = getModule();
				}
			}
			finally {
				progressMonitor.done();
				iWorkspace = null;
				iApplicationInfo = null;
				iBuildPendingProjects = null;
			}
			return status;
		}
		
		private ModuleInfo getModule() {
			ModuleInfo module = null;
			Set<ModuleInfo> modules = iApplicationInfo.getModules();
			synchronized(modules) {
				for (ModuleInfo currentModule : modules) {
					module = currentModule;
					break;
				}
				if (module != null) {
					modules.remove(module);
				}
			}
			return module;
		}
	}
	
	/**
	 * Invokes a custom "resolve imports" function on every generated file in every module so that any new imports will
	 * be added so that the generated code will compile.
	 * 
	 * @author ADMINIBM
	 *
	 */
	private static class ResolveImportsJob extends Job {
		private Collection<IProject> iProjects;
		private Collection<IProject> iBuildPendingProjects;
		private ASTParser iASTParser = ASTParser.newParser(AST.JLS3);

		public ResolveImportsJob(Collection<IProject> projects, Collection<IProject> buildPendingProjects) {
			super("Resolve imports");
			iProjects = projects;
			iBuildPendingProjects = buildPendingProjects;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Resolve imports", IProgressMonitor.UNKNOWN);
				IProject project = getProject();
				while (project != null) {
					if (!progressMonitor.isCanceled()) {
						IFile generatedFileList = project.getFile(".jpaGeneratedFileList");
						if (generatedFileList.exists()) {
							ResolveImportsUtil resolveImportsUtil = new ResolveImportsUtil(iASTParser, project, project.getFile(".jpaGeneratedFileList"));
							try {
								resolveImportsUtil.resolveImports(new SubProgressMonitor(progressMonitor, 1000));
								iBuildPendingProjects.add(project);
							}
							catch (CoreException e) {
								e.printStackTrace();
								status = Status.CANCEL_STATUS;
							}
						}
						project = getProject();
					}
					else {
						status = Status.CANCEL_STATUS;
						break;
					}
				}
			}
			finally {
				progressMonitor.done();
				iProjects = null;
				iBuildPendingProjects = null;
				iASTParser = null;
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
	
	private static class GeneratePersistenceXmlJob extends Job {
		private PersistenceUnitInfo iPersistenceUnitInfo;
		
		public GeneratePersistenceXmlJob(PersistenceUnitInfo persistenceUnitInfo) {
			super("Generate persistence.xml");
			iPersistenceUnitInfo = persistenceUnitInfo;
		}
		
		public IStatus run(IProgressMonitor progressMonitor) {
			IStatus status = Status.OK_STATUS;
			try {
				progressMonitor.beginTask("Generate persistence.xml", IProgressMonitor.UNKNOWN);
				if (!progressMonitor.isCanceled()) {
					PersistenceXmlGenerator persistenceXmlGenerator = new PersistenceXmlGenerator(iPersistenceUnitInfo);
					persistenceXmlGenerator.generate(progressMonitor);
				}
				else {
					status = Status.CANCEL_STATUS;
				}
			}
			finally {
				progressMonitor.done();
				iPersistenceUnitInfo = null;
			}
			return status;
		}
	}
}
