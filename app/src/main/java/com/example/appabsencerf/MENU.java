package com.example.appabsencerf;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MENU extends AppCompatActivity {

    private String profUrl = Constante.PROF_URL;
    private Spinner spinnerProfs;
    private String idProf=null;
    HashMap<Integer, String> profess ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_m_e_n_u);
        spinnerProfs = (Spinner) findViewById(R.id.profList);


        getAllProfs();


//        spinnerProfs.getSelectedItem().toString();
        Button button = (Button) findViewById(R.id.btn1);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
//                Intent custaddress = new Intent();
                Intent intent = new Intent().setClass(MENU.this, MainActivity.class);
                Bundle b = new Bundle();
                b.putInt("key", Integer.parseInt(idProf)); //Your id
                intent.putExtras(b); //Put your id to your next Intent
                startActivity(intent);
                finish();

//                Log.e("test response", " resultat " + myString);
                MainActivity();
            }
        });
    }
    public void MainActivity(){
         Intent intent = new Intent(this, MainActivity.class);
         startActivity(intent);
    }

    private void getAllProfs(){

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest( Request.Method.GET, profUrl,null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response)
            {
                Log.e("test response", "Error parsing data " + response.toString());

                List<String> al = new ArrayList<>();
                profess = new HashMap<Integer, String>();
                try {
                    for(int i=0; i < response.length(); i++) {
                        JSONObject jsonobject = response.getJSONObject(i);
                        String name = jsonobject.getString("nom_prof")+" "+jsonobject.getString("prenom_prof");
                        Log.e("JSON Parser", "le nom prof" + name);
                        al.add(name);
                        profess.put(i,jsonobject.getString("id"));
                    }
                } catch (JSONException e) {
                    Log.e("JSON Parser", "Error parsing data " + e.toString());
                }


                ArrayAdapter<String> bb = new ArrayAdapter<String>(MENU.this, android.R.layout.simple_spinner_item,  al);
                bb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                      //Setting the ArrayAdapter data on the Spinner
                spinnerProfs.setAdapter(bb);

            }
        },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.e("Test error", "amine test amine: ");
                    }
                }) {


        };

        requestQueue.add(jsonObjectRequest);

        spinnerProfs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Message mSelected = (Message) parent.getItemAtPosition(position);
//                sstate = (String) parent.getItemAtPosition(position);
//                Log.i("les pros :", String.valueOf(profess));
                idProf = profess.get(position);
                Log.i("Id test messaoudi :", profess.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i("Message", "Nothing is selected");
            }
        });
    }

}