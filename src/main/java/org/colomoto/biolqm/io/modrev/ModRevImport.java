package org.colomoto.biolqm.io.modrev;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.colomoto.biolqm.helper.implicants.Term;
import org.colomoto.biolqm.io.BaseLoader;
import org.colomoto.biolqm.io.antlr.*;
import org.colomoto.mddlib.logicalfunction.FunctionNode;
import org.colomoto.mddlib.logicalfunction.OperandFactory;
import org.colomoto.mddlib.logicalfunction.SimpleOperandFactory;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.colomoto.biolqm.LogicalModel;
import org.colomoto.biolqm.NodeInfo;
import org.w3c.dom.Node;
import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.colomoto.biolqm.io.modrev.ModRevImport.removeQuotationMarks;

public class ModRevImport extends BaseLoader {

    public LogicalModel performTask() throws IOException {

        CharStream input = new ANTLRInputStream(streams.reader());
        ErrorListener errors = new ErrorListener();

        ModRevParser parser = getParser(input, errors);
        ModRevParser.ModelContext mctx = parser.model();

        if (errors.hasErrors()) {
            System.out.println("Found some errors:");
            for (String s: errors.getErrors()) {
                System.out.println(" "+s);
            }
            return null;
        }


        List<NodeInfo> variables = new ArrayList<>();
        Map<String, NodeInfo> id2var = new HashMap<String, NodeInfo>();

        // get all nodes to create operand factory
        for (int i = 0; i < mctx.children.size(); i++) {
            ParseTree child = mctx.children.get(i);
            child = child.getChild(0);

            if (child instanceof ModRevParser.VertexContext) {
                String vertex = removeQuotationMarks(child.getChild(1).getText());
                if (id2var.containsKey(vertex)) {
                    continue;
                }

                NodeInfo ni = new NodeInfo(vertex);

                id2var.put(vertex, ni);
                variables.add(ni);
            }
        }

        // create operand factory
        OperandFactory operandFactory = new SimpleOperandFactory<NodeInfo>(variables);

        // walk the entire tree to parse useful stuff
        ModRevParserListener listener = new ModRevParserListener();
        ParseTreeWalker.DEFAULT.walk(listener, mctx);

        // get stuff
        List<Edge> edges = listener.getEdges();
        List<FunctionOR> functionORs = listener.getFunctionORs();
        List<FunctionAnd> functionAnds = listener.getFunctionAnds();

        Map<NodeInfo, FunctionNode> var2function = new HashMap<NodeInfo, FunctionNode>();


        // TODO: construct FunctionNode for each node
        for (NodeInfo ni : variables) {
            // based on the arrays of FunctionORs and functionAnds, construct FunctionNode
            ExpressionStack stack = new ExpressionStack(operandFactory);
            stack.clear();

            for (FunctionOR functionOR : functionORs) {

                if (functionOR.getFirst().equals(ni.getNodeID())) {
                    String range = functionOR.getRange();
                    String[] rangeArray = range.split("\\.\\.");
                    int start = Integer.parseInt(rangeArray[0]);
                    int end = start;
                    if (rangeArray.length > 1) {
                        end = Integer.parseInt(rangeArray[1]);
                    }


                    // create all And terms and OR them together
                    for (int term = start; term <= end; term++) {
                        int num_nodes = 0;
                        for (FunctionAnd functionAnd : functionAnds) {

                            if (functionAnd.getFirst().equals(ni.getNodeID()) &&
                                    functionAnd.getTerm().equals(Integer.toString(term))) {

                                String target = functionAnd.getSecond();

                                int interaction = -1;
                                // from edges, print the edge(target, ni)
                                for (Edge edge : edges) {
                                    if (edge.getTarget().equals(ni.getNodeID()) && edge.getSource().equals(target)) {
                                        interaction = Integer.parseInt(edge.getIntval());
                                        break;
                                    }
                                }

                                stack.ident(target);
                                if (interaction == 0) {
                                    stack.not();
                                }
                                num_nodes++;
                                if (num_nodes > 1) {
                                    stack.operator(Operator.AND);
                                }
                            }
                        }


                    }

                    if (end > start) {
                        // end is supposed to reflect stack.size
                        while (end > 1) {
                            stack.operator(Operator.OR);
                            end--;
                        }
                    }

                    try {
                        FunctionNode fn = stack.done();
                        var2function.put(ni, fn);
                        break;
                    } catch (Exception e) {
                        // stack was empty
                        // no AND functions specified
                    }
                }
            }
        }

        return ExpressionStack.constructModel(operandFactory, variables, var2function);
    }

    private ModRevParser getParser(CharStream input, ErrorListener errors) {
        ModRevLexer lexer = new ModRevLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);
        ModRevParser parser = new ModRevParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(errors);

        return parser;
    }

    public static String removeQuotationMarks(String node_id) {
        if (node_id != null && !node_id.isEmpty()) {
            node_id = node_id.trim(); // Trim whitespace
            if (node_id.startsWith("'") && node_id.endsWith("'") && node_id.length() > 1) {
                return node_id.substring(1, node_id.length() - 1);
            }
        }
        return node_id;
    }


}


class Edge {
    private String source;
    private String target;
    private String intval;

    public Edge(String source, String target, String intval) {
        this.source = source;
        this.target = target;
        this.intval = intval;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getIntval() {
        return intval;
    }
}


class FunctionOR {
    private String first;
    private String range;

    public FunctionOR(String first, String range) {
        this.first = first;
        this.range = range;
    }

    public String getFirst() {
        return first;
    }

    public String getRange() {
        return range;
    }
}

class FunctionAnd {
    private String first;
    private String second;
    private String value;

    public FunctionAnd(String first, String second, String value) {
        this.first = first;
        this.second = second;
        this.value = value;
    }

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    public String getTerm() {
        return value;
    }
}

class ModRevParserListener extends ModRevBaseListener {
    private final ParseTreeWalker walker = new ParseTreeWalker();
    private List<NodeInfo> vertices = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private List<FunctionOR> functionORs = new ArrayList<>();
    private List<FunctionAnd> functionAnds = new ArrayList<>();

    public ModRevParserListener() {
        super();
        // TODO: Might be able to use
        // this.stack = new ExpressionStack(operandFactory);
        // this.operandFactory = operandFactory;
    }

    public List<NodeInfo> getVariables() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<FunctionOR> getFunctionORs() {
        return functionORs;
    }

    public List<FunctionAnd> getFunctionAnds() {
        return functionAnds;
    }

    @Override
    public void exitVertex(@NotNull ModRevParser.VertexContext ctx) {
        String vertexID = removeQuotationMarks(ctx.children.get(1).getText());
        NodeInfo ni = new NodeInfo(vertexID);
        vertices.add(ni);
    }

    public void exitEdge(@NotNull ModRevParser.EdgeContext ctx) {
        String source = removeQuotationMarks(ctx.ID(0).getText());
        String target = removeQuotationMarks(ctx.ID(1).getText());
        String intval = ctx.INT().getText();

        Edge edge = new Edge(source, target, intval);
        edges.add(edge);

    }

    @Override
    public void exitFunctionAnd(@NotNull ModRevParser.FunctionAndContext ctx) {
        String first = removeQuotationMarks(ctx.ID(0).getText());
        String second = removeQuotationMarks(ctx.ID(1).getText());
        String value = ctx.INT().getText();

        FunctionAnd functionAnd = new FunctionAnd(first, second, value);
        functionAnds.add(functionAnd);
    }

    @Override
    public void exitFunctionOr(@NotNull ModRevParser.FunctionOrContext ctx) {
        String first = removeQuotationMarks(ctx.ID().getText());
        String range = ctx.range().getText();

        FunctionOR function = new FunctionOR(first, range);
        functionORs.add(function);
    }

    @Override
    public void exitFixed(@NotNull ModRevParser.FixedContext ctx) {}
}
