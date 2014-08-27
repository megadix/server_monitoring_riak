package monitoring;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "monitorin.riak")
public class RiakConfiguration {
    private String pbcHost;
    private int pbcPort;

    public RiakConfiguration() {
    }

    public String getPbcHost() {
        return pbcHost;
    }

    public void setPbcHost(String pbcHost) {
        this.pbcHost = pbcHost;
    }

    public int getPbcPort() {
        return pbcPort;
    }

    public void setPbcPort(int pbcPort) {
        this.pbcPort = pbcPort;
    }

    @Bean
    public IRiakClient pbcClient() throws RiakException {
        return RiakFactory.pbcClient(pbcHost, pbcPort);
    }
}
