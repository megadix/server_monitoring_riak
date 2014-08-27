function(v) {
    var result = {};
    // From the Riak object, pull data and parse it as JSON
    var parsed_data = JSON.parse(v.values[0].data);
    // Key: date up to hours
    var timestamp = v.key.substr(0, 11);
    // Value: 'value' property
    result[timestamp] = parsed_data.data;
    
    return [result];
}
