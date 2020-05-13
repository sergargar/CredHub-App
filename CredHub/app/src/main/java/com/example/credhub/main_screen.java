package com.example.credhub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import net.sqlcipher.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import net.sqlcipher.database.SQLiteDatabase;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.ksoap2.HeaderProperty;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.transport.HttpTransportSE;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import static com.example.credhub.login_screen.isDeviceSecured;
import static com.example.credhub.login_screen.keyStore;
import static com.example.credhub.login_screen.loadPrivteKey;
import static com.example.credhub.login_screen.decryptString;
import static com.example.credhub.login_screen.refreshKeys;
import static com.example.credhub.login_screen.loadKeyStore;

public class main_screen extends AppCompatActivity {

    Button newb, importb;
    TextView mostrar;
    ListView listCre;
    private Context context = this;
    static final String TAG = "SimpleKeystoreApp";
    KeyStore keyStore;
    List<String> keyAliases;

    protected void onResume(){
        super.onResume();
        refreshKeys();
        if(!isDeviceSecured(context)) {
            Toast.makeText(context, "You were log out as you have removed the device's security", Toast.LENGTH_LONG).show();
            //database is deleted
            SQLiteDatabase.loadLibs(context);
            getDatabasePath("Key.db").delete();
            Intent importr = new Intent(main_screen.this, login_screen.class);
            startActivity(importr);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);



        //Initializing the UI elements as objects
        newb = findViewById(R.id.NewRecord);
        importb = findViewById(R.id.ImportRecord);
        mostrar = findViewById(R.id.tvMostrar);
        listCre = findViewById(R.id.list_view);
        String ws_url;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Setting up the shared preferences
        SharedPreferences prefs;
        prefs = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if (!prefs.contains("ws")){
            SharedPreferences.Editor mEditor = prefs.edit();
            mEditor.putString("ws", "http://sdm_webrepo/");
            mEditor.commit();
        }
        //get encryptedPass
        String encryptedPass = prefs.getString("encryptedPass","");
        //decrypt pass
        loadKeyStore();
        final String pass = decryptString(encryptedPass);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    //Initialize the connection to the local database to obtain the stored records:
                    SQLiteDatabase.loadLibs(context);
                    File databaseFile = getDatabasePath("Key.db");
                    SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile, pass, null);

                    String[] projection = {
                            KeyDatabase.KeyEntry.COLUMN_NAME_ID,
                            KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME,
                            KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD
                    };

                    //Query for obtaining all the rows from the records table
                    Cursor cursor = db.query(
                            KeyDatabase.KeyEntry.TABLE_NAME,
                            projection,
                            null,
                            null,
                            null,
                            null,
                            null
                    );


                    List<String> arrayCre = new ArrayList<String>();
                    if(cursor.getCount()==0){
                        mostrar.setText("No records were found");
                    }
                    //Traversing the cursor assigning the app names to the arrayList
                    else {
                        cursor.moveToFirst();
                        mostrar.setText("List of records locally stored: ");
                        while (!cursor.isAfterLast()) {
                            arrayCre.add(cursor.getString(0));
                            cursor.moveToNext();
                        }



                        //Setting up the UI list of records in the local database
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(main_screen.this, R.layout.listitem, R.id.textview, arrayCre);
                        //change the list on an UiThread

                                listCre.setAdapter(adapter);
                    }
                    cursor.close();
                    db.close();



                } catch (Exception ex) {
                    System.out.println("ERROR - " + ex.toString());
                } finally {
                    Intent intent = getIntent();
                    //Selectable buttons to open a local record
                    listCre.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                       @Override
                                                       public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                                           //Delegates the ID of the selected record to the objective activity
                                                           Intent importr = new Intent(main_screen.this, open_record.class);
                                                           importr.putExtra("id", parent.getItemAtPosition(position).toString()).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                           startActivity(importr);
                                                           finish();
                                                       }
                                                   }
                        );
                    //Import button
                    importb.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       Intent importr = new Intent(main_screen.this, import_record.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                       startActivity(importr);
                                                       finish();

                                                   }
                                               }
                    );
                    //New record button
                   newb.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent newr = new Intent(main_screen.this, new_record.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                    startActivity(newr);
                                                    finish();
                                                }
                                            }
                    );
                }

            }
        });
    }
}
