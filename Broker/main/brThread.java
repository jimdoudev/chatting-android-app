
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.assignment.library.*;


public class brThread implements Runnable{
    Broker broker;

    public brThread(Broker broker) {
        this.broker = broker;
    }

    public void run() {
        ServerSocket providedSocket = null;
        Socket connection = null;
        String message = null;

        try {
            providedSocket = new ServerSocket(Broker.port);
            while (true) {

                connection = providedSocket.accept(); //accepts a connection
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

                try {
                    message = (String) in .readObject();
                    //distributes broker list to servers
                    if (message.equals("I need info!")) {
                        out.writeObject(broker.Broker_list);
                        out.flush();
                        System.out.println("Here's the Broker List!");
                    } 
                    //distributes broker list to client
                    else if (message.equals("I need broker info!")) {
                        out.writeObject(broker.BrokerListClient);
                        out.flush();
                        System.out.println("Here's the Broker List!");
                    } 
                    //distributes topics list
                    else if(message.equals("I need the topics list")){
                        out.writeObject(Broker.AvailableTopics);
                        out.flush();
                        System.out.println("Here's the Topics List!");
                    }
                    //Creates unique profile IDs on root broker
                    else if (message.equals("I need a profile ID")){
                        int profileID = broker.IDcounter++;
                        String profileName = (String) in.readObject();
                        out.writeObject(profileID);
                        out.flush();
                        broker.profiles.add(new Profile (profileName, profileID));
                        //broker.userQueues.put(profileID, new ArrayList<Value>());
                        broker.profiles.get(profileID-1).setUserQueue(new ArrayList<Value>());

                        System.out.println("New profile added!" + broker.profiles.get(profileID-1));

                    } 
                    //Creates unique value IDs on root broker
                    else if (message.equals("I need a value ID")){
                        int valueID = broker.ValueIDCounter++;
                        out.writeObject(valueID);
                        out.flush();
                        System.out.println("New value ID!" + valueID);
                        
                    } 
                    //Connects brokers to root broker
                    else if (message.equals("I am a Broker")) {
                        out.writeObject("Connection successful");
                        out.flush();
                        System.out.println("New Broker connected");
                        BrokerInfo broker_info=(BrokerInfo)in.readObject();  //waits for broker to send his info
                        broker.Broker_list.add(broker_info);

                    } 
                    //Creates subThread for subscribers
                    else if (message.equals("I want to subscribe to a topic")) {
                        String topic = (String) in.readObject();
                        Profile profile = (Profile) in.readObject();
                        //Sub Thread takes control
                        subThread t = new subThread(connection, in , out, topic, broker, profile.ProfileID);
                        t.start();
                        if(broker.registeredUsers.get(profile.ProfileID) == null) {
                            broker.registeredUsers.put(profile.ProfileID, new ArrayList<subThread>());
                        }
                        //adding the thread to the list
                        broker.registeredUsers.get(profile.ProfileID).add(t); 
                        //Checking if profile exists on broker and adding it
                        boolean exists = false;
                        int index = 0;
                        for(int i = 0; i < broker.profiles.size(); i++) {
                            if(broker.profiles.get(i).ProfileID == profile.ProfileID) {
                                exists = true;
                                index = i;
                                break;
                            }
                            index = i;
                        }

                        if(exists == false) {
                            broker.profiles.add(profile);
                            if(index != 0){
                               index += 1;
                            }
                            broker.profiles.get(index).setUserQueue(new ArrayList<Value>());
                        }
                        if(!broker.profiles.get(index).Subscribed_To.contains(topic)){
                            broker.profiles.get(index).Subscribed_To.add(topic);
                        }
                        //Checking if there are any messages for reconnecting subscribers and distributes them
                        int size = broker.profiles.get(index).UserQueue.size();
                        for(int i = 0; i < size; i++) {
                            if(broker.profiles.get(index).UserQueue.get(i).topic.equals(t.topic)){
                                try{
                                    t.queue.put(broker.profiles.get(index).UserQueue.get(i));
                                    t.wakeUp();
                                    broker.profiles.get(index).UserQueue.remove(i);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                                }
                        }
                    }

                    //Creates a pubThread for new publishers
                    } else if(message.equals("I want to post a message")){
                        int profileID = (int) in.readObject();
                        //String topic = (String) in.readObject();
                        pubThread t = new pubThread(connection, in, out, broker, profileID);
                        //Pub Thread takes control
                        t.start();
                        broker.registeredPublishers.add(t); //adding the publisher to the pub Buffer!!
                    }

                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (ClassNotFoundException cnf) {
                    System.out.println("Class not found exception thrown.");
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}