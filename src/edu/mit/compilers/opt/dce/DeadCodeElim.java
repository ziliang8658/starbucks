package edu.mit.compilers.opt.dce;

import java.util.Set;

import edu.mit.compilers.LogCenter;
import edu.mit.compilers.codegen.nodes.MidCallNode;
import edu.mit.compilers.codegen.nodes.MidNode;
import edu.mit.compilers.codegen.nodes.MidSaveNode;
import edu.mit.compilers.codegen.nodes.memory.MidConstantNode;
import edu.mit.compilers.codegen.nodes.memory.MidLocalMemoryNode;
import edu.mit.compilers.codegen.nodes.memory.MidMemoryNode;
import edu.mit.compilers.codegen.nodes.regops.MidArithmeticNode;
import edu.mit.compilers.codegen.nodes.regops.MidLoadNode;
import edu.mit.compilers.codegen.nodes.regops.MidNegNode;
import edu.mit.compilers.codegen.nodes.regops.MidUseNode;
import edu.mit.compilers.opt.AnalyzerHelpers;
import edu.mit.compilers.opt.Block;
import edu.mit.compilers.opt.Transformer;
import edu.mit.compilers.opt.regalloc.LivenessState;

public class DeadCodeElim extends Transformer<LivenessState> {

	@Override
	protected void transform(Block b, LivenessState state) {

		LivenessState localState;
		if (state == null) {
			// Should be the return block
			assert b.getSuccessors().isEmpty();
			localState = new LivenessState();
		} else {
			localState = state.clone();
		}

		for (MidNode node : b.reverse()) {
			LogCenter.debug("DCEDEBUG", "ITERATING OVER " + node.toString());
			if (node instanceof MidUseNode) {
				// Use.
				localState.processUse((MidUseNode) node);
			} else if (node instanceof MidSaveNode) {
				Set<MidUseNode> uses = localState.getUses(((MidSaveNode) node)
						.getDestinationNode());
				if (uses == null || uses.isEmpty()) {
					// Delete dead code, only if dealing with local variables.
					MidMemoryNode destNode = ((MidSaveNode) node)
							.getDestinationNode();
					if (destNode instanceof MidLocalMemoryNode
							|| destNode instanceof MidConstantNode) {
						deleteSaveNodeEtAl(b, (MidSaveNode) node);
					}
				} else {
					// Definition.
					localState.processDefinition((MidSaveNode) node);
				}
			}
		}

	}

	private void deleteSaveNodeEtAl(Block block, MidSaveNode saveNode) {
		LogCenter.debug("DCE", "DELETING " + saveNode);
		if (saveNode.savesRegister()) {
			if (saveNode.getRegNode() instanceof MidCallNode) {
				AnalyzerHelpers.completeDeleteMethodSave(saveNode, block);
			} else if (saveNode.getRegNode() instanceof MidLoadNode) {
				AnalyzerHelpers.completeDeleteAssignment(saveNode, block);
			} else if (saveNode.getRegNode() instanceof MidNegNode) {
				// a = -x
				AnalyzerHelpers.completeDeleteUnary(saveNode, block);
			} else if (saveNode.getRegNode() instanceof MidArithmeticNode) {
				// a = x + y
				AnalyzerHelpers.completeDeleteBinary(saveNode, block);
			} else {
				assert false : "Could not handle this.";
			}
		}
	}

}
