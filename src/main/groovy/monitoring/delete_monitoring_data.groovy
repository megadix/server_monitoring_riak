package monitoring

import util.RiakUtils;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;

/*
 * Constants
 */

HOST = "192.168.56.101"
PORT = 10017

/*
 * Main execution
 */

IRiakClient client = RiakFactory.pbcClient(HOST, PORT)

[
    "mon_ATTILA",
    "mon_BUBBA",
    "mon_CALIGOLA",
    "mon_DEMOTAPE",
    "mon_functions"
].each {
    RiakUtils.deleteBucket(client, it, true)
}

client.shutdown()
