package com.securestegox;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
public class DatabaseHelper extends SQLiteOpenHelper {
    // DATABASE
    private static final String DB_NAME =
            "StegoSecureX.db";
    private static final int DB_VERSION = 10;
    private static final String TABLE_USERS =
            "users";
    private static final String TABLE_FILES =
            "encoded_files";
    private static final String TABLE_HISTORY =
            "history";
    // DATABASE SECRET KEY ENCRYPTION
    private static final String DB_AES_ALGORITHM =
            "AES/GCM/NoPadding";
    private static final int DB_GCM_TAG_LENGTH =
            128;
    private static final int DB_IV_LENGTH =
            12;
    private static final String DB_ENCRYPTION_KEY =
            "SecureStegoX_DB_2026_AES_Key";
    // CONSTRUCTOR
    public DatabaseHelper(Context context) {
        super(
                context,
                DB_NAME,
                null,
                DB_VERSION
        );
    }
    // DATABASE CREATE
    @Override
    public void onCreate(SQLiteDatabase db) {
        // USERS
        db.execSQL(
                "CREATE TABLE " +
                        TABLE_USERS +
                        " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "fullname TEXT NOT NULL, " +
                        "username TEXT UNIQUE NOT NULL, " +
                        "email TEXT UNIQUE NOT NULL, " +
                        "password TEXT NOT NULL)"
        );
        // FILES
        db.execSQL(
                "CREATE TABLE " +
                        TABLE_FILES +
                        " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT NOT NULL, " +
                        "file_name TEXT NOT NULL, " +
                        "secret_key TEXT NOT NULL, " +
                        "UNIQUE(username, file_name), " +
                        "FOREIGN KEY(username) REFERENCES " +
                        TABLE_USERS +
                        "(username) ON DELETE CASCADE)"
        );
        // HISTORY
        createHistoryTable(db);
    }
    // HISTORY TABLE
    private void createHistoryTable(
            SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " +
                        TABLE_HISTORY +
                        " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT NOT NULL, " +
                        "operation_type TEXT NOT NULL, " +
                        "file_name TEXT NOT NULL, " +
                        "file_path TEXT, " +
                        "date_time TEXT NOT NULL, " +
                        "status TEXT, " +
                        "FOREIGN KEY(username) REFERENCES " +
                        TABLE_USERS +
                        "(username) ON DELETE CASCADE)"
        );
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                        "idx_history_username ON " +
                        TABLE_HISTORY +
                        "(username)"
        );
    }
    // DATABASE UPGRADE
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        createHistoryTable(db);
        // Convert old plain-text passwords to SHA-256
        if (oldVersion < 9) {
            Cursor cursor =
                    db.rawQuery(
                            "SELECT username, password FROM " +
                                    TABLE_USERS,
                            null
                    );
            if (cursor.moveToFirst()) {
                do {
                    String username =
                            cursor.getString(0);
                    String oldPassword =
                            cursor.getString(1);
                    String hashedPassword =
                            SecurityUtils.hashPassword(
                                    oldPassword
                            );
                    ContentValues values =
                            new ContentValues();
                    values.put(
                            "password",
                            hashedPassword
                    );
                    db.update(
                            TABLE_USERS,
                            values,
                            "username=?",
                            new String[]{
                                    username
                            }
                    );
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // Secret-key encryption
    }
    // =========================================================
    // REGISTER USER
    // =========================================================

    public boolean registerUser(
            String fullname,
            String username,
            String email,
            String password) {

        SQLiteDatabase db =
                this.getWritableDatabase();

        ContentValues values =
                new ContentValues();

        values.put(
                "fullname",
                fullname
        );

        values.put(
                "username",
                username
        );

        values.put(
                "email",
                email
        );


        String hashedPassword =
                SecurityUtils.hashPassword(
                        password
                );

        values.put(
                "password",
                hashedPassword
        );


        long result =
                db.insert(
                        TABLE_USERS,
                        null,
                        values
                );

        db.close();

        return result != -1;
    }


    // =========================================================
    // CHECK USER EXISTS
    // =========================================================

    public boolean checkUserExists(
            String username) {

        SQLiteDatabase db =
                this.getReadableDatabase();

        Cursor cursor =
                db.rawQuery(
                        "SELECT id FROM " +
                                TABLE_USERS +
                                " WHERE username=?",
                        new String[]{
                                username
                        }
                );

        boolean exists =
                cursor.moveToFirst();

        cursor.close();

        db.close();

        return exists;
    }


    // =========================================================
    // LOGIN USER
    // =========================================================
    //
    // If login is successful:
    // LOGIN history is automatically inserted.
    //
    // =========================================================

    public boolean loginUser(
            String username,
            String password) {

        SQLiteDatabase db =
                this.getReadableDatabase();

        Cursor cursor =
                db.rawQuery(
                        "SELECT password FROM " +
                                TABLE_USERS +
                                " WHERE username=?",
                        new String[]{
                                username
                        }
                );

        boolean success = false;

        if (cursor.moveToFirst()) {

            String storedHash =
                    cursor.getString(0);

            success =
                    SecurityUtils.verifyPassword(
                            password,
                            storedHash
                    );
        }

        cursor.close();

        db.close();


        // =====================================================
        // LOGIN SUCCESS HISTORY
        // =====================================================

        if (success) {

            insertLoginHistory(username);
        }


        return success;
    }


    // =========================================================
    // CHECK USER
    // =========================================================

    public boolean checkUser(
            String username,
            String password) {

        return loginUser(
                username,
                password
        );
    }


    // =========================================================
    // VERIFY ACCOUNT FOR PASSWORD RESET
    // =========================================================

    public boolean verifyUserForPasswordReset(
            String username,
            String email) {

        SQLiteDatabase db =
                this.getReadableDatabase();

        Cursor cursor =
                db.rawQuery(
                        "SELECT id FROM " +
                                TABLE_USERS +
                                " WHERE username=? AND email=?",
                        new String[]{
                                username,
                                email
                        }
                );

        boolean verified =
                cursor.moveToFirst();

        cursor.close();

        db.close();

        return verified;
    }


    // =========================================================
    // UPDATE PASSWORD
    // =========================================================

    public boolean updatePassword(
            String username,
            String newPassword) {

        SQLiteDatabase db =
                this.getWritableDatabase();

        ContentValues values =
                new ContentValues();

        String hashedPassword =
                SecurityUtils.hashPassword(
                        newPassword
                );

        values.put(
                "password",
                hashedPassword
        );

        int rows =
                db.update(
                        TABLE_USERS,
                        values,
                        "username=?",
                        new String[]{
                                username
                        }
                );

        db.close();

        return rows > 0;
    }


    // =========================================================
    // SAVE FILE + ENCRYPTED SECRET KEY
    // =========================================================

    public boolean saveFile(
            String username,
            String fileName,
            String secretKey) {

        if (username == null ||
                username.trim().isEmpty() ||
                fileName == null ||
                fileName.trim().isEmpty() ||
                secretKey == null ||
                secretKey.trim().isEmpty()) {

            return false;
        }


        SQLiteDatabase db =
                this.getWritableDatabase();


        try {

            ContentValues values =
                    new ContentValues();

            values.put(
                    "username",
                    username
            );

            values.put(
                    "file_name",
                    fileName
            );


            String encryptedKey =
                    encryptDatabaseKey(
                            secretKey
                    );

            values.put(
                    "secret_key",
                    encryptedKey
            );


            long result =
                    db.insertWithOnConflict(
                            TABLE_FILES,
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_REPLACE
                    );


            return result != -1;


        } catch (Exception e) {

            e.printStackTrace();

            return false;


        } finally {

            db.close();
        }
    }


    // =========================================================
    // GET KEY
    // =========================================================

    public String getKey(
            String username,
            String fileName) {

        SQLiteDatabase db =
                this.getReadableDatabase();

        Cursor cursor =
                db.rawQuery(
                        "SELECT secret_key FROM " +
                                TABLE_FILES +
                                " WHERE username=? AND file_name=?",
                        new String[]{
                                username,
                                fileName
                        }
                );

        String key = null;

        if (cursor.moveToFirst()) {

            String encryptedKey =
                    cursor.getString(0);

            key =
                    decryptDatabaseKey(
                            encryptedKey
                    );
        }

        cursor.close();

        db.close();

        return key;
    }


    // =========================================================
    // GET LATEST KEY
    // =========================================================

    public String getLatestKey(
            String username) {

        SQLiteDatabase db =
                this.getReadableDatabase();

        Cursor cursor =
                db.rawQuery(
                        "SELECT secret_key FROM " +
                                TABLE_FILES +
                                " WHERE username=? " +
                                "ORDER BY id DESC LIMIT 1",
                        new String[]{
                                username
                        }
                );

        String key = null;

        if (cursor.moveToFirst()) {

            String encryptedKey =
                    cursor.getString(0);

            key =
                    decryptDatabaseKey(
                            encryptedKey
                    );
        }

        cursor.close();

        db.close();

        return key;
    }


    // =========================================================
    // UPDATE KEY
    // =========================================================

    public boolean updateKey(
            String username,
            String fileName,
            String newKey) {

        if (newKey == null ||
                newKey.trim().isEmpty()) {

            return false;
        }


        SQLiteDatabase db =
                this.getWritableDatabase();


        try {

            String encryptedKey =
                    encryptDatabaseKey(
                            newKey
                    );

            ContentValues values =
                    new ContentValues();

            values.put(
                    "secret_key",
                    encryptedKey
            );


            int rows =
                    db.update(
                            TABLE_FILES,
                            values,
                            "username=? AND file_name=?",
                            new String[]{
                                    username,
                                    fileName
                            }
                    );


            return rows > 0;


        } catch (Exception e) {

            e.printStackTrace();

            return false;


        } finally {

            db.close();
        }
    }


    // =========================================================
    // UPDATE LATEST KEY
    // =========================================================

    public boolean updateLatestKey(
            String username,
            String newKey) {

        if (newKey == null ||
                newKey.trim().isEmpty()) {

            return false;
        }


        SQLiteDatabase db =
                this.getWritableDatabase();

        Cursor cursor =
                db.rawQuery(
                        "SELECT id FROM " +
                                TABLE_FILES +
                                " WHERE username=? " +
                                "ORDER BY id DESC LIMIT 1",
                        new String[]{
                                username
                        }
                );


        if (!cursor.moveToFirst()) {

            cursor.close();

            db.close();

            return false;
        }


        int id =
                cursor.getInt(0);

        cursor.close();


        try {

            String encryptedKey =
                    encryptDatabaseKey(
                            newKey
                    );

            ContentValues values =
                    new ContentValues();

            values.put(
                    "secret_key",
                    encryptedKey
            );


            int rows =
                    db.update(
                            TABLE_FILES,
                            values,
                            "id=?",
                            new String[]{
                                    String.valueOf(id)
                            }
                    );


            db.close();

            return rows > 0;


        } catch (Exception e) {

            e.printStackTrace();

            db.close();

            return false;
        }
    }


    // =========================================================
    // DELETE FILE
    // =========================================================

    public boolean deleteFile(
            String username,
            String fileName) {

        SQLiteDatabase db =
                this.getWritableDatabase();

        int rows =
                db.delete(
                        TABLE_FILES,
                        "username=? AND file_name=?",
                        new String[]{
                                username,
                                fileName
                        }
                );

        db.close();

        return rows > 0;
    }


    // =========================================================
    // INSERT LOGIN HISTORY
    // =========================================================

    private void insertLoginHistory(
            String username) {

        insertHistory(
                username,
                "LOGIN",
                "Session",
                "",
                "Successful"
        );
    }


    // =========================================================
    // INSERT LOGOUT HISTORY
    // =========================================================
    //
    // Call this method before opening LoginActivity.
    //
    // =========================================================

    public void insertLogoutHistory(
            String username) {

        if (username == null ||
                username.trim().isEmpty()) {

            return;
        }


        insertHistory(
                username,
                "LOGOUT",
                "Session",
                "",
                "Successful"
        );
    }


    // =========================================================
    // INSERT HISTORY
    // =========================================================

    public void insertHistory(
            String username,
            String operationType,
            String fileName,
            String filePath,
            String status) {

        if (username == null ||
                username.trim().isEmpty()) {

            return;
        }


        SQLiteDatabase db =
                this.getWritableDatabase();


        String dateTime =
                new SimpleDateFormat(
                        "dd MMM yyyy - hh:mm a",
                        Locale.getDefault()
                ).format(
                        new Date()
                );


        ContentValues values =
                new ContentValues();


        values.put(
                "username",
                username
        );


        values.put(
                "operation_type",
                operationType == null ||
                        operationType.trim().isEmpty()
                        ? "UNKNOWN"
                        : operationType
        );


        values.put(
                "file_name",
                fileName == null ||
                        fileName.trim().isEmpty()
                        ? "Unknown"
                        : fileName
        );


        values.put(
                "file_path",
                filePath == null
                        ? ""
                        : filePath
        );


        values.put(
                "date_time",
                dateTime
        );


        values.put(
                "status",
                status == null
                        ? ""
                        : status
        );


        db.insert(
                TABLE_HISTORY,
                null,
                values
        );


        db.close();
    }


    // =========================================================
    // GET USER HISTORY
    // =========================================================

    public List<HistoryModel> getUserHistory(
            String username) {

        List<HistoryModel> list =
                new ArrayList<>();


        SQLiteDatabase db =
                this.getReadableDatabase();


        Cursor cursor =
                db.rawQuery(
                        "SELECT operation_type, " +
                                "file_name, " +
                                "date_time, " +
                                "status " +
                                "FROM " +
                                TABLE_HISTORY +
                                " WHERE username=? " +
                                "ORDER BY id DESC",
                        new String[]{
                                username
                        }
                );


        if (cursor.moveToFirst()) {

            do {

                String operation =
                        cursor.getString(0);

                String fileName =
                        cursor.getString(1);

                String dateTime =
                        cursor.getString(2);

                String status =
                        cursor.getString(3);


                list.add(
                        new HistoryModel(
                                operation,
                                fileName,
                                dateTime,
                                status
                        )
                );


            } while (cursor.moveToNext());
        }


        cursor.close();

        db.close();

        return list;
    }


    // =========================================================
    // CLEAR USER HISTORY
    // =========================================================

    public void clearUserHistory(
            String username) {

        SQLiteDatabase db =
                this.getWritableDatabase();


        db.delete(
                TABLE_HISTORY,
                "username=?",
                new String[]{
                        username
                }
        );


        db.close();
    }


    // =========================================================
    // RESET COMPLETE DATABASE
    // =========================================================

    public boolean resetDatabase() {

        SQLiteDatabase db =
                this.getWritableDatabase();


        db.beginTransaction();


        try {

            db.delete(
                    TABLE_HISTORY,
                    null,
                    null
            );


            db.delete(
                    TABLE_FILES,
                    null,
                    null
            );


            db.delete(
                    TABLE_USERS,
                    null,
                    null
            );


            db.execSQL(
                    "DELETE FROM sqlite_sequence " +
                            "WHERE name=?",
                    new String[]{
                            TABLE_USERS
                    }
            );


            db.execSQL(
                    "DELETE FROM sqlite_sequence " +
                            "WHERE name=?",
                    new String[]{
                            TABLE_FILES
                    }
            );


            db.execSQL(
                    "DELETE FROM sqlite_sequence " +
                            "WHERE name=?",
                    new String[]{
                            TABLE_HISTORY
                    }
            );


            db.setTransactionSuccessful();

            return true;


        } catch (Exception e) {

            e.printStackTrace();

            return false;


        } finally {

            db.endTransaction();

            db.close();
        }
    }


    // =========================================================
    // ENCRYPT DATABASE SECRET KEY
    // =========================================================

    private String encryptDatabaseKey(
            String plainKey) {

        if (plainKey == null ||
                plainKey.trim().isEmpty()) {

            return null;
        }


        try {

            byte[] iv =
                    new byte[DB_IV_LENGTH];


            SecureRandom random =
                    new SecureRandom();

            random.nextBytes(iv);


            SecretKeySpec keySpec =
                    createDatabaseAESKey();


            Cipher cipher =
                    Cipher.getInstance(
                            DB_AES_ALGORITHM
                    );


            GCMParameterSpec gcmSpec =
                    new GCMParameterSpec(
                            DB_GCM_TAG_LENGTH,
                            iv
                    );


            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec,
                    gcmSpec
            );


            byte[] encrypted =
                    cipher.doFinal(
                            plainKey.trim()
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );


            byte[] combined =
                    new byte[
                            iv.length +
                                    encrypted.length
                            ];


            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );


            System.arraycopy(
                    encrypted,
                    0,
                    combined,
                    iv.length,
                    encrypted.length
            );


            return Base64.encodeToString(
                    combined,
                    Base64.NO_WRAP
            );


        } catch (Exception e) {

            throw new RuntimeException(
                    "Database key encryption failed",
                    e
            );
        }
    }


    // =========================================================
    // DECRYPT DATABASE SECRET KEY
    // =========================================================

    private String decryptDatabaseKey(
            String encryptedKey) {

        if (encryptedKey == null ||
                encryptedKey.trim().isEmpty()) {

            return null;
        }


        try {

            byte[] combined =
                    Base64.decode(
                            encryptedKey,
                            Base64.NO_WRAP
                    );


            if (combined.length <=
                    DB_IV_LENGTH) {

                return null;
            }


            byte[] iv =
                    new byte[DB_IV_LENGTH];


            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    DB_IV_LENGTH
            );


            int encryptedLength =
                    combined.length -
                            DB_IV_LENGTH;


            byte[] encrypted =
                    new byte[encryptedLength];


            System.arraycopy(
                    combined,
                    DB_IV_LENGTH,
                    encrypted,
                    0,
                    encryptedLength
            );


            SecretKeySpec keySpec =
                    createDatabaseAESKey();


            Cipher cipher =
                    Cipher.getInstance(
                            DB_AES_ALGORITHM
                    );


            GCMParameterSpec gcmSpec =
                    new GCMParameterSpec(
                            DB_GCM_TAG_LENGTH,
                            iv
                    );


            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec,
                    gcmSpec
            );


            byte[] decrypted =
                    cipher.doFinal(
                            encrypted
                    );


            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            ).trim();


        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }


    // =========================================================
    // CREATE DATABASE AES-256 KEY
    // =========================================================

    private SecretKeySpec createDatabaseAESKey() {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );


            byte[] keyBytes =
                    digest.digest(
                            DB_ENCRYPTION_KEY.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            return new SecretKeySpec(
                    keyBytes,
                    "AES"
            );


        } catch (Exception e) {

            throw new RuntimeException(
                    "Database AES key creation failed",
                    e
            );
        }
    }
}