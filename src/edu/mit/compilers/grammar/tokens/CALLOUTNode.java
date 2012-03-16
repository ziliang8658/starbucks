package edu.mit.compilers.grammar.tokens;

import edu.mit.compilers.codegen.MidSymbolTable;
import edu.mit.compilers.crawler.Scope;
import edu.mit.compilers.crawler.VarType;
import edu.mit.compilers.grammar.ExpressionNode;


@SuppressWarnings("serial")
public class CALLOUTNode extends ExpressionNode {
	
	@Override
	public VarType getReturnType(Scope scope) {
		return VarType.INT;
	}
	@Override
	public VarType getMidVarType(MidSymbolTable symbolTable){
		return VarType.INT;
	}
}