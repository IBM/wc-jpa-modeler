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

import org.eclipse.jdt.core.dom.Expression;

public class InstanceVariableInfo {
	private String iVariableName;
	private String iType;
	private Expression iInitializationExpression;
	
	public InstanceVariableInfo(String variableName, String type, Expression initializationExpression) {
		iVariableName = variableName;
		iType = type;
		iInitializationExpression = initializationExpression;
	}
	
	public String getVariableName() {
		return iVariableName;
	}
	
	public String getType() {
		return iType;
	}
	
	public Expression getInitializationExpression() {
		return iInitializationExpression;
	}
}
