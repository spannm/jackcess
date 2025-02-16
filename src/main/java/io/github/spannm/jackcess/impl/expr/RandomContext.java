package io.github.spannm.jackcess.impl.expr;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class effectively encapsulates the stateful logic of the "Rnd" function.
 */
public class RandomContext {
    private Source               defRnd;
    private Map<Integer, Source> rnds;
    // default to the value access uses for "last val" when none has been returned yet
    private float                lastVal = 1.953125E-02f;

    public RandomContext() {
    }

    public float getRandom(Integer _seed) {

        if (_seed == null) {
            if (defRnd == null) {
                defRnd = new SimpleSource(createRandom(System.currentTimeMillis()));
            }
            return defRnd.get();
        }

        if (rnds == null) {
            // note, we don't use a SimpleCache here because if we discard a Random
            // instance, that will cause the values to be reset
            rnds = new HashMap<>();
        }

        Source rnd = rnds.get(_seed);
        if (rnd == null) {

            int seedInt = _seed;
            if (seedInt > 0) {
                // normal random with a user specified seed
                rnd = new SimpleSource(createRandom(seedInt));
            } else if (seedInt < 0) {
                // returns the same value every time and resets all randoms
                rnd = new ResetSource(createRandom(seedInt));
            } else {
                // returns the last random value returned
                rnd = new LastValSource();
            }

            rnds.put(_seed, rnd);
        }
        return rnd.get();
    }

    private float withLast(float _lastVal) {
        lastVal = _lastVal;
        return _lastVal;
    }

    private void reset() {
        if (rnds != null) {
            rnds.clear();
        }
    }

    private static Random createRandom(long seed) {
        // TODO, support SecureRandom?
        return new Random(seed);
    }

    private abstract class Source {
        public float get() {
            return withLast(getImpl());
        }

        protected abstract float getImpl();
    }

    private class SimpleSource extends Source {
        private final Random mrnd;

        private SimpleSource(Random _rnd) {
            mrnd = _rnd;
        }

        @Override
        protected float getImpl() {
            return mrnd.nextFloat();
        }
    }

    private class ResetSource extends Source {
        private final float mval;

        private ResetSource(Random _rnd) {
            mval = _rnd.nextFloat();
        }

        @Override
        protected float getImpl() {
            reset();
            return mval;
        }
    }

    private class LastValSource extends Source {
        private LastValSource() {
        }

        @Override
        protected float getImpl() {
            return lastVal;
        }
    }
}
