package de.tum.msrg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Chaos {
    private List<Server> servers;
    private String operation;
    private List<OperationOptn> operationOptns;
    private String suboperation;
    private List<OperationOptn> suboperationOptns;

    public Chaos(){
        this(new ArrayList<>(), "", new ArrayList<>(), "", new ArrayList<>());
    }

    public Chaos(List<Server> servers, String operation, List<OperationOptn> operationOptns, String suboperation, List<OperationOptn> suboperationOptns) {
        this.servers = servers;
        this.operation = operation;
        this.operationOptns = operationOptns;
        this.suboperation = suboperation;
        this.suboperationOptns = suboperationOptns;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
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

    @Override
    public String toString() {
        return "Chaos{" +
                "servers=" + servers +
                ", operation='" + operation + '\'' +
                ", operationOptns=" + operationOptns +
                ", suboperation='" + suboperation + '\'' +
                ", suboperationOptns=" + suboperationOptns +
                '}';
    }
}
class ServerElements {
    public ServerElements() {
        this(new Server());
    }

    public ServerElements(Server server) {
        this.server = server;
    }

    private Server server;

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public String toString() {
        return "ServerElements{" +
                "server=" + server +
                '}';
    }
}
class Server {
    private String address;
    private List<String> containers;

    public Server(){
        this("localhost:52923", new ArrayList<>());
    }

    public Server(String address, List<String> containers) {
        this.address = address;
        this.containers = containers;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    @Override
    public String toString() {
        return "Server{" +
                "address='" + address + '\'' +
                ", containers=" + containers +
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
