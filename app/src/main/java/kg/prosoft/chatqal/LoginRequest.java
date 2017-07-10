package kg.prosoft.chatqal;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import kg.prosoft.chatqal.app.EndPoints;

/**
 * Created by ProsoftPC on 9/28/2016.
 */
public class LoginRequest extends StringRequest {
    private static final String LOGIN_REQUEST_URL = EndPoints.LOGIN;
    private Map<String, String> params;

    public LoginRequest(String email, String password, Response.Listener<String> listener){
        super(Method.POST, LOGIN_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("email",email);
        params.put("password", password);
    }

    @Override
    public Map<String,String> getParams(){
        return params;
    }
}
