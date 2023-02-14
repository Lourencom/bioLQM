package org.colomoto.biolqm.io.modrev;

import org.colomoto.biolqm.ConnectivityMatrix;
import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.ModelLayout;
import org.colomoto.biolqm.NodeInfo;
import org.colomoto.biolqm.io.BaseExporter;
import org.colomoto.biolqm.io.sbml.SBMLQualBundle;
import org.colomoto.biolqm.metadata.annotations.URI;
import org.colomoto.biolqm.metadata.constants.Qualifier;
import org.colomoto.mddlib.MDDManager;
import org.colomoto.mddlib.MDDVariable;
import org.colomoto.mddlib.PathSearcher;
import org.colomoto.biolqm.metadata.Pair;
import org.colomoto.biolqm.metadata.Annotator;

import org.colomoto.mddlib.VariableEffect;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.ext.layout.*;
import org.sbml.jsbml.ext.qual.*;
import org.sbml.jsbml.xml.XMLNode;
import scala.xml.Null;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


public class ModRevExport extends BaseExporter {
    // To export, need to create a file with all edges, then boolean functions,
    // like so:
    // vertex(ci1).
    // edge(ci1, ciact_b2, 1).
    // edge(ciact_b1, ciact_b2, 1).
    // ...
    // functionOr(slp, 1).
    // functionAnd(en, 1, pka_b2).
    // functionOr(fz, 1..2).

    public ModRevExport(LogicalModel model) {
        super(model);
    }

    @Override
    public void export() throws IOException {
        final List<NodeInfo> nodes = model.getComponents();

        Writer writer = streams.writer();

        MDDManager ddmanager = model.getMDDManager();

        ConnectivityMatrix matrix = new ConnectivityMatrix(model);


        writer.write("ModRev exporter\n");
        for (NodeInfo i: nodes) {
            writer.write("vertex(" + i.getNodeID() +").\n");
        }

        // Get the edges between the nodes and write
        // edge(v1, v2, 1(0)) for each edge depending on if interaction is positive or negative
        // get regulators
        for (int i = 0; i < nodes.size(); i++) {
            int[] regulators = matrix.getRegulators(i, false);
            VariableEffect[][] effects = matrix.getRegulatorEffects(i, false);
            for (int j = 0; j < regulators.length; j++) {
                int regulator = regulators[j];
                VariableEffect[] effect = effects[j];
                if (effect[0] == VariableEffect.POSITIVE) {
                    writer.write("edge(" + nodes.get(regulator).getNodeID() + ", " + nodes.get(i).getNodeID() + ", 1).\n");
                } else if (effect[0] == VariableEffect.NEGATIVE) {
                    writer.write("edge(" + nodes.get(regulator).getNodeID() + ", " + nodes.get(i).getNodeID() + ", 0).\n");
                } else {
                    writer.write("edge(" + nodes.get(regulator).getNodeID() + ", " + nodes.get(i).getNodeID() + ", 1).\n");
                    writer.write("edge(" + nodes.get(i).getNodeID() + ", " + nodes.get(regulator).getNodeID() + ", 1).\n");
                }
            }
        }
        // Get the functions
        // The predicate functionOr(V,1..N) indicates that the regulatory function of V is a disjunction of N terms.
        // The predicate functionAnd(V,T,R) is then used to represent that node R is a regulator
        // of V and is present in the term T of the regulatory function.
        int[] functions = model.getLogicalFunctions();


        writer.close();
    }
}


