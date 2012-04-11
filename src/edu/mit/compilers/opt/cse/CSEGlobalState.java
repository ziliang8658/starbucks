package edu.mit.compilers.opt.cse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.LogCenter;
import edu.mit.compilers.codegen.nodes.memory.MidMemoryNode;
import edu.mit.compilers.codegen.nodes.memory.MidTempDeclNode;
import edu.mit.compilers.opt.State;

public class CSEGlobalState implements State<CSEGlobalState>, Cloneable {

	// Note: we use a List<GlobalExpr> here *in case* we want to store multiple
	// ways of representing a memory node
	// i.e. a = b+c, d = a-e could produce d = [a-e, b+c-e)]
	HashMap<MidMemoryNode, GlobalExpr> refToExprMap;
	HashMap<GlobalExpr, List<MidMemoryNode>> exprToRefMap;
	HashMap<MidMemoryNode, List<GlobalExpr>> mentionMap;

	public CSEGlobalState() {
		refToExprMap = new HashMap<MidMemoryNode, GlobalExpr>();
		exprToRefMap = new HashMap<GlobalExpr, List<MidMemoryNode>>();
		mentionMap = new HashMap<MidMemoryNode, List<GlobalExpr>>();
	}

	public CSEGlobalState(HashMap<MidMemoryNode, GlobalExpr> refToExprMap,
			HashMap<GlobalExpr, List<MidMemoryNode>> exprToRefMap,
			HashMap<MidMemoryNode, List<GlobalExpr>> mentionMap) {
		this.refToExprMap = refToExprMap;
		this.exprToRefMap = exprToRefMap;
		this.mentionMap = mentionMap;
	}

	@Override
	public CSEGlobalState getInitialState() {
		return new CSEGlobalState();
	}

	@Override
	public CSEGlobalState getBottomState() {
		return new CSEGlobalState();
	}

	@Override
	public CSEGlobalState clone() {
		CSEGlobalState out = new CSEGlobalState(deepClone(refToExprMap),
				deepCloneList(exprToRefMap), deepCloneList(mentionMap));
		return out;
	}

	private <K, V> HashMap<K, List<V>> deepCloneList(HashMap<K, List<V>> map) {
		HashMap<K, List<V>> out = new HashMap<K, List<V>>();
		for (K key : map.keySet()) {
			out.put(key, new ArrayList<V>(map.get(key)));
		}
		return out;
	}

	private <K, V> HashMap<K, V> deepClone(HashMap<K, V> map) {
		HashMap<K, V> out = new HashMap<K, V>();
		for (K key : map.keySet()) {
			out.put(key, map.get(key));
		}
		return out;
	}

	@Override
	public CSEGlobalState join(CSEGlobalState s) {
		LogCenter.debug("[OPT] JOINING " + this);
		LogCenter.debug("[OPT] WITH " + s);
		// Take common expressions, only if they're temp vars or equal non-temp
		// vars.
		if (s == null) {
			CSEGlobalState out = this.clone();
			LogCenter.debug("[OPT] RESULT: " + out);
			return out;
		}
		Set<GlobalExpr> sharedSet = exprToRefMap.keySet();
		sharedSet.retainAll(s.exprToRefMap.keySet());
		HashMap<GlobalExpr, List<MidMemoryNode>> newExprToRefMap = new HashMap<GlobalExpr, List<MidMemoryNode>>();
		HashMap<MidMemoryNode, GlobalExpr> newRefToExprMap = new HashMap<MidMemoryNode, GlobalExpr>();
		HashMap<MidMemoryNode, List<GlobalExpr>> newMentionMap = new HashMap<MidMemoryNode, List<GlobalExpr>>();
		for (GlobalExpr e : sharedSet) {
			MidMemoryNode newMemNode = null;
			List<MidMemoryNode> thisMemoryNodes = exprToRefMap.get(e);
			List<MidMemoryNode> thatMemoryNodes = s.exprToRefMap.get(e);
			// Find just one shared reference for the given expression to keep.
			// Join identical memory nodes (not temp nodes).
			for (MidMemoryNode memNode : thisMemoryNodes) {
				if (thatMemoryNodes.contains(memNode)) {
					newMemNode = memNode;
					break;
				}
			}
			if (newMemNode == null) {
				// Get the first instances of temps.
				for (MidMemoryNode m1 : thisMemoryNodes) {
					if (m1 instanceof MidTempDeclNode) {
						for (MidMemoryNode m2 : thatMemoryNodes) {
							if (m2 instanceof MidTempDeclNode) {
								// Link the two temps.
								((MidTempDeclNode) m1)
										.linkTempDecl((MidTempDeclNode) m2);
								// Return just one of them.
								newMemNode = m1;
								break;
							}
						}
						break;
					}
				}
			}
			assert newMemNode != null;
			List<MidMemoryNode> newMemNodes = new ArrayList<MidMemoryNode>();
			newMemNodes.add(newMemNode);

			newExprToRefMap.put(e, newMemNodes);
			newRefToExprMap.put(newMemNode, e);
			for (MidMemoryNode mem : e.getMemoryNodes()) {
				if (newMentionMap.containsKey(mem)) {
					List<GlobalExpr> existingList = newMentionMap.get(mem);
					existingList.add(e);
				} else {
					List<GlobalExpr> eList = new ArrayList<GlobalExpr>();
					eList.add(e);
					newMentionMap.put(mem, new ArrayList<GlobalExpr>(eList));
				}
			}
		}
		CSEGlobalState out = new CSEGlobalState(newRefToExprMap,
				newExprToRefMap, newMentionMap);
		LogCenter.debug("[OPT] RESULT: " + out);
		return out;
	}

	public void genReference(MidMemoryNode node, GlobalExpr expr) {
//		LogCenter.debug("[OPTJ] Generating reference " + node + " -> " + expr);
//		 LogCenter.debug("[OPTJ] mentionMap:\n[OPTJ] " + mentionMap);
//		 LogCenter.debug("[OPTJ] refToExprMap:\n[OPTJ] " + refToExprMap);
//		 LogCenter.debug("[OPTJ] exprToRefMap:\n[OPTJ] " + exprToRefMap);

		// Would potentially expand expr here (and loop through them below).
		refToExprMap.put(node, expr);

		if (!exprToRefMap.containsKey(expr)) {
			exprToRefMap.put(expr, new ArrayList<MidMemoryNode>());
		}
		exprToRefMap.get(expr).add(node);

		for (MidMemoryNode m : expr.getMemoryNodes()) {
			if (!mentionMap.containsKey(m)) {
				mentionMap.put(m, new ArrayList<GlobalExpr>());
			}
			mentionMap.get(m).add(expr);
		}
//		 LogCenter.debug("[OPTJ] AFTER:");
//		 LogCenter.debug("[OPTJ] mentionMap:\n[OPTJ] " + mentionMap);
//		 LogCenter.debug("[OPTJ] refToExprMap:\n[OPTJ] " + refToExprMap);
//		 LogCenter.debug("[OPTJ] exprToRefMap:\n[OPTJ] " + exprToRefMap);
//		 LogCenter.debug("[OPTJ] ");
	}

	// TODO function calls need to killreferences to all field decls
	// TODO (this always gets called before gen reference, so just make them one
	// method) ?
	public void killReferences(MidMemoryNode node) {
		LogCenter.debug("[OPTJ] Killing references to " + node);
		// LogCenter.debug("[OPTJ] mentionMap:\n[OPTJ] " + mentionMap);
		// LogCenter.debug("[OPTJ] refToExprMap:\n[OPTJ] " + refToExprMap);
		// LogCenter.debug("[OPTJ] exprToRefMap:\n[OPTJ] " + exprToRefMap);
		if (mentionMap.containsKey(node)) {
			// Remove stuff for each expr that is affected by the node.
			for (GlobalExpr e : new ArrayList<GlobalExpr>(mentionMap.get(node))) {
				// Kill the expression e by deleting expression from
				// expr -> [R] and all R -> expr

				// For every reference m used in the expression
				for (MidMemoryNode m : e.getMemoryNodes()) {
					// Remove "e mentions m"
					if (mentionMap.containsKey(m)) {
						List<GlobalExpr> mentions = mentionMap.get(m);
						mentions.remove(e);
						// If it's empty clear the list completely.
						if (mentions.size() == 0) {
							mentionMap.remove(m);
						}
					}
				}

				// Remove reference to this expr for every variable it defines
				// assert exprToRefMap.containsKey(e);
				if (exprToRefMap.containsKey(e)) {
					for (MidMemoryNode m : exprToRefMap.get(e)) {
						// this should always be true?
						if (refToExprMap.containsKey(m)) {
							refToExprMap.remove(m);
						}
					}
				}
				exprToRefMap.remove(e);
			}

		}
		// LogCenter.debug("[OPTJ] AFTER:");
		// LogCenter.debug("[OPTJ] mentionMap:\n[OPTJ] " + mentionMap);
		// LogCenter.debug("[OPTJ] refToExprMap:\n[OPTJ] " + refToExprMap);
		// LogCenter.debug("[OPTJ] exprToRefMap:\n[OPTJ] " + exprToRefMap);
		// LogCenter.debug("[OPTJ] ");
	}

	public Map<GlobalExpr, List<MidMemoryNode>> getExpressionsMap() {
		return exprToRefMap;
	}

	public Map<MidMemoryNode, GlobalExpr> getReferenceMap() {
		return refToExprMap;
	}

	public Map<MidMemoryNode, List<GlobalExpr>> getMentionMap() {
		return mentionMap;
	}

	public List<MidMemoryNode> getReferences(GlobalExpr expr) {
		if (exprToRefMap.containsKey(expr)) {
			return exprToRefMap.get(expr);
		}
		return new ArrayList<MidMemoryNode>();
	}

	/**
	 * When we have expressions like c = a+b, this becomes t1=a, t2=b, c=t1+t2.
	 * Need to map that to c=a+b, so this looks up t1 => a.
	 */
	public MidMemoryNode getNonTempMapping(MidMemoryNode tempNode) {
		GlobalExpr nonTemp = refToExprMap.get(tempNode);
		LogCenter.debug("[OPT] LOOKING UP " + tempNode + " AND FOUND " + nonTemp);
		LogCenter.debug("[OPT] For reference, map was: " + toMapString(refToExprMap));
		if (nonTemp == null) {
			return tempNode;
		}
		if (nonTemp instanceof LeafGlobalExpr) {
			LogCenter.debug("[OPT] RETURNING " + ((LeafGlobalExpr) nonTemp).getMemoryNode());
			return ((LeafGlobalExpr) nonTemp).getMemoryNode();
		}
		return tempNode;
	}

	@Override
	public String toString() {
		return "CSEGlobalState=> mentionMap:\n[OPT]  "
				+ toMapString(mentionMap) + "\n[OPT] refToExprMap:\n[OPT]  "
				+ toMapString(refToExprMap) + "\n[OPT] exprToRefMap:\n[OPT]  "
				+ toMapString(exprToRefMap);
	}

	public static <K, V> String toMapString(HashMap<K, V> map) {
		String out = "{";
		for (K key : map.keySet()) {
			out += "\n[OPT]  " + key.toString() + " = "
					+ map.get(key).toString() + ",";
		}
		out += "\n[OPT]  }";
		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CSEGlobalState)) {
			return false;
		}
		CSEGlobalState global = (CSEGlobalState) o;
		return (getExpressionsMap().equals(global.getExpressionsMap())
				&& getReferenceMap().equals(global.getReferenceMap()) && getMentionMap()
				.equals(global.getMentionMap()));
	}

	public void clear() {
		this.exprToRefMap.clear();
		this.mentionMap.clear();
		this.refToExprMap.clear();
	}

}
