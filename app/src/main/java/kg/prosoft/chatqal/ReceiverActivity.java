package kg.prosoft.chatqal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import kg.prosoft.chatqal.R;
import kg.prosoft.chatqal.adapter.ReceiverThreadAdapter;
import kg.prosoft.chatqal.app.Config;
import kg.prosoft.chatqal.app.EndPoints;
import kg.prosoft.chatqal.service.MsgStatus;
import kg.prosoft.chatqal.service.MyVolley;
import kg.prosoft.chatqal.service.SessionManager;
import kg.prosoft.chatqal.utils.NotificationUtils;
import kg.prosoft.chatqal.model.Message;
import kg.prosoft.chatqal.model.User;
import java.util.UUID;

public class ReceiverActivity extends AppCompatActivity {

    private String TAG = ReceiverActivity.class.getSimpleName();

    private String toUserId;
    private RecyclerView recyclerView;
    private ReceiverThreadAdapter mAdapter;
    private ArrayList<Message> messageArrayList;
    private Map<String, Message> messageMap;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private EditText inputMessage;
    private Button btnSend;
    private SessionManager session;
    private String selfUserId;
    SQLiteDatabase articlesDB;
    public Context appCtx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        session = new SessionManager(getApplicationContext());
        appCtx=getApplication();

        inputMessage = (EditText) findViewById(R.id.message);
        btnSend = (Button) findViewById(R.id.btn_send);

        Intent intent = getIntent();
        toUserId = intent.getStringExtra("to_user_id");
        String title = intent.getStringExtra("name");

        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (toUserId == null) {
            Toast.makeText(getApplicationContext(), "Receiver not found!", Toast.LENGTH_SHORT).show();
            finish();
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        messageMap=new LinkedHashMap<>();

        // self user id is to identify the message owner
        selfUserId = session.getUser().getId();

        mAdapter = new ReceiverThreadAdapter(this, messageArrayList, selfUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "line 102");
                if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push message is received
                    Log.e(TAG, "line 105");
                    handlePushNotification(intent);
                }
                else if(intent.getAction().equals(Config.STATUS_NOTIFICATION)){
                    Log.e(TAG, "line 122");
                    handleStatusNotification(intent);
                }
            }
        };

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        fetchChatThread();
        //fetchLocal();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // registering the receiver for new notification
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.STATUS_NOTIFICATION));

        NotificationUtils.clearNotifications(this);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Handling new push message, will add the message to
     * recycler view and scroll it to bottom
     * */
    private void handlePushNotification(Intent intent) {
        Log.e(TAG, "line 161");

        Bundle bundle = intent.getExtras();
        Object msg = bundle.get("message");
        if(msg instanceof Message){
            Message message = (Message) intent.getSerializableExtra("message");
            String toUserId = intent.getStringExtra("to_user_id");
            Log.e(TAG, "line 168 message: "+message+" id: "+toUserId);

            if (message != null && toUserId != null) {
                MsgStatus.send(appCtx,message.getId(),2, message.getSenderId(), message.getUniqueId());
                Log.e(TAG, "line 172");
                String uniqueId=UUID.randomUUID().toString();
                messageMap.put(uniqueId,message);
                for (Message msgVal : messageMap.values()) {
                    messageArrayList.add(msgVal);
                }
                //messageArrayList = new ArrayList<Message>(messageMap.values());
                //messageArrayList.add(message);
                Log.e(TAG, "line 180 sender_id:"+message.getSenderId());
                mAdapter.notifyDataSetChanged();
                if (mAdapter.getItemCount() > 1) {
                    recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount() - 1);
                }
            }
        }
    }

    /**
     * Handling new push message, will change icon to 'received' or 'read'
     * */
    private void handleStatusNotification(Intent intent) {
        Log.e(TAG, "line 193");

        //String msg_id = intent.getStringExtra("message_id");
        String unique_id = intent.getStringExtra("unique_id");
        String status = intent.getStringExtra("status");
        Message msgObj=messageMap.get(unique_id);

        if (msgObj != null) {
            msgObj.setStatus(Integer.parseInt(status));
            messageMap.put(unique_id,msgObj);

            messageArrayList.clear();
            for (Message msgVal : messageMap.values()) {
                messageArrayList.add(msgVal);
            }
            mAdapter.notifyDataSetChanged();
            if (mAdapter.getItemCount() > 1) {
                recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount() - 1);
            }
        }
    }

    /**
     * Posting a new message in chat room
     * will make an http call to our server. Our server again sends the message
     * to all the devices as push notification
     * */
    private void sendMessageOld() {
        final String message = this.inputMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = EndPoints.USER_MESSAGE;

        Log.e(TAG, "endpoint: " + endPoint);

        this.inputMessage.setText("");

        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (obj.getBoolean("error") == false) {
                        JSONObject commentObj = obj.getJSONObject("messageObject");

                        String commentId = commentObj.getString("message_id");
                        String commentText = commentObj.getString("message");
                        String createdAt = commentObj.getString("created_at");
                        String sender = commentObj.getString("from_user_id");

                        Message message = new Message();
                        message.setId(commentId);
                        message.setMessage(commentText);
                        message.setCreatedAt(createdAt);
                        message.setSenderId(sender);

                        messageArrayList.add(message);

                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1) {
                            // scrolling to bottom of the recycler view
                            //recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
                            recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount() - 1);
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "" + obj.getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "216 json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                inputMessage.setText(message);
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("from_user_id", selfUserId);
                params.put("message", message);
                params.put("to_user_id", toUserId);
                params.put("type", "individual");

                Log.e(TAG, "Params: " + params.toString());

                return params;
            }
        };


        // disabling retry policy so that it won't make
        // multiple http calls
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        strReq.setRetryPolicy(policy);

        //Adding request to request queue
        MyVolley.getInstance(this).addToRequestQueue(strReq);
    }
    private void sendMessage() {
        final String message = this.inputMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = EndPoints.USER_MESSAGE;
        final String uniqueId=UUID.randomUUID().toString();

        Log.e(TAG, "endpoint: " + endPoint);

        this.inputMessage.setText("");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateInst = Calendar.getInstance().getTime();
        String createdAt=sdf.format(dateInst);

        Message messageObj = new Message();
        messageObj.setId("0");
        messageObj.setMessage(message);
        messageObj.setCreatedAt(createdAt);
        messageObj.setSenderId(selfUserId);
        messageObj.setStatus(0);
        messageObj.setUniqueId(uniqueId);
        messageMap.put(uniqueId,messageObj);

        //messageArrayList = new ArrayList<Message>(messageMap.values());
        //messageArrayList.add(messageObj);
        for (Message msg : messageMap.values()) {
            messageArrayList.add(msg);
        }

        mAdapter.notifyDataSetChanged();
        if (mAdapter.getItemCount() > 1) {
            // scrolling to bottom of the recycler view
            recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount() - 1);
        }


        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (!obj.getBoolean("error")) {
                        Message msgToUpdate=messageMap.get(uniqueId);
                        msgToUpdate.setStatus(1);
                        messageMap.put(uniqueId,msgToUpdate);
                        messageArrayList.clear();
                        for (Message msg : messageMap.values()) {
                            messageArrayList.add(msg);
                        }

                        mAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(getApplicationContext(), "" + obj.getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "216 json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                inputMessage.setText(message);
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("from_user_id", selfUserId);
                params.put("message", message);
                params.put("to_user_id", toUserId);
                params.put("type", "individual");
                params.put("status", "1");
                params.put("unique_id", uniqueId);

                Log.e(TAG, "Params: " + params.toString());

                return params;
            }
        };


        // disabling retry policy so that it won't make
        // multiple http calls
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        strReq.setRetryPolicy(policy);

        //Adding request to request queue
        MyVolley.getInstance(this).addToRequestQueue(strReq);
    }


    /**
     * Fetching all the messages of a single chat room from remote server
     * */
    private void fetchChatThread() {

        String endPoint = EndPoints.CHAT_THREAD;
        Log.e(TAG, "endPoint: " + endPoint);

        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONArray respArr =new JSONArray(response);

                    for (int i = 0; i < respArr.length(); i++) {
                        JSONObject respObj = (JSONObject) respArr.get(i);

                        String commentId = respObj.getString("id");
                        String commentText = respObj.getString("message");
                        String createdAt = respObj.getString("created_at");
                        String sender_id = respObj.getString("from_user_id");
                        String unique_id = respObj.getString("unique_id");
                        int status = respObj.getInt("status");

                        Message message = new Message();
                        message.setId(commentId);
                        message.setMessage(commentText);
                        message.setCreatedAt(createdAt);
                        message.setSenderId(sender_id);
                        message.setStatus(status);
                        message.setUniqueId(unique_id);

                        messageMap.put(unique_id,message);

                        //messageArrayList.add(message);
                    }

                    for (Message msg : messageMap.values()) {
                        messageArrayList.add(msg);
                    }

                    //messageArrayList = new ArrayList<Message>(messageMap.values());
                    mAdapter.notifyDataSetChanged();
                    if (mAdapter.getItemCount() > 1) {
                        recyclerView.getLayoutManager().scrollToPosition(mAdapter.getItemCount() - 1);
                    }

                    /*// check for error
                    if (obj.getBoolean("error") == false) {


                    } else {
                        Toast.makeText(getApplicationContext(), "" + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                    }*/

                } catch (JSONException e) {
                    Log.e(TAG, "313 json parsing error: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("sender_id", selfUserId);
                params.put("receiver_id", toUserId);

                Log.e(TAG, "Params: " + params.toString());

                return params;
            }
        };

        //Adding request to request queue
        MyVolley.getInstance(this).addToRequestQueue(strReq);
    }


    /**
     * Fetching all the messages of a single chat room from local sql
     * */
    private void fetchLocal() {

    }

}
