package cd.transform;

import cd.ToDoException;
import cd.ir.Ast;
import cd.ir.Ast.MethodDecl;
import cd.ir.BasicBlock;
import cd.ir.ControlFlowGraph;

public class CfgBuilder {
	
	ControlFlowGraph cfg;
	
	public void build(MethodDecl mdecl) {
		cfg = mdecl.cfg = new ControlFlowGraph();
		cfg.start = cfg.newBlock(); // Note: Use newBlock() to create new basic blocks
		cfg.end = cfg.newBlock(); // unique exit block to which all blocks that end with a return stmt. lead

		Context ctx = new Context();
		ctx.currentBlock = cfg.start;

		AstCfgBuilderVisitor visitor = new AstCfgBuilderVisitor();
		visitor.cfg = cfg;
		visitor.end = cfg.end;
		BasicBlock lastBlock = visitor.visit(mdecl, ctx);
		if (lastBlock != null)
			cfg.connect(lastBlock, cfg.end);
		// CFG and AST are not synchronized, only use CFG from now on
		mdecl.setBody(null);
		
	}

}
