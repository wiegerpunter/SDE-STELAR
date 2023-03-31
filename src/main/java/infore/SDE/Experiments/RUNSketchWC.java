package infore.SDE.Experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import infore.SDE.messages.Datapoint;
import infore.SDE.sources.kafkaProducerEstimation;
import infore.SDE.sources.kafkaStringConsumer_Earliest;
import infore.SDE.transformations.IngestionMultiplierFlatMap;
import infore.SDE.transformations.NaiveCoFlatMap;
import infore.SDE.transformations.SketchFlatMap;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.util.ArrayList;

public class RUNSketchWC {


    private static String kafkaDataInputTopic;
    private static String kafkaRequestInputTopic;
    private static String kafkaBrokersList;
    private static int parallelism;
    private static int multi;
    private static String kafkaOutputTopic;
    private static String Source;

    /**
     * @param args Program arguments. You have to provide 4 arguments otherwise
     *             DEFAULT values will be used.<br>
     *             <ol>
     *             <li>args[0]={@link #kafkaDataInputTopic} DEFAULT: "Forex")
     *             <li>args[1]={@link #kafkaRequestInputTopic} DEFAULT: "Requests")
     *             <li>args[2]={@link #kafkaBrokersList} (DEFAULT: "localhost:9092")
     *             <li>args[3]={@link #parallelism} Job parallelism (DEFAULT: "4")
     *             <li>args[4]={@link #kafkaOutputTopic} DEFAULT: "OUT")
     *             "O10")
     *             </ol>
     *
     */

    public static void main(String[] args) throws Exception {
        // Initialize Input Parameters
        initializeParameters(args);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        kafkaStringConsumer_Earliest kc = new kafkaStringConsumer_Earliest(kafkaBrokersList, kafkaDataInputTopic);
        DataStream<String> datastream = env.addSource(kc.getFc());

        //map kafka data input to tuple2<int,double>
        DataStream<Datapoint> dataStream = datastream
                .map(new MapFunction<String, Datapoint>() {
                    @Override
                    public Datapoint map(String node) throws IOException {
                        // TODO Auto-generated method stub
                        ObjectMapper objectMapper = new ObjectMapper();
                        Datapoint dp = objectMapper.readValue(node, Datapoint.class);
                        return dp;
                    }
                }).name("DATA_SOURCE").keyBy((KeySelector<Datapoint, String>) Datapoint::getKey);

        DataStream<Datapoint>  DataStream2 = dataStream.flatMap(new SketchFlatMap()).setParallelism(1);


        env.execute("StreamingSketch"+parallelism+"_"+multi+"_"+kafkaDataInputTopic);

    }

    private static void initializeParameters(String[] args) {

        if (args.length > 1) {

            System.out.println("[INFO] User Defined program arguments");
            //User defined program arguments
            kafkaDataInputTopic = args[0];
            kafkaRequestInputTopic = args[1];
            multi = Integer.parseInt(args[2]);
            parallelism = Integer.parseInt(args[3]);
            Source ="non";
            kafkaBrokersList = "clu02.softnet.tuc.gr:6667,clu03.softnet.tuc.gr:6667,clu04.softnet.tuc.gr:6667,clu06.softnet.tuc.gr:6667";
            kafkaOutputTopic = "RAD_OUT";

        }else{

            System.out.println("[INFO] Default values");
            kafkaDataInputTopic = "RAD_DATA_RE_4";
            kafkaRequestInputTopic = "RAD_REQUEST_5";
            Source ="non";
            multi = 10;
            parallelism = 4;
            //parallelism2 = 4;
            kafkaBrokersList = "clu02.softnet.tuc.gr:6667,clu03.softnet.tuc.gr:6667,clu04.softnet.tuc.gr:6667,clu06.softnet.tuc.gr:6667";
            kafkaOutputTopic = "RAD_OUT";
        }
    }
}
