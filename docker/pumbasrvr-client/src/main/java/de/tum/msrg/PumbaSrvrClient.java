package de.tum.msrg;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PumbaSrvrClient {

    private static final Logger LOGGER= LogManager.getLogger(PumbaSrvrClient.class);
    private Map<String, Integer> servers;
    private Operation operation;
    private Map<String, String> options;
    private String subCommand;
    private Map<String, String> subCmdOptions;
    private List<String> containers;


    public PumbaSrvrClient(Map<String, Integer> servers){
        this.servers = servers;
    }

    public void sendCommand(String command){
        for (Map.Entry<String, Integer> server: this.servers.entrySet()){
            try (Socket serverSocket = new Socket()) {

                serverSocket.connect(new InetSocketAddress(server.getKey(), server.getValue()));
                serverSocket.getOutputStream().write(String.format("%s\n", command).getBytes(StandardCharsets.UTF_8));
                serverSocket.getOutputStream().flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String buildCommand(){
        String retVal = "";

        if (this.operation == Operation.KILL){
            retVal = buildKillCommand();
        }else if (this.operation == Operation.NETEM){
            retVal = buildNetemCommand();
        }

        LOGGER.info(String.format("Command built: %s", retVal));
        return retVal;
    }

    protected String buildKillCommand(){
        StringBuilder sb = new StringBuilder();
        sb.append("kill ");

        configureCommandOptions(sb);

        configureContainers(sb);


        return sb.toString();
    }

    protected String buildNetemCommand(){
        StringBuilder sb = new StringBuilder();

        sb.append("netem ");

        configureCommandOptions(sb);

        sb.append(this.subCommand).append(" ");

        configureSubCmdOptions(sb);
        configureContainers(sb);



        return sb.toString();
    }

    protected void configureCommandOptions(StringBuilder sb){
        if (this.options != null && !this.options.isEmpty()){
            for (Map.Entry<String, String> option:this.options.entrySet()){
                sb.append(option.getKey()).append("=").append(option.getValue()).append(" ");
            }
        }
    }

    protected void configureContainers(StringBuilder sb){
        if (this.containers != null && !this.containers.isEmpty()){
            for (String container: this.containers){
                sb.append(container).append(" ");
            }
        }
    }


    protected void configureSubCmdOptions(StringBuilder sb){
        if (this.subCmdOptions != null && !this.subCmdOptions.isEmpty()){
            for (Map.Entry<String, String> option:this.subCmdOptions.entrySet()){
                sb.append(option.getKey()).append("=").append(option.getValue()).append(" ");
            }
        }
    }



    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public String getSubCommand() {
        return subCommand;
    }

    public void setSubCommand(String subCommand) {
        this.subCommand = subCommand;
    }

    public Map<String, String> getSubCmdOptions() {
        return subCmdOptions;
    }

    public void setSubCmdOptions(Map<String, String> subCmdOptions) {
        this.subCmdOptions = subCmdOptions;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public static void main (String[] args)  {


        try {

            String resourceDir = System.getenv().getOrDefault("PUMBA_SRVR_CLIENT_RESOURCE_DIR", "");
            String yamlFileName = "experiment.yaml";
            String fileToRead = String.format("%s%s", resourceDir, yamlFileName);

            File yamlFile = new File(fileToRead);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            Chaos chaos = mapper.readValue(yamlFile, Chaos.class);
            List<Server> servers = chaos.getServers();
            Map<String, Integer> serverMap = new HashMap<>();
            for (Server server: servers){
                List<String> containers = server.getContainers();
                String adrs = server.getAddress();
                serverMap.put(adrs.split(":")[0], Integer.parseInt(adrs.split(":")[1]));


                String operation = chaos.getOperation();
                Map<String, String> operationOptns = new HashMap<>();
                for (OperationOptn optn: chaos.getOperationOptns()){
                    operationOptns.put(optn.getOption(), optn.getValue());
                }

                String suboperation = chaos.getSuboperation();
                Map<String, String> suboperationOptns = new HashMap<>();
                for (OperationOptn optn: chaos.getSuboperationOptns()){
                    suboperationOptns.put(optn.getOption(), optn.getValue());
                }

                PumbaSrvrClient client = new PumbaSrvrClient(serverMap);

                switch (operation.toLowerCase()){
                    case "kill":
                        client.setOperation(Operation.KILL);
                        break;
                    case "netem":
                        client.setOperation(Operation.NETEM);
                        break;
                    default:
                        LOGGER.error("Unknown operation");
                        break;
                }

                client.setOptions(operationOptns);
                client.setSubCommand(suboperation);
                client.setSubCmdOptions(suboperationOptns);
                client.setContainers(containers);

                client.sendCommand(client.buildCommand());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
