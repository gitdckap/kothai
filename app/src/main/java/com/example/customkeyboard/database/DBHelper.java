package com.example.customkeyboard.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.example.customkeyboard.model.Clipboard;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DBNAME = "Clipboard";

    public DBHelper(@Nullable Context context) {
        super(context, DBNAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table clipboard (id integer primary key, content text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS clipboard");
        onCreate(sqLiteDatabase);
    }

    public void insertClipboard(String content) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("content", content);

        if (rowValidation()) {
            sqLiteDatabase.execSQL("Delete FROM clipboard WHERE id in (select id FROM clipboard LIMIT 1)");
        }
        sqLiteDatabase.execSQL("DELETE FROM clipboard WHERE content = '" + content + "'");
        sqLiteDatabase.insert("clipboard", null, contentValues);

    }

    @SuppressLint({"Recycle", "Range"})
    public ArrayList<Clipboard> getAllContent() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        ArrayList<Clipboard> arrayList = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.rawQuery("Select * from clipboard ORDER BY id DESC", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            arrayList.add(new Clipboard(cursor.getString(cursor.getColumnIndex("content")), cursor.getString(cursor.getColumnIndex("id"))));
            cursor.moveToNext();
        }
        return arrayList;
    }

    public void deleteRow(List<String> idList) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String[] list = idList.toArray(new String[0]);
        String idsCSV = TextUtils.join(",", list);
        sqLiteDatabase.delete("clipboard", "id IN (" + idsCSV + ")", null);
        sqLiteDatabase.close();
    }

    private boolean rowValidation() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(sqLiteDatabase, DBNAME);
        return numRows >= 22;
    }
}
