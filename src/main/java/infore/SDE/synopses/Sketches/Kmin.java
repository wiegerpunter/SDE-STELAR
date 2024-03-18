package infore.SDE.synopses.Sketches;

import java.io.Serializable;
import java.util.*;
import java.util.Collections;
import java.util.Random;
import java.util.TreeSet;

public class Kmin implements Serializable {
    public int n = 0;
    int curSampleSize = 0;
    int K;
    int b;
    int seed;
    long maxHash;
    public TreeSet<Long> sketch;
    long curTreeRoot = Long.MAX_VALUE;
    Random rn = new Random();

    public Kmin(int B, int b, int seed) {
        this.K = B;
        this.b = b;
        this.seed = seed;
        this.sketch = new TreeSet<>(Collections.reverseOrder());
        this.maxHash = (long) Math.min(Math.pow(2, b), Integer.MAX_VALUE);
    }

    public long hash(String key) {
        rn.setSeed(Math.abs((key).hashCode()) + seed);
        return rn.nextLong(); // TODO: why can't we use nextLong(maxHash) here?
    }

    public void add(long hx, long value) {
        if (value != 1) {
            throw new IllegalArgumentException("OmniSketch currently only supports 1 as value");
        }
        n++;
        if (curSampleSize < K - 1) {
            sketch.add(hx);
            curSampleSize++;
        } else if (curSampleSize == K) {
            curTreeRoot = sketch.first();
            if (hx < curTreeRoot) {
                sketch.pollFirst();
                sketch.add(hx);
                curTreeRoot = sketch.first();
            }
        } else {
            if (hx < curTreeRoot) {
                sketch.pollFirst();
                sketch.add(hx);
                curTreeRoot = sketch.first();
            }
        }
    }


    public TreeSet<Long> getSampleToQuery() {
        return sketch;
    }
}
