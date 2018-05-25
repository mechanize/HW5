package cd.transform;

import cd.ir.Ast;
import cd.ir.AstVisitor;
import cd.ir.BasicBlock;
import cd.ir.ControlFlowGraph;

/**
 * The return value of the visitor only matters to check if there was a return stmt before.
 * If so the visitor should not generate additional BasicBlocks in this subtree.
 * The current BasicBlock gets stored in Context.
 */
public class AstCfgBuilderVisitor extends AstVisitor<BasicBlock, Context> {
    BasicBlock end;
    ControlFlowGraph cfg;

    public BasicBlock ifElse(Ast.IfElse ast, Context ctx) {
        /**
         *  @param currentBlock
         *  if (condition)
         *      @param currentBlock.trueSuccessor()
         *      .
         *      @param trueBlock
         *  else
         *      @param currentBlock.falseSuccessor()
         *      .
         *      @param falseBlock
         *  @param next
         *  Note that currentBlock.trueSuccessor and trueBlock may be the same BasicBlock.
         *  Same with currentBlock.falseSuccessor() and falseBlock.
         */
        BasicBlock currentBlock = ctx.currentBlock;
        cfg.terminateInCondition(currentBlock, ast.condition());
        ctx.currentBlock = currentBlock.trueSuccessor();
        BasicBlock trueBlock = visit(ast.then(), ctx);
        ctx.currentBlock = currentBlock.falseSuccessor();
        BasicBlock falseBlock = visit(ast.otherwise(), ctx);
        BasicBlock next;

        if (trueBlock != null && falseBlock != null) {
            next = cfg.join(trueBlock, falseBlock);
        } else if (trueBlock != null) {
            next = cfg.join(trueBlock);
        } else if (falseBlock != null) {
            next = cfg.join(falseBlock);
        } else {
            return null;
        }
        ctx.currentBlock = next;
        return next;
    }

    public BasicBlock whileLoop(Ast.WhileLoop ast, Context ctx) {
        /**
         * @param ctx.currentBlock
         * @param whileBlock (condition)
         *      @param whileBlock.trueSuccessor()
         *      .
         *      .
         *      @param whileBody
         *  @param whileBlock.falseSuccessor()
         *  Note that whileBlock.trueSuccessor() and whileBody may be the same BasicBlock
         */

        BasicBlock whileBlock = cfg.newBlock();
        cfg.connect(ctx.currentBlock, whileBlock);
        cfg.terminateInCondition(whileBlock, ast.condition());
        ctx.currentBlock = whileBlock.trueSuccessor();
        BasicBlock whileBody = visit(ast.body(), ctx);
        if(whileBody != null) {
            cfg.connect(whileBody, whileBlock);
        }
        ctx.currentBlock = whileBlock.falseSuccessor();
        return whileBlock.falseSuccessor();
    }
    
    public BasicBlock returnStmt(Ast.ReturnStmt ast, Context ctx) {
        ctx.currentBlock.stmts.add(ast);
        cfg.connect(ctx.currentBlock, end);
        return null;
    }

    public BasicBlock dfltStmt(Ast.Stmt ast, Context ctx) {
        ctx.currentBlock.stmts.add(ast);
        return dflt(ast, ctx);
    }

    public BasicBlock visitChildren(Ast ast, Context ctx) {
        BasicBlock lastValue = ctx.currentBlock;
        for (Ast child : ast.children()) {
            lastValue = visit(child, ctx);
            if (lastValue == null) {
                return lastValue;
            }
        }
        return lastValue;
    }
}
