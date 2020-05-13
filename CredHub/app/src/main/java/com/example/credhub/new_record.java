package com.example.credhub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import static com.example.credhub.login_screen.isDeviceSecured;
import static com.example.credhub.login_screen.keyStore;
import static com.example.credhub.login_screen.loadPrivteKey;
import static com.example.credhub.login_screen.decryptString;
import static com.example.credhub.login_screen.randomPass;
import static com.example.credhub.login_screen.refreshKeys;
import static com.example.credhub.login_screen.loadKeyStore;

public class new_record extends AppCompatActivity {

    //Declaring the objects that will be used throughout the class
    Button create,cancelb,randomp;
    TextView mostrar;
    EditText input_username, input_password;
    ListView appList;
    private Context context = this;
    List<String> arrayAppNames;
    String currentApp="";
    static final String TAG = "SimpleKeystoreApp";
    KeyStore keyStore;
    List<String> keyAliases;

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    protected void onResume(){
        super.onResume();
        refreshKeys();
        if(!isDeviceSecured(context)) {
            Toast.makeText(context, "You were log out as you have removed the device's security", Toast.LENGTH_LONG).show();
            Intent importr = new Intent(new_record.this, login_screen.class);
            startActivity(importr);
            finish();
        }
    }

    public static void main(String[] args) {
   }
        @Override
        protected void onCreate (Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_new_record);
            //decrypt pass
            SharedPreferences prefs = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
            //get encryptedPass
            String encryptedPass = prefs.getString("encryptedPass","");
            loadKeyStore();
            final String pass = decryptString(encryptedPass);
            //Initializing the UI elements as objects
            cancelb = findViewById(R.id.Cancel);
            create = findViewById(R.id.Send);
            randomp = findViewById(R.id.RandomPassword);
            mostrar = findViewById(R.id.tvMostrar);
            input_username = findViewById(R.id.username_input);
            input_password = findViewById(R.id.password_input);
            appList = findViewById(R.id.list_view_apps);

            final Context context = getApplicationContext();
            final CharSequence text = "Record successfully created.";
            final CharSequence text1 = "Record with that package already exists.";
            final int duration = Toast.LENGTH_SHORT;
            final Toast toast = Toast.makeText(context, text, duration);
            final Toast toast1 = Toast.makeText(context, text1, duration);

            final Intent intent = getIntent();


            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Thread connect = new Thread() {
                public void run() {
                    try {

                        final PackageManager pm = getPackageManager();
                        //Getting the list of installed apps.
                        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

                        arrayAppNames = new ArrayList<String>();
                        for (int i = 0; i < packages.size(); i++) {
                            arrayAppNames.add(packages.get(i).processName);
                        }
                        //Inserting the elements of the obtained list of apps into the UI
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(new_record.this,R.layout.listitem,R.id.textview, arrayAppNames );
                        //change the list on an UiThread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appList.setAdapter(adapter);
                            }
                        });


                    } catch (Exception ex) {
                        System.out.println("ERROR - " + ex.toString());
                    } finally {


                        create.setOnClickListener(new View.OnClickListener() {
                                                      @Override
                                                      public void onClick(View v) {
                                                          //User error avoidance with mistake notifications
                                                          if (currentApp.compareTo("") == 0) {
                                                              Context context = getApplicationContext();
                                                              CharSequence text = "Select an application, please.";
                                                              int duration = Toast.LENGTH_SHORT;

                                                              Toast toast = Toast.makeText(context, text, duration);
                                                              toast.show();
                                                          }
                                                          else if(input_password.getText().toString().compareTo("") == 0){
                                                              Context context = getApplicationContext();
                                                              CharSequence text = "Type a valid password, please.";
                                                              int duration = Toast.LENGTH_SHORT;

                                                              Toast toast = Toast.makeText(context, text, duration);
                                                              toast.show();
                                                              }

                                                          else if(input_username.getText().toString().compareTo("") == 0){
                                                              Context context = getApplicationContext();
                                                              CharSequence text = "Type a valid username, please.";
                                                              int duration = Toast.LENGTH_SHORT;

                                                              Toast toast = Toast.makeText(context, text, duration);
                                                              toast.show();
                                                          }
                                                           else {
                                                              SQLiteDatabase.loadLibs(context);
                                                              File databaseFile = getDatabasePath("Key.db");
                                                              SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile, pass, null);

                                                              try {

                                                                //First it is checked that in the local database the record with the selected ID already exists:
                                                                //Subset of elements of the table to extract form the database.
                                                                String[] projection = {
                                                                        KeyDatabase.KeyEntry.COLUMN_NAME_ID,
                                                                        KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME,
                                                                        KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD
                                                                };

                                                                //Query to obtain the data of the selected ID.
                                                                Cursor cursor = db.query(
                                                                        KeyDatabase.KeyEntry.TABLE_NAME,
                                                                        projection,
                                                                        KeyDatabase.KeyEntry.COLUMN_NAME_ID+"="+"'"+currentApp+"'",
                                                                        null,
                                                                        null,
                                                                        null,
                                                                        null
                                                                );
                                                                //THe cursor will be empty if the record with such ID does not exist yet.
                                                                cursor.moveToFirst();
                                                                cursor.close();
                                                                db.close();
                                                                //If the record with the selected ID does not exist in the local database
                                                                if(cursor.getCount() == 0){
                                                                    SQLiteDatabase.loadLibs(context);
                                                                    File databaseFile1 = getDatabasePath("Key.db");
                                                                    SQLiteDatabase db1 = SQLiteDatabase.openOrCreateDatabase(databaseFile1, pass, null);

                                                                    String insert = "INSERT INTO " + KeyDatabase.KeyEntry.TABLE_NAME
                                                                            + " ( " + KeyDatabase.KeyEntry.COLUMN_NAME_ID + "," + KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME + "," + KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD + ")" +
                                                                            " VALUES ('" + currentApp + "', '" + input_username.getText().toString() + "','"
                                                                            + input_password.getText().toString() + "')";

                                                                    db1.execSQL(insert);
                                                                    toast.show();
                                                                    db1.close();
                                                                }
                                                                else {
                                                                    if (cursor.getCount() > 0) {
                                                                        //Show that a record with the selected ID already exists
                                                                        toast1.show();
                                                                    }
                                                                }


                                                              } catch (Exception ex) {
                                                                  System.out.println("ERROR - " + ex.toString());
                                                              }
                                                              //Clear inputs
                                                              input_username.getText();
                                                              input_password.getText();
                                                              Intent importr = new Intent(new_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                              startActivity(importr);
                                                              finish();

                                                          }
                                                      }
                                                  }
                        );
                        //Cancel button
                        cancelb.setOnClickListener(new View.OnClickListener() {
                                                       @Override
                                                       public void onClick(View v) {

                                                           Intent importr = new Intent(new_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                           startActivity(importr);
                                                           finish();

                                                       }
                                                   }
                        );
                        //App selection list
                        appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                           @Override
                                                           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                               currentApp=parent.getItemAtPosition(position).toString();
                                                           }
                                                       }
                        );
                        //Random password button
                        randomp.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            input_password.setText(randomPass());

                                                        }
                                                    }
                        );




                    }
                }


            };
            connect.start();
        }
    }
