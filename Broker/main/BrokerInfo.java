
import java.io.Serializable;
import java.util.*;

public class BrokerInfo extends Object implements Serializable {

    public String ip;
    private static final long serialVersionUID = 1L;
    public ArrayList <String> responsibleForTopics=new ArrayList <String>();

    public void setIp (String ip) {

        this.ip = ip;
    }

    public String getIp(){
        return ip; }

    public boolean isResponsible(String topic){
        return responsibleForTopics.contains(topic);
    }

}