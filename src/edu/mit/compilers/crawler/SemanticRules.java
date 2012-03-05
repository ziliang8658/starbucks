package edu.mit.compilers.crawler;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ErrorCenter;
import edu.mit.compilers.crawler.Scope.BlockType;
import edu.mit.compilers.grammar.BranchNode;
import edu.mit.compilers.grammar.DecafNode;
import edu.mit.compilers.grammar.DeclNode;
import edu.mit.compilers.grammar.ExpressionNode;
import edu.mit.compilers.grammar.expressions.OpIntInt2IntNode;
import edu.mit.compilers.grammar.expressions.OpSameSame2BoolNode;
import edu.mit.compilers.grammar.tokens.ASSIGNNode;
import edu.mit.compilers.grammar.tokens.CLASSNode;
import edu.mit.compilers.grammar.tokens.FIELD_DECLNode;
import edu.mit.compilers.grammar.tokens.FOR_INITIALIZENode;
import edu.mit.compilers.grammar.tokens.FOR_TERMINATENode;
import edu.mit.compilers.grammar.tokens.IDNode;
import edu.mit.compilers.grammar.tokens.INT_LITERALNode;
import edu.mit.compilers.grammar.tokens.METHOD_CALLNode;
import edu.mit.compilers.grammar.tokens.METHOD_DECLNode;
import edu.mit.compilers.grammar.tokens.METHOD_IDNode;
import edu.mit.compilers.grammar.tokens.RETURNNode;

public class SemanticRules {

	static String REDECLARE_IDENTIFIER_ERROR = "Cannot redeclare identifier `%1$s`.";
	static String ID_BEFORE_DECLARATION_ERROR = "Cannot access identifier `%1$s` before declaration.";
	static String UNALLOWED_JUMP_ERROR = "Cannot call `%1$s` from outside a while/for loop.";
	static String REDECLARE_METHOD_ERROR = "Cannot declare method `%1$s`; variable or field `%1$s` already exists.";
	static String MISSING_MAIN_ERROR = "Program must contain definition for 'main' with no parameters.";
	static String INCORRECT_MAIN_ERROR = "Program must contain definition for 'main' with no parameters; %1$s found instead.";
	static String METHOD_ARGS_ERROR = "Cannot call method `%1$s` with arguments `%2$s`. Expected %3$s.";
	static String RETURN_TYPE_ERROR = "Return type mismatch. Expected `%1$s` but got `%2$s` instead.";
	static String ARRAY_INDEX_TYPE_ERROR = "Invalid array index: expected "
			+ VarType.INT.name() + ", found %1$s instead.";
	static String INVALID_CLASS_NAME_ERROR = "The class must be named `Program`. It is currently `%1$s`.";
	static String METHOD_BEFORE_DECLARATION_ERROR = "Method `%1$s` is not visible or valid.";
	static String INVALID_ARRAY_ACCESS_ERROR = "Cannot access `%1$s` as an array: `%1$s` has type %2$s.";
	static String ARRAY_INDEX_NEGATIVE_ERROR = "Size of array `%1$s` cannot be negative.";
	static String INT_OPERAND_ERROR = "Incorrect use of `%1$s` operator. Expecting <"
			+ VarType.INT.name() + ">, found <%2$s>.";
	static String FOR_LOOP_TERMINATE_INT_ERROR = "For loop termination condition must be an int.";
	static String FOR_LOOP_INIT_INT_ERROR = "For loop initial condition must be an int.";
	static String OP_SAME_SAME_BAD_TYPE = "Incorrect use of equality operator. Expecting INT or BOOLEAN, found `%1$s`";
	static String OP_SAME_SAME_NOT_SAME_TYPE = "Comparison type mismatch. Expecting INT INT or BOOLEAN BOOLEAN. Found `%1$s` `%2$s`";
	
	static public void apply(DecafNode node, Scope scope) {
		if (node instanceof METHOD_DECLNode) {
			apply((METHOD_DECLNode) node, scope);
			return;
		}
		if (node instanceof METHOD_CALLNode) {
			apply((METHOD_CALLNode) node, scope);
			return;
		}
		if (node instanceof DeclNode) {
			apply((DeclNode) node, scope);
			return;
		}
		if (node instanceof IDNode) {
			apply((IDNode) node, scope);
			return;
		}
		if (node instanceof BranchNode) {
			apply((BranchNode) node, scope);
			return;
		}

		if (node instanceof CLASSNode) {
			apply((CLASSNode) node, scope);
			return;
		}

		if (node instanceof RETURNNode) {
			apply((RETURNNode) node, scope);
			return;
		}

		if (node instanceof OpIntInt2IntNode) {
			apply((OpIntInt2IntNode) node, scope);
			return;
		}

		if (node instanceof OpSameSame2BoolNode) {
			apply((OpSameSame2BoolNode) node, scope);
			return;
		}

		if (node instanceof FOR_TERMINATENode) {
			apply((FOR_TERMINATENode) node, scope);
			return;
		}

		if (node instanceof FOR_INITIALIZENode) {
			apply((FOR_INITIALIZENode) node, scope);
			return;
		}

		// TODO: enable this when all rules are done.
		// assert false :
		// "apply on DecafNode should not be called, only its children.";
		return;
	}

	static public void apply(DeclNode node, Scope scope) {
		// Rule 1, Rule 9
		// TODO: Should apply to methods also
		IDNode idNode = node.getIDNode();
		String id = idNode.getText();
		VarType t = node.getVarType();

		// FIELD_DECLs are not allowed to shadow methods, other DECLs are.
		if ((node instanceof FIELD_DECLNode && scope.hasSymbol(id))
				|| (scope.hasLocalVar(id))) {
			ErrorCenter
					.reportError(idNode.getLine(), idNode.getColumn(), String
							.format(REDECLARE_IDENTIFIER_ERROR, id));
		} else {
			scope.addVar(id, new VarDecl(t, id, idNode.getLine(), idNode
					.getColumn()));
		}

		// Rule 4
		// If the node's VarTypeNode has children, check the array size.
		if (node.getVarTypeNode().getNumberOfChildren() == 1) {
			assert node.getVarTypeNode().getFirstChild() instanceof INT_LITERALNode;
			INT_LITERALNode intNode = (INT_LITERALNode) node.getVarTypeNode()
					.getFirstChild();
			if (!intNode.isPositive()) {
				ErrorCenter
						.reportError(intNode.getLine(), intNode.getColumn(), String
								.format(ARRAY_INDEX_NEGATIVE_ERROR, id));
			}
		}

	}

	static public void apply(IDNode node, Scope scope) {
		// Rule 2
		String id = node.getText();
		if (!scope.hasVar(node.getText())) {
			ErrorCenter.reportError(node.getLine(), node.getColumn(), String
					.format(ID_BEFORE_DECLARATION_ERROR, id));
		}

		// Rule 10
		assert node.getNumberOfChildren() <= 1;
		DecafNode indexNode;
		// If there's a child, it must be array access, i.e. a[5]
		if ((indexNode = node.getFirstChild()) != null) {
			// Check that the IDNode is an array.
			if (!(scope.getType(id) == VarType.INT_ARRAY)) {
				ErrorCenter
						.reportError(node.getLine(), node.getColumn(), String
								.format(INVALID_ARRAY_ACCESS_ERROR, id, scope
										.getType(id)));
				return;
			}
			// Check that the index is an INT.
			assert indexNode instanceof ExpressionNode;
			VarType indexType = ((ExpressionNode) indexNode)
					.getReturnType(scope);
			if (indexType != VarType.INT) {
				ErrorCenter.reportError(indexNode.getLine(), indexNode
						.getColumn(), String
						.format(ARRAY_INDEX_TYPE_ERROR, indexType.name()));
			}
		}
	}

	static public void apply(BranchNode node, Scope scope) {
		// Rule 18.
		Scope currentScope = scope;
		while (currentScope != null) {
			if (currentScope.getBlockType() == BlockType.WHILE
					|| currentScope.getBlockType() == BlockType.FOR) {
				break;
			} else {
				currentScope = currentScope.getParent();
			}
		}
		if (currentScope == null) {
			ErrorCenter.reportError(node.getLine(), node.getColumn(), String
					.format(UNALLOWED_JUMP_ERROR, node.getText()));
		}
	}

	static public void apply(METHOD_DECLNode node, Scope scope) {
		VarType returnType = node.getReturnType();
		String id = node.getId();
		List<VarType> params = node.getParams();
		// Don't allow shadowing existing methods or fields.
		if (scope.hasSymbol(id)) {
			ErrorCenter.reportError(node.getLine(), node.getColumn(), String
					.format(REDECLARE_METHOD_ERROR, id));
		} else {
			scope.getMethods().put(id, new MethodDecl(returnType, id, params,
					node.getLine(), node.getColumn()));
		}
	}

	static public void apply(RETURNNode node, Scope scope) {
		if (node.getReturnType(scope) != scope.getReturnType()) {
			ErrorCenter.reportError(node.getLine(), node.getColumn(), String
					.format(RETURN_TYPE_ERROR, scope.getReturnType(), node
							.getReturnType(scope)));
		}
	}

	static public void apply(CLASSNode node, Scope scope) {
		DecafNode child = node.getFirstChild();
		if (!child.getText().equals("Program")) {
			ErrorCenter.reportError(child.getLine(), child.getColumn(), String
					.format(INVALID_CLASS_NAME_ERROR, child.getText()));
		}
	}

	static public void finalApply(CLASSNode node, Scope scope) {
		// Rule 3.
		if (!scope.getMethods().containsKey("main")) {
			ErrorCenter.reportError(1, 1, MISSING_MAIN_ERROR);
		} else {
			MethodDecl mainDecl = scope.getMethods().get("main");
			if (mainDecl.getParams().size() != 0) {
				StringBuilder paramsStringBuilder = new StringBuilder();
				paramsStringBuilder.append("<");
				for (int i = 0; i < mainDecl.getParams().size(); i++) {
					paramsStringBuilder.append(mainDecl.getParams().get(i));
					if (i != mainDecl.getParams().size() - 1) {
						paramsStringBuilder.append(", ");
					}
				}
				paramsStringBuilder.append(">");
				ErrorCenter.reportError(mainDecl.getLine(), mainDecl
						.getColumn(), String
						.format(INCORRECT_MAIN_ERROR, paramsStringBuilder
								.toString()));
			}
		}
	}

	static public void apply(METHOD_CALLNode node, Scope scope) {
		// Rule 2b
		assert node.getNumberOfChildren() > 0;
		assert node.getChild(0) instanceof METHOD_IDNode;

		METHOD_IDNode methodIDNode = (METHOD_IDNode) node.getChild(0);
		String methodName = methodIDNode.getText();
		if (scope.hasMethod(methodName)) {
			// Rule 5
			// Construct a list of passed in parameters.
			List<VarType> args = new ArrayList<VarType>();
			DecafNode arg = node.getFirstChild().getNextSibling();
			while (arg != null) {
				assert arg instanceof ExpressionNode;
				VarType returnType = ((ExpressionNode) arg)
						.getReturnType(scope);
				args.add(returnType);
				arg = arg.getNextSibling();
			}

			MethodDecl method = scope.getMethods().get(methodName);
			List<VarType> params = method.getParams();

			if (reportErrorForParams(params, args)) {
				ErrorCenter
						.reportError(methodIDNode.getLine(), methodIDNode
								.getColumn(), String
								.format(METHOD_ARGS_ERROR, methodName, args
										.toString(), params.toString()));
			}
		} else {
			ErrorCenter.reportError(node.getLine(), node.getColumn(), String
					.format(METHOD_BEFORE_DECLARATION_ERROR, methodName));
		}
	}

	private static boolean reportErrorForParams(List<VarType> params,
			List<VarType> args) {
		// Silently fail for undeclared variables
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == VarType.UNDECLARED) {
				return false;
			}
		}
		if (params.size() == args.size()) {
			for (int i = 0; i < params.size(); i++) {
				if (args.get(i) != params.get(i))
					return true;
			}
			return false;
		}
		return true;
	}

	static public void apply(OpIntInt2IntNode node, Scope scope) {
		// Rule 12
		assert node.getNumberOfChildren() == 2;
		assert node.getChild(0) instanceof ExpressionNode;
		assert node.getChild(1) instanceof ExpressionNode;

		ExpressionNode child = (ExpressionNode) node.getFirstChild();
		ExpressionNode[] children = new ExpressionNode[] { child,
				(ExpressionNode) child.getNextSibling() };

		for (int i = 0; i < children.length; i++) {
			child = children[i];
			VarType type = child.getReturnType(scope);
			if (type != VarType.INT) {
				ErrorCenter
						.reportError(child.getLine(), child.getColumn(), String
								.format(INT_OPERAND_ERROR, node.getText(), child
										.getReturnType(scope)));
			}
		}
	}

	static public void apply(OpSameSame2BoolNode node, Scope scope) {
		// Rule 13
		assert node.getNumberOfChildren() == 2;
		assert node.getChild(0) instanceof ExpressionNode;
		assert node.getChild(1) instanceof ExpressionNode;
		
		ExpressionNode first = (ExpressionNode) node.getFirstChild();
		ExpressionNode second = (ExpressionNode) first.getNextSibling();
		VarType firstType = first.getReturnType(scope);
		VarType secondType = second.getReturnType(scope);
		
		// Could get away only checking first type but doing both in order to
		// identify the second token as error causing
		// Probably ugly to call report error multiple times, store in temp variable
		// and call at the end if exists
		if (firstType != VarType.INT && firstType != VarType.BOOLEAN) {
			ErrorCenter.reportError(first.getLine(), first.getColumn(), String
					.format(OP_SAME_SAME_BAD_TYPE, firstType));
		} else if (secondType != VarType.INT && secondType != VarType.BOOLEAN) {
			ErrorCenter.reportError(second.getLine(), second.getColumn(), String
					.format(OP_SAME_SAME_BAD_TYPE, secondType));
		} else if (firstType != secondType) {
			ErrorCenter.reportError(second.getLine(), second.getColumn(), String
					.format(OP_SAME_SAME_NOT_SAME_TYPE, firstType, secondType));
		}
	}

	static public void apply(FOR_TERMINATENode node, Scope scope) {
		// Rule 17

		assert node.getNumberOfChildren() == 1 : "Should only have one child in For Terminate";

		if (!(node.getFirstChild() instanceof ExpressionNode)
				|| ((ExpressionNode) node.getFirstChild()).getReturnType(scope) != VarType.INT) {
			ErrorCenter.reportError(node.getFirstChild().getLine(), node
					.getFirstChild().getColumn(), String
					.format(FOR_LOOP_TERMINATE_INT_ERROR));
		}
	}

	static public void apply(FOR_INITIALIZENode node, Scope scope) {
		// Rule 17

		System.out.println("AHA");
		assert node.getNumberOfChildren() == 1 : "Should only have one child in For INIT";
		assert node.getFirstChild().getNumberOfChildren() == 2;
		// assert node.getFirstChild() instanceof ASSIGNNode;

		if (!(node.getFirstChild() instanceof ASSIGNNode)
				|| !(node.getFirstChild().getChild(1) instanceof ExpressionNode)
				|| ((ExpressionNode) node.getFirstChild().getChild(1))
						.getReturnType(scope) != VarType.INT) {
			ErrorCenter.reportError(node.getFirstChild().getLine(), node
					.getFirstChild().getColumn(), String
					.format(FOR_LOOP_INIT_INT_ERROR));
		}
	}
}
