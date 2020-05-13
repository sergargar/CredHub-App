package com.example.credhub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static com.example.credhub.login_screen.isDeviceSecured;
import static com.example.credhub.login_screen.keyStore;
import static com.example.credhub.login_screen.loadPrivteKey;
import static com.example.credhub.login_screen.decryptString;
import static com.example.credhub.login_screen.refreshKeys;
import static com.example.credhub.login_screen.loadKeyStore;

public class import_record extends AppCompatActivity {

    //Initializing objects from XML
    Button cancelb;
    ListView listCre1;
    private static Vector<SoapPrimitive> listIds = new Vector<SoapPrimitive>();
    HttpTransportSE androidHttpTransport;
    private Context context=this;
    //Setting up elements for the keystore
    static final String TAG = "SimpleKeystoreApp";
    List<String> keyAliases;



    List<HeaderProperty> headerList_basicAuth = null;
    List<String> arrayCre;
    ArrayAdapter<String> adapter;

    //Function to log out the user in case the security (PIN, etc.) was deactivated
    protected void onResume(){
        super.onResume();
        refreshKeys();
        if(!isDeviceSecured(context)) {
            Toast.makeText(context, "You were log out as you have removed the device's security", Toast.LENGTH_LONG).show();
            Intent importr = new Intent(import_record.this, login_screen.class);
            startActivity(importr);
            finish();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_record);

        SharedPreferences prefs = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        //get encryptedPass for database access
        loadKeyStore();
        String encryptedPass = prefs.getString("encryptedPass","");
        final String pass = decryptString(encryptedPass);
        //Declare objects from XML
        cancelb = findViewById(R.id.Cancel);
        listCre1 = findViewById(R.id.list_import);

        final String WS_NAMESPACE=(prefs.getString("ws", ""));

        //HTTPS protocol setup
        // Create a trust manager that does not validate certificate chains,
        // and also disable hostname verification.
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0]; }
                    @Override public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) { }
                    @Override public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) { }
                }
        };
        HttpsURLConnection.setDefaultHostnameVerifier ((hostname, session) -> true);
        try {
            // Initialize TLS context
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new java.security.SecureRandom()); // *Set 2nd argument to NULL for default trust managers
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        //Declaring objects to be used throughout the code
        final Context context = getApplicationContext();
        final CharSequence text = "Record successfully updated.";
        final CharSequence text1 = "Record successfully imported.";
        final int duration = Toast.LENGTH_SHORT;
        final Toast toast = Toast.makeText(context, text, duration);
        final Toast toast1 = Toast.makeText(context, text1, duration);
        final Intent intent = getIntent();

        headerList_basicAuth = new ArrayList<HeaderProperty>();
        String strUserPass = intent.getStringArrayListExtra("cred").get(0) + ":" + intent.getStringArrayListExtra("cred").get(1);
        headerList_basicAuth.add(new HeaderProperty("Authorization", "Basic " + org.kobjects.base64.Base64.encode(strUserPass.getBytes())));


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Thread connect = new Thread() {
            public void run() {
                try {
                    // Connect to theWeb repository
                    // Set HTTPS URL

                    String WS_METHOD_LIST = "ListCredentials";
                    androidHttpTransport = new HttpTransportSE("https://10.0.2.2/SDM/WebRepo?wsdl");
                    // Read list of all record identifiers stored on the repository
                    SoapObject request = new SoapObject(WS_NAMESPACE, WS_METHOD_LIST);
                    SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                    envelope.setOutputSoapObject(request);
                    androidHttpTransport.call("\"" + WS_NAMESPACE + WS_METHOD_LIST + "\"", envelope, headerList_basicAuth);
                    listIds.clear();
                    if (envelope.getResponse() instanceof Vector) // 2+ elements
                        listIds.addAll((Vector<SoapPrimitive>) envelope.getResponse());
                    else if (envelope.getResponse() instanceof SoapPrimitive) // 1 element
                        listIds.add((SoapPrimitive) envelope.getResponse());




                } catch (Exception ex) {
                    System.out.println("ERROR - " + ex.toString());
                } finally {
                    //Store IDs from the repository to ListView using an ArrayList
                    arrayCre = new ArrayList<String>();
                    for (int i = 0; i < listIds.size(); i++) {
                        arrayCre.add(listIds.get(i).toString());
                    }
                    adapter = new ArrayAdapter<String>(import_record.this,R.layout.listitem,R.id.textview, arrayCre );
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                    //Generating the UI list of importable elements
                    listCre1.setAdapter(adapter);
                }
            });

                    //Cancel button
                    cancelb.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       Intent importr = new Intent(import_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                       startActivity(importr);
                                                       finish();

                                                   }
                                               }
                    );

                    //Selectable buttons for record import
                    listCre1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                   @Override
                                                   public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                       //Extract selected credential from repository
                                                       Thread connect1 = new Thread() {
                                                           public void run() {
                                                        try{
                                                            //Getting all the data from the selected record from the WebService
                                                            String WS_METHOD_IMPORT = "ImportRecord";
                                                            PropertyInfo propId = new PropertyInfo();
                                                            HttpTransportSE androidHttpTransport = new HttpTransportSE("https://10.0.2.2/SDM/WebRepo?wsdl");
                                                            SoapObject request = new SoapObject(WS_NAMESPACE, WS_METHOD_IMPORT);
                                                            propId.name = "arg0"; propId.setValue(parent.getItemAtPosition(position).toString()); propId.type = PropertyInfo.STRING_CLASS;
                                                            request.addProperty(propId);
                                                            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                                            envelope.setOutputSoapObject(request);
                                                            androidHttpTransport.call("\"" + WS_NAMESPACE + WS_METHOD_IMPORT + "\"", envelope, headerList_basicAuth);

                                                            Vector<SoapPrimitive> importedRecord = (Vector<SoapPrimitive>)envelope.getResponse();

                                                            //Setting up the DB access with SQLCipher
                                                            SQLiteDatabase.loadLibs(context);
                                                            File databaseFile = getDatabasePath("Key.db");
                                                            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile, pass, null);

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
                                                                    KeyDatabase.KeyEntry.COLUMN_NAME_ID+"="+"'"+parent.getItemAtPosition(position).toString()+"'",
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    null
                                                            );
                                                            //THe cursor will be empty if the record with such ID does not exist yet.
                                                            cursor.moveToFirst();
                                                            db.close();

                                                            if(importedRecord.size() == 3 && (cursor.getCount() == 0))
                                                            {
                                                                SQLiteDatabase.loadLibs(context);
                                                                File databaseFile1 = getDatabasePath("Key.db");
                                                                SQLiteDatabase db1 = SQLiteDatabase.openOrCreateDatabase(databaseFile1, pass, null);
                                                                //Sentence to insert the imported record into the local database:
                                                                String insert="INSERT INTO "+ KeyDatabase.KeyEntry.TABLE_NAME
                                                                      +" ( " + KeyDatabase.KeyEntry.COLUMN_NAME_ID+","+ KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME+","+ KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD+")" +
                                                                      " VALUES ('"+importedRecord.get(0)+"', '"+importedRecord.get(1)+"','"
                                                                      +importedRecord.get(2)+"')";

                                                                db1.execSQL(insert);
                                                                //Display the user that the insertion was successful
                                                                toast1.show();
                                                                db1.close();
                                                            }
                                                            else {
                                                                if (cursor.getCount() > 0) {
                                                                    //Notify through the UI that the record already exists locally.


                                                                    SQLiteDatabase.loadLibs(context);
                                                                    File databaseFile1 = getDatabasePath("Key.db");
                                                                    SQLiteDatabase db1 = SQLiteDatabase.openOrCreateDatabase(databaseFile1, pass, null);

                                                                    //Query for updating using the input data and the extracted ID.
                                                                    String update = "UPDATE " + KeyDatabase.KeyEntry.TABLE_NAME + " SET " + KeyDatabase.KeyEntry.COLUMN_NAME_ID+"='"+
                                                                            parent.getItemAtPosition(position).toString()+"',"+KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME+"='"+importedRecord.get(1)+"',"+
                                                                            KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD+"='"+importedRecord.get(2)+"' WHERE "+
                                                                            KeyDatabase.KeyEntry.COLUMN_NAME_ID+"='"+parent.getItemAtPosition(position).toString()+"'";

                                                                    //Execution of the update and closing the connection.
                                                                    db1.execSQL(update);
                                                                    db1.close();
                                                                    //Display the user that the update was successful
                                                                    toast.show();
                                                                }
                                                                else System.out.println("Import error - " + importedRecord.get(0));
                                                            }
                                                            cursor.close();
                                                        }
                                                        catch (Exception ex){
                                                            System.out.println("ERROR - " + ex.toString());
                                                        }

                                                           }
                                                       };
                                                        connect1.start();


                                                       Intent importr = new Intent(import_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                       startActivity(importr);
                                                       finish();


                                                   }
                                               }

                    );

                }

            }
        };
        connect.start();


    }
}
