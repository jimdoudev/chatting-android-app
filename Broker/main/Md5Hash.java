
import java.math.BigInteger;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Hash {

    private final SortedMap<Long, String> circle = new TreeMap<Long, String>();

    public Md5Hash(ArrayList<BrokerInfo> nodes) {

        for (int i=0;i<nodes.size();i++) {

            add(nodes.get(i).ip);

        }
    }
    public Long md5 (String ip){

        BigInteger bi=null;
        MessageDigest messageDigest;
        try{
            messageDigest = MessageDigest.getInstance("MD5");

            messageDigest.update(ip.getBytes());
            byte[] messageDigestMD5 = messageDigest.digest();
            StringBuffer stringBuffer = new StringBuffer();
            for (byte bytes : messageDigestMD5) {
                stringBuffer.append(String.format("%02x", bytes & 0xff));
            }
            String temp= stringBuffer.toString();
            bi = new BigInteger(temp, 16);

        } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
        }
        return bi.longValue();


    }

    public void add(String ip) {

        circle.put(md5(ip),ip);

    }

    public void remove(BrokerInfo node) {

        circle.remove(md5(node.ip));

    }

    public String get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        //key=String.valueOf(Integer.parseInt(key)*Integer.parseInt(key)*Integer.parseInt(key)*Integer.parseInt(key)*Integer.parseInt(key)); // ypsonoyme stin 5h gia na dimioyrgisoyme megalytero gap sto LineId oste na hasharistei pio sosta
        long hash = md5(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Long, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }
}