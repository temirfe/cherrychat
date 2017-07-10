package kg.prosoft.chatqal.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import kg.prosoft.chatqal.ReceiverActivity;
import kg.prosoft.chatqal.app.EndPoints;

/**
 * Created by ProsoftPC on 6/22/2017.
 */

public class MsgStatus {

    public static void send(final Context ctx, final String msgId, final int status, final String sender_id, final String unique_id){
        final String TAG="Status.java";
        String endPoint = EndPoints.MSG_STATUS;

        StringRequest strReq = new StringRequest(Request.Method.POST,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONObject obj = new JSONObject(response);

                    // check for error
                    if (!obj.getBoolean("error")) {

                    } else {
                        Toast.makeText(ctx, "" + obj.getString("message"), Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "216 json parsing error: " + e.getMessage());
                    Toast.makeText(ctx, "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("message_id", msgId);
                params.put("status", status+"");
                params.put("sender_id", sender_id);
                params.put("unique_id", unique_id);

                Log.e(TAG, "Params: " + params.toString());

                return params;
            }
        };

        //Adding request to request queue
        MyVolley.getInstance(ctx).addToRequestQueue(strReq);
    }
}
