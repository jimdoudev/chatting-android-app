package com.assignment.dsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Conversations extends AppCompatActivity {

    ListView LvConversations;
    SQLiteDatabase DB;
    ArrayList<String> topics = new ArrayList<>();
    ArrayAdapter<String> mArrayAdapter;
    ImageButton BtAddTopic;
    TextView TvInfo;
    String RootIP;
    int Port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hide the title bar
        getSupportActionBar().hide();
        setContentView(R.layout.activity_conversations);
        BtAddTopic = findViewById(R.id.BtAddTopic);
        LvConversations = findViewById(R.id.LvConversations);
        TvInfo = findViewById(R.id.TvInfo);
        RootIP = this.getIntent().getStringExtra("RootIP");
        Port = this.getIntent().getIntExtra("Port", 0);
        DB = SQLiteDatabase.openDatabase(getApplicationContext().getFilesDir()+"/DSApp.db", null, 0);
        this.getSubscribedTopics(DB);
        if(topics.size() == 0) {
            TvInfo.setVisibility(View.VISIBLE);
        }
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, topics);
        LvConversations.setAdapter(mArrayAdapter);
        BtAddTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Intent intent = new Intent(getApplicationContext(), AvailableTopics.class);
                intent.putExtra("RootIP", RootIP);
                intent.putExtra("Port", Port);
                startActivity(intent);
                finish();
            }
        });

        LvConversations.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = LvConversations.getItemAtPosition(position);
                String str = o.toString(); //As you are using Default String Adapter
                Toast.makeText(getBaseContext(),str,Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MessageList.class);
                intent.putExtra("Topic", str);
                intent.putExtra("RootIP", RootIP);
                intent.putExtra("Port", Port);
                startActivity(intent);
                finish();
            }
        });
    }

    //get topics already subscribed to from the database
    void getSubscribedTopics (SQLiteDatabase DB){
        Cursor Cur = DB.rawQuery("SELECT TopicName FROM Sub_Topics", null);
        if(Cur.moveToFirst()) {
            do{
                topics.add(Cur.getString(0));
            } while (Cur.moveToNext());
            Cur.close();
        }
    }

    //flow of the app when going back
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), WelcomePage.class);
        startActivity(intent);
        finish();
    }

}