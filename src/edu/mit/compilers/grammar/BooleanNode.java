package edu.mit.compilers.grammar;

import edu.mit.compilers.crawler.Scope;
import edu.mit.compilers.crawler.VarType;

@SuppressWarnings("serial")
public class BooleanNode extends ExpressionNode {

	@Override
	public VarType getReturnType(Scope scope) {
		return VarType.BOOLEAN;
	}

}