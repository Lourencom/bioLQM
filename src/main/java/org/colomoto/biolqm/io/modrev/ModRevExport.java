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
		return "\"" + node_id + "\"";
	}

	@Override
	public void export() throws IOException {
		MDDManager ddmanager = model.getMDDManager();
		this.components = model.getComponents();
		MDD2PrimeImplicants primer = new MDD2PrimeImplicants(ddmanager);

		StringBuilder sbSpec = new StringBuilder();
		sbSpec.append("% List of model components\n");

		for (NodeInfo i: components) {
			sbSpec.append("vertex(")
				.append(formatNodeToValidString(i.getNodeID()))
				.append(").\n");
		}

		int[] functions = model.getLogicalFunctions();
		for (int idx = 0; idx < functions.length; idx++) {
			NodeInfo node = components.get(idx);
			String formatedNode = formatNodeToValidString(components.get(idx).getNodeID());
			int max = node.getMax();
			int node_function = functions[idx];

			sbSpec.append("\n% Regulation of ")
				.append(node.getNodeID())
				.append("\n");
			if (node.isInput()) {
				sbSpec.append("input(")
					.append(formatedNode)
					.append(").\n");
				continue;
			}
			
			for (int f = 1; f <= max; f++) {
				Formula formula = primer.getPrimes(node_function, f);
				sbSpec.append("functionOr(")
					.append(formatedNode)
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
						sbSpec.append("functionAnd(")
							.append(formatedNode).append(",")
							.append(term_number).append(",")
							.append(regulator_T)
							.append(").\n");

						int interaction;
						if (var_value == 0) {
							interaction = 0;
						} else {
							interaction = 1;
						}

						sbSpec.append("edge(")
							.append(regulator_T).append(",")
							.append(formatedNode).append(",")
							.append(interaction)
							.append(").\n");
					}
					term_number++;
				}
			}
		}

		Writer writer = streams.writer();
		writer.write(sbSpec.toString());
		writer.close();
	}
}
