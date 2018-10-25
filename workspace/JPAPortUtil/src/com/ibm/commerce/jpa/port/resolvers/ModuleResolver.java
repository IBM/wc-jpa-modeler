package com.ibm.commerce.jpa.port.resolvers;

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

import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.ModuleInfo;

public class ModuleResolver {
	private ModuleInfo iModuleInfo;
	
	public ModuleResolver(ModuleInfo moduleInfo) {
		iModuleInfo = moduleInfo;
	}
	
	public void resolve() {
		Collection<EntityInfo> entities = iModuleInfo.getEntities();
		for (EntityInfo entityInfo : entities) {
			EntityResolver entityResolver = new EntityResolver(entityInfo);
			entityResolver.resolve();
		}
	}
}
