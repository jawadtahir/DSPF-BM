package org.apache.flink.playgrounds.ops.clickcount;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Playground {

    public static void main (String[] args) throws IOException {
        Socket sock = new Socket("clouddatabases.msrg.in.tum.de", 5551);
        sock.getOutputStream().write("03694196".getBytes(StandardCharsets.UTF_8));

        byte[] res = new byte [100];

        sock.getInputStream().read(res);

        System.out.println(new String(res, StandardCharsets.UTF_8));
        sock.close();

    }
}
