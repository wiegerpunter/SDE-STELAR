package infore.SDE.synopses;



import com.fasterxml.jackson.databind.JsonNode;
import infore.SDE.messages.Estimation;
import infore.SDE.messages.Request;
import infore.SDE.synopses.Sketches.Kmin;
import infore.SDE.synopses.Sketches.OmniAttributeSketch;

import java.util.*;

import static java.lang.Math.ceil;

public class OmniSketch extends Synopsis {

	private OmniAttributeSketch[] sketch;
	private final Kmin kminTemp;
	public final int B;
	private final int depth;
	private boolean mapFilled = false;
	private HashMap<String, Integer> attrMapStrToInt = new HashMap<>();
	private HashMap<Integer, String> attrMapIntToStr = new HashMap<>();

	public OmniSketch(int uid, String[] parameters) {
		// Format: [keyIndex, valueIndex, operationMode, numAttrs, depth, width, B, b, seed]
		super(uid, parameters[0], parameters[1], parameters[2]);
		B = Integer.parseInt(parameters[6]);
		depth = (int) Math.ceil(Math.log(1 / Double.parseDouble(parameters[4])));
		kminTemp = new Kmin(B, Integer.parseInt(parameters[7]), Integer.parseInt(parameters[8]));
		sketch = new OmniAttributeSketch[Integer.parseInt(parameters[3])];
		for (int i = 0; i < sketch.length; i++) {
			sketch[i] = new OmniAttributeSketch(depth, Double.parseDouble(parameters[5]), B, Integer.parseInt(parameters[7]), Integer.parseInt(parameters[8]));
		}
	}

	@Override
	public void add(Object k) {
		//ObjectMapper mapper = new ObjectMapper();
		JsonNode node = (JsonNode) k;
        /*try {
            node = mapper.readTree(j);
        } catch (IOException e) {
            e.printStackTrace();
        } */
		String key = node.get(this.keyIndex).asText();
		long hx = kminTemp.hash(key);
		String value = node.get(this.valueIndex).asText();

		// loop over node..get("values") and add each value to the sketch

		if (!mapFilled) {
			int i = 0;
			Iterator<Map.Entry<String, JsonNode>> it = node.fields();
			while (it.hasNext() && i < sketch.length) {
				Map.Entry<String, JsonNode> entry = it.next();
				if (entry.getKey().equals(this.valueIndex) || entry.getKey().equals(this.keyIndex)) {
					continue;
				}
				attrMapStrToInt.put(entry.getKey(), i);
				attrMapIntToStr.put(i, entry.getKey());
				i++;
			}
			mapFilled = true;
		}
		// add each attribute to the sketch
		for (String attr: attrMapStrToInt.keySet()) {
			int attrIndex = attrMapStrToInt.get(attr);
			String attrValStr = node.get(attr).asText();
			sketch[attrIndex].add(Math.abs((attrValStr).hashCode()), (long) Double.parseDouble(value), hx);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public Object estimate(Object k)
	{
		throw new UnsupportedOperationException("OmniSketch does not support estimate(Object k)");
	}

	@Override
	public Synopsis merge(Synopsis sk) {
		return sk;	
	}

	@Override
	public Estimation estimate(Request rq) {

		if(rq.getRequestID() % 10 == 6){
			throw new UnsupportedOperationException("OmniSketch does not support estimate(Request rq) with requestID % 10 == 6");
//			String[] par = rq.getParam();
//			par[2]= ""+rq.getUID();
//			rq.setUID(Integer.parseInt(par[1]));
//			rq.setParam(par);
//			rq.setNoOfP(rq.getNoOfP()*Integer.parseInt(par[0]));
//			return new Estimation(rq, cm, par[1]);

		}
		System.out.println("Start query");
		Kmin[] samples = getSamples(rq);
		if (samples == null) {
			return new Estimation(rq, "0", Integer.toString(rq.getUID()) + "_" + Arrays.toString(rq.getParam()));
		}
		double S_cap = 0;
		int n_max = 0;

		TreeSet<Long>[] flatSamples = new TreeSet[samples.length];
		System.out.println("samples: " + Arrays.toString(samples));
		for (int i = 0; i < samples.length; i++) {
			TreeSet<Long> kmin = samples[i].getSampleToQuery();
			flatSamples[i] = kmin;
		}
		n_max = getNmax(samples);
		System.out.println("n_max: " + n_max);
		S_cap = getAltEstKMV(flatSamples);
		System.out.println("S_cap: " + S_cap);
//		queryInfo.setScap((int) S_cap, n_max);
//        double constraint = 3 * Math.log((4 * numPreds * Main.depth * Math.sqrt(Main.maxSize))
//                / Main.delta)/(Main.eps * Main.eps);

		return new Estimation(rq, Double.toString(ceil(S_cap * n_max / B)), Integer.toString(rq.getUID()) + "_" + Arrays.toString(rq.getParam()));
	}

	private int getNmax(Kmin[] samples) {
		int n_max = 0;
		for (Kmin kmin : samples) {
			if (kmin.n > n_max) {
				n_max = kmin.n;
			}
		}
		return n_max;
	}
	private double getAltEstKMV(TreeSet<Long>[] samples) {
		int numJoins = samples.length;
		int c = 0;
		Iterator<Long> iter = samples[0].iterator();
		while (iter != null && iter.hasNext()) {
			boolean found = true;
			Long i = iter.next();
			for (int j = 1; j < numJoins; j++) {
				Long otherElement = samples[j].ceiling(i);
				if (otherElement == null) {
					found = false;
					iter = null;
					break;
				} // not contained
				else if (otherElement.equals(i)) continue; // is contained
				else {
					iter = samples[0].tailSet(otherElement).iterator(); // fast forward iter0
					found = false;
					break; // but now you need to start from iter.hasNext() again
				}
			}
			if (found) c++;

		}
		return c;
	}

	private Kmin[] getSamples(Request rq) {
		System.out.println("Get samples");

		long[] q = new long[sketch.length];
		for (int i = 0; i < sketch.length; i++) {
			q[i] = -1;
		}
		String[] param = rq.getParam();
		System.out.println("param: " + Arrays.toString(param));
		int numPredicates=0;
		for (int i = 0; i < param.length; i+=2) {
			System.out.println("i: " + i);
			System.out.println("param[i]: " + param[i]);
			if (attrMapStrToInt.containsKey(param[i])) {
				q[attrMapStrToInt.get(param[i])] = Math.abs(param[i+1].hashCode());
				numPredicates++;
			}
		}
		if (numPredicates == 0) {
			return null;
		}
		Kmin[] samples = new Kmin[numPredicates * depth];
		int attrWithoutPred = 0;
		for (int i = 0; i < q.length; i++) {
			if (q[i] != -1) { // Find way to not take the -1s into account in query.
				Kmin[] temp = sketch[i].queryKmin(q[i]);
				if (depth >= 0) {
					//System.arraycopy(temp[j], 0, samples, (i - attrWithoutPred) * Main.depth + j, 1);
					System.arraycopy(temp, 0, samples, (i - attrWithoutPred) * depth, depth);
				}
			} else {
				attrWithoutPred++;
			}
		}
		return samples;
	}
	
	
	
	
}
