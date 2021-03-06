package com.fafaffy.contacts;

/* Created by Alex Casasola & Brian Gardner */


import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.fafaffy.contacts.Adapters.ContactRecyclerAdapter;
import com.fafaffy.contacts.Controllers.DatabaseController;
import com.fafaffy.contacts.Controllers.FileController;
import com.fafaffy.contacts.Controllers.SensorController;
import com.fafaffy.contacts.Models.Contact;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainContactActivity extends AppCompatActivity{

    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private MenuItem LoadContactsFromFile;
    private MenuItem Reinitialize;
    final int ACTIVITY_CHOOSE_FILE = 1;
    public ArrayList<Contact> mData;
    ContactRecyclerAdapter recyclerAdapter;
    private boolean sortAscending = true;

    // Initializes the recycler view and overall main activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_contact);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        fab = (FloatingActionButton) findViewById(R.id.addContactButton);
        recyclerView = (RecyclerView) findViewById(R.id.mainPageRecyclerView);

        // Create an empty array list in order to define our recycler view adapter
        mData = new ArrayList<>();

//        FileController fw = new FileController(getApplicationContext());


        //NEW CODE ADDED TO GET ALL THE SQLite DB data on create
        DatabaseController myDb = new DatabaseController(this);

        // Call get all data func from db instance
        mData = myDb.getAllData();

        // Old file controller code to read contacts
        //mData = fw.readContacts();



        Collections.sort(mData);
        recyclerAdapter = new ContactRecyclerAdapter(mData);

        // Set the properties of the recyclerview (the layout, and animations)
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        // Logic on when to hide or show the FAB button
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if (dy > 0 && fab.isShown())
                    fab.hide();
                if (dy < 0 && !fab.isShown())
                    fab.show();
            }
        });

        // Set our listview's adapter
        recyclerView.setAdapter(recyclerAdapter);

        // Create sensorController object:
        // created March 4, 2018
        SensorController sensorController = new SensorController(getApplicationContext());
        sensorController.setOnShakeListener(new SensorController.OnShakeListener() {

            @Override
            public void OnShake() {
                sortAscending = !sortAscending;
                if (sortAscending) {
                    Collections.sort(mData);
                }else{
                    Collections.sort(mData, Collections.<Contact>reverseOrder());
                }
                recyclerAdapter.notifyDataSetChanged();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // If we're loading from a file, call appropriate method
        if (id == R.id.load_from_file) {
            loadContactsFromFile();
            return true;
        }

        if (id == R.id.save_to_file)
        {
            saveContactToFile();
            return true;
        }

        // If we're reinitializing the db, call appropriate method
        if (id == R.id.reinitialize) {
            reinitialize();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Launch the create contact activity
    public void launchDetailContactActivity(View view){
        Intent addContact = new Intent(this,
                DetailContact.class);
        startActivityForResult(addContact, 1);
    }

    // When the user creates/deletes or edits an activity, refresh the contacts list
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //NEW CODE ADDED TO GET ALL THE SQLite DB data on create
        DatabaseController myDb = new DatabaseController(getApplicationContext());

        switch(requestCode) {
            case ACTIVITY_CHOOSE_FILE: {
                if (resultCode == RESULT_OK){
                    Uri uri = data.getData();

                    myDb.loadContactsFromFile(uri);

                }
            }
        }

        // Call get all data func from db instance
        mData = myDb.getAllData();

        recyclerAdapter.mDataset = mData;
        recyclerAdapter.notifyDataSetChanged();
    }

    public void loadContactsFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
    }

    public void saveContactToFile() {
        final EditText txtUrl = new EditText(this);

        // Set the default text to a link of the Queen
        txtUrl.setText("contacts.txt");
        final DatabaseController myDb = new DatabaseController(this);
        new AlertDialog.Builder(this)
                .setTitle("Save Contacts to File")
                .setMessage("Choose file name to store on device")
                .setView(txtUrl)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        myDb.saveContactsToFile(txtUrl.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    public void reinitialize() {
        DatabaseController myDb = new DatabaseController(getApplicationContext());
        // Call delete all users function
        myDb.deleteAllUsers();
        // Refresh users list
        mData = myDb.getAllData();

        recyclerAdapter.mDataset = mData;
        recyclerAdapter.notifyDataSetChanged();
    }
}
