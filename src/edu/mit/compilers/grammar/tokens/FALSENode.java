package edu.mit.compilers.grammar.tokens;

import edu.mit.compilers.codegen.MidNodeList;
import edu.mit.compilers.codegen.MidShortCircuitVisitor;
import edu.mit.compilers.codegen.MidSymbolTable;
import edu.mit.compilers.codegen.nodes.MidLabelNode;
import edu.mit.compilers.grammar.BooleanNode;

@SuppressWarnings("serial")
public class FALSENode extends BooleanNode {

	/*
	@Override
	public MidNodeList convertToMidLevel(MidSymbolTable symbolTable) {
		return MidVisitor.visit(this, symbolTable);
	}
	*/
	@Override
	public MidNodeList shortCircuit(MidSymbolTable symbolTable, MidLabelNode trueLabel, MidLabelNode falseLabel){
		return MidShortCircuitVisitor.shortCircuit(this, symbolTable, trueLabel, falseLabel);
	}
	
}