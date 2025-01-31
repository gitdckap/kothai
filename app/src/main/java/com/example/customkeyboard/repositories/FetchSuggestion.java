package com.example.customkeyboard.repositories;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FetchSuggestion {
    Context context;
    FirebaseFirestore firestore;

    public FetchSuggestion(Context context) {
        this.context = context;
        init();
    }

    public void init() {
        FirebaseApp.initializeApp(context);
        FirebaseFirestoreSettings firestoreSettings = new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
        firestore = FirebaseFirestore.getInstance();
        firestore.setFirestoreSettings(firestoreSettings);
    }

    public void fetchTamilSuggestion() {
        firestore.collection("tamil_suggestion").get().addOnSuccessListener(queryDocumentSnapshots -> {
        });
    }
}
