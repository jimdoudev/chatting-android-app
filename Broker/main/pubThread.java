
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.assignment.library.*;

public class pubThread extends Thread {
    final ObjectInputStream in;
    final ObjectOutputStream out;
    final Socket s;
    Broker myBroker;
    int profileID;

    public pubThread(Socket connection,ObjectInputStream in, ObjectOutputStream out,Broker br, int profileID){
        this.in = in;
        this.out = out;
        this.s = connection;
        myBroker=br;
        this.profileID = profileID;
    }

    public void run() {
        try {
            out.writeObject("Connection successful!");
            out.flush();
            System.out.println("A new Publisher connected");
            System.out.println(" ");
            while(true) {
                Value v=(Value)in.readObject();
                try {
                    myBroker.brQueue.put(v);
                } catch (InterruptedException ex) {
                    Logger.getLogger(pubThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }  catch (ClassNotFoundException ex) {
            Logger.getLogger(pubThread.class.getName()).log(Level.SEVERE, null, ex);
        } 
        //if this exception occurs we delete our subscriber because connection lost or he shut down the connection and delete any
        //available subThread too
        catch(SocketException se){ 
            System.out.println("Publisher disconnected");
            myBroker.registeredPublishers.remove(this);
            for(int i = 0; i < myBroker.registeredUsers.get(profileID).size(); i++) {
                    myBroker.registeredUsers.get(profileID).remove(i);
                    System.out.println("Subscriber disconnected");
            }
        }
        //if this exception occurs we delete our subscriber because connection lost or he shut down the connectionnd delete any
        //available subThread too
        catch(IOException io){                    
            System.out.println("Publisher disconnected");
            myBroker.registeredPublishers.remove(this);
            for(int i = 0; i < myBroker.registeredUsers.get(profileID).size(); i++) {
                    myBroker.registeredUsers.get(profileID).remove(i);
                    System.out.println("Subscriber disconnected");
            }
        }
    }
    //end run
}