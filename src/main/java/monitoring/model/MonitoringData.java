package monitoring.model;

import com.basho.riak.client.convert.RiakIndex;
import com.basho.riak.client.convert.RiakKey;

import java.util.HashMap;
import java.util.Map;

public class MonitoringData {
    @RiakKey
    @RiakIndex(name = "timestamp")
    private String timestamp;

    private Map<String, Object> data = new HashMap<>();

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void putValue(String key, Object value) {
        data.put(key, value);
    }
}
