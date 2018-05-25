package cd.transform.analysis;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import cd.ToDoException;
import cd.util.Pair;
import cd.util.Tuple;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Stmt;
import cd.ir.BasicBlock;
import cd.ir.Symbol.VariableSymbol;

import cd.transform.analysis.NonNullVisitor.VarState;

/**
 * A data-flow analysis that determines if a variable is guaranteed to be non-<code>null</code> at a
 * given point in the program. The state of this analysis represents the set of
 * non-<code>null</code> variables.
 */
public class NonNullAnalysis extends DataFlowAnalysis<Set<VariableSymbol>> {

    final NonNullVisitor nnv = new NonNullVisitor();
    final Map<BasicBlock, Set<VariableSymbol>> gen = new HashMap<>();
    final Map<BasicBlock, Set<VariableSymbol>> kill = new HashMap<>();
    final Map<BasicBlock, Set<Pair<VariableSymbol> >> prop = new HashMap<>();
    final Map<Stmt, Set<VariableSymbol>> nonNullBefore = new HashMap<>();

    public NonNullAnalysis(MethodDecl method) {
        super(method.cfg);
        if(method.cfg == null)
            throw new IllegalArgumentException("method is missing CFG");

        // gen(B) := {var | last assignment to var in B is guaranteed not null}
        // kill(B) := {var | last statement in B using var is null-possible
        //             assign or method call}
        // prop(B) := {(var1, var2) | var1 is last assigned to var2 in B}

        for(BasicBlock block : method.cfg.allBlocks) {
            Map<VariableSymbol, Tuple<VarState, VariableSymbol>> varStates =
                computeVarStates(block);

            Set<VariableSymbol> genSet = new HashSet<>();
            Set<VariableSymbol> killSet = new HashSet<>();
            Set<Pair<VariableSymbol>> propSet = new HashSet<>();

            for (Map.Entry<VariableSymbol, Tuple<VarState, VariableSymbol>> me : varStates.entrySet()) {
                if (me.getValue().a == VarState.TOP) {
                    genSet.add(me.getKey());
                } else if (me.getValue().a == VarState.BOTTOM) {
                    killSet.add(me.getKey());
                } else {
                    Pair<VariableSymbol> propEntry = new Pair<>(me.getKey(), me.getValue().b);
                    propSet.add(propEntry);
                }
            }
            gen.put(block, genSet);
            kill.put(block, killSet);
            prop.put(block, propSet);
        }

        iterate();
    }

    Map<VariableSymbol, Tuple<VarState, VariableSymbol>> computeVarStates(BasicBlock block) {
        Map<VariableSymbol, Tuple<VarState, VariableSymbol>> ret = new HashMap<>();
        for (Stmt stmt : block.stmts) {
            nnv.visit(stmt, ret);
        }
        if (block.condition != null)
            nnv.visit(block.condition, ret);
        return ret;
    }


    @Override
    protected Set<VariableSymbol> initialState() {
        Set<VariableSymbol> ret = new HashSet<>();
        ret.addAll(gen.get(cfg.start));
        return ret;
    }

    @Override
    protected Set<VariableSymbol> startState() {
        return new HashSet<>();
    }

    /*@Override
    protected boolean equalStates(Map<BasicBlock, Set<VariableSymbol>> first,
                                  Map<BasicBlock, Set<VariableSymbol>> second) {
        for (BasicBlock block : first.keySet()) {
            if (second.get(block) == null ||
                !(first.get(block).equals(second.get(block)))) {
                return false;
            }
        }
        return true;
    }*/

    @Override
    protected Set<VariableSymbol> transferFunction(BasicBlock block, Set<VariableSymbol> inState) {
        // take difference of gen and kill
        // for each variable in that set
        Set<VariableSymbol> outState = new HashSet<>();
        outState.addAll(inState);
        outState.addAll(gen.get(block));
        if(kill.get(block) != null) {
            outState.removeAll(kill.get(block));
        }
        if (prop.get(block) != null) {
            for (Pair<VariableSymbol> varPair : prop.get(block)) {
                if (outState.contains(varPair.b)) {
                    outState.add(varPair.a);
                }
            }
        }
        return outState;

        //Set<VariableSymbol> ret = new HashSet<>();
        //ret.addAll(inState);
        //Map<VariableSymbol, Tuple<VarState, VariableSymbol>> varStates =
        //    new HashMap<>();
        //for (Stmt stm : block.stmts) {
        //    nnv.visit(stm, varStates);
        //}

        //for(Map.Entry<VariableSymbol, Tuple<VarState, VariableSymbol>> me :
        //        varStates.entrySet()) {
        //    if(me.getValue().a == VarState.TOP) {
        //        ret.add(me.getKey());
        //    } else if (me.getValue().a == VarState.PROP) {
        //        Tuple<VarState, VariableSymbol> curr = me.getValue();
        //        Tuple<VarState, VariableSymbol> next = null;
        //        while (curr.a == VarState.PROP) {
        //            next = varStates.get(curr.b);
        //            if (next != null) {
        //                curr = next;
        //            } else {
        //                break;
        //            }
        //        }
        //        if (curr.a == VarState.TOP) {
        //            ret.add(me.getKey());
        //        }
        //    } else {
        //        ret.remove(me.getKey());
        //    }
        //}
        //return ret;
    }

    @Override
    protected Set<VariableSymbol> join(Set<Set<VariableSymbol>> states) {
        Set<VariableSymbol> joinedStates = new HashSet<>();
        Set<VariableSymbol> toRemove = new HashSet<>();
        for (Set<VariableSymbol> s: states) {
            joinedStates.addAll(s);
        }
        for (Set<VariableSymbol> s: states) {
            for (VariableSymbol sym: joinedStates) {
                if (!(s.contains(sym))) {
                    toRemove.add(sym);
                }
            }
        }
        joinedStates.removeAll(toRemove);
        return joinedStates;
    }

    /**
     * Returns the set of variables that are guaranteed to be non-<code>null</code> before
     * the given statement.
     */
    public Set<VariableSymbol> nonNullBefore(BasicBlock block, Stmt stmt) {
        Set<VariableSymbol> ret = new HashSet<>();
        ret.addAll(inStateOf(block));
        Map<VariableSymbol, Tuple<VarState, VariableSymbol>> varStates =
            new HashMap<>();

        for (Stmt stm : block.stmts) {
            if (stm.equals(stmt))
                break;
            nnv.visit(stm, varStates);
        }

        for(Map.Entry<VariableSymbol, Tuple<VarState, VariableSymbol>> me :
                varStates.entrySet()) {
            if(me.getValue().a == VarState.TOP) {
                ret.add(me.getKey());
            } else if (me.getValue().a == VarState.PROP) {
                Tuple<VarState, VariableSymbol> curr = me.getValue();
                while (curr != null && curr.a == VarState.PROP) {
                    curr = varStates.get(curr.b);
                }
                if (curr != null && curr.a == VarState.TOP) {
                    ret.add(me.getKey());
                }
            } else {
                ret.remove(me.getKey());
            }
        }

        return ret;
    }

    public Set<VariableSymbol> nonNullBefore(Stmt stmt) {
        for (BasicBlock block : cfg.allBlocks) {
            if (block.stmts.contains(stmt)) {
                return nonNullBefore(block, stmt);
            }
        }
        return new HashSet<>();
    }

    /**
     * Returns the set of variables that are guaranteed to be non-<code>null</code> before
     * the condition of the given basic block.
     */
    public Set<VariableSymbol> nonNullBeforeCondition(BasicBlock block) {
        Set<VariableSymbol> ret = new HashSet<>();
        ret.addAll(inStateOf(block));
        Map<VariableSymbol, Tuple<VarState, VariableSymbol>> varStates =
            new HashMap<>();
        for (Stmt stm : block.stmts) {
            nnv.visit(stm, varStates);
        }
        for(Map.Entry<VariableSymbol, Tuple<VarState, VariableSymbol>> me :
                varStates.entrySet()) {
            if(me.getValue().a == VarState.TOP) {
                ret.add(me.getKey());
            } else if (me.getValue().a == VarState.PROP) {
                Tuple<VarState, VariableSymbol> curr = me.getValue();
                while (curr.a == VarState.PROP) {
                    curr = varStates.get(curr.b);
                }
                if (curr.a == VarState.TOP) {
                    ret.add(me.getKey());
                }
            } else {
                ret.remove(me.getKey());
            }
        }
        return ret;
    }
}
