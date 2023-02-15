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

	@Override
	public void export() throws IOException {
		MDDManager ddmanager = model.getMDDManager();
		this.components = model.getComponents();
		MDD2PrimeImplicants primer = new MDD2PrimeImplicants(ddmanager);

		Writer writer = streams.writer();

		for (NodeInfo i: components) {
			writer.write("vertex(" + i.getNodeID() +").\n");
		}


		int[] functions = model.getLogicalFunctions();
		for (int idx = 0; idx < functions.length; idx++) {

			String node_id = components.get(idx).getNodeID();
			int node_function = functions[idx];

			Formula formula = primer.getPrimes(node_function, 1);
			writer.write("functionOr(" + node_id + ", 1" + (formula.toArray().length > 1 ? ".." + formula.toArray().length : "") + ").\n");

			int term_number = 1;
			for (int[] term : formula.toArray()) {
				for (int i = 0; i < term.length; i++) {
					int var_value = term[i];
					if (var_value < 0) {
						continue;
					}

					String regulator_T = components.get(formula.regulators[i]).getNodeID();
					writer.write("functionAnd(" + node_id + ", " + term_number + ", " + regulator_T + ").\n");

					int interaction;
					if (var_value == 0) {
						interaction = 0;
					} else {
						interaction = 1;
					}

					writer.write("edge(" + regulator_T + ", " + node_id + "," + interaction + ").\n");
				}
				term_number++;
			}
		}

		writer.close();
	}
}
