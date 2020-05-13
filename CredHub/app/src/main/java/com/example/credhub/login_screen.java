package com.example.credhub;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.security.KeyPairGeneratorSpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class login_screen extends AppCompatActivity {

    //Declaring the objects that will be used throughout the class
    static final String TAG = "SimpleKeystoreApp";
    Button login;
    EditText input_username, input_password;
    static KeyStore keyStore;
    static List<String> keyAliases;
    ArrayList<String> cred = new ArrayList<String>();


    //function to convert an array of bytes to a String
    private static String convertToHex(byte[] paramArrayOfbyte) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        int j = paramArrayOfbyte.length;
        for (int i = 0;; i++) {
            if (i >= j)
                return stringBuffer.toString();
            stringBuffer.append(Integer.toString((paramArrayOfbyte[i] & 0xFF) + 256, 16).substring(1));
        }
    }
    //function to compute the SHA-256 hash of a string
    public String calcSHAHash(String paramString) {
        String password = getString(R.string.mySalt)+paramString;
        MessageDigest digest = null;
        String pswhash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            pswhash = convertToHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            return pswhash;
        }
    }

    //Random password generator for the database access:
    public static String randomPass(){
        String SaltChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 10) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SaltChars.length());
            salt.append(SaltChars.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    //DB encryption initialization
    private void InitializeSQLCipher() {
        //generates and encrypt pass of DB and save it in shared preferences
        String randomPass=randomPass();
        try {
            SharedPreferences prefs = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
            if (prefs.contains("encryptedPass")){
                randomPass=decryptString((prefs.getString("encryptedPass", "")));}
            else{
            SharedPreferences.Editor mEditor = prefs.edit();
            mEditor.putString("encryptedPass", encryptString(randomPass));
            mEditor.commit();
        }}
        catch (Exception e) {
            e.printStackTrace();
        }
        SQLiteDatabase.loadLibs(this);
        File databaseFile = getDatabasePath("Key.db");
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, randomPass, null);
        database.execSQL(KeyDatabase.SQL_CREATE_ENTRIES);
        database.close();
    }
    public static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static void loadKeyStore() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Functions to check the access control security measures on the device
    public static boolean isDeviceSecured(Context context){
        KeyguardManager keyguardManager = (KeyguardManager)
                context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        }else{
            return (isPatternEnabled(context) ||
                    isPassOrPinEnabled(keyguardManager));
        }
    }
    private static boolean isPatternEnabled(Context context){
        return (Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCK_PATTERN_ENABLED, 0) == 1);
    }
    private static boolean isPassOrPinEnabled(KeyguardManager keyguardManager){
        return keyguardManager.isKeyguardSecure();
    }

    //Key pair generation function and string encryption/decryption for key management
    public void generateNewKeyPair(Context context)
            throws Exception {

            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
// expires 1 year from today
            end.add(Calendar.YEAR, 1);
            KeyPairGeneratorSpec spec = new
                    KeyPairGeneratorSpec.Builder(context)
                    .setAlias("DB")
                    .setSubject(new X500Principal("CN=" + "DB"))
                    .setSerialNumber(BigInteger.TEN)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .setEncryptionRequired() //protect the key pair with the secure lock screen credential
                    .build();
// use the Android keystore
            KeyPairGenerator gen =
                    KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE);
            gen.initialize(spec);
// generates the keypair
            gen.generateKeyPair();
    }

    public String encryptString(String text) throws Exception{
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry("DB", null);
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();
            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, inCipher);
            cipherOutputStream.write(text.getBytes(StandardCharsets.UTF_8));
            cipherOutputStream.close();

            byte [] vals = outputStream.toByteArray();
            return Base64.encodeToString(vals, Base64.DEFAULT);
    }
    public static PrivateKey loadPrivteKey(String alias) throws Exception {
        if (!keyStore.isKeyEntry(alias)) {
            Log.e(TAG, "Could not find key alias: " + alias);
            return null;
        }
        KeyStore.Entry entry = keyStore.getEntry(alias, null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.e(TAG, " alias: " + alias + " is not a PrivateKey");
            return null;
        }
        return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }
    public static String decryptString(String cipherText){
        try {

            PrivateKey privateKeyEntry = loadPrivteKey("DB");
            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            String finalText = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
            return finalText;
        }
        catch (Exception e) {
            return e.toString();
        }
    }
    public static void refreshKeys() {
        keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                keyAliases.add(aliases.nextElement());
            }
        }
        catch(Exception e) {}
    }

    protected void onResume(){
        super.onResume();
        refreshKeys();
    }

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        setContentView(R.layout.activity_login_screen);
        final int duration = Toast.LENGTH_SHORT;

        //Initializing the layout elements as objects.
        login = findViewById(R.id.login);
        input_username = findViewById(R.id.username_input1);
        input_password = findViewById(R.id.password_input1);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Thread for the handling of UI and interaction:
        Thread connect = new Thread() {
            public void run() {

                    //Setting up the listeners for the UI elements.
                    //Login button
                    login.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View v) {
                                                       if(!isDeviceSecured(context)){
                                                           Toast.makeText(context,"You cannot log in as your device is not secured",Toast.LENGTH_LONG).show();
                                                       }
                                                       else {
                                                           try {
                                                               loadKeyStore();
                                                               generateNewKeyPair(context);
                                                               InitializeSQLCipher();
                                                           } catch (Exception e) {
                                                               Log.e(TAG, Log.getStackTraceString(e));
                                                           }

                                                           if ((calcSHAHash(input_password.getText().toString()).equals(getString(R.string.strRandomSHA))) && (calcSHAHash(input_username.getText().toString()).equals(getString(R.string.strRandomSHA1)))) {
                                                               //if credentials are correct, they are saved in an ArrayList
                                                               cred.add(input_username.getText().toString());
                                                               cred.add(input_password.getText().toString());
                                                               //Toast is shown
                                                               CharSequence text = "Login successfully.";
                                                               Toast toast = Toast.makeText(context, text, duration);
                                                               toast.show();
                                                               //go to main screen
                                                               Intent importr = new Intent(login_screen.this, main_screen.class).putExtra("cred", cred);
                                                               startActivity(importr);
                                                               finish();
                                                           } else {
                                                               //credentials are invalid and toast is shown
                                                               CharSequence text1 = "Credentials invalid, try again please.";
                                                               Toast toast1 = Toast.makeText(context, text1, duration);
                                                               toast1.show();
                                                           }
                                                       }

                                                   }
                                               }
                    );
            }
        };
        connect.start();
    }
}
