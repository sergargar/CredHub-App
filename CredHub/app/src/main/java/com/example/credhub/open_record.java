package com.example.credhub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Random;
import java.util.Vector;

import org.ksoap2.HeaderProperty;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

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

import static com.example.credhub.login_screen.randomPass;

public class open_record extends AppCompatActivity {

    //Declaring the objects that will be used throughout the class
    Button cancelb,randompw,showpw,modify,delete,export;
    TextView id,mostrar;
    EditText input_username, input_password;
    private Context context=this;
    private static final String WS_METHOD_EXPORT = "ExportRecord";
    List<HeaderProperty> headerList_basicAuth = null;
    static final String TAG = "SimpleKeystoreApp";
    KeyStore keyStore;
    List<String> keyAliases;

    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    protected void onResume(){
        super.onResume();
        refreshKeys();
        if(!isDeviceSecured(context)) {
            Toast.makeText(context, "You were log out as you have removed the device's security", Toast.LENGTH_LONG).show();
            Intent importr = new Intent(open_record.this, login_screen.class);
            startActivity(importr);
            finish();
        }
    }
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_record);
        loadKeyStore();
        SharedPreferences prefs;
        prefs = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        //get encryptedPass
        String encryptedPass = prefs.getString("encryptedPass","");
        //decrypt pass
        final String pass = decryptString(encryptedPass);

        final String WS_NAMESPACE=(prefs.getString("ws", ""));
        // Create a trust manager that does not validate certificate chains,
        // and also disable hostname verification.
        // (**IMPORTANT NOTE: This is used here to allow our custom certificates
        // for TESTING purposes, it is not suitable for a production environment)
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
        final Context context = getApplicationContext();
        final CharSequence text = "Record successfully exported.";
        final CharSequence text1 = "Record successfully deleted.";
        final CharSequence text2 = "Record successfully updated.";
        final int duration = Toast.LENGTH_SHORT;
        final Toast toast = Toast.makeText(context, text, duration);
        final Toast toast1 = Toast.makeText(context, text1, duration);
        final Toast toast2 = Toast.makeText(context, text2, duration);

        //Initializing the layout elements as objects.
        randompw = findViewById(R.id.Random_pass);
        showpw = findViewById(R.id.Show_pass);
        modify = findViewById(R.id.Modify);
        delete = findViewById(R.id.Delete);
        export = findViewById(R.id.Export);
        cancelb = findViewById(R.id.Cancel);
        mostrar = findViewById(R.id.tvMostrar);
        id = findViewById(R.id.id);
        input_username = findViewById(R.id.username_input1);
        input_password = findViewById(R.id.password_input1);
        HttpTransportSE androidHttpTransport = new HttpTransportSE("https://10.0.2.2/SDM/WebRepo?wsdl");

        final Intent intent = getIntent();
        headerList_basicAuth = new ArrayList<HeaderProperty>();
        String strUserPass = intent.getStringArrayListExtra("cred").get(0) + ":" + intent.getStringArrayListExtra("cred").get(1);
        headerList_basicAuth.add(new HeaderProperty("Authorization", "Basic " + org.kobjects.base64.Base64.encode(strUserPass.getBytes())));




        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Thread for the handling of UI and interaction:
        Thread connect = new Thread() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override

                        public void run() {
                    //Getting the selected ID from the record list from the intent.
                    Intent intent = getIntent();
                    String str = intent.getStringExtra("id");
                    id.setText(str);
                    //Initialize the connection to the local database.
                    SQLiteDatabase.loadLibs(context);
                    File databaseFile = getDatabasePath("Key.db");
                    SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile, pass, null);


                    String[] projection = {
                            KeyDatabase.KeyEntry.COLUMN_NAME_ID,
                            KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME,
                            KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD
                    };

                    //Query to obtain the data of the selected ID.
                    Cursor cursor = db.query(
                            KeyDatabase.KeyEntry.TABLE_NAME,
                            projection,
                            KeyDatabase.KeyEntry.COLUMN_NAME_ID+"="+"'"+str+"'",
                            null,
                            null,
                            null,
                            null
                    );

                    cursor.moveToFirst();
                    //Setting the text inputs with the contents from the database.

                            input_username.setText(cursor.getString(1));
                            input_password.setText(cursor.getString(2));
                            cursor.close();

                            db.close();

                        }
                    });




                } catch (Exception ex) {
                    System.out.println("ERROR - " + ex.toString());
                } finally {

                    //Setting up the listeners for the UI elements.
                    modify.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      //Checks for invalid inputs.
                                                      if (input_password.getText().toString().compareTo("") == 0) {
                                                          Context context = getApplicationContext();
                                                          CharSequence text = "Type a valid password, please.";
                                                          int duration = Toast.LENGTH_SHORT;

                                                          Toast toast = Toast.makeText(context, text, duration);
                                                          toast.show();
                                                      } else if (input_username.getText().toString().compareTo("") == 0) {
                                                          Context context = getApplicationContext();
                                                          CharSequence text = "Type a valid username, please.";
                                                          int duration = Toast.LENGTH_SHORT;

                                                          Toast toast = Toast.makeText(context, text, duration);
                                                          toast.show();
                                                      } else {
                                                          //If inputted data is valid, update the local database:
                                                          try {
                                                              SQLiteDatabase.loadLibs(context);
                                                              File databaseFile = getDatabasePath("Key.db");
                                                              SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile, pass, null);


                                                              //Query for updating using the input data and the extracted ID.
                                                              String update = "UPDATE " + KeyDatabase.KeyEntry.TABLE_NAME + " SET " + KeyDatabase.KeyEntry.COLUMN_NAME_ID+"='"+
                                                                      id.getText().toString()+"',"+KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME+"='"+input_username.getText().toString()+"',"+
                                                                      KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD+"='"+input_password.getText().toString()+"' WHERE "+
                                                                      KeyDatabase.KeyEntry.COLUMN_NAME_ID+"='"+id.getText().toString()+"'";

                                                              //Execution of the update and closing the connection.
                                                              db.execSQL(update);
                                                              db.close();

                                                          } catch (Exception ex) {
                                                              System.out.println("ERROR - " + ex.toString());
                                                          }
                                                          input_username.getText().clear();
                                                          input_password.getText().clear();
                                                          toast2.show();
                                                          Intent importr = new Intent(open_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
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

                                                       Intent importr = new Intent(open_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                       startActivity(importr);
                                                       finish();

                                                   }
                                               }
                    );
                    //Delete button
                    delete.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {

                                                      //Hacer delete

                                                      try {
                                                          SQLiteDatabase.loadLibs(context);
                                                          File databaseFile = getDatabasePath("Key.db");
                                                          SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(databaseFile, pass, null);


                                                          String delete = "DELETE FROM " + KeyDatabase.KeyEntry.TABLE_NAME + " WHERE "+ KeyDatabase.KeyEntry.COLUMN_NAME_ID+"='"+id.getText().toString()+"'";

                                                          db.execSQL(delete);
                                                          db.close();

                                                      } catch (Exception ex) {
                                                          System.out.println("ERROR - " + ex.toString());
                                                      }
                                                      toast1.show();
                                                      Intent importr = new Intent(open_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                      startActivity(importr);
                                                      finish();

                                                  }
                                              }
                    );
                    //Show password button
                    showpw.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {

                                                      input_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                                      final long changeTime = 3000L;
                                                      showpw.postDelayed(new Runnable() {
                                                          @Override
                                                          public void run() {
                                                              input_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                                          }
                                                      }, changeTime);


                                                  }
                                              }
                    );
                    //Random password button
                    randompw.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {

                                                        input_password.setText(randomPass());

                                                    }
                                                }
                    );
                    //Export record button
                    export.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      //User error avoidance
                                                      if(input_password.getText().toString().compareTo("") == 0){
                                                          Context context = getApplicationContext();
                                                          CharSequence text = "Type a password, please.";
                                                          int duration = Toast.LENGTH_SHORT;

                                                          Toast toast = Toast.makeText(context, text, duration);
                                                          toast.show();
                                                      }

                                                      else if(input_username.getText().toString().compareTo("") == 0){
                                                          Context context = getApplicationContext();
                                                          CharSequence text = "Type an username, please.";
                                                          int duration = Toast.LENGTH_SHORT;

                                                          Toast toast = Toast.makeText(context, text, duration);
                                                          toast.show();
                                                      }
                                                      else {
                                                          Thread thread = new Thread(new Runnable() {

                                                              @Override
                                                              public void run() {
                                                                  try {
                                                                      //Exporting the record to the WebService
                                                                      SoapObject request = new SoapObject(WS_NAMESPACE, WS_METHOD_EXPORT);
                                                                      PropertyInfo propId = new PropertyInfo();
                                                                      propId.name = "arg0";
                                                                      propId.setValue(id.getText().toString());
                                                                      propId.type = PropertyInfo.STRING_CLASS;
                                                                      request.addProperty(propId);
                                                                      PropertyInfo propUser = new PropertyInfo();
                                                                      propUser.name = "arg1";
                                                                      propUser.setValue(input_username.getText().toString());
                                                                      propUser.type = PropertyInfo.STRING_CLASS;
                                                                      request.addProperty(propUser);
                                                                      PropertyInfo propPass = new PropertyInfo();
                                                                      propPass.name = "arg2";
                                                                      propPass.setValue(input_password.getText().toString());
                                                                      propPass.type = PropertyInfo.STRING_CLASS;
                                                                      request.addProperty(propPass);
                                                                      SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                                                      envelope.setOutputSoapObject(request);
                                                                      androidHttpTransport.call("\"" + WS_NAMESPACE + WS_METHOD_EXPORT + "\"", envelope, headerList_basicAuth);

                                                                  } catch (Exception e) {
                                                                      System.out.println("ERROR - " + e.toString());
                                                                  }
                                                              }
                                                          });

                                                          thread.start();
                                                          toast.show();
                                                          Intent importr = new Intent(open_record.this, main_screen.class).putExtra("cred",intent.getStringArrayListExtra("cred"));
                                                          startActivity(importr);
                                                          finish();
                                                      }
                                                  }
                                              }
                    );




                }
            }


        };
        connect.start();
    }
}
