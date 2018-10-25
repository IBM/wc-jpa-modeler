package com.ibm.commerce.jpa.port.info;

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

public class SessionInfo {
	private ModuleInfo iModuleInfo;
	private String iSessionId;
	private String iEjbName;
	private String iHome;
	private String iRemote;
	private String iLocalHome;
	private String iLocal;
	private String iEjbClass;
	private String iSessionType;
	private String iTransactionType;
	private String iAccessBeanName;
	private String iJndiName;

	public SessionInfo(ModuleInfo moduleInfo, String sessionId) {
		iModuleInfo = moduleInfo;
		iSessionId = sessionId;
	}
	
	public ModuleInfo getModuleInfo() {
		return iModuleInfo;
	}
	
	public String getSessionId() {
		return iSessionId;
	}
	
	public void setEjbName(String ejbName) {
		iEjbName = ejbName;
		if (ejbName != null) {
//			iModuleInfo.setSessionName(this, ejbName);
		}
	}
	
	public String getEjbName() {
		return iEjbName;
	}
	
	public void setHome(String home) {
		iHome = home;
	}
	
	public String getHome() {
		return iHome;
	}
	
	public void setRemote(String remote) {
		iRemote = remote;
	}
	
	public String getRemote() {
		return iRemote;
	}
	
	public void setLocalHome(String localHome) {
		iLocalHome = localHome;
	}
	
	public String getLocalHome() {
		return iLocalHome;
	}
	
	public void setLocal(String local) {
		iLocal = local;
	}
	
	public String getLocal() {
		return iLocal;
	}
	
	public void setEjbClass(String ejbClass) {
		iEjbClass = ejbClass;
	}
	
	public String getEjbClass() {
		return iEjbClass;
	}
	
	public void setSessionType(String sessionType) {
		iSessionType = sessionType;
	}
	
	public String getSessionType() {
		return iSessionType;
	}
	
	public void setTransactionType(String transactionType) {
		iTransactionType = transactionType;
	}
	
	public String getTransactionType() {
		return iTransactionType;
	}
	
	public void setAccessBeanName(String accessBeanName) {
		iAccessBeanName = accessBeanName;
	}
	
	public String getAccessBeanName() {
		return iAccessBeanName;
	}
	
	public void setJndiName(String jndiName) {
		iJndiName = jndiName;
	}
	
	public String getJndiName() {
		return iJndiName;
	}
}
