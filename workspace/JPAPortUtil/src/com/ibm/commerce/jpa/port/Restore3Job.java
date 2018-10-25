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

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.util.BackupUtil;

/**
 * This job is invoked from the "Restore Entity Beans" menu option.
 * 
 * 
 *
 */
public class Restore3Job extends Job {
	private static final int PARALLEL_JOBS = 4;
	private IWorkspace iWorkspace;
	private ApplicationInfo iApplicationInfo = new ApplicationInfo();
	private Collection<IProject> iBuildPendingProjects = Collections.synchronizedCollection(new HashSet<IProject>());
	private IProgressMonitor iProgressGroup;
	
	public Restore3Job() {
		super("Restore EJBs");
		iWorkspace = ResourcesPlugin.getWorkspace();
		iProgressGroup = Job.getJobManager().createProgressGroup();
		setProgressGroup(iProgressGroup, 6000);
	}
	
	public IStatus run(IProgressMonitor progressMonitor) {
		IStatus status = Status.OK_STATUS;
		try {
			iProgressGroup.beginTask("Restore EJBs", 6000);
			progressMonitor.beginTask("Restore EJBs", 6000);
			
			//restores all projects in the workspace via BackupUtil.restore3()
			//this causes all files listed in .jpaGeneratedFileList3 to be deleted and all files in .jpaBackup3 to be restored
			restoreProjects(progressMonitor);				// 1000 ticks
			
			//invokes a full build on the entire workspace
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
	
	/**
	 * Creates a set of all projects in the workspace.  Then creates a set of RestoreProjectJob jobs and passes the set of projects.  
	 * The size of the RestoreProjectJob set is equal to the PARALLEL_JOBS parameter.
	 * 
	 * @param progressMonitor
	 * @throws InterruptedException
	 */
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

	private void buildProjects(IProgressMonitor progressMonitor) throws InterruptedException {
		if (!progressMonitor.isCanceled()) {
			if (iBuildPendingProjects.size() > 0) {
				try {
					iWorkspace.build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(progressMonitor, 5000));
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
			}
//			Set<BuildProjectJob> buildProjectJobs = new HashSet<BuildProjectJob>();
//			for (int i = 0; i < PARALLEL_JOBS; i++) {
//				BuildProjectJob buidProjectJob = new BuildProjectJob(iBuildPendingProjects);
//				buidProjectJob.setProgressGroup(iProgressGroup, 5000 / PARALLEL_JOBS);
//				buidProjectJob.schedule();
//				buildProjectJobs.add(buidProjectJob);
//			}
//			for (BuildProjectJob buidProjectJob : buildProjectJobs) {
//				buidProjectJob.join();
//				progressMonitor.worked(5000 / PARALLEL_JOBS);
//			}
		}
	}
	
	/**
	 * Invokes BackupUtil.restore3 on the next project in the projects variable.  This restore deletes all files listed in .jpaGeneratedFiles3
	 * and restores all files from the .jpaBackup3 directory.
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
	
//	private static class BuildProjectJob extends Job {
//		private Collection<IProject> iBuildPendingProjects;
//		
//		public BuildProjectJob(Collection<IProject> buildPendingProjects) {
//			super("Build projects");
//			iBuildPendingProjects = buildPendingProjects;
//		}
//		
//		public IStatus run(IProgressMonitor progressMonitor) {
//			IStatus status = Status.OK_STATUS;
//			try {
//				progressMonitor.beginTask("Build projects", IProgressMonitor.UNKNOWN);
//				IProject project = getProject();
//				while (project != null) {
//					if (!progressMonitor.isCanceled() && status != Status.CANCEL_STATUS) {
//						try {
//							System.out.println("building "+project.getName());
//							project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(progressMonitor, 1000));
//						}
//						catch (CoreException e) {
//							e.printStackTrace();
//							status = Status.CANCEL_STATUS;
//						}
//					}
//					else {
//						status = Status.CANCEL_STATUS;
//					}
//					if (!progressMonitor.isCanceled() && status != Status.CANCEL_STATUS) {
//						project = getProject();
//					}
//					else {
//						project = null;
//					}
//				}
//			}
//			finally {
//				progressMonitor.done();
//				iBuildPendingProjects = null;
//			}
//			return status;
//		}
//		
//		private IProject getProject() {
//			IProject project = null;
//			synchronized(iBuildPendingProjects) {
//				for (IProject currentProject : iBuildPendingProjects) {
//					project = currentProject;
//					break;
//				}
//				if (project != null) {
//					iBuildPendingProjects.remove(project);
//				}
//			}
//			return project;
//		}
//	}
}
