package de.tum.msrg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playground {

    public static void main(String[] args) throws IOException {
        List<String> containers = new ArrayList<>();
        containers.add("some-nginx");
        Server server = new Server("localhost", 52923);

//        List<Server> servers = new ArrayList<>();
//        servers.add(server);

        String operation = "kill";
//        OperationOptn operationOptn1 = new OperationOptn("-d", "10s");
//        OperationOptn operationOptn2 = new OperationOptn("--tc-image", "gaiadocker/iproute2");
        List<OperationOptn> operationOptns = new ArrayList<>();
//        operationOptns.add(operationOptn1);
//        operationOptns.add(operationOptn2);

//        String suboperation = "delay";
        String suboperation = "";
//        OperationOptn suboperationoptn = new OperationOptn("-t", "1000");
        List<OperationOptn> suboperationoptns = new ArrayList<>();
//        suboperationoptns.add(suboperationoptn);


        Task task1 = new Task(server, Collections.singletonList("some"), "kill", operationOptns, "", suboperationoptns, 10);
        Task task2 = new Task(server, Collections.singletonList("some"), "kill", operationOptns, "", suboperationoptns, 10);

        Experiment newExperiment = new Experiment();

        newExperiment.addTask(task1);
        newExperiment.addTask(task2);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValueAsString(newExperiment);

        mapper.writeValue(new File("experiment.yaml"), newExperiment);


    }
}
