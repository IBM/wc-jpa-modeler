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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import com.ibm.commerce.jpa.port.generators.PortVisitor;
import com.ibm.commerce.jpa.port.info.ApplicationInfo;

public class RefreshOnceUtil {
	private static final String REFRESH_ONCE_ACCESS_BEAN_HELPER = "com.ibm.commerce.datatype.RefreshOnceAccessBeanHelper";
	private static final Map<String, String> REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP;
	static {
		REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP = new HashMap<String, String>();
		REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP.put("isRefreshed", "isInstantiated");
		REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP.put("setRefreshed", "setInstantiated");
		REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP.put("preRefreshCopyHelper", "preInstantiateEntity");
		REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP.put("postRefreshCopyHelper", "postInstantiateEntity");
	}
	
	public static boolean isRefreshOnceAccessBeanHelper(ITypeBinding typeBinding) {
		boolean result = false;
		if (typeBinding != null) {
			if (REFRESH_ONCE_ACCESS_BEAN_HELPER.equals(typeBinding.getQualifiedName())) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean portRefreshOnceAccessBeanHelperMethodInvocation(ApplicationInfo applicationInfo, MethodInvocation methodInvocation, PortVisitor portVisitor) {
		boolean visitChildren = true;
		ITypeBinding typeBinding = methodInvocation.getExpression().resolveTypeBinding();
		if (typeBinding != null) {
			String methodName = methodInvocation.getName().getIdentifier();
			String newMethodName = REFRESH_ONCE_ACCESS_BEAN_METHOD_MAP.get(methodName);
			if (newMethodName != null) {
				SimpleName newName = methodInvocation.getAST().newSimpleName(newMethodName);
				portVisitor.replaceASTNode(methodInvocation.getName(), newName);
			}
		}
		return visitChildren;
	}
}
