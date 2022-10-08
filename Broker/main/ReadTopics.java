
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ReadTopics {

    public ReadTopics() {

    }

    String[] tokenizer(String line) {
        String[] tokens = line.split(",");
        for (int i=0; i<line.length(); i++)
            System.out.println(tokens[i]);

        return tokens;
    }
    ArrayList<String> txt = new ArrayList<String>();

    public ArrayList<String> getTopics_all() {
        return topics_all;
    }

    ArrayList <String> topics_all = new ArrayList <String>();
    void readFile(String path) {


        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            txt.add(line);
            topics_all.add(line);
            while (line != null) {
                line = reader.readLine();
                txt.add(line);
                topics_all.add(line);
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    String[] final_tokens() {
        String[] final_txt = new String[txt.size()-1];

        int i = 0;
        for (i=0; i<txt.size()-1; i++) {
            final_txt[i]=txt.get(i);

        }
        System.out.println("i: " + i + " completed...");
        return final_txt;
    }

}
