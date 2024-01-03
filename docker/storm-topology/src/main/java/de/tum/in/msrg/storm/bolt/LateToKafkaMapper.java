package de.tum.in.msrg.storm.bolt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.msrg.datamodel.ClickEvent;
import de.tum.in.msrg.datamodel.ClickUpdateEvent;
import de.tum.in.msrg.datamodel.PageStatistics;
import de.tum.in.msrg.datamodel.UpdateEvent;
import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.tuple.ITuple;
import org.apache.storm.tuple.Tuple;

public class LateToKafkaMapper implements TupleToKafkaMapper<byte[], String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Override
    public byte[] getKeyFromTuple(Tuple tuple) {
        try {
            ClickEvent event = (ClickEvent) tuple.getValueByField("late_tuple");
            return MAPPER.writeValueAsBytes(event.getPage());
        } catch (ClassCastException e){
            try {
                UpdateEvent event = (UpdateEvent) tuple.getValueByField("late_tuple");
                return MAPPER.writeValueAsBytes(event.getPage());
            } catch (ClassCastException e1){
                try {
                    ClickUpdateEvent event = (ClickUpdateEvent) tuple.getValueByField("late_tuple");
                    return MAPPER.writeValueAsBytes(event.getPage());
                } catch (ClassCastException | JsonProcessingException e2){
                    e.printStackTrace();
                    return null;
                }
            } catch (JsonProcessingException e1) {
                e.printStackTrace();
                return null;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getMessageFromTuple(Tuple tuple) {
        try {
            ClickEvent event = (ClickEvent) tuple.getValueByField("late_tuple");
            return MAPPER.writeValueAsString(event);
        } catch (ClassCastException e) {
            try {
                UpdateEvent event = (UpdateEvent) tuple.getValueByField("late_tuple");
                return MAPPER.writeValueAsString(event);
            } catch (ClassCastException e1) {
                try {
                    ClickUpdateEvent event = (ClickUpdateEvent) tuple.getValueByField("late_tuple");
                    return MAPPER.writeValueAsString(event);
                } catch (ClassCastException | JsonProcessingException e2) {
                    e.printStackTrace();
                    return null;
                }
            } catch (JsonProcessingException e1) {
                e.printStackTrace();
                return null;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
