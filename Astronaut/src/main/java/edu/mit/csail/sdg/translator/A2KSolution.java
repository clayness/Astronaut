package edu.mit.csail.sdg.translator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import kodkod.ast.Formula;
import kodkod.ast.NaryFormula;
import kodkod.ast.RelationPredicate;
import kodkod.ast.visitor.AbstractReplacer;
import kodkod.engine.config.AbstractReporter;
import kodkod.engine.config.Options;
import kodkod.engine.fol2sat.FormulaFlattener;
import kodkod.engine.fol2sat.SymmetryBreaker;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.util.nodes.AnnotatedNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class A2KSolution {

    // Annotated (and optimized) copy of the Alloy formula
    private final AnnotatedNode<Formula> annotated;
    // Bounds for the Alloy solution
    private final Bounds bounds;
    // (Immutable) set of all formulae in the Alloy solution
    private final Set<Formula> formulae;
    // Alloy solution object containing the formulae, etc.
    private final A4Solution frame;
    // Options for solving
    private final Options options;

    public A2KSolution(A4Solution solution) throws Exception {
        this.frame = solution;
        this.bounds = getBounds(this.frame);
        this.formulae = getFormulae(this.frame);
        this.annotated = optimize(this.bounds, AnnotatedNode.annotate(Formula.and(this.formulae)));
        this.options = convert(defaultOptions());
    }

    public A2KSolution(Module world, Command cmd) throws Exception {
        this(world, cmd, defaultOptions());
    }

    public A2KSolution(Module world, Command cmd, A4Options opt) throws Exception {
        this.frame = getFrame(world, cmd, new A4Options());
        this.bounds = getBounds(this.frame);
        this.formulae = getFormulae(this.frame);
        this.annotated = optimize(this.bounds, AnnotatedNode.annotate(Formula.and(this.formulae)));
        this.options = convert(opt);
    }

    public A2KSolution(Module world, int cmdidx) throws Exception {
        this(world, world.getAllCommands().get(cmdidx));
    }

    public A2KSolution(Path file, int cmdidx) throws Exception {
        this(CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, file.toString()), cmdidx);
    }

    private static A4Options defaultOptions() {
        var retval = new A4Options();
        retval.noOverflow = true;
        retval.solver = A4Options.SatSolver.SAT4J;
        return retval;
    }

    // Annotates and optimizes the bounds and formulae from the Alloy
    // problem by dropping unused relations and adding symmetry breaking
    private static AnnotatedNode<Formula> optimize(Bounds bounds, AnnotatedNode<Formula> annotd) {
        // trim to only the relations used in the formula
        bounds.relations().retainAll(annotd.relations());
        if (!annotd.usesInts())
            bounds.ints().clear();
        // add in the symmetry breaking protocols and inline, too
        var breaker = new SymmetryBreaker(bounds, annotd, new AbstractReporter() {
        });
        var inliner = new AbstractReplacer(annotd.sharedNodes()) {
            @Override
            public Formula visit(RelationPredicate pred) {
                Formula ret = lookup(pred);
                if (ret != null)
                    return ret;
                return breaker.breakMatrixSymmetries(annotd.predicates(), true).keySet().contains(pred)
                        ? cache(pred, Formula.TRUE)
                        : cache(pred, pred.toConstraints());
            }
        };
        return AnnotatedNode.annotate(annotd.node().accept(inliner));
    }

    /**
     * Returns the annotated and optimized formula
     *
     * @return
     */
    public AnnotatedNode<Formula> annotated() {
        return this.annotated;
    }

    /**
     * Returns the bitwidth for the Alloy problem
     *
     * @return
     */
    public int bitwidth() {
        return this.frame.getBitwidth();
    }

    /**
     * Returns the optimized bounds
     *
     * @return
     */
    public Bounds bounds() {
        return this.bounds;
    }

    public Options convert(A4Options opt) {
        var retval = new Options();
        if (this.bitwidth() > 0)
            retval.setBitwidth(this.bitwidth());
        retval.setCoreGranularity(opt.coreGranularity);
        retval.setNoOverflow(opt.noOverflow);
        retval.setSkolemDepth(opt.skolemDepth);
        retval.setSymmetryBreaking(opt.symmetry);
        retval.setReporter(new AbstractReporter() {
        });
        if (opt.solver == A4Options.SatSolver.GlucoseJNI) {
            retval.setSolver(SATFactory.Glucose);
        } else if (opt.solver == A4Options.SatSolver.MiniSatJNI) {
            retval.setSolver(SATFactory.MiniSat);
        } else {
            retval.setSolver(SATFactory.DefaultSAT4J);
        }
        return retval;
    }

    /**
     * Returns the individual clauses of the formula as an immutable set
     *
     * @return
     */
    public Set<Formula> formulae() {
        return this.formulae;
    }

    /**
     * Retrieves the universe of the passed solution by calling
     * "A4Solution.getBounds" via reflection and getting the universe of the
     * returned value.
     *
     * @param frame
     * @return
     * @throws Exception
     */
    private Bounds getBounds(A4Solution frame) throws Exception {
        return frame.getBounds();
    }

    /**
     * Retrieves the (private) "A4Solution.formulas" field via reflection. The
     * returned set is immutable.
     *
     * @param frame
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Set<Formula> getFormulae(A4Solution frame) throws Exception {
        Field formulasField = frame.getClass().getDeclaredField("formulas");
        formulasField.setAccessible(true);

        List<Formula> original = (List<Formula>) formulasField.get(frame);

        AnnotatedNode<Formula> annotated = AnnotatedNode.annotate(Formula.and(original));
        AnnotatedNode<Formula> flattened = FormulaFlattener.flatten(annotated, true);
        assert flattened.node() instanceof NaryFormula;

        return Streams.stream((NaryFormula) flattened.node()).collect(ImmutableSet.toImmutableSet());
    }

    /**
     * Creates a new {@link TranslateAlloyToKodkod} object, generates the formula,
     * and returns the "frame" solution used to store the top level formula, all via
     * reflection (big "warning" on that part)
     *
     * @param world
     * @param cmd
     * @param opt
     * @return
     * @throws Exception
     */
    private A4Solution getFrame(Module world, Command cmd, A4Options opt) throws Exception {
        Constructor<TranslateAlloyToKodkod> cons = TranslateAlloyToKodkod.class.getDeclaredConstructor(A4Reporter.class,
                A4Options.class, Iterable.class, Command.class);
        cons.setAccessible(true);
        TranslateAlloyToKodkod tatk = cons.newInstance(A4Reporter.NOP, opt, world.getAllReachableSigs(), cmd);

        Method makeFactsMethod = tatk.getClass().getDeclaredMethod("makeFacts", Expr.class);
        makeFactsMethod.setAccessible(true);
        makeFactsMethod.invoke(tatk, cmd.formula);

        Field frameField = tatk.getClass().getDeclaredField("frame");
        frameField.setAccessible(true);
        return (A4Solution) frameField.get(tatk);
    }

    /**
     * Returns the solver options
     *
     * @return
     */
    public Options options() {
        return this.options;
    }
}
