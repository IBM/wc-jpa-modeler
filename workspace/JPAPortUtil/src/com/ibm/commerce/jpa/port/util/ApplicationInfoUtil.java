package com.ibm.commerce.jpa.port.util;

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
import java.util.List;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.ibm.commerce.jpa.port.info.ApplicationInfo;
import com.ibm.commerce.jpa.port.info.EntityInfo;
import com.ibm.commerce.jpa.port.info.FieldInfo;

public class ApplicationInfoUtil {
	public static void resolveTypeMappings(ApplicationInfo applicationInfo, EntityInfo entityInfo) {
		if (entityInfo.getEjbType() != null) {
			String ejbType = entityInfo.getEjbType().getFullyQualifiedName('.');
			applicationInfo.addTypeMapping(ejbType, entityInfo.getEntityClassInfo().getQualifiedClassName());
			applicationInfo.addEntityType(ejbType);
			applicationInfo.setEntityInfoForType(ejbType, entityInfo);
		}
		if (entityInfo.getEjbBaseType() != null) {
			String ejbBaseType = entityInfo.getEjbBaseType().getFullyQualifiedName('.');
			applicationInfo.addTypeMapping(ejbBaseType, entityInfo.getEntityBaseClassInfo().getQualifiedClassName());
			applicationInfo.addEntityType(ejbBaseType);
			applicationInfo.setEntityInfoForType(ejbBaseType, entityInfo);
		}
		if (entityInfo.getPrimaryKeyType() != null && !entityInfo.getPrimaryKeyType().isBinary()) {
			String primaryKeyType = entityInfo.getPrimaryKeyType().getFullyQualifiedName('.');
			if (entityInfo.getEntityKeyClassInfo() != null) {
				applicationInfo.addTypeMapping(primaryKeyType, entityInfo.getEntityKeyClassInfo().getQualifiedClassName());
			}
			else if (entityInfo.getKeyFields().size() == 1) {
				List<FieldInfo> keyFields = entityInfo.getKeyFields();
				for (FieldInfo fieldInfo : keyFields) {
					applicationInfo.addTypeMapping(primaryKeyType, fieldInfo.getTypeName());
				}
			}
			applicationInfo.addEntityKeyType(primaryKeyType);
			applicationInfo.setEntityInfoForType(primaryKeyType, entityInfo);
		}
		if (entityInfo.getEjbFinderObjectType() != null && entityInfo.getEntityQueryHelperClassInfo() != null) {
			String ejbFinderObjectType = entityInfo.getEjbFinderObjectType().getFullyQualifiedName('.');
			applicationInfo.addTypeMapping(ejbFinderObjectType, entityInfo.getEntityQueryHelperClassInfo().getQualifiedClassName());
		}
		if (entityInfo.getEjbFinderObjectBaseType() != null && entityInfo.getEntityQueryHelperBaseClassInfo() != null) {
			String ejbFinderObjectBaseType = entityInfo.getEjbFinderObjectBaseType().getFullyQualifiedName('.');
			applicationInfo.addTypeMapping(ejbFinderObjectBaseType, entityInfo.getEntityQueryHelperBaseClassInfo().getQualifiedClassName());
		}
		if (entityInfo.getAccessBeanInfo() != null && entityInfo.getEntityAccessBeanClassInfo() != null) {
			String accessBeanType = entityInfo.getAccessBeanInfo().getQualifiedAccessBeanName();
			applicationInfo.addTypeMapping(accessBeanType, entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
			if (entityInfo.getAccessBeanInfo().getDataClassType()) {
				applicationInfo.addDataClassType(accessBeanType);
			}
			else {
				applicationInfo.addAccessBeanType(accessBeanType);
			}
			applicationInfo.setEntityInfoForType(accessBeanType, entityInfo);
//			if (entityInfo.getEntityDataType() != null) {
//				applicationInfo.addTypeMapping(entityInfo.getEntityDataType().getFullyQualifiedName('.'), entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
//			}
			Collection<String> accessBeanInterfaces = entityInfo.getAccessBeanInfo().getAccessBeanInterfaces();
			for (String accessBeanInterface : accessBeanInterfaces) {
				applicationInfo.addTypeMapping(accessBeanInterface, entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
				applicationInfo.addAccessBeanInterfaceType(accessBeanInterface);
			}
		}
		if (entityInfo.getHomeType() != null) {
			String homeType = entityInfo.getHomeType().getFullyQualifiedName('.');
			applicationInfo.addHomeInterfaceType(homeType);
			applicationInfo.setEntityInfoForType(homeType, entityInfo);
		}
		if (entityInfo.getLocalHomeType() != null) {
			String localHomeType = entityInfo.getLocalHomeType().getFullyQualifiedName('.');
			applicationInfo.addHomeInterfaceType(localHomeType);
			applicationInfo.setEntityInfoForType(localHomeType, entityInfo);
		}
		if (entityInfo.getRemoteType() != null) {
			String remoteType = entityInfo.getRemoteType().getFullyQualifiedName('.');
			applicationInfo.addEntityInterfaceType(remoteType);
//			applicationInfo.addTypeMapping(remoteType, entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
			applicationInfo.addTypeMapping(remoteType, entityInfo.getEntityClassInfo().getQualifiedClassName());
			applicationInfo.setEntityInfoForType(remoteType, entityInfo);
		}
		if (entityInfo.getLocalType() != null) {
			String localType = entityInfo.getLocalType().getFullyQualifiedName('.');
			applicationInfo.addEntityInterfaceType(localType);
			if (entityInfo.getAccessBeanInfo().getDataClassType()) {
				applicationInfo.addTypeMapping(localType, entityInfo.getEntityClassInfo().getQualifiedClassName());
			}
			else {
				applicationInfo.addTypeMapping(localType, entityInfo.getEntityAccessBeanClassInfo().getQualifiedClassName());
			}
			applicationInfo.setEntityInfoForType(localType, entityInfo);
		}
		if (entityInfo.getEntityAccessHelperClassInfo() != null) {
			applicationInfo.addTypeMapping(entityInfo.getEjbAccessHelperType().getFullyQualifiedName('.'), entityInfo.getEntityAccessHelperClassInfo().getQualifiedClassName());
		}
		if (entityInfo.getEntityEntityCreationDataClassInfo() != null) {
			applicationInfo.addTypeMapping(entityInfo.getEjbEntityCreationDataType().getFullyQualifiedName('.'), entityInfo.getEntityEntityCreationDataClassInfo().getQualifiedClassName());
		}
		if (entityInfo.getJndiName() != null) {
			applicationInfo.addEntityJndiName(entityInfo.getJndiName());
		}
	}
	
	public static void addJpaTypeMapping(ApplicationInfo applicationInfo, IType type) {
		String qualifiedName = type.getFullyQualifiedName('.');
		if (applicationInfo.isStubType(qualifiedName)) {
			applicationInfo.removeStubType(qualifiedName);
		}
		applicationInfo.addTypeMapping(qualifiedName, EntityUtil.getJpaName(type));
		try {
			IType[] types = type.getTypes();
			if (types != null) {
				for (IType innerType : types) {
					addJpaTypeMapping(applicationInfo, innerType);
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	public static void addJpaStubTypeMapping(ApplicationInfo applicationInfo, IType type) {
		String fullyQualifiedName = type.getFullyQualifiedName('.');
		if (applicationInfo.getTypeMapping(fullyQualifiedName) == null) {
			applicationInfo.addStubType(fullyQualifiedName);
			applicationInfo.addTypeMapping(fullyQualifiedName, EntityUtil.getJpaStubName(type));
			try {
				IType[] types = type.getTypes();
				if (types != null) {
					for (IType innerType : types) {
						addJpaStubTypeMapping(applicationInfo, innerType);
					}
				}
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}
}
