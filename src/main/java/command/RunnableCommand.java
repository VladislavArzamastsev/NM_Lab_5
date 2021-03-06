package command;

import framework.command.AbstractRunnableCommand;
import framework.utils.ConsoleUtils;
import framework.utils.ValidationUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;

public class RunnableCommand extends AbstractRunnableCommand {

    private static final String NAME = "run";

    private static final int MAX_STEPS_AMOUNT = 1_000_000;

    public RunnableCommand() {
        super(NAME);
    }

    @Override
    public void execute(String[] strings) {
        assertStateSanity();
        int nParts = getNParts();
        ConsoleUtils.println(String.format("Steps: %d", nParts));
        double integral = calculateIntegral(nParts);

        SimpsonIntegrator simpson = new SimpsonIntegrator();
        Interval interval = (Interval) applicationState.getVariable("interval");
        UnivariateFunction function = (UnivariateFunction) applicationState.getVariable("function");
        double actualIntegral = simpson.integrate(Integer.MAX_VALUE, function, interval.getInf(), interval.getSup());

        ConsoleUtils.println(String.format("My integral = %f", integral));
        ConsoleUtils.println(String.format("My integral - actual integral = %f", integral - actualIntegral));
    }

    /**
     * Find 'n' from: |Precision| <= max[a, b]( |f'(x)| ) * ((b - a) ^ 2) / 2n
     * */
    private int getNParts() {
        double precision = (Double) applicationState.getVariable("precision");
        Interval interval = (Interval) applicationState.getVariable("interval");
        double factor = Math.pow(interval.getSup() - interval.getInf(), 2) / (2 * precision);

        MaxEval maxEval = new MaxEval(1000);
        double relativeTolerance = 0.001;
        double absoluteTolerance = 0.001;
        UnivariateFunction derivative = (UnivariateFunction) applicationState.getVariable("derivative");
        UnivariateFunction absOfFirstDerivative = (x) -> Math.abs(derivative.value(x));
        UnivariateObjectiveFunction objective = new UnivariateObjectiveFunction(absOfFirstDerivative);
        SearchInterval searchInterval = new SearchInterval(interval.getInf(), interval.getSup());

        BrentOptimizer optimizer = new BrentOptimizer(relativeTolerance, absoluteTolerance);
        double pointOfMaximum = optimizer.optimize(objective, GoalType.MAXIMIZE, searchInterval, maxEval).getPoint();

        int out = (int) Math.floor(factor * absOfFirstDerivative.value(pointOfMaximum));
        out = Math.max(out, 1);
        return Math.min(out, MAX_STEPS_AMOUNT);
    }

    /**
     * Method of right squares
     * */
    private double calculateIntegral(int nParts) {
        ValidationUtils.requireGreaterOrEqualThan(nParts, 1, "nParts must be >= 1");

        UnivariateFunction function = (UnivariateFunction) applicationState.getVariable("function");
        Interval interval = (Interval) applicationState.getVariable("interval");
        double dx = (interval.getSup() - interval.getInf()) / nParts;
        double result = 0;
        for (int i = 1; i <= nParts; i++) {
            result += function.value(interval.getInf() + i * dx);
        }
        return result * dx;
    }

    private void assertStateSanity() {
        ValidationUtils.requireNonNull(
                applicationState.getVariable("interval"),
                applicationState.getVariable("precision"),
                applicationState.getVariable("function"),
                applicationState.getVariable("derivative"));
    }

}
