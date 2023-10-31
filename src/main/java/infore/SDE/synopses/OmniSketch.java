package infore.SDE.synopses;



import com.fasterxml.jackson.databind.JsonNode;
import infore.SDE.messages.Estimation;
import infore.SDE.messages.Request;
import infore.SDE.synopses.Sketches.CM;
import infore.SDE.synopses.Sketches.KMinwiseHash;
import infore.SDE.synopses.Sketches.OmniAttrSketch;
import infore.SDE.synopses.Synopsis;

import java.util.Iterator;
import java.util.Random;

public class OmniSketch extends Synopsis {

    //private CM cm;
    private OmniAttrSketch[] AttrSketches;
    private double delta;
    private int p;
    private int d;
    private int B;
    private  int w;
    private int M;
    private int seed;

    private int maxHash;

    public OmniSketch(int uid, String[] parameters) {
        super(uid,parameters[0],parameters[1], parameters[2]);
        // parammeter[3] = p, parammeter[4] = epsilon, parammeter[5] = delta, parameter[6] = M, parammeter[7] = seed
        this.p = (int) Double.parseDouble(parameters[3]); // Number of attributes.

        this.seed = Integer.parseInt(parameters[7]);
        int[] pars = new int[4];
        delta = 1 - Double.parseDouble(parameters[5]);
        M = Integer.parseInt(parameters[6]);
        pars = computePars(Double.parseDouble(parameters[4]));
        this.d = pars[0];
        this.B = pars[2];

        AttrSketches = new OmniAttrSketch[p];
        for (int i = 0; i < p; i++) {
            AttrSketches[i] = new OmniAttrSketch(pars[0], pars[1], pars[2], seed);
        }
        maxHash = Math.min((int) Math.pow(2, pars[3]), Integer.MAX_VALUE);
    }

    private int[] computePars(double epsilon) {
        d = (int) Math.ceil(Math.log(2 / delta));
        double epsCMpowD = epsilon / (1 + epsilon);
        double epsCM = Math.pow(epsCMpowD, 1.0 / d);
        w = 1 + (int) Math.ceil(Math.E / epsCM);

        int B = searchB(0, 2, 0); // Make function that determines B from M, depth and width
        int b = (int) Math.ceil(Math.log(4*Math.pow(B, (double) 5/2)/delta));

        return new int[]{d, w, B, b};
    }

    int highB = Integer.MAX_VALUE;

    public int searchB(int prevB, int B, int iters) {
        if (B <= 1) {
            return B;
        }
        if (iters > 100){
            if (compRam(prevB) < M) {
                return prevB;
            } else {
                throw new RuntimeException("Too many iterations");
            }
        }
        iters++;
        double usedM = compRam(B);

        if (M*0.99 <= usedM && usedM <= M) {
            return B;
        } else if (usedM < M*0.99) {
            if (highB == Integer.MAX_VALUE)
                return searchB(B, B*2, iters);
            else
                return searchB(B, Math.min(highB, B*2), iters);
        } else {
            highB = B;
            return searchB(prevB, (int) Math.ceil((double) (B + prevB)/2), iters);
        }
    }

    public double compRam(int B) {
        int smallb = (int) Math.ceil(Math.log(4*Math.pow(B, (double) 5/2)/delta));
        return (double) d * w*(B * (smallb + 3 * 32 + 1) + 32) * p;
    }


    Random rn = new Random();


    int hashKmin(int x) {
        int k;
        rn.setSeed(x + seed);
        // Hash function hashing x to [0, 1]^b with base = log(2m^2/delta)

        k = rn.nextInt(maxHash);

        return k;
    }

    @Override
    public void add(Object k) {
        //ObjectMapper mapper = new ObjectMapper();
        JsonNode node = (JsonNode)k;
        /*try {
            node = mapper.readTree(j);
        } catch (IOException e) {
            e.printStackTrace();
        } */
        int hx = hashKmin(node.get(this.keyIndex).asText().hashCode());
        String[] values = getValues(node);
        for (int i = 0; i < p; i++) {
            AttrSketches[i].add(hx, values[i].hashCode());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object estimate(Object k) {
        JsonNode node = (JsonNode)k;
        String[] values = getValues(node);
        return estimate(values);
    }

    private String[] getValues(JsonNode node) {
        JsonNode value = node.get(this.valueIndex);
        String[] values = new String[p];
        Iterator<String> fieldNames = value.fieldNames();
        int i = 0;
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            values[i] = value.get(fieldName).asText();
            i++;
        }
        return values;
    }

    private Object estimate(String[] values) {
        double S_cap = 0;
        int n_max = 0;
        KMinwiseHash[] C = new KMinwiseHash[p * d];
        for (int i = 0; i < p; i++) {
            KMinwiseHash[] samplePerAttribute = AttrSketches[i].query(values[i].hashCode());
            System.arraycopy(samplePerAttribute, 0, C, i * d, d);
        }
        n_max = getNmax(C);
        S_cap = getScap(C);
        return S_cap * n_max / B;
    }

    private int getNmax(KMinwiseHash[] samples) {
        int n_max = 0;
        for (KMinwiseHash sample : samples) {
            if (sample.n > n_max) {
                n_max = sample.n;
            }
        }
        return n_max;
    }

    private double getScap(KMinwiseHash[] samples) {
        int numJoins = samples.length;
        int c = 0;
        Iterator<Integer> iter = samples[0].sample.iterator();
        while (iter != null && iter.hasNext()) {
            boolean found = true;
            Integer i = iter.next();
            for (int j = 1; j < numJoins; j++) {
                Integer otherElement = samples[j].sample.ceiling(i);
                if (otherElement == null) {
                    found = false;
                    iter = null;
                    break;
                } // not contained
                else if (otherElement.equals(i)) continue; // is contained
                else {
                    iter = samples[0].sample.tailSet(otherElement).iterator(); // fast forward iter0
                    found = false;
                    break; // but now you need to start from iter.hasNext() again
                }
            }
            if (found) c++;

        }
        return c;
    }


    @Override
    public Synopsis merge(Synopsis sk) {
        return sk;
    }


    // {
    //  "streamID" : "INTEL",
    //  "synopsisID" : 4,
    //  "requestID" : 1,
    //  "dataSetkey" : "Forex",
    //  "param" : [ "StockID", "price","Queryable", “1“, “720", “3600", "8" ],
    //  "noOfP" : 4,
    //  "uid" : 1110
    //}

    @Override
    public Estimation estimate(Request rq) {

        String[] values = rq.getParam()[0].split(",");// TODO: make sure to get right param, don't understand order now.

        if(rq.getRequestID() % 10 == 6){

            String[] par = rq.getParam();
            par[2]= ""+rq.getUID();
            rq.setUID(Integer.parseInt(par[1]));
            rq.setParam(par);
            rq.setNoOfP(rq.getNoOfP()*Integer.parseInt(par[0]));
            return new Estimation(rq, estimate(values), par[1]);

        }
        return new Estimation(rq, estimate(values), Integer.toString(rq.getUID()));
    }




}
