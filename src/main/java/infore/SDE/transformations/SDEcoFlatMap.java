package infore.SDE.transformations;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import lib.WDFT.controlBucket;
import lib.WLSH.Bucket;
import infore.SDE.synopses.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction;
import org.apache.flink.util.Collector;
import infore.SDE.messages.Estimation;
import infore.SDE.messages.Request;
import infore.SDE.messages.Datapoint;

public class SDEcoFlatMap extends RichCoFlatMapFunction<Datapoint, Request, Estimation> {

	private static final long serialVersionUID = 1L;
	private HashMap<String,ArrayList<Synopsis>> M_Synopses = new HashMap<>();
	private HashMap<String,ArrayList<ContinuousSynopsis>> MC_Synopses = new HashMap<>();
	private int pId;

	@Override
	public void flatMap1(Datapoint node, Collector<Estimation> collector) {
		//System.out.println(node.toString());
		//System.out.println(MC_Synopses.size());
		ArrayList<Synopsis>  Synopses =  M_Synopses.get(node.getKey());
		if (Synopses != null) {
			for (Synopsis ski : Synopses) {
				ski.add(node.getValues());
			}
		M_Synopses.put(node.getKey(),Synopses);
		}
		ArrayList<ContinuousSynopsis>  C_Synopses =  MC_Synopses.get(node.getKey());
		if (C_Synopses != null) {

			for (ContinuousSynopsis c_ski : C_Synopses) {

				Estimation e =c_ski.addEstimate(node);
				if(e!=null){
					if(e.getEstimation()!=null)
						collector.collect(e);
				}
				//Radius_Grid rg = (Radius_Grid)c_ski;
				//rg.add_and_provide_estimates(node);
			}
		MC_Synopses.put(node.getKey(),C_Synopses);
		}
	}

	@Override
	public void flatMap2(Request rq, Collector<Estimation> collector) throws Exception {

		System.out.println(rq.toString());
		ArrayList<Synopsis>  Synopses =  M_Synopses.get(rq.getKey());
		ArrayList<ContinuousSynopsis>  C_Synopses =  MC_Synopses.get(rq.getKey());

		if (rq.getRequestID() == 1 || rq.getRequestID() == 4 ) {
			if(Synopses==null){
				Synopses = new ArrayList<>();
				C_Synopses = new ArrayList<>();
			}

		Synopsis sketch = null;
		switch (rq.getSynopsisID()) {
			// countMin
			case 1:
				if (rq.getParam().length > 4)
					sketch = new CountMin(rq.getUID(), rq.getParam());
				//{ "1", "2", "0.0002", "0.99", "4" };
				Synopses.add(sketch);
			break;
			// BloomFliter
			case 2:
				if (rq.getParam().length > 3)
					sketch = new Bloomfilter(rq.getUID(), rq.getParam());
				//	String[] _tmp = { "1", "1", "100000", "0.0002" };
				Synopses.add(sketch);
			break;
			// AMS sketch
			case 3:
				if (rq.getParam().length > 3)
					sketch = new AMSsynopsis(rq.getUID(), rq.getParam());
				//	String[] _tmp = { "1", "2", "1000", "10" };
				Synopses.add(sketch);
			break;
			// DFT
			case 4:
				if (rq.getParam().length > 3)
					sketch = new MultySynopsisDFT(rq.getUID(), rq.getParam());
				//String[] _tmp = {"1", "2", "5", "30", "8"};
				Synopses.add(sketch);
			break;
			//LSH - unfinished
			case 5:
				sketch = new Bloomfilter(rq.getUID(), rq.getParam());
				Synopses.add(sketch);

			break;
			// lib.Coresets
			case 6:
				if (rq.getParam().length > 10)
					sketch = new FinJoinCoresets(rq.getUID(), rq.getParam());
				//	String[] _tmp = { "1","2", "5", "10" };
				Synopses.add(sketch);
			break;
			// HyperLogLog
			case 7:
				if (rq.getParam().length > 2)
					sketch = new HyperLogLogSynopsis(rq.getUID(), rq.getParam());
				//String[] _tmp = { "1", "1", "0.001" };
				Synopses.add(sketch);
			break;
			// StickySampling
			case 8:

				if (rq.getParam().length > 4)
					sketch = new StickySamplingSynopsis(rq.getUID(), rq.getParam());
				//String[] _tmp = { "1", "2", "0.01", "0.01", "0.0001"};
				Synopses.add(sketch);
			break;
			// LossyCounting
			case 9:

				if (rq.getParam().length > 2)
					sketch = new LossyCountingSynopsis(rq.getUID(), rq.getParam());
				//String[] _tmp = { "1", "2", "0.0001" };

				Synopses.add(sketch);
			break;
			// ChainSampler
			case 10:

				if (rq.getParam().length > 3)
					sketch = new ChainSamplerSynopsis(rq.getUID(), rq.getParam());
				//String[] _tmp = { "2", "2", "1000", "100000" };
				Synopses.add(sketch);
			break;
			// GKQuantiles
			case 11:

				if (rq.getParam().length > 3)
					sketch = new GKsynopsis(rq.getUID(), rq.getParam());
				//String[] _tmp = { "2", "2", "0.01"};
				Synopses.add(sketch);
			break;
			// lib.TopK
			case 13:
				if (rq.getParam().length > 3)
					sketch = new SynopsisTopK(rq.getUID(), rq.getParam());
				//String[] _tmp = { "2", "2", "0.01"};
				Synopses.add(sketch);
				System.out.println("Synopses Added");
			break;
			// windowQuantiles
			case 16:
				if (rq.getParam().length > 3)
					sketch = new windowQuantiles(rq.getUID(), rq.getParam());
				//String[] _tmp = { "2", "2", "0.01"};
				Synopses.add(sketch);
			break;
			// 6-> dynamic load sketch
			case 25:

				Object instance;

				if (rq.getParam().length == 4) {

					File myJar = new File(rq.getParam()[2]);
					URLClassLoader child = new URLClassLoader(new URL[]{myJar.toURI().toURL()},
					this.getClass().getClassLoader());
					Class<?> classToLoad = Class.forName(rq.getParam()[3], true, child);
					instance = classToLoad.getConstructor().newInstance();
					Synopses.add((Synopsis) instance);

				} else {

					File myJar = new File("C:\\Users\\ado.kontax\\Desktop\\flinkSketches.jar");
					URLClassLoader child = new URLClassLoader(new URL[]{myJar.toURI().toURL()},
					this.getClass().getClassLoader());
					Class<?> classToLoad = Class.forName("com.yahoo.sketches.sampling.NewSketch", true, child);
					instance = classToLoad.getConstructor().newInstance();
					Synopses.add((Synopsis) instance);

				}
			break;
			// FINJOIN
			case 26:

				if (rq.getParam().length > 3)
					sketch = new FinJoinSynopsis(rq.getUID(), rq.getParam());
				//String[] _tmp = { "0", "0", "10", "100", "8", "3" };
				Synopses.add(sketch);

			break;
			// COUNT
			case 27:

				if (rq.getParam().length > 3)
					sketch = new Counters(rq.getUID(), rq.getParam());
				else {
					String[] _tmp = {"0", "0", "10", "100", "8", "3"};
					sketch = new Counters(rq.getUID(), _tmp);
				}
				Synopses.add(sketch);
			break;
			//window lsh
			case 28:
				System.out.println("ADD-> _ " +rq.toString());
				if (rq.getParam().length > 3)
					sketch = new WLSHSynopses(rq.getUID(), rq.getParam());

				Synopses.add(sketch);
			break;
			//window pastDFT
			case 29:
				System.out.println("ADD-> _ " +rq.toString());
				if (rq.getParam().length > 3)
					sketch = new PastDFTSynopsis(rq.getUID(), rq.getParam());
				Synopses.add(sketch);
				break;
			case 30:
				if (rq.getParam().length > 9)
					sketch = new SpatialSketch(rq.getUID(), rq.getParam());
				//{ "1", "2", "0.0002", "0.99", "4" };
				Synopses.add(sketch);
				break;
			case 31:
				if (rq.getParam().length > 8)
					sketch = new OmniSketch(rq.getUID(), rq.getParam());
				Synopses.add(sketch);
				break;
		}
			M_Synopses.put(rq.getKey(),Synopses);
		}
	//Continuous Synopsis
	else if(rq.getRequestID() == 5) {

			if (C_Synopses == null){
				C_Synopses = new ArrayList<>();
			}
			ContinuousSynopsis sketch = null;

			switch (rq.getSynopsisID()) {

				case 1:
					if (rq.getParam().length > 4)
						sketch = new ContinuousCM(rq.getUID(), rq, rq.getParam());
						//String[] _tmp = { "StockID", "Volume", "0.0002", "0.99", "4" };
						C_Synopses.add(sketch);
					MC_Synopses.put(rq.getKey(), C_Synopses);
					break;
				// RadiusSketch
				case 100:
					if (rq.getParam().length > 4)
						sketch = new Radius_Grid(rq);
					C_Synopses.add(sketch);
					MC_Synopses.put(rq.getKey(), C_Synopses);
					break;
				case 12:
					rq.setNoOfP(1);
					if (rq.getParam().length > 5)
						sketch = new ContinuousMaritimeSketches(rq.getUID(), rq, rq.getParam());
					//String[] _tmp = {"1", "1", "18000","10000","50","50"};
					C_Synopses.add(sketch);
					MC_Synopses.put(rq.getKey(), C_Synopses);
					break;
				case 15:
					if (rq.getParam().length > 5)
						sketch = new ISWoR(rq.getUID(), rq, rq.getParam());
					//String[] _tmp = {"1", "1", "18000","10000","50","50"};
					C_Synopses.add(sketch);
					MC_Synopses.put(rq.getKey(), C_Synopses);
					break;

			}
		}
		// Estimate - delete
		else {
			if(Synopses==null){
				System.out.println("create Synopses first before estimation");
			}else {
				for (Synopsis syn : Synopses) {

					if (rq.getUID() == syn.getSynopsisID()) {
						if (rq.getRequestID() % 10 == 2) {
							System.out.println("removed");
							Synopses.remove(syn);
							M_Synopses.put(rq.getKey(), Synopses);

						} else if ((rq.getRequestID() % 10 == 3) || (rq.getRequestID() % 10 == 6)) {

							Estimation e = syn.estimate(rq);
							if (e.getEstimation() != null) {
								if (rq.getSynopsisID() == 28) {

									HashMap<Integer, Bucket> buckets = (HashMap<Integer, Bucket>) e.getEstimation();

									for (Map.Entry<Integer, Bucket> entry : buckets.entrySet()) {
										Integer key = entry.getKey();
										Bucket value = entry.getValue();
										System.out.println("Bucket No. -> " + key + "Pid:" + pId + "\n INFO -> " + value.toString());
										e.setKey(e.getUID() + "_" + key);
										e.setEstimationkey(e.getUID() + "_" + key + "_" + pId);
										e.setEstimation(value);
										//Estimation e1 = new Estimation(e);
										collector.collect(e);

									}
								} else if (rq.getSynopsisID() == 29) {

									HashMap<String, controlBucket> buckets = (HashMap<String, controlBucket>) e.getEstimation();

									for (Map.Entry<String, controlBucket> entry : buckets.entrySet()) {

										String key = entry.getKey();
										//System.out.println("Keys -> " + key);
										controlBucket value = entry.getValue();
										if (value != null)
											System.out.println("Bucket BEFORE with KEY ->" + key + " INFO -> " + value.toString());
										e.setKey(key);
										e.setEstimationkey(e.getUID() + "_" + key + "_" + pId);
										e.setEstimation(value);
										//System.out.println(e.toString());
										collector.collect(e);
									}
								} else {
									collector.collect(e);
								}
							}

						}
					}

				}
			}
		}
	}
	public void open(Configuration config)  {
	 	pId = getRuntimeContext().getIndexOfThisSubtask();
	}

}
