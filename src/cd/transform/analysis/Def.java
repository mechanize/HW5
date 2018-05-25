package cd.transform.analysis;

import cd.ir.Ast;
import cd.util.debug.AstOneLine;

public class Def {
    public Ast.Assign stmt;
    public String target;
    public boolean used;

    public Def(Ast.Assign stmt) {
        if (!(stmt.left() instanceof Ast.Var)) {
            throw new IllegalArgumentException(
                    "UnusedStatementsAnalysis: left hand side of Assign needs to be a Var");
        }
        this.stmt = stmt;
        this.target = ((Ast.Var) stmt.left()).name;
        this.used = false;
    }
    public void setUsed(boolean used) {
        this.used = used;
    }

    @Override
    public String toString() {
        return AstOneLine.toString(stmt);
    }
}
