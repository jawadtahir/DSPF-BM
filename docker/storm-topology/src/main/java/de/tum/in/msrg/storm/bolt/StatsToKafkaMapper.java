package de.tum.in.msrg.storm.bolt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEventStatistics;
import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.tuple.Tuple;

public class StatsToKafkaMapper implements TupleToKafkaMapper<String, String> {


    @Override
    public String getKeyFromTuple(Tuple tuple) {
        return null;
    }

    @Override
    public String getMessageFromTuple(Tuple tuple) {
        ClickEventStatistics stats = (ClickEventStatistics) tuple.getValueByField("stats");
        System.out.println(stats);
        try {
            return new ObjectMapper().writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}