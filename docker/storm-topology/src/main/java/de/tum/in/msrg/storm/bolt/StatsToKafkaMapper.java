package de.tum.in.msrg.storm.bolt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.PageStatistics;
import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.tuple.Tuple;

public class StatsToKafkaMapper implements TupleToKafkaMapper<byte[], String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Override
    public byte[] getKeyFromTuple(Tuple tuple) {
        PageStatistics stats = (PageStatistics) tuple.getValueByField("stats");
        try {
            return MAPPER.writeValueAsBytes(stats.getPage());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getMessageFromTuple(Tuple tuple) {
        PageStatistics stats = (PageStatistics) tuple.getValueByField("stats");
//        System.out.println(stats);
        try {
            return MAPPER.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
