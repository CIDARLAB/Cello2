/**
 * Copyright (C) 2017-2018
 * Massachusetts Institute of Technology (MIT)
 * Boston University (BU)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.cellocad.cello2.results.logicSynthesis.logic;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cellocad.cello2.common.CObjectCollection;
import org.cellocad.cello2.common.Utils;
import org.cellocad.cello2.common.graph.algorithm.SinkDFS;
import org.cellocad.cello2.common.graph.algorithm.Tarjan;
import org.cellocad.cello2.results.logicSynthesis.LSResults;
import org.cellocad.cello2.results.logicSynthesis.LSResultsUtils;
import org.cellocad.cello2.results.logicSynthesis.logic.truthtable.State;
import org.cellocad.cello2.results.logicSynthesis.logic.truthtable.States;
import org.cellocad.cello2.results.logicSynthesis.logic.truthtable.TruthTable;
import org.cellocad.cello2.results.netlist.Netlist;
import org.cellocad.cello2.results.netlist.NetlistEdge;
import org.cellocad.cello2.results.netlist.NetlistNode;

/**
 * The LSLogicEvaluation class is class evaluating the logic of a netlist in the <i>logicSynthesis</i> stage.
 *
 * @author Vincent Mirian
 * @author Timothy Jones
 *
 * @date 2018-05-21
 *
 */
public class LSLogicEvaluation {

	/**
	 *  Initialize class members
	 */
	private void init() {
		this.truthtables = new HashMap<NetlistNode, TruthTable<NetlistNode, NetlistNode>>();
	}

	/**
	 *  Initializes a newly created LSLogicEvaluation using the Netlist defined by parameter <i>netlist</i>
	 *
	 *  @param netlist the Netlist
	 */
	public LSLogicEvaluation (Netlist netlist) {
		this.init();
		if (!netlist.isValid()) {
			throw new RuntimeException("netlist is not valid!");
		}
		if (!this.isSupportedTopology(netlist)) {
			throw new RuntimeException("Topology not supported!");
		}
		CObjectCollection<NetlistNode> inputNodes = LSResultsUtils.getPrimaryInputNodes(netlist);
		inputNodes.addAll(this.getStatefulNodes(netlist));
		Boolean One = new Boolean(true);
		Boolean Zero = new Boolean(false);
		States<NetlistNode> states = new States<NetlistNode>(inputNodes, One, Zero);
		this.setStates(states);
		List<NetlistNode> outputNodes = new ArrayList<NetlistNode>();
		for(int i = 0; i < netlist.getNumVertex(); i++) {
			NetlistNode node = netlist.getVertexAtIdx(i);
			outputNodes.clear();
			outputNodes.add(node);
			TruthTable<NetlistNode, NetlistNode> truthTable = new TruthTable<NetlistNode, NetlistNode>(states, outputNodes);
			this.getTruthTables().put(node, truthTable);
		}
		this.evaluate(netlist);
		Collection<State<NetlistNode>> forbidden = this.getForbiddenStates(netlist,states);
		states.removeStates(forbidden);
		this.setStates(states);
		outputNodes = new ArrayList<NetlistNode>();
		for(int i = 0; i < netlist.getNumVertex(); i++) {
			NetlistNode node = netlist.getVertexAtIdx(i);
			outputNodes.clear();
			outputNodes.add(node);
			TruthTable<NetlistNode, NetlistNode> truthTable = new TruthTable<NetlistNode, NetlistNode>(states, outputNodes);
			this.getTruthTables().put(node, truthTable);
		}
		this.evaluate(netlist);
	}

	private Collection<State<NetlistNode>> getForbiddenStates(Netlist netlist, States<NetlistNode> states) {
		Collection<State<NetlistNode>> rtn = new HashSet<>();
		Tarjan<NetlistNode,NetlistEdge,Netlist> tarjan = new Tarjan<>(netlist);
		CObjectCollection<NetlistNode> component = null;
		while ((component = tarjan.getNextComponent()) != null) {
			if (this.isNORLatch(component)) {
				NetlistNode a = component.get(0);
				NetlistNode b = component.get(1);
				NetlistNode in1 = null;
				NetlistNode in2 = null;
				for (int j = 0; j < a.getNumInEdge(); j++) {
					NetlistEdge e = a.getInEdgeAtIdx(j);
					NetlistNode src = e.getSrc();
					if (src.equals(b))
						continue;
					else
						in1 = src;
				}
				for (int j = 0; j < b.getNumInEdge(); j++) {
					NetlistEdge e = b.getInEdgeAtIdx(j);
					NetlistNode src = e.getSrc();
					if (src.equals(a))
						continue;
					else
						in2 = src;
				}
				TruthTable<NetlistNode,NetlistNode> in1TruthTable = this.getTruthTable(in1);
				TruthTable<NetlistNode,NetlistNode> in2TruthTable = this.getTruthTable(in2);
				TruthTable<NetlistNode,NetlistNode> aTruthTable = this.getTruthTable(a);
				TruthTable<NetlistNode,NetlistNode> bTruthTable = this.getTruthTable(b);
				for (int i = 0; i < states.getNumStates(); i++) {
					State<NetlistNode> state = states.getStateAtIdx(i);
					State<NetlistNode> in1StateOutput = in1TruthTable.getStateOutput(state);
					State<NetlistNode> in2StateOutput = in2TruthTable.getStateOutput(state);
					State<NetlistNode> aStateOutput = aTruthTable.getStateOutput(state);
					State<NetlistNode> bStateOutput = bTruthTable.getStateOutput(state);
					if (aStateOutput.getState(a).equals(bStateOutput.getState(b)))
						rtn.add(state);
					if (in1StateOutput.getState(in1).equals(true)
						&&
						in2StateOutput.getState(in2).equals(true))
						rtn.add(state);
				}
			}
		}
		return rtn;
	}

	/**
	 * Returns a CObjectCollection of NetlistNode from the netlist with nodes that hold state
	 *
	 * @return a CObjectCollection of NetlistNode from the netlist with nodes that hold state
	 */
	private CObjectCollection<NetlistNode> getStatefulNodes(Netlist netlist) {
		CObjectCollection<NetlistNode> rtn = new CObjectCollection<>();
		Tarjan<NetlistNode,NetlistEdge,Netlist> tarjan = new Tarjan<>(netlist);
		CObjectCollection<NetlistNode> component = null;
		while ((component = tarjan.getNextComponent()) != null) {
			if (component.size() <= 1)
				continue;
			for (int i = 0; i < component.size(); i++) {
				rtn.add(component.get(i));
			}
		}
		return rtn;
	}

	private Boolean isUpstreamOnceOf(NetlistNode src, NetlistNode dst) {
		Boolean rtn = false;
		for (int i = 0; i < src.getNumOutEdge(); i++) {
			NetlistEdge e = src.getOutEdgeAtIdx(i);
			for (int j = 0; j < e.getNumDst(); j++) {
				NetlistNode temp = e.getDstAtIdx(j);
				if (temp.equals(dst)) {
					rtn = rtn ^ true;
				}
			}
		}
		return rtn;
	}

	/**
	 * Returns true if the collection of NetlistNode is in a NOR latch configuration
	 *
	 * @param component the collection of NetlistNode
	 * @return true if the collection of NetlistNode is in a NOR latch configuration
	 */
	private Boolean isNORLatch(CObjectCollection<NetlistNode> component) {
		Boolean rtn = false;
		if (component.size() != 2)
			return false;
		NetlistNode a = component.get(0);
		NetlistNode b = component.get(1);
		if (
			this.isUpstreamOnceOf(a,b)
			&&
			this.isUpstreamOnceOf(b,a)
			&&
			a.getResultNetlistNodeData().getNodeType().equals(LSResults.S_NOR)
			&&
			b.getResultNetlistNodeData().getNodeType().equals(LSResults.S_NOR)
			) {
			rtn = true;

		}
		return rtn;
	}

	/**
	 * Returns true if the netlist topology is supported, false otherwise
	 *
	 * @param netlist the Netlist
	 * @return true if the netlist topology is supported, false otherwise
	 */
	private Boolean isSupportedTopology(Netlist netlist) {
		Boolean rtn = true;
		Tarjan<NetlistNode,NetlistEdge,Netlist> tarjan = new Tarjan<>(netlist);
		CObjectCollection<NetlistNode> component = null;
		while ((component = tarjan.getNextComponent()) != null) {
			if (component.size() <= 1)
				continue;
			if (component.size() > 2) {
				rtn = false;
				break;
			}
			if (!this.isNORLatch(component)) {
				rtn = false;
				break;
			}
		}
		return rtn;
	}

	/**
	 *  Returns a Boolean representation of the evaluation of the NodeType defined by <i>nodeType</i>
	 *  with input defined by parameters <i>inputs</i>
	 *
	 *  @param inputs a List of inputs
	 *  @param nodeType the NodeType
	 *  @return a Boolean representation of the evaluation of the NodeType defined by <i>nodeType</i>
	 *  with input defined by parameters <i>inputs</i>
	 */
	private Boolean computeLogic(final List<Boolean> inputs, final String nodeType) {
		Boolean rtn = inputs.get(0);
		for (int i = 1; i < inputs.size(); i++) {
			Boolean value = inputs.get(i);
			switch (nodeType) {
			case LSResults.S_AND:{
				rtn = rtn && value;
				break;
			}
			case LSResults.S_OR:{
				rtn = rtn || value;
				break;
			}
			case LSResults.S_XOR:{
				rtn = rtn ^ value;
				break;
			}
			default:{
				throw new RuntimeException("Unknown nodeType");
			}
			}
		}
		return rtn;
	}

	/**
	 *  Returns a List of Boolean representation of the input values for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return a List of Boolean representation of the input values for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private List<Boolean> getInputLogic(final NetlistNode node, final State<NetlistNode> state) {
		List<Boolean> rtn = new ArrayList<Boolean>();
		for (int i = 0; i < node.getNumInEdge(); i++) {
			NetlistNode inputNode = node.getInEdgeAtIdx(i).getSrc();
			if (state.getState(inputNode) != null) {
				rtn.add(state.getState(inputNode));
				continue;
			}
			TruthTable<NetlistNode, NetlistNode> truthTable = this.getTruthTables().get(inputNode);
			truthTable.getStateOutput(state);
			State<NetlistNode> outputState = truthTable.getStateOutput(state);
			if (outputState.getNumStatePosition() != 1) {
				throw new RuntimeException("Invalid number of output(s)!");
			}
			rtn.add(outputState.getState(inputNode));
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for a Primary Output for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for a Primary Output for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computePrimaryOutput(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() == 1) {
			rtn = inputList.get(0);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for a Primary Input for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for a Primary Input for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computePrimaryInput(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() == 0) {
			rtn = state.getState(node);
		}
		if (inputList.size() > 1) {
			rtn = this.computeOR(node,state);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for a NOT NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for a NOT NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeNOT(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() == 1) {
			rtn = inputList.get(0);
			rtn = !(rtn);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for an AND NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for an AND NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeAND(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() > 1) {
			rtn = this.computeLogic(inputList, LSResults.S_AND);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for an NAND NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for an NAND NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeNAND(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() > 1) {
			rtn = this.computeLogic(inputList, LSResults.S_AND);
			rtn = !(rtn);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for an OR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for an OR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeOR(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() > 1) {
			rtn = this.computeLogic(inputList, LSResults.S_OR);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for an NOR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for an NOR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeNOR(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() > 1) {
			rtn = this.computeLogic(inputList, LSResults.S_OR);
			rtn = !(rtn);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for an XOR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for an XOR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeXOR(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() > 1) {
			rtn = this.computeLogic(inputList, LSResults.S_XOR);
		}
		return rtn;
	}

	/**
	 *  Returns the evaluation for an XNOR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 *
	 *  @param node the NetlistNode
	 *  @param state the state
	 *  @return the evaluation for an XNOR NodeType for NetlistNode defined by parameter <i>node</i>
	 *  at the state defined by parameter <i>state</i>
	 */
	private Boolean computeXNOR(final NetlistNode node, final State<NetlistNode> state) {
		Boolean rtn = null;
		List<Boolean> inputList = this.getInputLogic(node, state);
		if (inputList.size() > 1) {
			rtn = this.computeLogic(inputList, LSResults.S_XOR);
			rtn = !(rtn);
		}
		return rtn;
	}

	/**
	 *  Evaluates the truth table for the NetlistNode defined by parameter <i>node</i>
	 *
	 *  @param node the NetlistNode
	 */
	private void evaluateTruthTable(final NetlistNode node) {
		Boolean result = null;
		final String nodeType = node.getResultNetlistNodeData().getNodeType();
		TruthTable<NetlistNode, NetlistNode> truthTable = this.getTruthTables().get(node);
		for (int i = 0; i < truthTable.getNumStates(); i ++) {
			State<NetlistNode> inputState = truthTable.getStateAtIdx(i);
			State<NetlistNode> outputState = truthTable.getStateOutput(inputState);
			if (outputState.getNumStatePosition() != 1) {
				throw new RuntimeException("Invalid number of output(s)!");
			}
			switch (nodeType) {
			case LSResults.S_PRIMARYINPUT:{
				result = this.computePrimaryInput(node, inputState);
				break;
			}
			case LSResults.S_PRIMARYOUTPUT:{
				result = this.computePrimaryOutput(node, inputState);
				break;
			}
			case LSResults.S_INPUT:{
				continue;
			}
			case LSResults.S_OUTPUT:{
				continue;
			}
			case LSResults.S_NOT:{
				result = this.computeNOT(node, inputState);
				break;
			}
			case LSResults.S_AND:{
				result = this.computeAND(node, inputState);
				break;
			}
			case LSResults.S_NAND:{
				result = this.computeNAND(node, inputState);
				break;
			}
			case LSResults.S_OR:{
				result = this.computeOR(node, inputState);
				break;
			}
			case LSResults.S_NOR:{
				result = this.computeNOR(node, inputState);
				break;
			}
			case LSResults.S_XOR:{
				result = this.computeXOR(node, inputState);
				break;
			}
			case LSResults.S_XNOR:{
				result = this.computeXNOR(node, inputState);
				break;
			}
			default:{
				throw new RuntimeException("Unknown nodeType");
			}
			}
			TruthTable<NetlistNode,NetlistNode> truthtable = this.getTruthTable(node);
			State<NetlistNode> state = truthtable.getStateOutput(inputState);
			// stateful nodes
			if (!nodeType.equals(LSResults.S_PRIMARYINPUT)
				&&
				inputState.getState(node) != null) {
				result = inputState.getState(node);
			}
			// System.out.println(node.getName());
			// System.out.println(result);
			// System.out.println(this.getInputLogic(node,inputState));
			// System.out.println();
			Utils.isNullRuntimeException(result, "result");
			if (!outputState.setState(node, result)) {
				throw new RuntimeException("Node does not exist");
			}
		}
	}

	/**
	 *  Evaluates the Netlist defined by parameter <i>netlist</i>
	 *
	 *  @param netlist the Netlist
	 */
	protected void evaluate(Netlist netlist){
		SinkDFS<NetlistNode, NetlistEdge, Netlist> DFS = new SinkDFS<NetlistNode, NetlistEdge, Netlist>(netlist);
		NetlistNode node = null;
		node = DFS.getNextVertex();
		while (node != null) {
			evaluateTruthTable(node);
			node = DFS.getNextVertex();
		}
	}

	protected Map<NetlistNode, TruthTable<NetlistNode, NetlistNode>> getTruthTables(){
		return this.truthtables;
	}

	protected void setStates(States<NetlistNode> states){
		this.states = states;
	}

	/**
	 *  Getter for <i>states</i>
	 *  @return the states of this instance
	 */
	public States<NetlistNode> getStates(){
		return this.states;
	}

	/**
	 *  Returns the truthTable of NetlistNode defined by parameter <i>node</i>
	 *
	 *  @param node the NetlistNode
	 *  @return the truthTable of NetlistNode defined by parameter <i>node</i>
	 */
	public TruthTable<NetlistNode, NetlistNode> getTruthTable(final NetlistNode node){
		TruthTable<NetlistNode, NetlistNode> rtn = null;
		rtn = this.getTruthTables().get(node);
		return rtn;
	}

	public String toString() {
		String rtn = "";
		rtn += Utils.getNewLine();
		rtn += S_HEADER + Utils.getNewLine();
		rtn += "LSLogicEvaluation" + Utils.getNewLine();
		rtn += S_HEADER + Utils.getNewLine();
		for (NetlistNode node : this.getTruthTables().keySet()) {
			rtn += String.format("%-15s",node.getName()) + Utils.getTabCharacter();
			TruthTable<NetlistNode,NetlistNode> truthtable = this.getTruthTables().get(node);
			for (int i = 0; i < truthtable.getNumStates(); i++) {
				State<NetlistNode> input = truthtable.getStateAtIdx(i);
				State<NetlistNode> output = truthtable.getStateOutput(input);
				rtn += output.getState(node) + Utils.getTabCharacter();
			}
			rtn += Utils.getNewLine();
		}
		rtn += S_HEADER + Utils.getNewLine();
		return rtn;
	}

	/**
	 *  Writes this instance in CSV format to the writer defined by parameter <i>os</i> with the delimiter equivalent to the parameter <i>delimiter</i>
	 *  @param delimiter the delimiter
	 *  @param os the writer
	 *  @throws IOException If an I/O error occurs
	 */
	public void writeCSV(String delimiter, Writer os) throws IOException {
		String str = "";
		for (NetlistNode node : this.getTruthTables().keySet()) {
			str += node.getName();
			TruthTable<NetlistNode,NetlistNode> truthtable = this.getTruthTable(node);
			for (int i = 0; i < truthtable.getNumStates(); i++) {
				State<NetlistNode> input = truthtable.getStateAtIdx(i);
				State<NetlistNode> output = truthtable.getStateOutput(input);
				str += delimiter;
				str += String.format("%s",output.getState(node));
			}
			str += Utils.getNewLine();
		}
		os.write(str);
	}

	private static final String S_HEADER = "--------------------------------------------";

	private Map<NetlistNode, TruthTable<NetlistNode, NetlistNode>> truthtables;
	private States<NetlistNode> states;
}
