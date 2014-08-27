function(v) {
    var result = {};
    
    var totalMem = {};
    var countMem = {};
    var totalCpu = {};
    var countCpu = {};
    
    var mon_accumulate = function(data, timestamp, accumulateMap, countMap, field) {
        var val = data[timestamp][field];
        if (val) {
            if (accumulateMap[timestamp]) {
                accumulateMap[timestamp] = accumulateMap[timestamp] + val;
                countMap[timestamp] = countMap[timestamp] + 1;
            }
            else {
                accumulateMap[timestamp] = val;
                countMap[timestamp] = 1;
            }
        }
    };
    
    for (var i in v) {
        for (var timestamp in v[i]) {
            // init result array
            result[timestamp] = {'mem': 0.0, 'cpu': 0.0};
            // accumulate values
            mon_accumulate(v[i], timestamp, totalMem, countMem, 'mem');
            mon_accumulate(v[i], timestamp, totalCpu, countCpu, 'cpu');
        }
    }
    
    for (timestamp in result) {
        if (countMem[timestamp] && countMem[timestamp] > 0) {
            result[timestamp].mem = totalMem[timestamp] / countMem[timestamp];
            result[timestamp].cpu = totalCpu[timestamp] / countCpu[timestamp];
        }
    }
    
    // TODO: sort
    
    return [result];
}

