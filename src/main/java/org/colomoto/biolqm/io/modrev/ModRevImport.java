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
import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // collect all variables
        List<NodeInfo> variables = new ArrayList<>();
        Map<String, NodeInfo> id2var = new HashMap<String, NodeInfo>();

        for (TerminalNode node : mctx.getTokens(ModRevParser.ID)) {
            String variable = node.getText();
            if (id2var.containsKey(variable)) {
                continue;
            }

            NodeInfo ni = new NodeInfo(variable);

            id2var.put(variable, ni);
            variables.add(ni);
        }

        // create operand factory to assist parser
        OperandFactory operandFactory = new SimpleOperandFactory<>(variables);
        ModRevParserListener listener = new ModRevParserListener(operandFactory);

        // TODO: load actual functions
        // the following code might be incorrect
        Map<NodeInfo, FunctionNode> var2function = new HashMap<NodeInfo, FunctionNode>();

        // walk the parse tree with the listener
        ParseTreeWalker.DEFAULT.walk(listener, mctx);

        // Extract boolean functions from the listener and load into var2function
        for (NodeInfo ni : variables) {
            FunctionNode fn = listener.stack.done(); // Extracting the function from the listener's stack
            var2function.put(ni, fn);
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

}


class ModRevParserListener extends ModRevBaseListener {

    private final ParseTreeWalker walker = new ParseTreeWalker();
    final ExpressionStack stack;
    private final List<String> variables = new ArrayList<>();
    public ModRevParserListener(OperandFactory operandFactory) {
        this.stack = new ExpressionStack(operandFactory);
    }

    public List<String> getVariables() {
        return variables; // Return the collected variables
    }

    @Override
    public void exitVertex(@NotNull ModRevParser.VertexContext ctx) {
        String vertexID = ctx.ID().getText();
        variables.add(vertexID);
        NodeInfo ni = new NodeInfo(vertexID);
    }

    public void exitEdge(@NotNull ModRevParser.EdgeContext ctx) {
        String source = ctx.ID(0).getText();
        String target = ctx.ID(1).getText();
        String intval = ctx.INT().getText();

    }

    @Override
    public void exitFunctionAnd(@NotNull ModRevParser.FunctionAndContext ctx) {
        String first = ctx.ID(0).getText();
        String second = ctx.ID(1).getText();
        String value = ctx.INT().getText();

        stack.operator(Operator.AND);
    }

    @Override
    public void exitFunctionOr(@NotNull ModRevParser.FunctionOrContext ctx) {
        String first = ctx.ID().getText();
        String range = ctx.range().getText();

        stack.operator(Operator.OR);
    }
}
