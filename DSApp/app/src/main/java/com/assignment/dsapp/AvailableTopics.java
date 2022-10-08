package com.assignment.dsapp;

import static java.lang.System.err;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class AvailableTopics extends AppCompatActivity {

    ListView LvAvailableTopics;
    SQLiteDatabase DB;
    ArrayList<String> topics = new ArrayList<>();
    ArrayAdapter<String> mArrayAdapter;
    String ChosenTopic;
    ObjectOutputStream out;
    ObjectInputStream in;
    Socket requestSocket;
    String RootIP;
    int Port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hide the title bar
        getSupportActionBar().hide();
        setContentView(R.layout.activity_available_topics);
        LvAvailableTopics = findViewById(R.id.LvAvailableTopics);
        RootIP = this.getIntent().getStringExtra("RootIP");
        Port = this.getIntent().getIntExtra("Port", 0);
        DB = SQLiteDatabase.openDatabase(getApplicationContext().getFilesDir()+"/DSApp.db", null, 0);
        //gets available topics from root server
        TopicGetter topicGetter = new TopicGetter();
        topicGetter.execute();
        try {
            topics = topicGetter.get();
            mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, topics);
            LvAvailableTopics.setAdapter(mArrayAdapter);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LvAvailableTopics.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = LvAvailableTopics.getItemAtPosition(position);
                ChosenTopic = o.toString();
                if(checkSubscription(DB, ChosenTopic)) {
                    Toast.makeText(getBaseContext(),"Already Subscribed to " + ChosenTopic,Toast.LENGTH_SHORT).show();
                } else {
                    addToSubscribedTopics(DB, ChosenTopic);
                    Toast.makeText(getBaseContext(),"Subscribed to " + ChosenTopic,Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(getApplicationContext(), MessageList.class);
                intent.putExtra("Topic", ChosenTopic);
                intent.putExtra("RootIP", RootIP);
                intent.putExtra("Port", Port);
                startActivity(intent);
                finish();
            }
        });
    }

    //AsyncTask to get the available topics from the root server
    private class TopicGetter extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            Socket requestSocket = null;
            ArrayList<String> temp = new ArrayList<>();

            try {

                requestSocket = new Socket(InetAddress.getByName(RootIP), Port);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeObject("I need the topics list");
                out.flush();
                temp = (ArrayList<String>) in.readObject();

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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), Conversations.class);
        intent.putExtra("RootIP", RootIP);
        intent.putExtra("Port", Port);
        startActivity(intent);
        finish();
    }

    //adds a new topic to the subscriptions in the database
    void addToSubscribedTopics (SQLiteDatabase DB, String str) {
        SQLiteStatement stmt = DB.compileStatement("INSERT INTO Sub_Topics (TopicName) VALUES (?)");
        stmt.bindString(1, str);
        stmt.executeInsert();
    }

    //checks if already subscribed to provided topic
    boolean checkSubscription (SQLiteDatabase DB, String str) {
        boolean flag = false;
        Cursor Cur = DB.rawQuery("SELECT TopicName FROM Sub_Topics", null);
        if(Cur.moveToFirst()) {
            do{
                if(Cur.getString(0).equals(str)) {
                    flag = true;
                }
            } while (Cur.moveToNext());
            Cur.close();
        }
        return flag;
    }
}