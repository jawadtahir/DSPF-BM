package de.tum.msrg;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PumbaSrvrClient {

    private static final Logger LOGGER= LogManager.getLogger(PumbaSrvrClient.class);

    private Experiment experiment;

    private static HTTPServer httpServer;
    private Gauge faultGauge;


    public PumbaSrvrClient(Experiment experiment){
        this.experiment = experiment;

        try {
            httpServer = new HTTPServer(9100);
            faultGauge = Gauge.build("de_tum_in_msrg_pumbasrvrclient_fault", "Fault gauge for annotations").register();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(String command, Task task){

        try (Socket serverSocket = new Socket()) {

            //start delay
            TimeUnit.SECONDS.sleep(task.getStartDelay());

            serverSocket.connect(new InetSocketAddress(task.getServer().getAddress(), task.getServer().getPort()));
            serverSocket.getOutputStream().write(String.format("%s\n", command).getBytes(StandardCharsets.UTF_8));
            serverSocket.getOutputStream().flush();



        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    protected void annotate(Task task) throws InterruptedException {
        long delayTO = extractTO(task);
        if (httpServer != null){
            if (task.getOperation().toLowerCase().equals("kill")){
                    faultGauge.inc(5);
                    TimeUnit.SECONDS.sleep(delayTO);
                    faultGauge.dec(5);
            } else {
                faultGauge.inc(5);
                TimeUnit.SECONDS.sleep(delayTO);
                faultGauge.dec(5);
            }

        }
    }

    private long extractTO(Task task) {
        AtomicReference<Long> retVal = new AtomicReference<>(0L);

        task.getOperationOptns().stream()
                .filter(opt -> opt.getOption().equalsIgnoreCase("-d")||
                                opt.getOption().equalsIgnoreCase("--duration"))
                .findFirst().ifPresentOrElse(
                        operationOptn -> retVal.set(Long.parseLong(operationOptn.getValue().substring(0, operationOptn.getValue().length()-1))),
                        () -> retVal.set(0L));

        return retVal.get();
    }

    public String buildCommand(Task task){
        String retVal = "";

        if (task.getOperation().toString().equals("kill")){
            retVal = buildKillCommand(task);
        }else if (task.getOperation().equals("netem")){
            retVal = buildNetemCommand(task);
        }else if (task.getOperation().equals("pause")){
            retVal = buildPauseCommand(task);
        }

        LOGGER.info(String.format("Command built: %s", retVal));
        return retVal;
    }

    protected String buildKillCommand(Task task){
        StringBuilder sb = new StringBuilder();
        sb.append("kill ");

        configureCommandOptions(sb, task);

        configureContainers(sb, task);


        return sb.toString();
    }

    protected String buildPauseCommand(Task task){
        StringBuilder sb = new StringBuilder();
        sb.append("pause ");

        configureCommandOptions(sb, task);

        configureContainers(sb, task);


        return sb.toString();
    }


    protected String buildNetemCommand(Task task){
        StringBuilder sb = new StringBuilder();

        sb.append("netem ");

        configureCommandOptions(sb, task);

        sb.append(task.getSuboperation()).append(" ");

        configureSubCmdOptions(sb, task);
        configureContainers(sb, task);



        return sb.toString();
    }

    protected void configureCommandOptions(StringBuilder sb, Task task){
        if (task.getOperation() != null && !task.getOperationOptns().isEmpty()){
            for (OperationOptn option : task.getOperationOptns()){
                sb.append(String.format("%s=%s ", option.getOption(), option.getValue()));
            }
        }
    }

    protected void configureContainers(StringBuilder sb, Task task){
        if (task.getContainers() != null && !task.getContainers().isEmpty()){
            for (String container: task.getContainers()){
                sb.append(String.format("'re2:%s*' ", container));
            }
        }
    }


    protected void configureSubCmdOptions(StringBuilder sb, Task task){
        if (task.getSuboperation() != null && !task.getSuboperationOptns().isEmpty()){
            for (OperationOptn option : task.getSuboperationOptns()){
                sb.append(String.format("%s=%s ", option.getOption(), option.getValue()));
            }
        }
    }

    protected void runExperiment() throws InterruptedException {
        for (Task task :
                experiment.getTasks()) {
            sendCommand(buildCommand(task), task);
            annotate(task);
        }
    }


    public static void main (String[] args)  {


        try {

//            String resourceDir = System.getenv().getOrDefault("PUMBA_SRVR_CLIENT_RESOURCE_DIR", "");
//            String yamlFileName = "experiment.yaml";
//            String fileToRead = Paths.get(resourceDir, yamlFileName).toString();

            System.out.println(System.getProperty("java.class.path"));
            InputStream yamlFile = PumbaSrvrClient.class.getResourceAsStream("/experiment.yaml");
            LOGGER.info(yamlFile);
//            File yamlFile = new File(yamlFilepath);
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            Experiment experiment = mapper.readValue(yamlFile, Experiment.class);

            LOGGER.info(experiment.toString());

            PumbaSrvrClient client = new PumbaSrvrClient(experiment);

            client.runExperiment();

//            client.sendCommand(client.buildCommand());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
