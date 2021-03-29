package com.example.appabsencerf;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.util.Base64Utils;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CapturePicture";
    private ImageView mimageView;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PICTURE_CAPTURE = 1;
    private String pictureFilePath;
    private FirebaseStorage firebaseStorage;
    private String deviceIdentifier;
    private  String requestBody;
    private byte[] bImage;
    private String url_matiere = Constante.FILE_URL_Mat;
    private String url_niveau = Constante.FILE_URL_Niveau;
    public ArrayList<String> matiers;
    private TimePicker timebeg ;
    private TimePicker timeend ;
    private Spinner spinnerMatiere;
    private String matiere;
    private Spinner spNiveau;
    private String selectNiveau;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Bundle b = getIntent().getExtras();
        int value = -1; // or other values
        if(b != null)
            value = b.getInt("key");

        Log.e("test response", "Error parsing data " +  value);
        getAllMatiere();
        getAllNiveau();

        String profId = getIntent().getStringExtra("PROF_ID");

        mimageView =  findViewById(R.id.imageview1);
        spinnerMatiere = (Spinner) findViewById(R.id.spinner1);
        spinnerMatiere.setPrompt("matière");
        spNiveau = (Spinner) findViewById(R.id.spinnerNiveau);
        spNiveau.setPrompt("Niveau");
        timebeg = (TimePicker)findViewById(R.id.begHour);
        timeend = (TimePicker)findViewById(R.id.endHour);
        timebeg.setIs24HourView(true);
        timeend.setIs24HourView(true);


        Button captureButton = findViewById(R.id.capture);
        Button btnSave = findViewById(R.id.save_cloud);
        captureButton.setOnClickListener(capture);
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            captureButton.setEnabled(true);
        }
        btnSave.setOnClickListener(saveLocal);
//        firebaseStorage = FirebaseStorage.getInstance();

    }

    private View.OnClickListener capture = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                takePicture();
            }
        }
    };

    public void takePicture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            File pictureFile = null;
            try {
                pictureFile = getPictureFile();
            } catch (IOException ex) {
                Toast.makeText(this,
                        "Photo file can't be created, please try again",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (pictureFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.zoftino.android.fileprovider",
                            pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_PICTURE_CAPTURE);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(pictureFilePath);
            if (imgFile.exists()) {
                mimageView.setImageURI(Uri.fromFile(imgFile));
            }
        }
    }
    private File getPictureFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String pictureFile = "image_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        pictureFilePath = image.getAbsolutePath();
        return image;
    }

   //save captured picture on local
    private View.OnClickListener saveLocal = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View view) {
            convertImageToBase64();
        }
    };

    //Convert image to byte
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void convertImageToBase64()  {

        String url = Constante.FILE_URL;


        final ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading...PLease wait");
        pDialog.show();

        Bitmap bm = BitmapFactory.decodeFile(pictureFilePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        bImage = baos.toByteArray();
        matiere = spinnerMatiere.getSelectedItem().toString();
        selectNiveau = spNiveau.getSelectedItem().toString();
        String hourStartMinute =  timebeg.getMinute()<10? "0" : "";
        String hourEndMinute =  timeend.getMinute()<10? "0" : "";
        String hourStart = timebeg.getHour()+ ":" + hourStartMinute + timebeg.getMinute();
        String hourEnd = timeend.getHour()+ ":" + hourEndMinute+timeend.getMinute();
        int hours =timebeg.getCurrentHour();

        final JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("file", Base64Utils.encode(bImage));
            jsonBody.put("idProf", 7);
            jsonBody.put("matiere", matiere);
            jsonBody.put("beghour",hourStart );
            jsonBody.put("endhour", hourEnd);
            jsonBody.put("niveau", selectNiveau);

        } catch (JSONException e) {
            e.printStackTrace();
        }
//        final String mRequestBody = jsonBody.toString();
        Log.d(TAG,"succes hamza: "
                +selectNiveau);




        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest sReq = new StringRequest(  Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        Log.d(TAG, "succes: "
                                +response.toString());
                        pDialog.hide();
                        Toast.makeText(MainActivity.this, "Image  enregistrée avec succes", Toast.LENGTH_SHORT).show();
//                        onPrepareDialog().hide();
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        pDialog.hide();
                        Log.e(TAG, "amine: "+error);
//                        pDialog.hide();
                    }
                }) {

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
                @Override
                public byte[] getBody() throws AuthFailureError {
                    return  jsonBody.toString().getBytes();
                }

        };
        sReq.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 90000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 90000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        requestQueue.add(sReq);
    }

    private void getAllMatiere(){

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(  Request.Method.GET, url_matiere,null,new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response)
                    {

                        Log.d(TAG, "succes test : "
                                +response.toString());
//
                        List<String> al = new ArrayList<>();
                        try {

                            for(int i=0; i < response.length(); i++) {
                                JSONObject jsonobject = response.getJSONObject(i);
                                String title    = jsonobject.getString("intitule");
                                al.add(title);
                            }
                        } catch (JSONException e) {
                            Log.e("JSON Parser", "Error parsing data " + e.toString());
                        }


                        ArrayAdapter<String> aa = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,  al);
                        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                      //Setting the ArrayAdapter data on the Spinner
                        spinnerMatiere.setAdapter(aa);

                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {

                        Log.e(TAG, "amine test amine: "+error);
//                        pDialog.hide();
                    }
                }) {


        };

        requestQueue.add(jsonObjectRequest);
    }
    private void getAllNiveau(){

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(  Request.Method.GET, url_niveau,null,new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response)
            {

                Log.d(TAG, "succes test : "
                        +response.toString());
//
                List<String> listNiveau = new ArrayList<>();
                try {

                    for(int i=0; i < response.length(); i++) {
                        JSONObject jsonobject = response.getJSONObject(i);
                        String intitule    = jsonobject.getString("intitule");
                        listNiveau.add(intitule);
                    }
                } catch (JSONException e) {
                    Log.e("JSON Parser", "Error parsing data " + e.toString());
                }


                ArrayAdapter<String> adNiveau = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,  listNiveau);
                adNiveau.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spNiveau.setAdapter(adNiveau);

            }
        },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {

                        Log.e(TAG, "amine test amine: "+error);
//                        pDialog.hide();
                    }
                }) {

        };

        requestQueue.add(jsonObjectRequest);
    }

}
