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

public class EjbLocalRefInfo {
	private String iId;
	private String iEjbRefName;
	private String iEjbRefType;
	private String iLocalHome;
	private String iLocal;
	private String iEjbLink;
	
	public EjbLocalRefInfo(String id) {
		iId = id;
	}
	
	public String getId() {
		return iId;
	}
	
	public void setEjbRefName(String ejbRefName) {
		iEjbRefName = ejbRefName;
	}
	
	public String getEjbRefName() {
		return iEjbRefName;
	}
	
	public void setEjbRefType(String ejbRefType) {
		iEjbRefType = ejbRefType;
	}
	
	public String getEjbRefType() {
		return iEjbRefType;
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
	
	public void setEjbLink(String ejbLink) {
		iEjbLink = ejbLink;
	}
	
	public String getEjbLink() {
		return iEjbLink;
	}
}
