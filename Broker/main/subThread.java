
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.assignment.library.*;

public class subThread extends Thread{
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket s;
    Broker myBroker;
    String topic;
    BlockingQueue<Value> queue = new LinkedBlockingQueue<Value>();
    int profileID;

    public subThread(Socket connection,ObjectInputStream in, ObjectOutputStream out,String topic,Broker br, int ProfileID){
        this.in = in;
        this.out = out;
        this.s = connection;
        this.topic=topic;
        myBroker=br;
        profileID = ProfileID;
    }

    public String getTopic(){
        return topic;
    }
    //@Override

    public void run() {
        try{
            out.writeObject("Connection Successful!");
            out.flush();
            System.out.println("Subscriber connected");
            System.out.println(" ");

            while (true){
                Value v=queue.poll();
                if(v!=null){
                    Value temp=new Value(v);
                    out.writeObject(temp);

                }
                synchronized(this){
                    this.wait(); //thread sleeps until notify all is called.
                }

            }

        } catch(SocketException se){
            System.out.println("Subscriber disconnected");
            myBroker.registeredUsers.get(profileID).remove(this);
        }catch(IOException io){
            System.out.println("Subscriber disconnected");
            myBroker.registeredUsers.get(profileID).remove(this);
        } catch (InterruptedException ex) {
            Logger.getLogger(subThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void wakeUp(){
        synchronized(this){
            this.notify();
        }
    }

}