import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.assignment.library.*;


public class Broker {
    static String root_ip = "192.168.1.205"; //ip of the root broker of our system

    public static void setPort(int port) {
        Broker.port = port;
    }

    static int port;

    ArrayList < pubThread > registeredPublishers = new ArrayList < pubThread > ();
    ArrayList < BrokerInfo > Broker_list = new ArrayList < BrokerInfo > ();
    public static ArrayList <String> AvailableTopics = new ArrayList<String>();
    int IDcounter = 1;
    ArrayList <Profile> profiles = new ArrayList<>();
    HashMap <String, ArrayList < String > > BrokerListClient = new HashMap<>();
    HashMap <Integer, ArrayList < subThread > > registeredUsers = new HashMap<>();
    int ValueIDCounter = 1;
    BrokerInfo Broker_info;
    BlockingQueue<Value> brQueue = new LinkedBlockingQueue<Value>();



    //Main function
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Scanner temp = new Scanner(System.in);
        System.out.print("Insert port num:");
        int num = temp.nextInt();
        Broker server=new Broker(num);
        String[] Topics=server.parseTopicTxt("/Users/ddoumpis/Desktop/Εργασία Μαρία/Java/topics.txt");

        for(int i=0;i<Topics.length;i++){
            System.out.print(Topics[i]+" ");
            System.out.println(Topics[i]);
            AvailableTopics.add(Topics[i]);

        }
        System.out.println(" ");
        System.out.print("Are you root?(y/n):  ");
        String answer=sc.nextLine();
        if(answer.equals("n")){
            server.informRoot(root_ip); //inform the root broker we are online
            System.out.println("Press any key when root is ready to start hashing..");
            sc.nextLine();

            server.getInfo();
            server.openBroker();
        }else if(answer.equals("y")){
            server.openBroker();
            server.Broker_list.add(server.Broker_info); //root broker adds connected brokers to the broker list
            for(int i=0;i<server.Broker_list.size();i++) {
                System.out.println(server.Broker_list.get(i).ip);
            }
            System.out.println("Press any button to start hashing.. ");
            sc.nextLine();
            server.Hash(Topics);
            for(int i = 0; i < server.Broker_list.size();i++) {
                server.BrokerListClient.put(server.Broker_list.get(i).getIp(), new ArrayList<String>() );
                for(int j = 0; j < server.Broker_list.get(i).responsibleForTopics.size(); j++) {
                    server.BrokerListClient.get(server.Broker_list.get(i).getIp()).add(server.Broker_list.get(i).responsibleForTopics.get(j));
                }
            }
        }
        System.out.println(" ");
        System.out.println("Broker is online and awaiting connections..");
        System.out.println(" ");
        server.sendToUser();
    }

    //constructor functions
    public Broker(){
        Broker_info = new BrokerInfo();
        this.port = 4321;
        String ip; //this specific broker's ip
        try {
            ip = InetAddress.getLocalHost().getHostAddress();//gets this pc Ip
            Broker_info.setIp(ip);
            System.out.println(ip);
        } catch (UnknownHostException ex) {
            System.out.println("Error fetching broker's IP Address");
        }
    }

    public Broker(int port){
        Broker_info = new BrokerInfo();
        this.port = port;
        String ip; //this specific broker's ip
        try {
            ip = InetAddress.getLocalHost().getHostAddress();//gets this pc Ip
            Broker_info.setIp(ip);
            System.out.println(ip);
        } catch (UnknownHostException ex) {
            System.out.println("Error fetching broker's IP Address");
        }
    }

    //parses topics from .txt file
    public String[] parseTopicTxt(String path){
        ReadTopics temp=new ReadTopics();
        temp.readFile(path);
        return temp.final_tokens();
    }
    
    //root broker hashes available topics and ips and creates hash ring
    public void Hash(String[] all_Topics){ 

        Md5Hash Md5Hash = new Md5Hash(Broker_list); //creates hash ring
        String ip;
        for (int i=0;i<all_Topics.length;i++) {
            ip=Md5Hash.get(all_Topics[i]); //hash
            for(int j=0;j<Broker_list.size();j++){
                if(ip.equals(Broker_list.get(j).ip)){ 
                    if(!Broker_list.get(j).responsibleForTopics.contains(all_Topics[i])) {
                        Broker_list.get(j).responsibleForTopics.add(all_Topics[i]);
                        System.out.println("Broker:"+ Broker_list.get(j).ip+" is responsible for Topics: "+all_Topics[i]);
                    }
                }
            }
        }
    }



    //Pushes values (when available) to the right users
    public void sendToUser() { 
        while (true) {
            Value v = brQueue.poll();
            if (v != null) {
                for (int i = 0; i < profiles.size(); i++) {
                    for(int j = 0; j < profiles.get(i).Subscribed_To.size(); j++) {
                        if (v.topic.equals(profiles.get(i).Subscribed_To.get(j))) {
                            try {

                                boolean sent = false;

                                for(int k = 0; k < registeredUsers.get(profiles.get(i).ProfileID).size(); k++) {
                                    int ID = profiles.get(i).ProfileID;
                                    subThread s = registeredUsers.get(ID).get(k);
                                    String stopic = s.topic;
                                    String vtopic = v.topic;
                                    if(stopic.equals(vtopic)) {
                                        registeredUsers.get(profiles.get(i).ProfileID).get(k).queue.put(v);
                                        registeredUsers.get(profiles.get(i).ProfileID).get(k).wakeUp();
                                        sent = true;
                                    }
                                }
                                if(sent == false) {
                                    profiles.get(i).UserQueue.add(v);
                                }
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        }
    }


    //brokers inform root of their broker info
    public void informRoot(String ip){
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        String message = "failed";
        try {
            requestSocket = new Socket(InetAddress.getByName(ip),port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("I am a Broker");
            out.flush();
            message = (String) in.readObject();
            System.out.println(message);
            out.writeObject((BrokerInfo)Broker_info);
            out.flush();

        } catch (UnknownHostException unknownHost) {
            System.err.println("Error: Unknown host");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException cnf){
            System.out.println("Error: Class not found");
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    //brokers reguest info (broker list) from root
    public void getInfo(){
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        //String message = "failed";
        try {
            requestSocket = new Socket(InetAddress.getByName(root_ip), port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("I need info!");
            out.flush();

            Broker_list = (ArrayList <BrokerInfo>) in.readObject();
            System.out.println("I got the Broker List");

        } catch (UnknownHostException unknownHost) {
            System.err.println("Error: Unknown host");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException cnf){
            System.out.println("Error: Class not found.");
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    //after successful hashing creates a broker thread (brThread) for each broker
    public void openBroker() {
        brThread t=new brThread(this); //stand-by broker threads
        Thread tr=new Thread(t);
        tr.start();
    }

}