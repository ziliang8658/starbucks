package edu.mit.compilers.opt.regalloc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.mit.compilers.LogCenter;
import edu.mit.compilers.codegen.MidSymbolTable;
import edu.mit.compilers.codegen.Reg;
import edu.mit.compilers.codegen.nodes.MidMethodDeclNode;
import edu.mit.compilers.codegen.nodes.MidNode;
import edu.mit.compilers.codegen.nodes.MidSaveNode;
import edu.mit.compilers.codegen.nodes.regops.MidLoadNode;
import edu.mit.compilers.opt.HashMapUtils;

public class RegisterAllocator {

	private final MidSymbolTable symbolTable;
	private final static Reg[] USABLE_REGISTERS = {
//		Reg.R10, Reg.R11,
		Reg.R12, Reg.R13, Reg.R14, Reg.R15
	};

	public RegisterAllocator(MidSymbolTable symbolTable) {
		this.symbolTable = symbolTable;
	}

	public void run() {
		LivenessDoctor analyzer = new LivenessDoctor();
		Map<MidSaveNode, Set<MidLoadNode>> defUseMap = analyzer
				.analyze(symbolTable);
		WebKnitter knitter = new WebKnitter(defUseMap);
		List<Web> webs = knitter.run();
		InterferenceGenerator gen = new InterferenceGenerator(
				knitter.getWebMapDefs(), knitter.getWebMapUses());
		gen.analyze(symbolTable);
		LogCenter.debug("RA", "Webs created:");
		for (Web w : webs) {
			LogCenter.debug("RA", String.format("%s: %s", w, w
					.getInterferences()));
		}
		GraphColorer crayola = new GraphColorer(USABLE_REGISTERS);
		Map<Web, Reg> mapping = crayola.color(webs);
		LogCenter.debug("RA", "Coloring results: "
				+ HashMapUtils.toMapString(mapping));

		for (Entry<String, MidMethodDeclNode> entry : symbolTable.getMethods()
				.entrySet()) {
			applyAllocations(entry.getValue(), mapping, knitter);
		}
	}

	private void applyAllocations(MidMethodDeclNode methodDeclNode,
			Map<Web, Reg> mapping, WebKnitter knitter) {
		for (MidNode node : methodDeclNode.getNodeList()) {
			if (node instanceof MidLoadNode) {
				MidLoadNode loadNode = (MidLoadNode) node;
				Reg allocatedReg = mapping.get(knitter.lookupWeb(loadNode));
				if (allocatedReg != null) {
					LogCenter.debug("RA", "Allocating " + allocatedReg.name()
							+ " to " + node);
					loadNode.allocateRegister(allocatedReg);
				}
			} else if (node instanceof MidSaveNode) {
				MidSaveNode saveNode = (MidSaveNode) node;
				Reg allocatedReg = mapping.get(knitter.lookupWeb(saveNode));
				if (allocatedReg != null) {
					LogCenter.debug("RA", "Allocating " + allocatedReg.name()
							+ " to " + node);
					saveNode.allocateRegister(allocatedReg);
				}
			}
		}
	}

	/**
	 * Debugging code for printing interference graph.
	 * 
	 * @param webs
	 */
	private void printInterferenceGraph(List<Web> webs) {
		System.out.println("digraph InterferenceGraph {\n");
		for (Web web : webs) {
			LogCenter.debug("RA", web.toString());
			System.out.println(String.format("%s [label=\"%s\"];", web
					.hashCode(), web.toString()));
			for (Web inter : web.getInterferences()) {
				System.out
						.println(String.format("%s -> %s;", web.hashCode(), inter
								.hashCode()));
			}
		}
		System.out.println("}");

	}

}
