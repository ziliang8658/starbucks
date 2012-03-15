package edu.mit.compilers.codegen.nodes;

public class MidLocalVarDeclNode extends MidVarDeclNode {
	private String name;
	
	public MidLocalVarDeclNode(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		String className = getClass().getName();
		int mid = className.lastIndexOf('.') + 1;
		return "<" + className.substring(mid) + ": " + ">";
	}
}