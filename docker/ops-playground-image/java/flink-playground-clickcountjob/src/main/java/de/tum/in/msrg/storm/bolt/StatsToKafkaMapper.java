package de.tum.in.msrg.storm.bolt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.playgrounds.ops.clickcount.ClickEventCount;
import org.apache.flink.playgrounds.ops.clickcount.records.ClickEventStatistics;
import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.tuple.Tuple;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class StatsToKafkaMapper implements TupleToKafkaMapper<String, String> {
    private final ObjectMapper objectMapper;

    public StatsToKafkaMapper() {
        this.objectMapper = new ObjectMapper();
    }



    @Override
    public String getKeyFromTuple(Tuple tuple) {
        return null;
    }

    @Override
    public String getMessageFromTuple(Tuple tuple) {
//        Date windowStart = (Date) tuple.getValueByField("windowStart");
//        Date windowEnd = (Date) tuple.getValueByField("windowEnd");
//        Date firstMsgTS = (Date) tuple.getValueByField("firstMsgTS");
//        String page = tuple.getStringByField("page");
//        Long count = tuple.getLongByField("count");

//        ClickEventStatistics stats = new ClickEventStatistics(windowStart, windowEnd, firstMsgTS, page, count);
        ClickEventStatistics stats = (ClickEventStatistics) tuple.getValueByField("stats");
//        System.out.println(stats);


        try {
            return this.objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
