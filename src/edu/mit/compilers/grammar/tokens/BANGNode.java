package edu.mit.compilers.grammar.tokens;

import edu.mit.compilers.codegen.MidNodeList;
import edu.mit.compilers.codegen.MidSymbolTable;
import edu.mit.compilers.codegen.MidVisitor;
import edu.mit.compilers.codegen.nodes.MidLabelNode;
import edu.mit.compilers.grammar.expressions.OpBool2BoolNode;

@SuppressWarnings("serial")
public class BANGNode extends OpBool2BoolNode {

	@Override
	public MidNodeList convertToMidLevel(MidSymbolTable symbolTable) {
		return MidVisitor.visit(this, symbolTable);
	}

	@Override
	public MidNodeList shortCircuit(MidSymbolTable symbolTable, MidLabelNode trueLabel, MidLabelNode falseLabel){
		return MidVisitor.shortCircuit(this, symbolTable, trueLabel, falseLabel);
	}
	
}