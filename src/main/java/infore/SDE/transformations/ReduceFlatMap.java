package infore.SDE.transformations;

import infore.SDE.reduceFunctions.*;
import infore.SDE.messages.Estimation;
import infore.SDE.reduceFunctions.WLSH_Reduce;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;

import java.util.Arrays;
import java.util.HashMap;

public class ReduceFlatMap extends RichFlatMapFunction<Estimation, Estimation> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private HashMap<String, ReduceFunction> rf = new HashMap<>();


    @Override
    public void flatMap(Estimation value, Collector<Estimation> out){
        ReduceFunction t_rf = rf.get("" + value.getEstimationkey());
        int id = value.getSynopsisID();
        String key = value.getEstimationkey();
            if (t_rf == null){

                t_rf = initReduceFunction(value, id);
                rf.put("" + key, t_rf);

            }else{
                if (t_rf.add(value)) {
                    Object output = t_rf.reduce();
                    if (output != null) {
                        value.setEstimation(output);
                        rf.remove("" + key);
                        if(id == 28)
                            value.setEstimationkey(value.getUID()+"");
                        out.collect(value);
                    }

                }
            }
        }

    private ReduceFunction initReduceFunction(Estimation value, int id) {
        ReduceFunction t_rf = null;
        //RadiusCount
        if (id == 100) {
            t_rf = new RadiusReduce(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }

        //MAX
        if (id == 11) {
            t_rf = new SimpleMaxFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }
        //AVG
        else if (id == 15) {
            t_rf = new SimpleAvgFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }
        //SUM
        else if (id == 1 || id == 3 || id == 8 || id == 9 || id == 7) {

            if (id == 1 && value.getRequestID() % 10 == 6) {
                new JoinEstimationFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            } else {
                t_rf = new SimpleSumFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            }
        }
        //OR
        else if (id == 2) {
            t_rf = new SimpleORFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }
        //DFT CORRELATION
        else if (id == 4){
            t_rf = new CorrelationDFTReduce(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }
        //KMEANS CORESETS
        else if( id == 6) {
            //System.out.println("START");
            t_rf = new KmeansReduce(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }
        //KMEANS CORESETS
        else if( id == 13) {
            //System.out.println("START");
            t_rf = new TopKReduce(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
            t_rf.add(value);
        }
        //WINDOW LSH SYNOPSIS
        else if (id == 28) {
            t_rf = new WLSH_Reduce(value.getNoOfP(), 0,value.getEstimationkey(), Double.parseDouble(value.getParam()[0]),Integer.parseInt(value.getParam()[1]));
            t_rf.add(value);
        }
        else if (id == 29) {
            t_rf = new WDFT_Reduce(value.getNoOfP(), Double.parseDouble(value.getParam()[0]),Integer.parseInt(value.getParam()[0]),Integer.parseInt(value.getParam()[0]),stringToStringArray(value.getParam()[0]));
            //int workers, double th, int k, int t, String[] stock
            t_rf.add(value);
        }
        else if (id == 30) {
            String[] params = value.getParam();
            if (params.length != 6) {
                throw new UnsupportedOperationException("Invalid number of parameters");
            }
            int basicSketchID = Integer.parseInt(params[1]);
            if (basicSketchID != 1) {
                throw new UnsupportedOperationException("Basic Sketch not available");
            }
            t_rf = new SimpleSumFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
        }
        else if (id == 31) {
            t_rf = new SimpleSumFunction(value.getNoOfP(), 0, value.getParam(), value.getSynopsisID(), value.getRequestID());
        }
        return t_rf;
    }


    private  String[] stringToStringArray(String param)
    {
        return param.split(";");
    }

}

