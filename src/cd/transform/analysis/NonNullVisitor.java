package cd.transform.analysis;

import java.util.Set;
import java.util.Map;

import cd.util.Tuple;
import cd.ir.AstVisitor;
import cd.ir.Symbol;
import cd.ir.Symbol.VariableSymbol;
import cd.ir.Ast.*;
import cd.transform.analysis.NonNullVisitor.VarState;

public class NonNullVisitor extends AstVisitor<VarState, Map<VariableSymbol, Tuple<VarState, VariableSymbol>>> {

    public enum VarState {
        TOP, BOTTOM, PROP
    };

    @Override
    public VarState assign(Assign ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        visit(ast.left(), arg);
        VarState state = visit(ast.right(), arg);
        if (!(ast.left() instanceof Var))
            return null;
        else {
            VariableSymbol sym = ((Var) ast.left()).sym;
            if (state != VarState.PROP) {
                arg.put(sym, new Tuple<>(state, null));
            } else if (ast.right() instanceof Cast) {
                VariableSymbol rsym = ((Var) ((Cast) ast.right()).arg()).sym;
                arg.put(sym, new Tuple<>(state, rsym));
            } else {
                VariableSymbol rsym = ((Var) ast.right()).sym;
                arg.put(sym, new Tuple<>(state, rsym));
            }
        }

        return null;
    }

    @Override
    public VarState binaryOp(BinaryOp ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        visitChildren(ast, arg);
        return VarState.BOTTOM;
    }

    @Override
    public VarState unaryOp(UnaryOp ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        visitChildren(ast, arg);
        return VarState.BOTTOM;
    }

    @Override
    public VarState methodCall(MethodCallExpr ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        visitChildren(ast, arg);
        if (ast.receiver() instanceof Var) {
            VariableSymbol rsym = ((Var) ast.receiver()).sym;
            arg.put(rsym, new Tuple<>(VarState.TOP, null));
        }
        return VarState.BOTTOM;
    }

    @Override
    public VarState methodCall(MethodCall ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return visit(ast.getMethodCallExpr(), arg);
    }

    @Override
    public VarState var(Var ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        Tuple<VarState, VariableSymbol> ret = arg.get(ast.sym);
        if (ret == null)
            return VarState.PROP;
        else
            return ret.a;
    }

    @Override
    public VarState field(Field ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        Expr rec = ast.arg();
        if (rec instanceof Var) {
            VariableSymbol rsym = ((Var) rec).sym;
            arg.put(rsym, new Tuple<>(VarState.TOP, null));
        }
        return VarState.BOTTOM;
    }


    @Override
    public VarState index(Index ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        Expr rec = ast.left();
        if (rec instanceof Var) {
            VariableSymbol rsym = ((Var) rec).sym;
            arg.put(rsym, new Tuple<>(VarState.TOP, null));
        }
        return VarState.BOTTOM;
    }

    @Override
    public VarState intConst(IntConst ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return VarState.BOTTOM;
    }

    @Override
    public VarState booleanConst(BooleanConst ast, Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return VarState.BOTTOM;
    }

    @Override
    public VarState newObject(NewObject ast,
                              Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return VarState.TOP;
    }

    @Override
    public VarState newArray(NewArray ast,
                             Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return VarState.TOP;
    }

    @Override
    public VarState nullConst(NullConst ast,
                              Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return VarState.BOTTOM;
    }

    @Override
    public VarState thisRef(ThisRef ast,
                            Map<VariableSymbol, Tuple<VarState, VariableSymbol>> arg) {
        return VarState.TOP;
    }
}
