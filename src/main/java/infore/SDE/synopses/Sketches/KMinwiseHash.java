package infore.SDE.synopses.Sketches;

import java.util.Random;
import java.util.TreeSet;

public class KMinwiseHash {
    private final int B;
    private int curSampleSize = 0;
    public TreeSet<Integer> sample;
    public int n;

    public KMinwiseHash(int B) {
        this.B = B;
    }



    long curTreeRoot = Long.MAX_VALUE;
    public void add(int hx) {
        n++;
        if (curSampleSize < B) {
            sample.add(hx);
            curSampleSize++;
        } else {
            if (hx < curTreeRoot) { // get tree root
                sample.pollFirst();
                sample.add(hx);
                curTreeRoot = sample.first();
            }
        }
    }

}
