package edu.mit.compilers.codegen;

import java.util.HashMap;
import java.util.Map;

import edu.mit.compilers.LogCenter;
import edu.mit.compilers.codegen.nodes.MidMethodDeclNode;
import edu.mit.compilers.codegen.nodes.MidNode;
import edu.mit.compilers.codegen.nodes.MidSaveNode;
import edu.mit.compilers.codegen.nodes.memory.ArrayReferenceNode;
import edu.mit.compilers.codegen.nodes.memory.MidFieldDeclNode;
import edu.mit.compilers.codegen.nodes.memory.MidLocalMemoryNode;
import edu.mit.compilers.codegen.nodes.memory.MidMemoryNode;
import edu.mit.compilers.codegen.nodes.regops.MidRegisterNode;
import edu.mit.compilers.codegen.nodes.regops.RegisterOpNode;

/**
 * Handles traversing through code in order to assign memory and register
 * locations.
 */
public class MemoryManager {
	// How many bytes each address takes. Use for calculating offsets!
	// Note that it's 8, not 4, in 64-bit since 8*8bytes = 64 bits.
	public static final int ADDRESS_SIZE = 8;
	public static final String ADDRESS_SIZE_STRING = Integer
			.toString(ADDRESS_SIZE);
	private static int localStackSize = 0;

	@SuppressWarnings("serial")
	public static Map<Reg, Boolean> registerAvailabilityMap = new HashMap<Reg, Boolean>() {
		{
			// Only allow R10 and R11
			put(Reg.R10, true);
			put(Reg.R11, true);
		}
	};

	/**
	 * Visits MidSymbolTable generated by MidVisitor and allocates memory
	 * storage.
	 */
	public static void assignStorage(MidSymbolTable codeRoot) {
		// Label the fields.
		Map<String, MidMemoryNode> localVars = codeRoot.getLocalVars();
		String prefix = "field";
		int count = 0;
		for (String fieldName : localVars.keySet()) {
			MidMemoryNode fieldMemoryNode = localVars.get(fieldName);
			assert fieldMemoryNode instanceof MidFieldDeclNode;
			fieldMemoryNode.setRawLocationReference(prefix + count);
			count++;
		}

		Map<String, MidMethodDeclNode> methods = codeRoot.getMethods();
		for (String methodName : methods.keySet()) {
			LogCenter.debug("METHOD: " + methodName);
			processMethod(methods.get(methodName));
		}

		Map<String, MidMethodDeclNode> starbucksMethods = codeRoot
				.getStarbucksMethods();
		for (String methodName : starbucksMethods.keySet()) {
			LogCenter.debug("STARBUCKS_METHOD: " + methodName);
			processMethod(starbucksMethods.get(methodName));
		}
	}
	
	public static MidNode temp;

	private static void processMethod(MidMethodDeclNode methodDeclNode) {
		deallocAllRegisters();
		// Reset the localStackSize.
		localStackSize = 0;
		for (MidNode m : methodDeclNode.getNodeList()) {
			LogCenter.debug("[MEM] " + m.toString());
			if (m instanceof MidLocalMemoryNode) {
				localStackSize += ADDRESS_SIZE;
				((MidMemoryNode) m).setRawLocationReference(Integer
						.toString(localStackSize));
			} else {
				if (m instanceof RegisterOpNode) {
					// We dealloc before alloc in order to allow a register node
					// to save to itself.
					temp = m;
					for (Reg r : ((RegisterOpNode) m).getOperandRegisters()) {
						assert r != null : m + "(" + m.getClass()
								+ ") is missing registers";
						deallocTempRegister(r);
					}
					if (m instanceof MidRegisterNode
							&& !((MidRegisterNode) m).hasRegister()) {
						((MidRegisterNode) m).setRegister(allocTempRegister());
					}
					if (m instanceof MidSaveNode
							&& ((MidSaveNode) m).savesRegister()) {
						deallocTempRegister(((MidSaveNode) m).getRegNode()
								.getRegister());
					}
					if (m instanceof ArrayReferenceNode) {
						ArrayReferenceNode arrayNode = (ArrayReferenceNode) m;
						if (arrayNode.usesArrayReference()) {
							LogCenter
									.debug("[MEM] deallocating array register of "
											+ m);
							deallocTempRegister(arrayNode.getArrayRegister());
						}
					}
				}
				if (m instanceof ArrayReferenceNode) {
					ArrayReferenceNode arrayNode = (ArrayReferenceNode) m;
					if (arrayNode.usesArrayReference()) {
						LogCenter.debug("[MEM] deallocating array register of "
								+ m);
						deallocTempRegister(arrayNode.getArrayRegister());
					}
				}
				if (m instanceof MidRegisterNode
						&& !((MidRegisterNode) m).hasRegister()) {
					((MidRegisterNode) m).setRegister(allocTempRegister());
				}
			}
		}
		methodDeclNode.setLocalStackSize(localStackSize);
	}

	public static Reg allocTempRegister() {
		for (Reg r : registerAvailabilityMap.keySet()) {
			if (registerAvailabilityMap.get(r)) {
				LogCenter.debug("[MEM]  alloc " + r.name());
				registerAvailabilityMap.put(r, false);
				LogCenter.debug("[MEM]  " + registerAvailabilityMap.toString());
				LogCenter.debug("[MEM]");
				return r;
			}
		}
		// return Reg.R9;
		throw new RuntimeException("Ran out of registers somehow!.");
	}

	public static void deallocTempRegister(Reg r) {
		registerAvailabilityMap.put(r, true);
		assert r != null : "Why is this register null?";
		LogCenter.debug("[MEM]  dealloc " + r.name());
		LogCenter.debug("[MEM]  " + registerAvailabilityMap.toString());
		LogCenter.debug("[MEM]");
	}

	private static void deallocAllRegisters() {
		for (Reg r : registerAvailabilityMap.keySet()) {
			deallocTempRegister(r);
		}
	}

}
