package infore.SDE.synopses.Sketches;

import java.io.Serializable;
import java.util.Random;

public class OmniAttributeSketch implements Serializable {

    int depth;
    int width;
    int seed;
    Kmin[][] sketch;
    Random rn = new Random();
    public OmniAttributeSketch(int depth, double epsilon, int B, int b, int seed) {
        this.depth = depth;// (int) Math.ceil(Math.log(1 / delta));
        width = (int) Math.ceil(Math.exp(1) / epsilon);
        this.seed = seed;
        sketch = new Kmin[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                sketch[i][j] = new Kmin(B, b, seed);
            }
        }
    }

    public void add(long attrValue, long value, long hx) {
        int[] hashes = hash(attrValue, depth, width);
        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            sketch[j][w].add(hx, value);
        }
    }

    int[] hash(long attrValue, int depth, int width) { // TODO: Use the hash function from Sketches.CM as this is better.
        int[] hashes = new int[depth];
        rn.setSeed(attrValue + seed);
        for (int i = 0; i < depth; i++) hashes[i] = rn.nextInt(width);
        return hashes;
    }

    public Object estimateCount(Long long1) {
        return null;
    }

    public Kmin[] queryKmin(long l) {
        System.out.println("Get Kmin samples for " + l);
        int[] hashes = hash(l, depth, width);
        Kmin[] result;
        result = new Kmin[depth];

        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            result[j] = sketch[j][w];
        }
        return result;
    }
}
