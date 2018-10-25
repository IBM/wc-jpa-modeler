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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class ExceptionUtil {
	private static Set<ITypeBinding> getFilteredExceptions(Set<ITypeBinding> unhandledExceptions) {
		Set<ITypeBinding> filteredExceptions = new HashSet<ITypeBinding>();
		for (ITypeBinding exception : unhandledExceptions) {
			boolean add = true;
			Set<ITypeBinding> removeTypes = new HashSet<ITypeBinding>();
			for (ITypeBinding currentException : filteredExceptions) {
				if (exception.isSubTypeCompatible(currentException)) {
					add = false;
					break;
				}
				else if (currentException.isSubTypeCompatible(exception)) {
					removeTypes.add(currentException);
				}
			}
			if (add) {
				filteredExceptions.add(exception);
			}
			for (ITypeBinding type : removeTypes) {
				filteredExceptions.remove(type);
			}
		}
		return filteredExceptions;
	}
	
	public static void suppressExceptions(ASTParser astParser, ICompilationUnit compilationUnit, IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("suppress exceptions in " + compilationUnit.getElementName(), 1000);
			IDocument document = new Document(compilationUnit.getSource());
			astParser.setSource(compilationUnit);
			astParser.setResolveBindings(true);
			CompilationUnit astCompilationUnit = (CompilationUnit) astParser.createAST(new SubProgressMonitor(progressMonitor, 200));
			astCompilationUnit.recordModifications();
			@SuppressWarnings("unchecked")
			List<TypeDeclaration> typeDeclarations = astCompilationUnit.types();
			for (TypeDeclaration typeDeclaration : typeDeclarations) {
				suppressExceptions(compilationUnit.getJavaProject(), typeDeclaration);
			}
			progressMonitor.worked(200);
			TextEdit edits = astCompilationUnit.rewrite(document, null);
			if (!progressMonitor.isCanceled()) {
				compilationUnit.becomeWorkingCopy(new SubProgressMonitor(progressMonitor, 200));
				if (!progressMonitor.isCanceled()) {
					compilationUnit.applyTextEdit(edits, new SubProgressMonitor(progressMonitor, 200));
					if (!progressMonitor.isCanceled()) {
						compilationUnit.commitWorkingCopy(true, new SubProgressMonitor(progressMonitor, 200));
					}
					compilationUnit.discardWorkingCopy();
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private static void suppressExceptions(IJavaProject javaProject, TypeDeclaration typeDeclaration) {
		@SuppressWarnings("unchecked")
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			switch (bodyDeclaration.getNodeType()) {
				case ASTNode.METHOD_DECLARATION: {
					suppressExceptions(javaProject, (MethodDeclaration) bodyDeclaration);
					break;
				}
				default: {
					break;
				}
			}
		}
	}
	
	public static void suppressExceptions(IJavaProject javaProject, MethodDeclaration methodDeclaration) {
		ExceptionVisitor exceptionVisitor = new ExceptionVisitor(javaProject);
		methodDeclaration.accept(exceptionVisitor);
		@SuppressWarnings("unchecked")
		List<Name> methodExceptions = methodDeclaration.thrownExceptions();
		methodExceptions.clear();
		Set<ITypeBinding> unhandledExceptions = exceptionVisitor.getUnhandledExceptions();
		if (unhandledExceptions.size() > 0) {
			Set<ITypeBinding> filteredExceptions = getFilteredExceptions(unhandledExceptions);
			AST ast = methodDeclaration.getAST();
			TryStatement tryStatement = ast.newTryStatement();
			@SuppressWarnings("unchecked")
			List<Statement> tryStatements = tryStatement.getBody().statements();
			@SuppressWarnings("unchecked")
			List<Statement> methodStatements = methodDeclaration.getBody().statements();
			while (methodStatements.size() > 0) {
				tryStatements.add(methodStatements.remove(0));
			}
			@SuppressWarnings("unchecked")
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			for (ITypeBinding exception : filteredExceptions) {
				CatchClause catchClause = ast.newCatchClause();
				SingleVariableDeclaration exceptionDeclaration = ast.newSingleVariableDeclaration();
				exceptionDeclaration.setType(ast.newSimpleType(ast.newName(exception.getQualifiedName())));
				exceptionDeclaration.setName(ast.newSimpleName("e"));
				catchClause.setException(exceptionDeclaration);
				Block catchBlock = ast.newBlock();
				ThrowStatement throwStatement = ast.newThrowStatement();
				ClassInstanceCreation newRuntimeExceptionExpression = ast.newClassInstanceCreation();
				newRuntimeExceptionExpression.setType(ast.newSimpleType(ast.newName("java.lang.RuntimeException")));
				@SuppressWarnings("unchecked")
				List<Expression> newRuntimeExceptionExpressionArguments = newRuntimeExceptionExpression.arguments();
				newRuntimeExceptionExpressionArguments.add(ast.newName("e"));
				throwStatement.setExpression(newRuntimeExceptionExpression);
				@SuppressWarnings("unchecked")
				List<Statement> catchBlockStatements = catchBlock.statements();
				catchBlockStatements.add(throwStatement);
				catchClause.setBody(catchBlock);
				catchClauses.add(catchClause);
			}
			methodStatements.add(tryStatement);
		}
	}
	
	private static class ExceptionVisitor extends ASTVisitor {
		private Set<ITypeBinding> iUnhandledExceptions = new HashSet<ITypeBinding>();
		private Deque<TryStatement> iTryStatementStack = new ArrayDeque<TryStatement>();
		private IJavaProject iJavaProject;
		
		public ExceptionVisitor(IJavaProject javaProject) {
			iJavaProject = javaProject;
		}

		public boolean visit(Block node) {
			if (node.getParent().getNodeType() == ASTNode.TRY_STATEMENT) {
				iTryStatementStack.push((TryStatement) node.getParent());
			}
			return true;
		}
		
		public void endVisit(Block node) {
			if (iTryStatementStack.size() > 0) {
				if (iTryStatementStack.peek().getBody() == node) {
					iTryStatementStack.pop();
				}
			}
		}
		
		public boolean visit(MethodInvocation node) {
			if (node.getExpression() != null) {
				IMethodBinding methodBinding = node.resolveMethodBinding();
				if (methodBinding != null) {
					ITypeBinding[] exceptionTypes = methodBinding.getExceptionTypes();
					if (exceptionTypes != null) {
						for (ITypeBinding exceptionType : exceptionTypes) {
							if (isUnhandledException(exceptionType)) {
								iUnhandledExceptions.add(exceptionType);
							}
						}
					}
				}
			}
			return true;
		}

		public boolean visit(ThrowStatement node) {
			ITypeBinding exceptionType = node.getExpression().resolveTypeBinding();
			if (!TargetExceptionUtil.isRuntimeException(iJavaProject, exceptionType.getQualifiedName()) && isUnhandledException(exceptionType)) {
				iUnhandledExceptions.add(exceptionType);
			}
			return true;
		}
		
		private boolean isUnhandledException(ITypeBinding exceptionTypeBinding) {
			boolean handled = false;
			for (TryStatement tryStatement : iTryStatementStack) {
				@SuppressWarnings("unchecked")
				List<CatchClause> catchClauses = tryStatement.catchClauses();
				for (CatchClause catchClause : catchClauses) {
					SingleVariableDeclaration exceptionDeclaration = catchClause.getException();
					ITypeBinding handledException = exceptionDeclaration.getType().resolveBinding();
					if (exceptionTypeBinding.equals(handledException) || exceptionTypeBinding.isSubTypeCompatible(handledException)) {
						handled = true;
						break;
					}
				}
				if (handled) {
					break;
				}
			}
			return !handled;
		}
		
		public Set<ITypeBinding> getUnhandledExceptions() {
			return iUnhandledExceptions;
		}
	}
}
