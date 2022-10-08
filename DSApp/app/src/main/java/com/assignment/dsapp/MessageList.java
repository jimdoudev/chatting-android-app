package com.assignment.dsapp;

import static java.lang.System.err;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import com.assignment.library.*;

import org.apache.commons.io.FileUtils;

public class MessageList extends AppCompatActivity {
    private RecyclerView RvMessages;
    private MessageListAdapter MessageAdapter;
    ImageButton BtSend;
    ImageButton BtAddFile;
    SQLiteDatabase DB;
    EditText EtMessage;
    ArrayList<ChatMessage> chatMessageList = new ArrayList<>();
    int UserID;
    String UserName;
    String Topic;
    String RootIP;
    int Port;
    Profile profile;
    HashMap<String, ArrayList<String>> BrokerList = new HashMap<>();
    HashMap<String, String> extensions = new HashMap<>();
    ObjectOutputStream SubOut;
    ObjectInputStream SubIn;
    Socket SubSocket;
    ObjectOutputStream PubOut;
    ObjectInputStream PubIn;
    Socket PubSocket;
    String dateSnap;
    String timeSnap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hide the title bar
        getSupportActionBar().hide();
        setContentView(R.layout.activity_message_list);
        RvMessages = (RecyclerView) findViewById(R.id.RvMessages);
        BtAddFile = findViewById(R.id.BtAddFile);
        BtSend = findViewById(R.id.BtSend);
        EtMessage = findViewById(R.id.EtMessage);
        RootIP = this.getIntent().getStringExtra("RootIP");
        Port = this.getIntent().getIntExtra("Port", 0);
        InfoGetter infoGetter = new InfoGetter();
        infoGetter.execute();
        try {
            BrokerList = infoGetter.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DB = SQLiteDatabase.openDatabase(getApplicationContext().getFilesDir()+"/DSApp.db", null, 0);
        Topic = this.getIntent().getStringExtra("Topic");
        this.getPersonalData(DB);
        profile = new Profile(UserName, UserID);
        this.getMessages(DB);

        SubscribeToBroker subscribeToBroker = new SubscribeToBroker();
        subscribeToBroker.execute();

        PublishToBroker publishToBroker = new PublishToBroker();
        publishToBroker.execute();

        MessageAdapter = new MessageListAdapter(this, chatMessageList);
        RvMessages.setLayoutManager(new LinearLayoutManager(this));
        RvMessages.setAdapter(MessageAdapter);
        RvMessages.scrollToPosition(chatMessageList.size() - 1);
        BtAddFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                showFileChooser();
            }
        });

        BtSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                if(!EtMessage.getText().toString().equals("")){
                    getDate();
                    GetValueID getValueID = new GetValueID();
                    getValueID.execute();
                    int valueID = 0;
                    try {
                        valueID = getValueID.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Value value = new Value(profile, valueID);
                    value.Date = dateSnap;
                    value.Time = timeSnap;
                    value.setTopic(Topic);
                    value.setMessage(EtMessage.getText().toString());
                    value.multimediaFileChunk = new MultimediaFile("", profile);
                    EtMessage.setText("");
                    pushToBroker(value);
                }
            }
        });

    }

    //leads to android file chooser
    public void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        // Update with mime types
        intent.setType("*/*");

        // Picks openable and local files
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        startActivityForResult(intent, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the user doesn't pick a file just return
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1 || resultCode != RESULT_OK) {
            return;
        }

        // Import the file to desired location
        try {
            importFile(data.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void importFile(Uri uri) throws IOException {
        String fileName = getFileName(uri);

        // The temp file could be whatever you want
        File tempFile = new File(getApplicationContext().getFilesDir() + "/" +fileName);
        copyToTempFile(uri, tempFile);
        //Prepares to send the file
        getDate();
        GetValueID getValueID = new GetValueID();
        getValueID.execute();
        int valueID = 0;
        try {
            valueID = getValueID.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Value value = new Value(profile, Topic, valueID);
        value.Date = dateSnap;
        value.Time = timeSnap;
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(tempFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList < Value > contentToSend;
        value.multimediaFileChunk = new MultimediaFile(fileName, profile);
        value.setFileLength(fileContent.length);
        value.setNumberOfChunks((value.fileLength + (512 * 1024))/ (512 * 1024));
        contentToSend = setChunkToValue(value, chunkContent(fileContent, value.numberOfChunks));
        //sends the file chunks to the broker
        for(int i=0;i<contentToSend.size();i++) {
            pushToBroker(contentToSend.get(i));
        }
    }

    //Obtains the Uri using content resolvers
    private String getFileName(Uri uri) throws IllegalArgumentException {
        // Obtain a cursor with information regarding this uri
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor.getCount() <= 0) {
            cursor.close();
            throw new IllegalArgumentException("Can't obtain file name, cursor is empty");
        }

        cursor.moveToFirst();

        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

        cursor.close();

        return fileName;
    }

    //Copies a Uri reference to a temp file
    private File copyToTempFile(Uri uri, File tempFile) throws IOException {
        // Obtain an input stream from the uri
        InputStream inputStream = getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            throw new IOException("Unable to obtain input stream from URI");
        }

        // Copy the stream to the temp file
        FileUtils.copyInputStreamToFile(inputStream, tempFile);

        return tempFile;
    }

    //Forwards incoming messages from the message getter thread
    public void newMessage (ChatMessage message) {
        Message msg = new Message ();
        Bundle bun = new Bundle ();
        bun.putSerializable ("Message", message);
        msg.setData (bun);
        MyHandler.sendMessage (msg);
    }

    //Handles incoming messages from the message getter thread
    //and adds them to the adapter on the main thread
    Handler MyHandler = new Handler ()
    {
        @Override
        public void handleMessage (Message Mess)
        {
            Bundle b = Mess.getData ();
            ChatMessage tbp = (ChatMessage) b.getSerializable("Message");
            chatMessageList.add(tbp);
            MessageAdapter.notifyDataSetChanged();
            RvMessages.scrollToPosition(chatMessageList.size() - 1);
        }
    };

    //Works as an adapter for the activity recycle view. Checks
    //if the messages are sent or received and inflates the appropriate layout
    public class MessageListAdapter extends RecyclerView.Adapter {
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        private Context Context;
        private ArrayList<ChatMessage> MessageList;

        public MessageListAdapter(Context context, ArrayList<ChatMessage> messageList) {
            Context = context;
            MessageList = messageList;
        }

        @Override
        public int getItemCount() {
            return MessageList.size();
        }

        // Determines the appropriate ViewType according to the sender of the message.
        @Override
        public int getItemViewType(int position) {
            ChatMessage chatMessage = (ChatMessage) MessageList.get(position);

            if (chatMessage.SenderID == UserID) {
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }

        // Inflates the appropriate layout according to the ViewType.
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageHolder(view);
            }

            return null;
        }

        // Passes the message object to a ViewHolder so that the contents can be bound to UI.
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ChatMessage chatMessage = (ChatMessage) MessageList.get(position);

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(chatMessage);
                    if(chatMessage.message.equals("multimedia file")) {
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String ext = chatMessage.FileName.substring(chatMessage.FileName.lastIndexOf(".") + 1, chatMessage.FileName.length());
                                //Uri uri = Uri.fromFile(new File(getApplicationContext().getFilesDir() + "/" + chatMessage.FileName));
                                File temp = new File(getApplicationContext().getFilesDir() + "/" + chatMessage.FileName);
                                Uri uri = FileProvider.getUriForFile(com.assignment.dsapp.MessageList.this, com.assignment.dsapp.MessageList.this.getApplicationContext().getPackageName() + ".provider", temp);
                                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(uri, mimeType);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                                Toast.makeText(getBaseContext(),"Opening file at " + chatMessage.FileName,Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(chatMessage);
                    if(chatMessage.message.equals("multimedia file")) {
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String ext = chatMessage.FileName.substring(chatMessage.FileName.lastIndexOf(".") + 1, chatMessage.FileName.length());
                                //Uri uri = Uri.fromFile(new File(getApplicationContext().getFilesDir() + "/" + chatMessage.FileName));
                                File temp = new File(getApplicationContext().getFilesDir() + "/" + chatMessage.FileName);
                                Uri uri = FileProvider.getUriForFile(com.assignment.dsapp.MessageList.this, com.assignment.dsapp.MessageList.this.getApplicationContext().getPackageName() + ".provider", temp);
                                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(uri, mimeType);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(intent);
                                Toast.makeText(getBaseContext(),"Opening file at " + chatMessage.FileName,Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
            }
        }

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, dateText;

            SentMessageHolder(View itemView) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_me);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
            }

            void bind(ChatMessage chatMessage) {
                if(chatMessage.message.equals("multimedia file")) {
                    messageText.setText(chatMessage.FileName);
                } else {
                    messageText.setText(chatMessage.message);
                }
                timeText.setText(chatMessage.Time);
                dateText.setText(chatMessage.Date);
            }
        }

        private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, dateText, nameText;

            ReceivedMessageHolder(View itemView) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_other);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
            }

            void bind(ChatMessage chatMessage) {
                if(chatMessage.message.equals("multimedia file")) {
                    messageText.setText(chatMessage.FileName);
                } else {
                    messageText.setText(chatMessage.message);
                }
                dateText.setText(chatMessage.Date);
                timeText.setText(chatMessage.Time);timeText.setText(chatMessage.Time);
                nameText.setText(chatMessage.SenderName);
            }
        }
    }

    //Gets saved messages from the database
    void getMessages (SQLiteDatabase DB){
        Cursor Cur = DB.rawQuery("SELECT ID, SenderID, SenderName, MesDate, Time, TopicName, ValueID, Message, FileName, NoChunks FROM Messages", null);
        if(Cur.moveToFirst()) {
            do{
                if(Cur.getString(5).equals(Topic)) {
                    if((Cur.getInt(9) != 0) && (checkMultimedia(DB, Cur.getInt(6)) == Cur.getInt(9)) && (!inMessageList(chatMessageList, Cur.getInt(6)))) {
                        chatMessageList.add(new ChatMessage(Cur.getInt(0), Cur.getInt(1), Cur.getString(2), Cur.getString(3), Cur.getString(4), Cur.getString(5), Cur.getInt(6), Cur.getString(7), Cur.getString(8), Cur.getInt(9)));
                    }else {
                        chatMessageList.add(new ChatMessage(Cur.getInt(0), Cur.getInt(1), Cur.getString(2), Cur.getString(3), Cur.getString(4), Cur.getString(5), Cur.getInt(6), Cur.getString(7)));
                    }
                }
            } while (Cur.moveToNext());
            Cur.close();
        }
    }

    //Checks if message exists in provided list
    boolean inMessageList(ArrayList<ChatMessage> list, int ValueID) {
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).ValueID == ValueID) {
                return true;
            }
        }
        return false;
    }

    //Adds a message to the database
    void addToMessages (SQLiteDatabase DB, ChatMessage message) {
        SQLiteStatement stmt = DB.compileStatement("INSERT INTO Messages (SenderID, SenderName, MesDate, Time, TopicName, ValueID, Message, FileContent, NoChunks, FileLength, FileName) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        stmt.bindLong(1, message.SenderID);
        stmt.bindString(2, message.SenderName);
        stmt.bindString(3, message.Date);
        stmt.bindString(4, message.Time);
        stmt.bindString(5, message.TopicName);
        stmt.bindLong(6, message.ValueID);
        stmt.bindString(7, message.message);
        if(message.FileContent != null) {
            stmt.bindBlob(8, message.FileContent);
        } else {
            stmt.bindNull(8);
        }
        if(message.FileContent != null) {
            stmt.bindLong(9, message.NoChunks);
        } else {
            stmt.bindLong(9, 0);
        }
        if(message.FileContent != null) {
            stmt.bindLong(10, message.fileLength);
        } else {
            stmt.bindLong(10, 0);
        }
        if(message.FileContent != null) {
            stmt.bindString(11, message.FileName);
        } else {
            stmt.bindNull(11);
        }

        stmt.executeInsert();
    }

    //Gets personal data from the database
    public void getPersonalData(SQLiteDatabase DB) {
        Cursor Cur = DB.rawQuery("SELECT ID, UserName FROM UserData", null);
        if(Cur.moveToFirst()) {
            UserID = Cur.getInt(0);
            UserName = Cur.getString(1);
            Cur.close();
        } else {
            UserID = 0;
            UserName = null;
        }
    }

    //Disconnects everything when going back
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), Conversations.class);
        intent.putExtra("RootIP", RootIP);
        intent.putExtra("Port", Port);
        startActivity(intent);
        try {
            disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    //Finds broker responsible for given topic
    public String findBroker(String topic_name) {
        for (String key: BrokerList.keySet()) {
            for(int i = 0; i < BrokerList.get(key).size(); i++){
                if (BrokerList.get(key).get(i).equals(topic_name)) {
                    return key;
                }
            }
        }
        return "not found";
    }

    //Disconnects everything
    public void disconnect() throws IOException {
        this.SubOut.close();
        this.SubIn.close();
        this.SubSocket.close();
        this.PubOut.close();
        this.PubIn.close();
        this.PubSocket.close();
    }

    //AsyncTask that gets broker info from the server
    private class InfoGetter extends AsyncTask<Void, Void, HashMap<String, ArrayList<String>>> {

        @Override
        protected HashMap<String, ArrayList<String>> doInBackground(Void... voids) {
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            Socket requestSocket = null;
            HashMap<String, ArrayList<String>> temp = new HashMap<>();

            try {

                requestSocket = new Socket(InetAddress.getByName(RootIP), Port);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeObject("I need broker info!");
                out.flush();
                temp = (HashMap<String, ArrayList<String>>) in.readObject();

            } catch (UnknownHostException unknownHost) {
                err.println("Unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException cnf) {
                cnf.printStackTrace();
                System.out.println("Class not found.");
            } finally {
                try {
                    in.close();
                    out.close();
                    requestSocket.close();

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            return temp;
        }
    }

    //AsyncTask that subThread on the broker
    private class SubscribeToBroker extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            String ip = findBroker(Topic);
            Socket rs = null;
            String serverResponse;

            try {

                rs = new Socket(InetAddress.getByName(ip), Port);

                oos = new ObjectOutputStream(rs.getOutputStream());
                ois = new ObjectInputStream(rs.getInputStream());

                oos.writeObject("I want to subscribe to a topic");
                oos.flush();
                oos.writeObject(Topic);
                oos.flush();
                oos.writeObject(profile);
                oos.flush();
                serverResponse = (String) ois.readObject();
                System.out.println(serverResponse);

                SubIn = ois;
                SubOut = oos;
                SubSocket = rs;

            } catch (UnknownHostException unknownHost) {
                err.println("Unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException cnf) {
                cnf.printStackTrace();
                System.out.println("Class not found.");
            }
            return null;
        }

    }

    //AsyncTask that opens a pubThread on the broker
    private class PublishToBroker extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            String ip = findBroker(Topic);
            Socket rs = null;
            String serverResponse;

            try {
                rs = new Socket(InetAddress.getByName(ip), Port);

                oos = new ObjectOutputStream(rs.getOutputStream());
                ois = new ObjectInputStream(rs.getInputStream());

                oos.writeObject("I want to post a message");
                oos.flush();
                oos.writeObject(UserID);
                oos.flush();
                serverResponse = (String) ois.readObject();
                System.out.println("2" + serverResponse);

                PubIn = ois;
                PubOut = oos;
                PubSocket = rs;

                MessageGetter messageGetter = new MessageGetter(MessageList.this, SubIn);
                messageGetter.start();

            } catch (UnknownHostException unknownHost) {
                err.println("Unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException cnf) {
                cnf.printStackTrace();
                System.out.println("Class not found.");
            }
            return null;
        }

    }

    //Thread that receives incoming messages
    class MessageGetter extends Thread {
        MessageList messageList;
        ObjectInputStream ois;
        public MessageGetter( MessageList m, ObjectInputStream in){
            this.ois = in;
            this.messageList = m;

        }
        public void run() {

            while (true) {
                try {
                    Value v = (Value) ois.readObject();
                    ChatMessage message;
                    if(!v.multimediaFileChunk.multimediaFileName.equals("")) {
                        message = new ChatMessage(v.profile.ProfileID, v.profile.ProfileName, v.Date, v.Time, v.topic, v.ID, v.message, v.multimediaFileChunk.multimediaFileChunkData, v.numberOfChunks, v.fileLength, v.multimediaFileChunk.multimediaFileName);
                        addToMessages(DB, message);
                        if(checkMultimedia(DB, message.ValueID) == message.NoChunks) {
                            ChunksToFile(message, DB);
                            messageList.newMessage(message);
                        }
                    } else {
                        message = new ChatMessage(v.profile.ProfileID, v.profile.ProfileName, v.Date, v.Time, v.topic, v.ID, v.message);
                        messageList.newMessage(message);
                        addToMessages(DB, message);
                    }

                } catch (SocketException socketException) {
                    socketException.printStackTrace();
                    break;
                } catch (IOException ex) {

                } catch (ClassNotFoundException ex) {

                } catch (NullPointerException nu) {
                    System.out.println("NullPointerException");
                    break;
                }
            }
        }
    }


    //Turns byte array to array of chunks of max 512kb
    public byte[][] chunkContent(final byte[] content, int chunks)
    {
        int length = content.length;

        byte[][] dest = new byte[chunks][];
        int destIndex = 0;
        int stopIndex = 0;

        for (int startIndex = 0; startIndex + 512*1024 <= length; startIndex += 512*1024)
        {
            stopIndex += 512*1024;
            dest[destIndex] = Arrays.copyOfRange(content, startIndex, stopIndex);
            destIndex++;
        }

        if (stopIndex < length)
            dest[destIndex] = Arrays.copyOfRange(content, stopIndex, length);

        return dest;
    }

    //Turns array of chunks to array of values/ assigns each chunk to a value
    public ArrayList < Value > setChunkToValue(Value v, byte[][] chunkContent){
        ArrayList < Value > valueChunks = new ArrayList < Value >();
        for(int i=0; i<chunkContent.length; i++) {
            Value temp = new Value(v);
            valueChunks.add(temp);
            int chunkLength = chunkContent[i].length;
            valueChunks.get(i).multimediaFileChunk.multimediaFileChunkData = new byte[chunkLength];
            for(int j=0; j < chunkLength; j++) {
                byte k = chunkContent[i][j];
                valueChunks.get(i).multimediaFileChunk.multimediaFileChunkData[j] = k;
            }
        }
        return valueChunks;
    }

    //Reconnects all chunks to become a new file
    public void ChunksToFile(ChatMessage message, SQLiteDatabase DB){ //combining chunks into a complete array
        byte[] temp = new byte[message.fileLength];
        ArrayList <byte[]> chunks = new ArrayList<>();
        Cursor Cur = DB.rawQuery("SELECT FileContent FROM Messages WHERE ValueID = ? ORDER BY ID", new String[] {Integer.toString(message.ValueID)});
        if(Cur.moveToFirst()) {
            do{
                chunks.add(Cur.getBlob(0));
            } while (Cur.moveToNext());
            Cur.close();
        }

        int i=0;
        int j=0;
        int k=0;

        while(k < chunks.size()  && i <= temp.length){
            while(j <= 512*1024 && j < chunks.get(k).length){
                temp[i] = chunks.get(k)[j];
                j++;
                i++;
            }
            j=0;
            k++;

        }

        File file = new File(getApplicationContext().getFilesDir() + "/" + message.FileName);

        FileOutputStream fos = null;

        try {

            fos = new FileOutputStream(file);

            // Writes bytes from the specified byte array to this file output stream
            fos.write(temp);

        }
        catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
        }
        catch (IOException ioe) {
            System.out.println("Exception while writing file " + ioe);
        }
        finally {
            // close the streams using close method
            try {
                if (fos != null) {
                    fos.close();
                }
                System.out.println("New incoming file from " + profile.ProfileName + " saved in home directory!");
            }
            catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }

        }

    }

    //Checks how many chunks with the same ValueID exist in the database
    public int checkMultimedia(SQLiteDatabase DB, int ValueID) {
        int entries = 0;
        Cursor Cur = DB.rawQuery("SELECT count(*) FROM Messages WHERE ValueID = ?", new String[] {Integer.toString(ValueID)});
        if(Cur.moveToFirst()) {
            entries = Cur.getInt(0);
            Cur.close();
        }
        return entries;
    }


    //Gets the current date and time and passes them to variables
    public void getDate() {
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        dateSnap = dateFormat.format(date);
        timeSnap = timeFormat.format(date);
    }


    //AsyncTask to get a new ValueID from the server
    private class GetValueID extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            Socket rs = null;
            int valueID = -1;

            try {
                rs = new Socket(InetAddress.getByName(RootIP), Port);

                oos = new ObjectOutputStream(rs.getOutputStream());
                ois = new ObjectInputStream(rs.getInputStream());

                oos.writeObject("I need a value ID");
                oos.flush();

                valueID = (int) ois.readObject();
                System.out.println("Here is the Value ID!");
                System.out.println(" ");

            } catch (UnknownHostException unknownHost) {
                err.println("Unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException cnf) {
                cnf.printStackTrace();
                System.out.println("Class not found.");
            }
            return valueID;
        }
    }

    //Pushes the provided value to the broker via the MessageThread
    public void pushToBroker(Value value) {
        String ip = findBroker(Topic);
        MessageThread t = new MessageThread(PubSocket, PubIn, PubOut, value);
        t.start();
    }

    //Thread that sends provided values to the connected server
    class MessageThread extends Thread{
        ObjectInputStream ois;
        ObjectOutputStream oos;
        Socket s;
        ArrayList<String> topics;
        BlockingQueue<Value> queue = new LinkedBlockingQueue<Value>();
        Value val;

        public MessageThread(Socket connection,ObjectInputStream in, ObjectOutputStream out,Value value){
            this.ois = in;
            this.oos = out;
            this.s = connection;
            this.val =value;
        }

        @Override
        public void run() {
            Value temp = new Value(val);
            try {
                oos.writeObject((Value) temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}