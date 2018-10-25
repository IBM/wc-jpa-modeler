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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class ImportUtil {
	private static String[] IMPORT_ORDER = {
		"java",
		"javax",
		"org",
		"com"
	};
	
	public static void appendImports(List<ImportDeclaration> importDeclarations, StringBuilder sb) {
		for (ImportDeclaration importDeclaration : importDeclarations) {
			sb.append("import ");
			sb.append(importDeclaration.getName().getFullyQualifiedName());
			if (importDeclaration.isOnDemand()) {
				sb.append(".*");
			}
			sb.append(";\r\n");
		}
	}
	
	public static void appendImports(Expression expression, StringBuilder sb) {
		expression.accept(new StringBuilderImportTypeVisitor(sb));
	}
	
	public static void resolveImports(ASTParser astParser, ICompilationUnit compilationUnit, IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("resolve imports for " + compilationUnit.getElementName(), 1200);
			compilationUnit.becomeWorkingCopy(new SubProgressMonitor(progressMonitor, 200));
			Map<String, String> declaredTypes = new HashMap<String, String>();
			IType[] compilationUnitTypes = compilationUnit.getTypes();
			Collection<IType> currentTypes = new HashSet<IType>();
			for (IType type : compilationUnitTypes) {
				currentTypes.add(type);
				declaredTypes.put(type.getElementName(), type.getFullyQualifiedName('.'));
			}
			boolean memberTypesAreLocal = true;
			while (!currentTypes.isEmpty()) {
				Collection<IType> newTypes = new HashSet<IType>();
				for (IType type : currentTypes) {
					Collection<IType> currentMemberTypes = new HashSet<IType>();
					IType[] memberTypes = type.getTypes();
					for (IType memberType : memberTypes) {
						currentMemberTypes.add(memberType);
					}
					while (!currentMemberTypes.isEmpty()) {
						Collection<IType> newMemberTypes = new HashSet<IType>();
						for (IType memberType : currentMemberTypes) {
							if (memberTypesAreLocal || !Flags.isPrivate(memberType.getFlags())) {
								declaredTypes.put(memberType.getElementName(), memberType.getFullyQualifiedName('.'));
							}
							memberTypes = memberType.getTypes();
							for (IType nestedMemberType : memberTypes) {
								newMemberTypes.add(nestedMemberType);
							}
						}
						currentMemberTypes = newMemberTypes;
					}
					String[] superInterfaceNames = type.getSuperInterfaceNames();
					for (String superInterfaceName : superInterfaceNames) {
						IType interfaceType = JavaUtil.resolveType(type, superInterfaceName);
						if (interfaceType != null) {
							newTypes.add(interfaceType);
						}
					}
					if (type.getSuperclassName() != null) {
						String superclassName = type.getSuperclassName();
						IType superclassType = JavaUtil.resolveType(type, superclassName);
						if (superclassType != null) {
							newTypes.add(superclassType);
						}
					}
				}
				memberTypesAreLocal = false;
				currentTypes = newTypes;
			}
			IDocument document = new Document(compilationUnit.getSource());
			ImportRewrite importRewrite = ImportRewrite.create(compilationUnit, false);
			importRewrite.setImportOrder(IMPORT_ORDER);
			astParser.setSource(compilationUnit);
			astParser.setResolveBindings(true);
			CompilationUnit astCompilationUnit = (CompilationUnit) astParser.createAST(new SubProgressMonitor(progressMonitor, 200));
			astCompilationUnit.recordModifications();
			ImportRewriteTypeVisitor typeVisitor = new ImportRewriteTypeVisitor(importRewrite, declaredTypes);
			@SuppressWarnings("unchecked")
			List<TypeDeclaration> typeDeclarations = astCompilationUnit.types();
			for (TypeDeclaration typeDeclaration : typeDeclarations) {
				typeDeclaration.accept(typeVisitor);
			}
			TextEdit edits = astCompilationUnit.rewrite(document, null);
			compilationUnit.applyTextEdit(edits, new SubProgressMonitor(progressMonitor, 200));
			edits = importRewrite.rewriteImports(new SubProgressMonitor(progressMonitor, 200));
			compilationUnit.applyTextEdit(edits, new SubProgressMonitor(progressMonitor, 200));
			compilationUnit.commitWorkingCopy(true, new SubProgressMonitor(progressMonitor, 200));
			compilationUnit.discardWorkingCopy();
		}
		catch (JavaModelException e) {
			e.printStackTrace();
			System.out.println("Import util problem with "+compilationUnit.getElementName());
		}
		catch (CoreException e) {
			e.printStackTrace();
			System.out.println("Import util problem with "+compilationUnit.getElementName());
		}
		catch (StringIndexOutOfBoundsException e) {
			e.printStackTrace();
			System.out.println("Import util problem with "+compilationUnit.getElementName());
			throw e;
		}
		finally {
			progressMonitor.done();
		}
	}
	
	private static class ImportRewriteTypeVisitor extends ASTVisitor {
		private ImportRewrite iImportRewrite;
		private Map<String, String> iDeclaredTypes;
		private Collection<String> iTypeNames = new HashSet<String>();
		
		public ImportRewriteTypeVisitor(ImportRewrite importRewrite, Map<String, String> declaredTypes) {
			iImportRewrite = importRewrite;
			iDeclaredTypes = declaredTypes;
		}
		
		public boolean preVisit2(ASTNode node) {
			if (node.toString().endsWith("KEYS.STORE_DEFAULT_LANGUAGE_ID")) {
				System.out.println("KEYS.STORE_DEFAULT_LANGUAGE_ID");
			}
			if (node.toString().equals("com.ibm.commerce.rest.order.handler.JPACartHandler.AddOrderItemBodyDescription.OrderItemBodyDescription.ItemAttributesBodyDescription")) {
				System.out.println("com.ibm.commerce.rest.order.handler.JPACartHandler.AddOrderItemBodyDescription.OrderItemBodyDescription.ItemAttributesBodyDescription");
			}
			return true;
		}
		
		public boolean visit(TypeDeclaration typeDeclaration) {
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding != null) {
				iTypeNames.add(typeBinding.getQualifiedName());
			}
			return true;
		}
		
		
		public void endVisit(TypeDeclaration typeDeclaration) {
			ITypeBinding typeBinding = typeDeclaration.resolveBinding();
			if (typeBinding != null) {
				iTypeNames.remove(typeBinding.getQualifiedName());
			}
		}
		
		public boolean visit(SimpleType node) {
			ITypeBinding typeBinding = node.resolveBinding();
			if (typeBinding != null) {
				boolean declaredName = false;
				if (node.getName().getNodeType() == ASTNode.QUALIFIED_NAME) {
					QualifiedName qualifiedName = (QualifiedName) node.getName();
					String simpleName = qualifiedName.getName().getIdentifier();
					if (iDeclaredTypes.containsKey(simpleName) && !iDeclaredTypes.get(simpleName).equals(typeBinding.getQualifiedName())) {
						qualifiedName.getQualifier().accept(this);
						declaredName = true;
					}
					else if (iTypeNames.contains(qualifiedName.getQualifier().getFullyQualifiedName())) {
						JavaUtil.replaceASTNode(node, node.getAST().newSimpleType(node.getAST().newSimpleName(simpleName)));
						declaredName = true;
					}
				}
				if (!declaredName) {
					Type newType = iImportRewrite.addImport(typeBinding, node.getAST());
					JavaUtil.replaceASTNode(node, newType);
				}
//				StructuralPropertyDescriptor location = node.getLocationInParent();
//				if (location.isChildListProperty()) {
//					@SuppressWarnings("unchecked")
//					List<ASTNode> childList = (List<ASTNode>) node.getParent().getStructuralProperty(location);
//					int index = childList.indexOf(node);
//					childList.remove(node);
//					childList.add(index, newType);
//				}
//				else {
//					node.getParent().setStructuralProperty(location, newType);
//				}
			}
			return false;
		}
		
		public boolean visit(ParameterizedType node) {
			ITypeBinding typeBinding = node.resolveBinding();
			if (typeBinding != null) {
				Type newType = iImportRewrite.addImport(typeBinding, node.getAST());
				JavaUtil.replaceASTNode(node, newType);
			}
			return false;
		}
		
//		public boolean visit(ArrayType node) {
//			ITypeBinding typeBinding = node.resolveBinding();
//			if (typeBinding != null) {
//				Type componentType = node.getComponentType();
//				if ("com.ibm.commerce.order.beans.JPAOrderCommentDataBean".equals(componentType.toString())) {
//					System.out.println("com.ibm.commerce.order.beans.JPAOrderCommentDataBean[]");
//				}
//				Type newType = iImportRewrite.addImport(typeBinding, node.getAST());
//				JavaUtil.replaceASTNode(node, newType);
//			}
//			return true;
//		}
		
		public boolean visit(SimpleName name) {
			if (!name.isDeclaration()) {
				IBinding binding = name.resolveBinding();
				if (binding != null && binding.getKind() == IBinding.TYPE) {
					String type = iImportRewrite.addImport((ITypeBinding) binding);
					if (!type.equals(name.getIdentifier())) {
						Name newName = name.getAST().newName(type);
						JavaUtil.replaceASTNode(name, newName);
					}
				}
			}
			return false;
		}
		
		public boolean visit(QualifiedName node) {
			boolean visitChildren = true;
			IBinding binding = node.resolveBinding();
			if (binding != null && binding.getKind() == IBinding.TYPE) {
				ITypeBinding typeBinding = (ITypeBinding) binding;
				if (typeBinding.getQualifiedName().equals("com.ibm.commerce.rest.order.handler.JPACartHandler.AddOrderItemBodyDescription.OrderItemBodyDescription.ItemAttributesBodyDescription")) {
					System.out.println("com.ibm.commerce.rest.order.handler.JPACartHandler.AddOrderItemBodyDescription.OrderItemBodyDescription.ItemAttributesBodyDescription");
				}
				String simpleName = node.getName().getIdentifier();
				if (iDeclaredTypes.containsKey(simpleName) && !iDeclaredTypes.get(simpleName).equals(typeBinding.getQualifiedName())) {
					node.getQualifier().accept(this);
					visitChildren = false;
				}
				else {
					String type = iImportRewrite.addImport(typeBinding);
					if (!type.equals(node.getFullyQualifiedName())) {
						Name newName = node.getAST().newName(type);
						JavaUtil.replaceASTNode(node, newName);
					}
//					StructuralPropertyDescriptor location = node.getLocationInParent();
//					if (location.isChildListProperty()) {
//						@SuppressWarnings("unchecked")
//						List<ASTNode> childList = (List<ASTNode>) node.getParent().getStructuralProperty(location);
//						int index = childList.indexOf(node);
//						childList.remove(node);
//						childList.add(index, newName);
//					}
//					else {
//						node.getParent().setStructuralProperty(location, newName);
//					}
					visitChildren = false;
				}
			}
			return visitChildren;
		}
		
		public boolean visit(QualifiedType qualifiedType) {
			boolean visitChildren = true;
			ITypeBinding typeBinding = qualifiedType.resolveBinding();
			if (typeBinding != null) {
				if (typeBinding.getQualifiedName().equals("com.ibm.commerce.rest.order.handler.JPACartHandler.AddOrderItemBodyDescription.OrderItemBodyDescription.ItemAttributesBodyDescription")) {
					System.out.println("com.ibm.commerce.rest.order.handler.JPACartHandler.AddOrderItemBodyDescription.OrderItemBodyDescription.ItemAttributesBodyDescription");
				}
				String simpleName = qualifiedType.getName().getIdentifier();
				if (iDeclaredTypes.containsKey(simpleName) && !iDeclaredTypes.get(simpleName).equals(typeBinding.getQualifiedName())) {
					qualifiedType.getQualifier().accept(this);
					visitChildren = false;
				}
				else if (iTypeNames.contains(typeBinding.getQualifiedName())) {
					JavaUtil.replaceASTNode(qualifiedType, qualifiedType.getAST().newSimpleType(qualifiedType.getAST().newSimpleName(simpleName)));
					visitChildren = false;
				}
			}
			return visitChildren;
		}

		public boolean visit(ImportDeclaration node) {
			return false;
		}
		
//		public boolean visit(MarkerAnnotation node) {
//			Name name = node.getTypeName();
//			ITypeBinding typeBinding = name.resolveTypeBinding();
//			if (typeBinding != null) {
//				iImportRewrite.addImport(typeBinding);
//			}
//			return true;
//		}
//		
//		public boolean visit(NormalAnnotation node) {
//			Name name = node.getTypeName();
//			ITypeBinding typeBinding = name.resolveTypeBinding();
//			if (typeBinding != null) {
//				iImportRewrite.addImport(typeBinding);
//			}
//			return true;
//		}
//		
//		public boolean visit(SingleMemberAnnotation node) {
//			Name name = node.getTypeName();
//			ITypeBinding typeBinding = name.resolveTypeBinding();
//			if (typeBinding != null) {
//				iImportRewrite.addImport(typeBinding);
//			}
//			return true;
//		}
	}
	
	private static class StringBuilderImportTypeVisitor extends ASTVisitor {
		private StringBuilder iStringBuilder;
		
		public StringBuilderImportTypeVisitor(StringBuilder stringBuilder) {
			iStringBuilder = stringBuilder;
		}
		
		public boolean visit(SimpleType node) {
			ITypeBinding typeBinding = node.resolveBinding();
			if (typeBinding != null) {
				iStringBuilder.append("import ");
				iStringBuilder.append(typeBinding.getQualifiedName());
				iStringBuilder.append(";\r\n");
			}
			return false;
		}
		
		public boolean visit(SimpleName node) {
			IBinding binding = node.resolveBinding();
			if (binding != null && binding.getKind() == IBinding.TYPE) {
				iStringBuilder.append("import ");
				iStringBuilder.append(((ITypeBinding) binding).getQualifiedName());
				iStringBuilder.append(";\r\n");
			}
			return false;
		}
		
		public boolean visit(QualifiedName node) {
			boolean visitChildren = true;
			IBinding binding = node.resolveBinding();
			if (binding != null && binding.getKind() == IBinding.TYPE) {
				visitChildren = false;
			}
			return visitChildren;
		}
	}
}
