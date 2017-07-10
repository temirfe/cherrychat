package kg.prosoft.chatqal;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kg.prosoft.chatqal.adapter.ReceiverAdapter;
import kg.prosoft.chatqal.app.Config;
import kg.prosoft.chatqal.app.EndPoints;
import kg.prosoft.chatqal.model.Message;
import kg.prosoft.chatqal.model.Receiver;
import kg.prosoft.chatqal.service.MyVolley;
import kg.prosoft.chatqal.service.SessionManager;
import kg.prosoft.chatqal.utils.NotificationUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ArrayList<Receiver> receiverArrayList;
    private ReceiverAdapter mAdapter;
    private RecyclerView recyclerView;
    public Context appContext;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();
        session = new SessionManager(appContext);
        session.checkLogin();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "line 74");
                // checking for type intent filter
                if (intent.getAction().equals(Config.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(Config.TOPIC_GLOBAL);

                    displayFirebaseRegId();
                    Log.e(TAG, "line 82");
                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    handlePushNotification(intent);
                    Log.e(TAG, "line 86");
                }
            }
        };

        displayFirebaseRegId();

        receiverArrayList = new ArrayList<>();
        mAdapter = new ReceiverAdapter(this, receiverArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(appContext));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new ReceiverAdapter.RecyclerTouchListener(appContext, recyclerView, new ReceiverAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                // when chat is clicked, launch full chat thread activity
                Receiver receiver = receiverArrayList.get(position);
                Intent intent = new Intent(MainActivity.this, ReceiverActivity.class);
                intent.putExtra("to_user_id", receiver.getId());
                intent.putExtra("name", receiver.getName());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        /**
         * Always check for google play services availability before
         * proceeding further with GCM
         * */
            fetchReceivers();
    }

    /**
     * Handles new push notification
     */
    private void handlePushNotification(Intent intent) {
        int type = intent.getIntExtra("type", -1);
        Log.e(TAG, "line 130");

        // if the push is of chat room message
        // simply update the UI unread messages count
        if (type == Config.PUSH_TYPE_CHATROOM) {
            Message message = (Message) intent.getSerializableExtra("message");
            // just showing the message in a toast
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
        } else if (type == Config.PUSH_TYPE_USER) {
            // push belongs to user alone
            Message message = (Message) intent.getSerializableExtra("message");
            String ReceiverId = intent.getStringExtra("to_user_id");
            Log.e(TAG, "line 142 message: "+message+" id: "+ReceiverId);

            if (message != null && ReceiverId != null) {
                updateRow(ReceiverId, message);
            }
        }
    }

    /**
     * Updates the chat list unread count and the last message
     */
    private void updateRow(String receiverId, Message message) {
        for (Receiver cr : receiverArrayList) {
            if (cr.getId().equals(receiverId)) {
                int index = receiverArrayList.indexOf(cr);
                cr.setLastMessage(message.getMessage());
                cr.setUnreadCount(cr.getUnreadCount() + 1);
                receiverArrayList.remove(index);
                receiverArrayList.add(index, cr);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    /**
     * fetching the chat rooms by making http call
     */
    private void fetchReceivers() {
        StringRequest strReq = new StringRequest(Request.Method.GET,
                EndPoints.RECEIVERS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONArray ReceiversArray = new JSONArray(response);
                    for (int i = 0; i < ReceiversArray.length(); i++) {
                        JSONObject ReceiversObj = (JSONObject) ReceiversArray.get(i);
                        Receiver cr = new Receiver();
                        cr.setId(ReceiversObj.getString("id"));
                        cr.setName(ReceiversObj.getString("name"));
                        cr.setLastMessage("");
                        cr.setUnreadCount(0);
                        cr.setTimestamp(ReceiversObj.getString("created_at"));

                        receiverArrayList.add(cr);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                mAdapter.notifyDataSetChanged();

                // subscribing to all chat room topics
                //subscribeToAllTopics();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Adding request to request queue
        MyVolley.getInstance(this).addToRequestQueue(strReq);
    }

    // Fetches reg id from shared preferences
    // and displays on the screen
    private void displayFirebaseRegId() {
        String regId = session.getRegId();
        Log.e(TAG, "Firebase reg id: " + regId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        Log.e(TAG, "line 237");

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_logout:
                session.logoutUser();
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
