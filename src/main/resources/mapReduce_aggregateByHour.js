{
  "inputs": {
    "bucket": "mon_ATTILA",
    "index": "timestamp_bin",
    "start": "20140101_000000",
    "end": "20140101_120000"
  },
  "query": [
    {"map":{
        "language": "javascript",
        "bucket": "mon_functions",
        "key": "map_aggregateByHours.js"
    }},
    {"reduce":{
        "language": "javascript",
        "bucket": "mon_functions",
        "key": "reduce_aggregateByHours.js"
      }
    }
  ]
}