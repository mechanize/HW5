package cd.transform.analysis;

import cd.transform.analysis.Def;
import cd.ir.Ast;
import cd.ir.BasicBlock;
import cd.ir.ControlFlowGraph;
import cd.ir.Symbol;

import java.util.*;


/**
 * ReachingDefsAnalysis
 * */

public class ReachingDefsAnalysis extends DataFlowAnalysis<Set<Def>> {

	public ReachingDefsAnalysis(ControlFlowGraph cfg) {
		super(cfg);
		Set<Def> allDefs = new HashSet<>();

		// Iterating through basicBlocks twice.
		// 1.   Add all Assign stmts with a local variable on the left to allDefs. We need allDefs later
		//      to compute the killSet
		//      Add the generating stmts to genSet.
		//      Set genSet and killSet of each Basicblock. killSet is empty at this time.
		//      For each Stmt check if it is used in its local block before getting killed.

		for (BasicBlock block: cfg.allBlocks) {
			Set<Def> killSet = new HashSet<>();
			Set<Def> genSet = new HashSet<>();
			List<Def> localDefs = new ArrayList<>(); // needed for unused variables optimization
			ListIterator<Ast.Stmt> li = block.stmts.listIterator(block.stmts.size());

			while (li.hasPrevious()) {
				Ast.Stmt stmt = li.previous();

				if (stmt instanceof Ast.Assign) {
					if (((Ast.Assign) stmt).left() instanceof Ast.Var) {
						if (!(((Ast.Var) ((Ast.Assign) stmt).left()).sym.kind == Symbol.VariableSymbol.Kind.FIELD)) {
							Def newDef = new Def((Ast.Assign) stmt);

							// check if the stmt kills any other stmt in genSet
							boolean isKilled = false;
							for (Def def : genSet)
								isKilled = isKilled || def.target.equals(newDef.target);
							if (!isKilled)
								genSet.add(newDef);

							localDefs.add(newDef);
							allDefs.add(newDef);
						}
					}
				}
			}
			block.genSet = genSet;
			block.killSet = killSet;
			block.localDefs = localDefs;  // needed for unused variables optimization
		}

		// 2.   Now we go through the genSet in each block and add all Defs with the same target
		//      to killSet.
		for (BasicBlock block: cfg.allBlocks) {
			for(Def killDef: block.genSet) {
				for(Def def: allDefs) {
					if (!killDef.equals(def) && killDef.target.equals(def.target)) {
						block.killSet.add(def); // killDef kills def
					}
				}
			}
		}

		iterate();
	}



	@Override
	protected Set<Def> initialState() {
		return new HashSet<>();    }

	@Override
	protected Set<Def> startState() {
		return new HashSet<>();
	}

	@Override
	protected Set<Def> transferFunction(BasicBlock block, Set<Def> inState) {
		Set<Def> outState = new HashSet<>(inState);
		outState.removeAll(block.killSet);
		outState.addAll(block.genSet);
		return outState;
	}

	@Override
	protected Set<Def> join(Set<Set<Def>> states) {
		Set<Def> result = new HashSet<>();
		for (Set<Def> defs : states) {
			result.addAll(defs);
		}
		return result;
	}




}