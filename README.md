# CredHub Application
- Android Studio project for the subject of Security in Mobile Devices where an Android Application was built using Secure Storage of Persistent Data (SQLCipher database). 
- The goal of this servie is to store the user's credentials in a safer way.
- A pair of keys, public and private, are generated and store in the Android KeyStore to have a transparent encryption/decryption of the SQLCipher database's password.
- The application uses a Web Repository to import and export credentials from the local database using a secure HTTPS connection, which can be executed with ```"%JAVA_HOME%\bin\java" -jar SDM_WebRepo.jar https+auth``` inside the WebRepository folder.
- A signed APK is given to test the application.
- The credentials for the login are ```sdm/repo4droid```
