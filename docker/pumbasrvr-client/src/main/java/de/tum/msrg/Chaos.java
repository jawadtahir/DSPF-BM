package de.tum.msrg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Chaos {
    private Server server;
    private List<String> containers;
    private String operation;
    private List<OperationOptn> operationOptns;
    private String suboperation;
    private List<OperationOptn> suboperationOptns;
    private Integer startDelay;

    public Chaos() {
        this(new Server(), new ArrayList<>(), "", new ArrayList<>(), "", new ArrayList<>(), 10);
    }

    public Chaos(Server server, List<String> containers, String operation, List<OperationOptn> operationOptns, String suboperation, List<OperationOptn> suboperationOptns, Integer startDelay) {
        this.server = server;
        this.containers = containers;
        this.operation = operation;
        this.operationOptns = operationOptns;
        this.suboperation = suboperation;
        this.suboperationOptns = suboperationOptns;
        this.startDelay = startDelay;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<OperationOptn> getOperationOptns() {
        return operationOptns;
    }

    public void setOperationOptns(List<OperationOptn> operationOptns) {
        this.operationOptns = operationOptns;
    }

    public String getSuboperation() {
        return suboperation;
    }

    public void setSuboperation(String suboperation) {
        this.suboperation = suboperation;
    }

    public List<OperationOptn> getSuboperationOptns() {
        return suboperationOptns;
    }

    public void setSuboperationOptns(List<OperationOptn> suboperationOptns) {
        this.suboperationOptns = suboperationOptns;
    }

    public Integer getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(Integer startDelay) {
        this.startDelay = startDelay;
    }

    @Override
    public String toString() {
        return "Chaos{" +
                "server=" + server +
                ", containers=" + containers +
                ", operation='" + operation + '\'' +
                ", operationOptns=" + operationOptns +
                ", suboperation='" + suboperation + '\'' +
                ", suboperationOptns=" + suboperationOptns +
                ", startDelay=" + startDelay +
                '}';
    }
}

class Server {
    private String address;
    private Integer port;

    public Server() {
    }

    public Server(String address, Integer port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Server{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}

class OperationOptn {
    private String option;
    private String value;

    public OperationOptn(){
        this("","");
    }

    public OperationOptn(String option, String value) {
        this.option = option;
        this.value = value;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "OperationOptn{" +
                "option='" + option + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
