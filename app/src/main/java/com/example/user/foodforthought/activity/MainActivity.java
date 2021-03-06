package com.example.user.foodforthought.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.foodforthought.R;
import com.parse.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MainActivity extends ActionBarActivity {

    Queue<String> userQueue = new LinkedList<String>();
    ParseUser currentUser;
    String currentlyViewedProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parse.initialize(this, "vKMS21EgxqmkWPbZ4KMRc4p7PmUWONtatA4ZM2bn", "6gMhVDU5xcakoNIXDpBeykmyCuy3ka0e7pVkm59C");
        currentUser = ParseUser.getCurrentUser();
        setContentView(R.layout.activity_main);

        // Updates the user stack
        updateUserStack();

        // Updates current card on the stack
        updateCurrentProfile();

        // Hides action bar
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().hide();
        }
    }

    // Responds to clicking image on activity_main.xml.
    // Should redirect to profile.xml
    public void clickMainProfileHandler(View view)
    {
        Intent intent = new Intent(this, FullProfileActivity.class);
        intent.putExtra("USER_ID", currentlyViewedProfile);
        startActivity(intent);
    }

    //Make a match
    public void yesMatchClickHandler(View view){
        final ParseUser currentUser = ParseUser.getCurrentUser();
        final String viewedUser = currentlyViewedProfile;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("swipe");
        query.whereEqualTo("recipient", viewedUser);
        query.whereEqualTo("sender", currentUser.getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> recipientList, ParseException e) {
                if (e == null) {
                    if (recipientList.size() == 0){
                        // If never seen before, add to DB
                        ParseObject swipe = new ParseObject("swipe");
                        swipe.put("recipient", viewedUser);
                        swipe.put("sender", currentUser.getUsername());

                        swipe.saveInBackground();
                    }
                    else
                        Toast.makeText(getApplicationContext(),
                                "Already swiped this user!",
                                Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error swiping user: " + e.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        if(!userQueue.isEmpty())
            userQueue.remove();

        updateCurrentProfile();
    }

    public void noMatchClickHandler(View view){

        if(!userQueue.isEmpty())
            userQueue.remove();
        updateCurrentProfile();
    }

    public void matchesButtonClickHandler(View view)
    {
        Intent intent = new Intent(this, MatchesListActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatementhe d
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Updates the stack of users to pull from
    private boolean updateUserStack() {
        // If there is more than 1 item left, then no need to restock the queue
        if (userQueue.size() > 1)
            return true;

        // List of users
        ParseQuery<ParseUser> userList = ParseUser.getQuery();
        userList.setLimit(900);

        // All swiped right by current user
        ParseQuery<ParseObject> swipedByUser = ParseQuery.getQuery("swipe");
        swipedByUser.whereEqualTo("sender", currentUser.getUsername());
        swipedByUser.setLimit(900);


        // We want the set of users which share usernames between the two different sets.
        // Aka, recipient and sender are the same
        ParseQuery<ParseUser> query = userList.whereDoesNotMatchKeyInQuery("username",
                "recipient", swipedByUser);

        // TODO: order the queries by distance away from the user
        try {
            List<ParseUser> matches = query.find();

            // Put users in the queue sequentially
            for (int i = 0; i < matches.size(); i++) {
                int pos = i;
                ParseUser singleUser = matches.get(pos);
                String name = singleUser.get("username").toString();
                if (!name.equals(currentUser.getUsername())) // Don't add yourself to the list.
                    userQueue.add(name);
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        if (!userQueue.isEmpty())
            return true;

        // There are no more users in the database to match with!
        return false;
    }

    // Updates the profile being shown
    private void updateCurrentProfile() {
        currentlyViewedProfile = userQueue.peek();

        if (userQueue.isEmpty())
            Toast.makeText(getApplicationContext(),
                    "No more users to match!",
                    Toast.LENGTH_LONG).show();

        else {
            String username = userQueue.peek();

            TextView matchName = (TextView) findViewById(R.id.matchName);
            matchName.setText(currentlyViewedProfile); // TODO: Needs to be updated to actual name after username == LinkedIn ID

            // Find the profile of the user being shown
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", currentlyViewedProfile);
            try {
                String objectID = query.getFirst().getString("imageID");
                // System.out.println(objectID);
                retrieveImage(objectID);
            } catch (Exception e) {
                // Profile doesn't exist
            }
        }

        if (userQueue.isEmpty()) {
            findViewById(R.id.imageView).setVisibility(View.GONE);
            findViewById(R.id.button).setVisibility(View.GONE);
            findViewById(R.id.button2).setVisibility(View.GONE);
            findViewById(R.id.matchName).setVisibility(View.GONE);
            findViewById(R.id.nomatches).setVisibility(View.VISIBLE);
        }
    }

    // Retrieves and image from the database
    private void retrieveImage(String id) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("UserPicture");
        try {
            ParseObject foundIMG = query.get(id);

            // Create a ParseFile to grab the image from the database
            ParseFile applicantResume = (ParseFile)foundIMG.get("mediaurl");
            applicantResume.getDataInBackground(new GetDataCallback() {
                public void done(byte[] data, ParseException e) {
                    // data has the bytes for the picture
                    if (e == null) {
                        // Replace the image in the imageView with the one in the database
                        ImageView image = (ImageView) findViewById(R.id.imageView);
                        Bitmap bMap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        image.setImageBitmap(bMap);
                    } else {
                        // something went wrong
                    }
                }
            });

        } catch (ParseException e) {
            // Message if failed
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, e.toString(), duration);
            toast.show();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
         userQueue.clear();
         updateUserStack();
         updateCurrentProfile();
    }
}
