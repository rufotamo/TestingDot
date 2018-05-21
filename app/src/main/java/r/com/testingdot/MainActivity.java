package r.com.testingdot;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextView tv = null;
    String sentID= null;
    Button btnAceptar = null, btnDenegar=null;
    public static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";
    OkHttpClient mClient = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    String token = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializar();
        processExtraData();
    }
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processExtraData();
        dbReadOnFeedback();
    }
    private void processExtraData() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
                btnDenegar.setVisibility(View.VISIBLE);
                btnAceptar.setVisibility(View.VISIBLE);
                String sMensaje = b.getString("theMessage");
                sentID = b.getString("UID");
                tv.setText(sMensaje);
            }
    }
    private void inicializar(){
        tv =tv= (TextView)findViewById(R.id.tv1);
        btnAceptar = (Button)findViewById(R.id.btnAceptar);
        btnDenegar = (Button)findViewById(R.id.btnDenegar);
        btnDenegar.setVisibility(View.INVISIBLE);
        btnAceptar.setVisibility(View.INVISIBLE);
    }
    public void recieve (View v){
        sendMessage(v,"Package can be recieved");
    }
    public void notRecieve(View v){
        sendMessage(v,"Package cannot be recieved");
        tv.setText("EL PAQUETE LLEGARA EL SIGUIENTE DÍA LABORABLE");
    }
    public void sendMessage(View view, String message){
        String id = generateMessageId();
        //HashMap<String,String> dataValues = new HashMap<String,String>();
        //dataValues.put("elMensaje", "Tu paquete esta cerca");
        JSONArray jsonArray = new JSONArray();
        //variable token para devolver mensaje en caso de ser el gerente leer de BBDD el UID sobre el mail y el
        //token sobre el UID
        jsonArray.put(token);
        sendFinal(jsonArray,"FEEDBACK NOTIFICATION", "FEEDBACK", null,message);
    }

    private String generateMessageId() {
        String id=null;
        id=UUID.randomUUID().toString();
        return id;
    }
//
    public void mostrarToken(View view) {
        String token = FirebaseInstanceId.getInstance().getToken();
        Toast t = Toast.makeText(this,token,Toast.LENGTH_LONG);
        t.show();
    }
    public void sendFinal(final JSONArray recipients, final String title, final String body, final String icon, final String message) {

        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    JSONObject root = new JSONObject();
                    JSONObject notification = new JSONObject();
                    notification.put("body", body);
                    notification.put("title", title);
                    notification.put("icon", icon);

                    JSONObject data = new JSONObject();
                    data.put("message", message);
                    data.put("UID", FirebaseAuth.getInstance().getUid());
                    root.put("notification", notification);
                    root.put("data", data);
                    root.put("registration_ids", recipients);

                    String result = postToFCM(root.toString());
                    String TAG2="RESULTADO HOSTIA";
                    Log.d(TAG2, "Result: " + result);
                    return result;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                try {
                    JSONObject resultJson = new JSONObject(result);
                    int success, failure;
                    success = resultJson.getInt("success");
                    failure = resultJson.getInt("failure");
                    Toast.makeText(getApplicationContext(), "Message Success: " + success + "Message Failed: " + failure, Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Message Failed, Unknown error occurred.", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    String postToFCM(String bodyString) throws IOException {
        RequestBody body = RequestBody.create(JSON,bodyString);
        Request request = new Request.Builder()
                .url(FCM_MESSAGE_URL)
                .post(body)
                .addHeader("Authorization", "key= AAAA1fgKyyw:APA91bHJ8Q2JULg6B2uIxB6DENoCf0lpyM-CEOlEibEUwjgqnzKrXJbGxXO4WY2JZ9zjr4nS2s3ye31ORYqDcdQga9XARir5dFjrYqHWNUZKDEX4c0veC84zG_QlZaXsVWdqPu-JiEM1")
                .build();
        Response response = mClient.newCall(request).execute();
        return response.body().string();
    }
    public void dbReadOnFeedback() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Token");
        ref.child(sentID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String sentTK = dataSnapshot.getValue(String.class);
                token=sentTK;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
