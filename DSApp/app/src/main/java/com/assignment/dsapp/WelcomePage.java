package com.assignment.dsapp;

import static java.lang.System.err;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import com.assignment.library.*;

public class WelcomePage extends AppCompatActivity {
    EditText EtUserName;
    Button BtStart;
    TextView TvWelcome;
    TextView TvEnterUsername;
    SQLiteDatabase DB;
    int UserID;
    String UserName;
    Profile profile;
    ObjectOutputStream out;
    ObjectInputStream in;
    Socket requestSocket;
    static String root_ip="192.168.1.207";
    static int port=4321;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hide the title bar
        getSupportActionBar().hide();
        setContentView(R.layout.welcome_page);
        EtUserName = findViewById(R.id.EtUserName);
        BtStart = findViewById(R.id.BtStart);
        TvWelcome = findViewById(R.id.TvWelcome);
        TvEnterUsername = findViewById(R.id.TvEnterUsername);
        if(!FileExists("DSApp.db")) {
            CopyDB("DSApp.db");
        }
        DB = SQLiteDatabase.openDatabase(getApplicationContext().getFilesDir()+"/DSApp.db", null, 0);
        this.getPersonalData(DB);
        //Case user is connecting for the first time
        if(UserID != 0) {
            EtUserName.setVisibility(View.INVISIBLE);
            TvEnterUsername.setVisibility(View.INVISIBLE);
            TvWelcome.setText("Welcome " + UserName + " !");

            BtStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick (View v) {
                    Intent intent = new Intent (getApplicationContext(), Conversations.class);
                    intent.putExtra("RootIP", root_ip);
                    intent.putExtra("Port", port);
                    startActivity(intent);
                    finish();
                }
            });
            profile = new Profile(UserName, UserID);
        }
        //Case user has connected before
        else {
            BtStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick (View v) {
                    UserName = EtUserName.getText().toString();
                    profile = new Profile(UserName);
                    //   user = new User(profile);
                    System.out.println("UserID before "+UserID);
                    profileIDGetter profileGetter = new profileIDGetter();
                    profileGetter.execute(profile);
                    try {
                        UserID = profileGetter.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    profile.setProfileID(UserID);
                    System.out.println("UserID after "+UserID);
                    setPersonalData(DB);
                    Intent intent = new Intent (getApplicationContext(), Conversations.class);
                    intent.putExtra("RootIP", root_ip);
                    intent.putExtra("Port", port);
                    startActivity(intent);
                    startActivity(intent);
                    finish();
                }
            });
        }

    }

    //Checks if database exists on phone storage
    boolean FileExists(String FileName) {
        File file = new File (getApplicationContext().getFilesDir(), FileName);
        return file.exists();
    }

    //Copies database to phone storage
    void CopyDB(String FileName) {
        AssetManager AssetMan = getAssets();
        InputStream Inp;
        OutputStream Outp;
        byte[] Buffer;
        int BR;
        try {
            Inp = AssetMan.open(FileName);
            File OutputFile = new File (getApplicationContext().getFilesDir(), FileName);
            Outp = new FileOutputStream(OutputFile);
            Buffer = new byte[1024];
            while ((BR = Inp.read(Buffer)) != -1) {
                Outp.write (Buffer, 0, BR);
            }
            Inp.close();
            Outp.flush();
            Outp.close();
        } catch (IOException e) {
            Toast tst = Toast.makeText (getApplicationContext (), "IO Error during DB copy.", Toast.LENGTH_LONG);
            tst.show ();
            finish();
        }
    }

    //Gets user data from the database
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

    //Inserts personal data to the database
    public void setPersonalData(SQLiteDatabase DB) {
        SQLiteStatement stmt = DB.compileStatement("INSERT INTO UserData (ID, UserName) VALUES (?, ?)");
        stmt.bindLong(1, UserID);
        stmt.bindString(2, UserName);
        stmt.executeInsert();
    }

    //AsyncTask to get a new profileID from the root server
    private class profileIDGetter extends AsyncTask<Profile, Void, Integer> {

        @Override
        protected Integer doInBackground(Profile... profile) {
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            int ID = -1;
            System.out.println(profile[0].ProfileName);

            try {

                requestSocket = new Socket(InetAddress.getByName(root_ip), port);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeObject("I need a profile ID");
                out.writeObject(profile[0].ProfileName);
                out.flush();

                ID = (int) in.readObject();
                System.out.println("Got the profileID");
                System.out.println(" ");
                System.out.println(ID);


            } catch (UnknownHostException unknownHost) {
                err.println("Unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException cnf) {
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
            return ID;
        }

    }


}

