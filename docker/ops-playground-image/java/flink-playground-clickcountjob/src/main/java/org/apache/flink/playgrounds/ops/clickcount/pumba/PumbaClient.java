package org.apache.flink.playgrounds.ops.clickcount.pumba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PumbaClient {

    private static final Logger LOGGER= LoggerFactory.getLogger(PumbaClient.class);
    private Map<String, Integer> servers;
    private Operation operation;
    private Map<String, String> options;
    private String subCommand;
    private Map<String, String> subCmdOptions;
    private List<String> containers;


    public PumbaClient(Map<String, Integer> servers){
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

    public static void main (String[] args){
        Map<String, Integer> servers = new HashMap<>();
        servers.put("localhost", 52923);

        List<String> containers = new ArrayList<String>();
        containers.add("some-nginx");

        Map<String, String> cmdOptn = new HashMap<>();
        cmdOptn.put("-d", "10s");
        cmdOptn.put("--tc-image", "gaiadocker/iproute2");

        Map<String, String> subCmdOptn = new HashMap<>();
        subCmdOptn.put("-t", "1000");

        PumbaClient client = new PumbaClient(servers);

        client.setOperation(Operation.NETEM);
        client.setOptions(cmdOptn);
        client.setSubCommand("delay");
        client.setSubCmdOptions(subCmdOptn);
        client.setContainers(containers);

//        client.setOperation(Operation.KILL);
////        client.setOptions(cmdOptn);
////        client.setSubCommand("delay");
////        client.setSubCmdOptions(subCmdOptn);
//        client.setContainers(containers);

        client.sendCommand(client.buildCommand());


    }
}
