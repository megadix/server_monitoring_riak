package monitoring.monitoring.service;

import com.basho.riak.client.IRiakClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringService {
    private IRiakClient riakClient;

    @Autowired
    public void setRiakClient(IRiakClient riakClient) {
        this.riakClient = riakClient;
    }
}
