package monitoring;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.MapReduceResult;
import com.basho.riak.client.query.functions.JSBucketKeyFunction;
import com.basho.riak.client.query.indexes.BinIndex;
import com.basho.riak.client.raw.query.indexes.BinRangeQuery;
import com.basho.riak.client.raw.query.indexes.IndexQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import monitoring.model.MonitoringData;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import util.data.DataSimulationUtils;

import java.util.*;

@EnableAutoConfiguration
@ComponentScan
public class Application implements CommandLineRunner {

    static class ExamplesServerConf {
        String serverName;
        double memMean;
        double memSd;
        double cpuMean;
        double cpuSd;

        ExamplesServerConf(String serverName, double memMean, double memSd, double cpuMean, double cpuSd) {
            this.serverName = serverName;
            this.memMean = memMean;
            this.memSd = memSd;
            this.cpuMean = cpuMean;
            this.cpuSd = cpuSd;
        }
    }

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private IRiakClient client;

    @Autowired
    public void setRiakClient(IRiakClient riakClient) {
        this.client = riakClient;
    }

    private void setupExample() throws Exception {
        Date startDate = DateFormatUtils.timestampFormat.parse("20140101_000000");
        Date endDate = DateFormatUtils.timestampFormat.parse("20140108_000000");

        List<ExamplesServerConf> configurations = new ArrayList<>();
        configurations.add(new ExamplesServerConf("ATTILA", 0.5, 0.01, 0.2, 0.05));
        configurations.add(new ExamplesServerConf("BUBBA", 0.3, 0.2, 0.5, 0.1));
        configurations.add(new ExamplesServerConf("CALIGOLA", 0.1, 0.2, 0.8, 0.2));
        configurations.add(new ExamplesServerConf("DEMOTAPE", 0.8, 0.3, 0.7, 0.4));

        // ----------------- 1) DELETE everything -----------------
        for (ExamplesServerConf conf : configurations) {
            String bucketName = "mon_" + conf.serverName;
            deleteBucket(bucketName);
        }
        deleteBucket("mon_functions");
        // wait for writes to propagate
        Thread.sleep(10000);

        // ----------------- 2) INSERT sample data -----------------
        for (ExamplesServerConf conf : configurations) {
            Bucket bucket = client.createBucket("mon_" + conf.serverName)
                    .r(1)
                    .dw(0)
                    .execute();

            SortedMap<Date, Double> memData = DataSimulationUtils.simulateServerLoad(startDate, endDate, 1,
                    conf.memMean, conf.memSd);
            SortedMap<Date, Double> cpuData = DataSimulationUtils.simulateServerLoad(startDate, endDate, 1,
                    conf.memMean, conf.memSd);

            int i = 0;

            for (Date date : memData.keySet()) {
                MonitoringData monData = new MonitoringData();
                monData.setTimestamp(DateFormatUtils.timestampFormat.format(date));
                monData.putValue("mem", memData.get(date));
                monData.putValue("cpu", cpuData.get(date));

                bucket.store(monData).execute();
                i++;
                if (i % 100 == 0) {
                    log.info("Created " + i + " objects");
                }
            }
        }

        // ----------------- 3) INSERT Map/Reduce Javascript functions -----------------

        Bucket functionsBucket = client.createBucket("mon_functions").execute();

        String[] filenames = {"map_aggregateByHours.js", "reduce_aggregateByHours.js"};
        for (String filename : filenames) {
            functionsBucket.store(filename, IOUtils.toString(getClass().getResourceAsStream("/" + filename))).execute();
        }

        System.exit(0);
    }

    private void runExample() throws Exception {
        // SAMPLE mapReduceQuery: aggregate ATTILA data by the hour
        String bucketName = "mon_ATTILA";
        IndexQuery query = new BinRangeQuery(BinIndex.named("timestamp_bin"),
                bucketName,
                "20140101_000000", "20140101_120000");

        MapReduceResult result = client.mapReduce(query)
                .addMapPhase(new JSBucketKeyFunction("mon_functions", "map_aggregateByHours.js"))
                .addReducePhase(new JSBucketKeyFunction("mon_functions", "reduce_aggregateByHours.js"))
                .execute();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result.getResultRaw());

        // build time series of "mem" and "cpu" metrics
        TimeSeries mem = new TimeSeries("Mem");
        TimeSeries cpu = new TimeSeries("Cpu");

        JsonNode root = node.get(0);
        Iterator<String> iter = root.fieldNames();
        while (iter.hasNext()) {
            String sDate = iter.next();
            JsonNode entry = root.get(sDate);

            int year = Integer.valueOf(sDate.substring(0, 4));
            int month = Integer.valueOf(sDate.substring(4, 6));
            int day = Integer.valueOf(sDate.substring(6, 8));
            int hour = Integer.valueOf(sDate.substring(9, 11));
            Hour h = new Hour(hour, day, month, year);
            mem.add(h, entry.get("mem").asDouble() * 100.0);
            cpu.add(h, entry.get("cpu").asDouble() * 100.0);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(mem);
        dataset.addSeries(cpu);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Server Load",
                "Time",
                "%",
                dataset,
                true, true, false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRangeAxis().setRange(0.0, 100.0);

        ChartFrame frame = new ChartFrame("Demo", chart);
        frame.pack();
        frame.setVisible(true);

    }

    /**
     * Application entry point
     *
     * @param args
     */
    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class);
        builder.headless(false).run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Missing command-line arguments: COMMAND");
            System.err.println("COMMAND:");
            System.err.println("  SETUP : create database, sample data and functions");
            System.err.println("  RUN : run example client");

            System.exit(1);
        }

        if ("SETUP".equalsIgnoreCase(args[0])) {
            setupExample();
        } else if ("RUN".equalsIgnoreCase(args[0])) {
            runExample();
        }
    }

    private void deleteBucket(String bucketName) throws RiakException {
        Bucket bucket = client.fetchBucket(bucketName).execute();

        if (bucket != null) {
            for (String key : bucket.keys()) {
                log.info("Deleting " + bucketName + " / " + key);
                bucket.delete(key).dw(3).execute();
            }
        }
    }

}
