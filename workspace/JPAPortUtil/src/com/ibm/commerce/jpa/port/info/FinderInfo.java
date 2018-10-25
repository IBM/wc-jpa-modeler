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

import java.util.ArrayList;
import java.util.List;

import com.ibm.commerce.jpa.port.util.JavaUtil;


public class FinderInfo {
	private static final String ENUMERATION = "java.util.Enumeration";
	private static final String COLLECTION = "java.util.Collection";
	private static final String ITERATOR = "java.util.Iterator";
	private static final String NO_RESULT_EXCEPTION = "javax.persistence.NoResultException";
	private String iFinderId;
	private String iFinderType;
	private String iFinderWhereClause;
	private String iOracleFinderWhereClause;
	private String iFinderSelectStatement;
	private String iFinderQuery;
	private String iFinderMethodName;
	private String[] iFinderMethodParameterTypes;
	private String[] iFinderMethodParameterNames;
	private String iFinderMethodType;
	private String iFinderMethodReturnType;
	private String iQueryName;
	private boolean iInHomeInterface;
	private String iMethodKey;
	private TargetExceptionInfo iTargetExceptions;
	
	public FinderInfo(String finderId) {
		iFinderId = finderId;
	}

	public String getFinderId() {
		return iFinderId;
	}

	public void setFinderType(String finderType) {
		iFinderType = finderType;
	}

	public String getFinderType() {
		return iFinderType;
	}
	
	public void setFinderWhereClause(String finderWhereClause) {
		iFinderWhereClause = finderWhereClause;
	}
	
	public String getFinderWhereClause() {
		return iFinderWhereClause;
	}
	
	public void setOracleFinderWhereClause(String oracleFinderWhereClause) {
		iOracleFinderWhereClause = oracleFinderWhereClause;
	}
	
	public String getOracleFinderWhereClause() {
		return iOracleFinderWhereClause;
	}
	
	public void setFinderSelectStatement(String finderSelectStatement) {
		iFinderSelectStatement = finderSelectStatement;
	}
	
	public String getFinderSelectStatement() { 
		return iFinderSelectStatement;
	}
	
	public void setFinderQuery(String finderQuery) {
		iFinderQuery = finderQuery;
	}
	
	public String getFinderQuery() {
		return iFinderQuery;
	}
	
	public void setFinderMethodName(String finderMethodName) {
		iFinderMethodName = finderMethodName;
	}
	
	public String getFinderMethodName() {
		return iFinderMethodName;
	}
	
	public void setFinderMethodParameterTypes(String finderMethodParameterTypes) {
		finderMethodParameterTypes = finderMethodParameterTypes.replaceAll(" \\[\\]", "[]");
		iFinderMethodParameterTypes = finderMethodParameterTypes.length() > 0 ? finderMethodParameterTypes.split(" ") : new String[0];
		iFinderMethodParameterNames = new String[iFinderMethodParameterTypes.length];
	}
	
	public void setFinderMethodParameterTypes(String[] finderMethodParms) {
		iFinderMethodParameterTypes = finderMethodParms;
		iFinderMethodParameterNames = new String[iFinderMethodParameterTypes.length];
	}
	
	public String[] getFinderMethodParameterTypes() {
		return iFinderMethodParameterTypes;
	}
	
	public void setFinderMethodParameterName(int parameterIndex, String parameterName) {
		iFinderMethodParameterNames[parameterIndex] = parameterName;
	}
	
	public String getFinderMethodParameterName(int parameterIndex) {
		return iFinderMethodParameterNames[parameterIndex];
	}
	
	public void setFinderMethodType(String finderMethodType) {
		iFinderMethodType = finderMethodType;
	}
	
	public String getFinderMethodType() {
		return iFinderMethodType;
	}
	
	public void setFinderMethodReturnType(String finderMethodReturnType) {
		iFinderMethodReturnType = finderMethodReturnType;
	}
	
	public String getFinderMethodReturnType() {
		return iFinderMethodReturnType;
	}

	public void setQueryName(String queryName) {
		iQueryName = queryName;
	}
	
	public String getQueryName() {
		return iQueryName;
	}
	
	public void setInHomeInterface(boolean inHomeInterface) {
		iInHomeInterface = inHomeInterface;
	}
	
	public boolean getInHomeInterface() {
		return iInHomeInterface;
	}
	
	public String getMethodKey() {
		if (iMethodKey == null) {
			iMethodKey = iFinderMethodName;
			if (iFinderMethodParameterTypes != null) {
				for (String parameterType : iFinderMethodParameterTypes) {
					iMethodKey += "+" + parameterType;
				}
			}
		}
		return iMethodKey;
	}
	
	public TargetExceptionInfo getTargetExceptions() {
		if (iTargetExceptions == null) {
			iTargetExceptions = new TargetExceptionInfo();
			if (!COLLECTION.equals(iFinderMethodReturnType) && !ENUMERATION.equals(iFinderMethodReturnType) && !ITERATOR.equals(iFinderMethodReturnType)) {
				iTargetExceptions.addTargetException(NO_RESULT_EXCEPTION);
			}
		}
		return iTargetExceptions;
	}
	
	public List<String> getNullableParmeters() {
		List<String> nullableParameters = new ArrayList<String>();
		if (iFinderMethodParameterTypes != null  && (iFinderWhereClause != null || iFinderQuery != null || iFinderSelectStatement != null)) {
			for (int i = 0; i < iFinderMethodParameterTypes.length; i++) {
				if (!JavaUtil.isPrimitiveType(iFinderMethodParameterTypes[i])) {
					if (iFinderMethodParameterNames[i] != null) {
						nullableParameters.add(iFinderMethodParameterNames[i]);
					}
				}
			}
		}
		return nullableParameters;
	}
}
