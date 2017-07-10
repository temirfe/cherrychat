package kg.prosoft.chatqal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import kg.prosoft.chatqal.model.User;
import kg.prosoft.chatqal.service.MyFirebaseInstanceIDService;
import kg.prosoft.chatqal.service.MyVolley;
import kg.prosoft.chatqal.service.SessionManager;

public class LoginActivity extends Activity implements View.OnKeyListener {

    SessionManager session;

    public EditText emailField;
    public EditText passField;
    public Button loginButton;
    public Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context=getApplicationContext();
        session = new SessionManager(getApplicationContext());

        emailField = (EditText) findViewById(R.id.etEmail);
        passField = (EditText) findViewById(R.id.etPassword);
        loginButton = (Button) findViewById(R.id.button_login);

        emailField.setOnKeyListener(this);
        passField.setOnKeyListener(this);

    }

    public void onClickLogin(View view){
        final String email=emailField.getText().toString();
        final String password=passField.getText().toString();

        Response.Listener<String> responseListener = new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    Log.i("BLYAAAA", jsonResponse.toString());
                    boolean success = jsonResponse.getBoolean("success");
                    if(success){
                        String username = jsonResponse.getString("username");
                        String regIdDb = jsonResponse.getString("red_id");
                        String user_id = jsonResponse.getString("id");

                        session.createLoginSession(username,email, user_id);


                        String regId = session.getRegId();
                        User user = session.getUser();
                        if(!regId.equals(regIdDb)){
                            MyFirebaseInstanceIDService.sendReg(user,regId,context);
                        }

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setMessage("Login Failed").setNegativeButton("Retry",null).create().show();
                    }
                } catch(JSONException e){
                    e.printStackTrace();
                }
            }
        };

        LoginRequest loginRequest = new LoginRequest(email, password, responseListener);
        MyVolley.getInstance(this).addToRequestQueue(loginRequest);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_ENTER){
            if(v.getId()==R.id.etPassword){
                onClickLogin(v);
            }
            /*if(TextUtils.isEmpty(emailField.getText())){
                Log.i("field","email is empty");
            }
            */
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueue queue = MyVolley.getInstance(this.getApplicationContext()).
                getRequestQueue();
        queue.cancelAll(this);
    }
}
