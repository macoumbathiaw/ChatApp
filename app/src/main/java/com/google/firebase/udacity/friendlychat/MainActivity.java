/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final String TAG = "MainActivity";
    //4-Declare an instance of the ChildEventListener and add it under the send button
    ChildEventListener mChildEventListener;
    //6-Variable for Authentication listener state of the user (login or not login)
    FirebaseAuth.AuthStateListener mAuthStateListener;
    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private String mUsername;
    //1-Fire up firebase with the reference. Firebase reference and database reference
    //This is the entry point to access the firebase datase
    private FirebaseDatabase mFirebaseDatabase;
    //This is going to be referencing a specific portion of the databse, and here it is the messaging portion of the database
    private DatabaseReference mMessageDatabaseReference;
    //This is the firebase instance for the Auth
    private FirebaseAuth mfirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //2-Get an instance of the database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //Initialiase the Auth
        mfirebaseAuth = FirebaseAuth.getInstance();
        //Get a specific portion of the database Reference which is the message
        mMessageDatabaseReference = mFirebaseDatabase.getReference().child("message");

        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        //2- Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                //Let's create a simple message from the FriendlyMessage class, but without the picture
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                //Let's add the new message to our Real time database
                mMessageDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });



        //6-1 let's create an OnListener for our mAuthStateListener
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                //check if the user is login or not
                FirebaseUser mfirebaseUser = mfirebaseAuth.getCurrentUser();

                if (mfirebaseUser != null) {
                    //user is signed in if the mfirebaseUser is not null
                    onSignedInInitialiser(mfirebaseUser.getDisplayName());
                    //Just put a Toast to welcome the user
                    Toast.makeText(MainActivity.this, "Welcome to Waxtaan", Toast.LENGTH_SHORT).show();
                } else {
                    //user is signed out if the mfirebaseUser is null
                    onSignedOutCleanUp();
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                    );

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);

                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu_id:
                AuthUI.getInstance().signOut(this);
                return true;

        default:

        return super.onOptionsItemSelected(item);
    }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                Toast.makeText(MainActivity.this,"You are signed in",Toast.LENGTH_SHORT).show();
            }else if (resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this,"Sign in cancelled",Toast.LENGTH_SHORT).show();
                finish();

            }
        }

    }

    //7-we are adding onResume and onPause to respectively resume and remove the sign in/sign out state of the user
    @Override
    protected void onResume() {
        super.onResume();
        mfirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null){
            mfirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detattachDatabaseReadListner();
        mMessageAdapter.clear();
    }

    public void onSignedInInitialiser(String firebaseUser) {

        mUsername = firebaseUser;
        attachDatabaseReadListner();
    }

    public void onSignedOutCleanUp() {

        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
    }

    public void attachDatabaseReadListner() {

        if (mChildEventListener == null) {
            //5-add the mChildEventListener for our user account to listen to new messages
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    // We are only listening to messages added to our databse for now
                    //let's create an instance of the FriendlyMessage class and get the value
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);


                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            //This attach the mChildEventListener to our mMessageDatabaseReference
            mMessageDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

public void detattachDatabaseReadListner(){

        if (mChildEventListener != null)
        mMessageDatabaseReference.removeEventListener(mChildEventListener);
        mChildEventListener = null;
}


}
