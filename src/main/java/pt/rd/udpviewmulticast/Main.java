package pt.rd.udpviewmulticast;

import pt.rd.udpviewmulticast.communication.Channel;
import pt.rd.udpviewmulticast.communication.channels.UnreliableChannel;
import pt.rd.udpviewmulticast.communication.packets.PacketHello;
import pt.rd.udpviewmulticast.communication.packets.PacketRegistry;
import pt.rd.udpviewmulticast.structures.View;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Main {

    public static void main(String[] _args) throws UnknownHostException {
        PacketRegistry.registerPacket((byte) 1, PacketHello.class);

        UUID id = UUID.randomUUID();

        View basicView = new View(
                10,
                null,
                new ArrayList<>(),
                InetAddress.getByName("230.0.0.0")
        );

        Channel channel = new UnreliableChannel();
        Thread thread = new Thread(() -> {
            channel.open(basicView);
            channel.listenForPackets();
        });

        thread.start();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("> ");

            String line;
            while((line = scanner.nextLine()) != null) {
                String[] split = line.split(" ");
                String[] args = Arrays.copyOfRange(split, 1, split.length);

                switch(split[0].toLowerCase()) {
                    case "id": {
                        System.out.println(id.toString());
                        break;
                    }

                    case "ip": {
                        try {
                            System.out.println(InetAddress.getLocalHost().getHostAddress());
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        break;
                    }

                    case "ping": {
                        if (args.length < 1) {
                            System.out.println("Use 'ping <host>.'");
                            break;
                        }

                        try {

                            System.out.print("pinging... ");
                            boolean reachable = InetAddress.getByName(args[0]).isReachable(5000);
                            System.out.println(reachable ? "success" : "failed");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        break;
                    }

                    case "hello": {
                        if (args.length < 1) {
                            System.out.println("Use 'hello <NUM>.'");
                            break;
                        }

                        try {
                            int num = Integer.parseInt(args[0]);
                            PacketHello packet = new PacketHello(num, "Hello World!");
                            channel.send(packet);
                        } catch(NumberFormatException ex) {
                            System.out.println("Invalid number");
                        }

                        break;
                    }
                }

                System.out.print("> ");

            }
        } catch(NoSuchElementException ignored) {
            System.out.println("goodbye");
        }

        channel.close();
    }

}
