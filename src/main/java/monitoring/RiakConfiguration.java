package monitoring;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.raw.pbc.PBClientConfig;
import com.basho.riak.client.raw.pbc.PBClusterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "monitorin.riak")
public class RiakConfiguration {
    private List<String> pbcHosts = new ArrayList<>();
    private List<Integer> pbcPorts = new ArrayList<>();

    public RiakConfiguration() {
    }

    public List<String> getPbcHosts() {
        return pbcHosts;
    }

    public void setPbcHosts(List<String> pbcHosts) {
        this.pbcHosts = pbcHosts;
    }

    public List<Integer> getPbcPorts() {
        return pbcPorts;
    }

    public void setPbcPorts(List<Integer> pbcPorts) {
        this.pbcPorts = pbcPorts;
    }

    @Bean
    public IRiakClient pbcClient() throws RiakException {
        PBClusterConfig clusterConfig = new PBClusterConfig(200);
        // used to retrieve default values
        PBClientConfig defaultClientConfig = PBClientConfig.defaults();

        for (int i = 0; i < pbcHosts.size(); i++) {
            PBClientConfig clientConfig = new PBClientConfig.Builder()
                    .withSocketBufferSizeKb(defaultClientConfig.getSocketBufferSizeKb())
                    .withHost(pbcHosts.get(i))
                    .withPort(pbcPorts.get(i))
                    .withPoolSize(defaultClientConfig.getPoolSize())
                    .withInitialPoolSize(defaultClientConfig.getInitialPoolSize())
                    .withIdleConnectionTTLMillis(defaultClientConfig.getIdleConnectionTTLMillis())
                    .withConnectionTimeoutMillis(defaultClientConfig.getConnectionWaitTimeoutMillis())
                    .withRequestTimeoutMillis(defaultClientConfig.getRequestTimeoutMillis())
                    .build();

                    clusterConfig.addClient(clientConfig);
        }

        return RiakFactory.newClient(clusterConfig);
    }
}
