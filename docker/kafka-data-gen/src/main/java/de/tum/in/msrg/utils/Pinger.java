package de.tum.in.msrg.utils;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Pinger {

    private List<String> servers;


    Pinger (List<String> servers){
        this.servers = servers;
    }

    public static void main(String[] args) {

        String serverString = System.getenv("SERVER_ADDRESS");
        List<String> servers = new ArrayList<>();
        for (String server : serverString.split(",")){
            servers.add(server.trim());
        }

        Pinger pinger = new Pinger(servers);

        pinger.ping();

    }

    private void ping() {
        try (HTTPServer promServer = new HTTPServer(52923)) {
            Gauge ping = Gauge.build("de_tum_in_msrg_pinger", "ping latency").labelNames("server").register();
            while (true) {
                for (String server : servers){
                    pingAndRecord(server, ping);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void pingAndRecord(String server, Gauge ping) throws IOException {
        InetAddress address = InetAddress.getByName(server);
        long ts1 = System.nanoTime();
        if (address.isReachable(10000)){
            long ts2 = System.nanoTime();
            long latency = ts2 - ts1;
            ping.labels(address.toString()).set(latency);
        }
    }
}
