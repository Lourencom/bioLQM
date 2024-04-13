package org.colomoto.biolqm.io.modrev;

import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.NodeInfo;
import org.colomoto.biolqm.helper.implicants.Formula;
import org.colomoto.biolqm.helper.implicants.MDD2PrimeImplicants;
import org.colomoto.biolqm.helper.implicants.RestrictedPathSearcher;
import org.colomoto.biolqm.helper.state.PatternStateIterator;
import org.colomoto.biolqm.io.BaseExporter;
import org.colomoto.mddlib.MDDManager;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Exports a logical model into the Model Revision format.
 * 
 * @author Pedro T. Monteiro
 */
public class ModRevExport extends BaseExporter {

	List<NodeInfo> components;

	public ModRevExport(LogicalModel model) {
		super(model);
	}


	public String formatNodeToValidString(String node_id) {
		for (char c : node_id.toCharArray()) {
			if (Character.isUpperCase(c)) {
				// If there's atleast an uppercase letter,
				// return the node_id enclosed in quotation marks
				return "'" + node_id + "'";
			}
		}
		return node_id;
	}

	@Override
	public void export() throws IOException {
		MDDManager ddmanager = model.getMDDManager();
		this.components = model.getComponents();
		MDD2PrimeImplicants primer = new MDD2PrimeImplicants(ddmanager);


		StringBuilder vertex_lines = new StringBuilder();
		StringBuilder edge_lines = new StringBuilder();
		StringBuilder function_lines = new StringBuilder();

		for (NodeInfo i: components) {
			vertex_lines.append("vertex(")
					.append(formatNodeToValidString(i.getNodeID()))
					.append(").\n");
		}


		int[] functions = model.getLogicalFunctions();
		for (int idx = 0; idx < functions.length; idx++) {

			NodeInfo node = components.get(idx);
			String node_id = formatNodeToValidString(components.get(idx).getNodeID());
			int max = node.getMax();
			int node_function = functions[idx];
			if (node.isInput()) {
				vertex_lines.append("fixed(")
						.append(node_id)
						.append(").\n");
				continue;
			}
			
			for (int f = 1; f <= max; f++) {
				Formula formula = primer.getPrimes(node_function, f);
				function_lines.append("functionOr(")
						.append(node_id)
						.append(",1").append(formula.toArray().length > 1 ? ".." + formula.toArray().length : "")
						.append(").\n");

				int term_number = 1;
				for (int[] term : formula.toArray()) {
					for (int i = 0; i < term.length; i++) {
						int var_value = term[i];
						if (var_value < 0) {
							continue;
						}

						String regulator_T = formatNodeToValidString(components.get(formula.regulators[i]).getNodeID());
						function_lines.append("functionAnd(")
								.append(node_id).append(",")
								.append(term_number).append(",")
								.append(regulator_T)
								.append(").\n");

						int interaction;
						if (var_value == 0) {
							interaction = 0;
						} else {
							interaction = 1;
						}

						edge_lines.append("edge(")
								.append(regulator_T).append(",")
								.append(node_id).append(",")
								.append(interaction)
								.append(").\n");
					}
					term_number++;
				}
			}
		}

		Writer writer = streams.writer();

		writer.write(vertex_lines.toString());
		writer.write(edge_lines.toString());
		writer.write(function_lines.toString());

		writer.close();
	}
}
