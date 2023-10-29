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
        String page = null;
        ITuple lateTuple = (ITuple) tuple.getValueByField("late_tuple");

        if (lateTuple.getFields().contains("clickEvent")) {
            page = ((ClickEvent) lateTuple.getValueByField("clickEvent")).getPage();

        } else if (lateTuple.getFields().contains("updateEvent")){
            page = ((UpdateEvent) lateTuple.getValueByField("updateEvent")).getPage();
        }

        else if (lateTuple.getFields().contains("clickUpdateEvent")){
            page = ((ClickUpdateEvent) lateTuple.getValueByField("clickUpdateEvent")).getPage();
        }

        try {
            return MAPPER.writeValueAsBytes(page);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getMessageFromTuple(Tuple tuple) {

        Object obj = null;
        ITuple lateTuple = (ITuple) tuple.getValueByField("late_tuple");

        if (lateTuple.getFields().contains("clickEvent")) {
            obj = ((ClickEvent) lateTuple.getValueByField("clickEvent"));

        } else if (lateTuple.getFields().contains("updateEvent")){
            obj = ((UpdateEvent) lateTuple.getValueByField("updateEvent"));
        }

        else if (lateTuple.getFields().contains("clickUpdateEvent")){
            obj = ((ClickUpdateEvent) lateTuple.getValueByField("clickUpdateEvent"));
        }


//        ClickUpdateEvent clickUpdateEvent = (ClickUpdateEvent) ((ITuple)tuple.getValueByField("late_tuple")).getValueByField("clickUpdateEvent");
//        System.out.println(stats);

        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }
}
