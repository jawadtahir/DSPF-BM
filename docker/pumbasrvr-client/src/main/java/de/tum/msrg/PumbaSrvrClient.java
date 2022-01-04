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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PumbaSrvrClient {

    private static final Logger LOGGER= LogManager.getLogger(PumbaSrvrClient.class);

    private Chaos chaos;

    public PumbaSrvrClient(Chaos chaos){
        this.chaos = chaos;
    }

    public void sendCommand(String command){

        try (Socket serverSocket = new Socket()) {

            //start delay
            TimeUnit.SECONDS.sleep(chaos.getStartDelay());

            serverSocket.connect(new InetSocketAddress(chaos.getServer().getAddress(), chaos.getServer().getPort()));
            serverSocket.getOutputStream().write(String.format("%s\n", command).getBytes(StandardCharsets.UTF_8));
            serverSocket.getOutputStream().flush();



        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public String buildCommand(){
        String retVal = "";

        if (chaos.getOperation().toString().equals("kill")){
            retVal = buildKillCommand();
        }else if (chaos.getOperation().equals("netem")){
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

        sb.append(chaos.getSuboperation()).append(" ");

        configureSubCmdOptions(sb);
        configureContainers(sb);



        return sb.toString();
    }

    protected void configureCommandOptions(StringBuilder sb){
        if (chaos.getOperation() != null && !chaos.getOperationOptns().isEmpty()){
            for (OperationOptn option : chaos.getOperationOptns()){
                sb.append(String.format("%s=%s ", option.getOption(), option.getValue()));
            }
        }
    }

    protected void configureContainers(StringBuilder sb){
        if (chaos.getContainers() != null && !chaos.getContainers().isEmpty()){
            for (String container: chaos.getContainers()){
                sb.append(String.format("'re2:%s*' ", container));
            }
        }
    }


    protected void configureSubCmdOptions(StringBuilder sb){
        if (chaos.getSuboperation() != null && !chaos.getSuboperationOptns().isEmpty()){
            for (OperationOptn option : chaos.getSuboperationOptns()){
                sb.append(String.format("%s=%s ", option.getOption(), option.getValue()));
            }
        }
    }


    public static void main (String[] args)  {


        try {

            String resourceDir = System.getenv().getOrDefault("PUMBA_SRVR_CLIENT_RESOURCE_DIR", "");
            String yamlFileName = "experiment.yaml";
            String fileToRead = Paths.get(resourceDir, yamlFileName).toString();
            File yamlFile = new File(fileToRead);
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            Chaos chaos = mapper.readValue(yamlFile, Chaos.class);

            LOGGER.info(chaos.toString());

            PumbaSrvrClient client = new PumbaSrvrClient(chaos);

            client.sendCommand(client.buildCommand());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
