package infore.SDE.synopses.Sketches;

import java.util.Random;

public class OmniAttrSketch {
    KMinwiseHash[][] sketch;
    final Random rn = new Random();
    int seed;
    int d;
    int w;

    public OmniAttrSketch(int w, int d, int B, int seed) {
        this.seed = seed;
        this.d = d;
        this.w = w;
        sketch = new KMinwiseHash[w][d];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < d; j++) {
                sketch[i][j] = new KMinwiseHash(B);
            }
        }
    }

    int[] hash(long attrValue, int depth, int width) {
        int[] hash = new int[depth];
        rn.setSeed(attrValue + seed);
        for (int i = 0; i < depth; i++) hash[i] = rn.nextInt(width);
        return hash;
    }

    public void add(int hx, int attrValue) {
        // Test if all element in A and B are consistent
        int[] hashes = hash(attrValue, d, w);
        for (int j = 0; j < d; j++) {
            int i = hashes[j];
            sketch[j][i].add(hx); // Hash in Sample based on id
        }
    }

    public KMinwiseHash[] query(int attrValue) {
        int[] hashes = hash(attrValue, d, w);
        KMinwiseHash[] result;
        result = new KMinwiseHash[d];
        for (int j = 0; j < d; j++) {
            int i = hashes[j];
            result[j] = sketch[j][i];
        }
        return result;

    }

}
