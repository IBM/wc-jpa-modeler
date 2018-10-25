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

public class CreatorInfo {
	private final static String CREATE_ENTITY = "createEntity";
	
	private AccessBeanMethodInfo iAccessBeanMethodInfo;
	
	public void setAccessBeanMethodInfo(AccessBeanMethodInfo accessBeanMethodInfo) {
		iAccessBeanMethodInfo = accessBeanMethodInfo;
	}
	
	public void addAccessBeanMethodInfo(AccessBeanMethodInfo accessBeanMethodInfo) {
		accessBeanMethodInfo.setTargetMethodName(CREATE_ENTITY);
		if (iAccessBeanMethodInfo == null) {
			iAccessBeanMethodInfo = accessBeanMethodInfo;
		}
		else {
			iAccessBeanMethodInfo.addSuperAccessBeanMethodInfo(accessBeanMethodInfo);
		}
	}
	
	public AccessBeanMethodInfo getAccessBeanMethodInfo() {
		return iAccessBeanMethodInfo;
	}
	
	public boolean isInvalid() {
		return iAccessBeanMethodInfo == null ? true : iAccessBeanMethodInfo.isInvalid();
	}
}
