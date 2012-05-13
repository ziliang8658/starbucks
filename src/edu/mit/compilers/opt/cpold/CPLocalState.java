package edu.mit.compilers.opt.cpold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.compilers.LogCenter;
import edu.mit.compilers.codegen.nodes.memory.MidMemoryNode;
import edu.mit.compilers.codegen.nodes.memory.MidTempDeclNode;
import edu.mit.compilers.opt.State;

public class CPLocalState implements State<CPLocalState> {

	private Map<MidTempDeclNode, MidMemoryNode> tempMap;
	private Map<MidMemoryNode, List<MidTempDeclNode>> mentionMap;

	public CPLocalState() {
		tempMap = new HashMap<MidTempDeclNode, MidMemoryNode>();
		mentionMap = new HashMap<MidMemoryNode, List<MidTempDeclNode>>();
	}

	@Override
	public CPLocalState getInitialState() {
		return new CPLocalState();
	}

	@Override
	public CPLocalState getBottomState() {
		return new CPLocalState();
	}

	@Override
	public CPLocalState join(CPLocalState s) {
		return new CPLocalState();
	}

	/**
	 * Stores t1 -> a
	 */
	public void putTempReference(MidTempDeclNode tempNode,
			MidMemoryNode sourceNode) {
		LogCenter.debug("CP", "Mapping " + tempNode + " to " + sourceNode);
		tempMap.put(tempNode, sourceNode);
		List<MidTempDeclNode> mentions = mentionMap.get(sourceNode);
		if (mentions == null) {
			mentions = new ArrayList<MidTempDeclNode>();
			mentionMap.put(sourceNode, mentions);
		}
		mentions.add(tempNode);
	}

	/**
	 * Looks up what t1 maps to, i.e. if b = t1, t1->a, then we'd like to know
	 * that b = a. Returns the node that we can replace t1 with, and null if
	 * none exist.
	 */
	public MidMemoryNode getReplacement(MidTempDeclNode tempNode) {
		LogCenter.debug("CP", "Checking if " + tempNode + " maps to anything: "
				+ tempMap.containsKey(tempNode));
		return tempMap.get(tempNode);
	}

	public void killReferences(MidMemoryNode destinationNode) {
		List<MidTempDeclNode> tempNodes = mentionMap.get(destinationNode);
		if (tempNodes == null) {
			return;
		}
		for (MidTempDeclNode tempNode : tempNodes) {
			tempMap.remove(tempNode);
		}
		mentionMap.remove(destinationNode);
	}

	public Map<MidTempDeclNode, MidMemoryNode> getTempMap() {
		return tempMap;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CPLocalState)) {
			return false;
		}
		CPLocalState other = (CPLocalState) o;
		return getTempMap().equals(other.getTempMap());
	}

}
