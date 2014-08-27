package monitoring

import com.basho.riak.client.IRiakClient
import com.basho.riak.client.RiakFactory
import com.basho.riak.client.convert.RiakIndex
import com.basho.riak.client.convert.RiakKey
import util.data.DataSimulationUtils

import java.text.DateFormat
import java.text.SimpleDateFormat

/* ========================================================================
 * Classes
 * ======================================================================== */

class OLDMonitoringData {
    @RiakKey
    @RiakIndex(name = "timestamp")
    String timestamp;

    Map value
}

/* ========================================================================
 * Functions
 * ======================================================================== */

def createMonitoringData(
        IRiakClient client,
        String serverName, Map serverConf,
        DateFormat dateFormat, Date startDate, Date endDate, int intervalMinutes) {

    println "Simulating data for server: ${serverName}"

    // create a bucket optimized for writes (eventual consistency)
    def bucket = client.createBucket("mon_${serverName}")
            .r(1)
            .dw(0)
            .execute()

    def memData = DataSimulationUtils.simulateServerLoad(startDate, endDate, intervalMinutes,
            serverConf.memMean, serverConf.memSd)

    def cpuData = DataSimulationUtils.simulateServerLoad(startDate, endDate, intervalMinutes,
            serverConf.cpuMean, serverConf.cpuSd)

    def i = 0
    
    memData.keySet().each { date ->
        def monData = new OLDMonitoringData()
        monData.timestamp = dateFormat.format(date)
        monData.value = [
            mem: (memData.get(date)),
            cpu: (cpuData.get(date))
        ]

        bucket.store(monData).execute()
        i++
        if (i % 100 == 0) {
            println "Created ${i} objects"
        }
    }
    
    println "Created ${i} objects"
}

/* ========================================================================
 * Configuration
 * ======================================================================== */

def riakPbcHost = "192.168.56.101"
def riakPbcPort = 10017

def dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")
def startDate = dateFormat.parse("20140101_000000")
def endDate =   dateFormat.parse("20140108_000000")
def intervalMinutes = 1

// servers to simulate
def serversConf = [
    "ATTILA": [memMean: 0.5, memSd: 0.01, cpuMean: 0.2, cpuSd: 0.05],
     "BUBBA": [memMean: 0.3, memSd: 0.2, cpuMean: 0.5, cpuSd: 0.1],
     "CALIGOLA": [memMean: 0.1, memSd: 0.2, cpuMean: 0.8, cpuSd: 0.2],
     "DEMOTAPE": [memMean: 0.8, memSd: 0.3, cpuMean: 0.7, cpuSd: 0.4]
]

/* ========================================================================
 * Main execution
 * ======================================================================== */

def client = null

try {
    client = RiakFactory.pbcClient(riakPbcHost, riakPbcPort)
    
    // store server data
    serversConf.each { serverName, serverConf ->
        createMonitoringData(client, serverName, serverConf, dateFormat, startDate, endDate, intervalMinutes)
    }
    
    // store map/reduce functions
    def functionsBucket = client.createBucket("mon_functions").execute()
    ["map_aggregateByHours.js", "reduce_aggregateByHours.js"].each { filename ->
        println "Storing function: ${filename}"
        functionsBucket.store(filename, getClass().getResourceAsStream(filename).text).execute()
    }

} catch (Exception e) {
    e.printStackTrace()
} finally {
    if (client != null) {
        client.shutdown()
    }
}



