package com.example.ani.quickitinerarycalculator;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.lyft.lyftbutton.RideTypeEnum;
import com.lyft.networking.ApiConfig;
import com.lyft.networking.LyftApiFactory;
import com.lyft.networking.apiObjects.CostEstimate;
import com.lyft.networking.apiObjects.CostEstimateResponse;
import com.lyft.networking.apis.LyftPublicApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    // constants
    private static final String TAG = "broken";
    private static final int CURRENT_BUTTON_REQUEST_CODE = 5;
    private static final int DEST_BUTTON_REQUEST_CODE = 6;

    private int PLACE_AUTOCOMPLETE_BUTTON_REQUEST_CODE;
    private String deptAirportCode;
    private String destAirportCode;
    private GoogleApiClient mGoogleApiClient;
    private int monthDate;
    private int yearDate;
    private int dayDate;
    private String deptDate;
    private String destDate;
    private float flightCost;
    private ApiConfig apiConfig;
    private LatLng startLatLngDept;
    private com.google.maps.model.LatLng startLatLngAirport;
    private com.google.maps.model.LatLng destLatLngAirport;
    private LatLng destLatLngDest;
    private GeoApiContext geoApiContext;
    private float lyftDeptCost;
    private float lyftDestCost;
    private String finalCost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize API client for Google Places API as mentioned in Google Places API documentation
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        // keep keyboard minimized at launch so it doesn't focus on the EditText elements
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // initialize Lyft API instance as per Lyft documentation
        apiConfig = new ApiConfig.Builder()
                .setClientId(BuildConfig.LYFT_CLIENT_ID)
                .setClientToken(BuildConfig.LYFT_CLIENT_TOKEN)
                .build();

        // initialize Google Geocoder API instance as per documentation
        geoApiContext = new GeoApiContext.Builder()
                .apiKey(BuildConfig.GOOGLE_GEOCODER_APIKEY)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * Creates the Google Places overlay fragment such that users can search for a location using Google Maps search
     *
     * @param view
     */
    public void findPlace(View view) {
        // set no type filter, effectively letting the user type whatever they want
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE).build();
        try {
            // create the overlay as per Google documentation
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(typeFilter)
                    .build(this);
            // set the variable based on which button was pressed for detection in onActivityResult()
            switch (view.getId()) {
                case R.id.currentLocButton:
                    PLACE_AUTOCOMPLETE_BUTTON_REQUEST_CODE = CURRENT_BUTTON_REQUEST_CODE;
                    break;
                case R.id.destLocButton:
                    PLACE_AUTOCOMPLETE_BUTTON_REQUEST_CODE = DEST_BUTTON_REQUEST_CODE;
                    break;
            }
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_BUTTON_REQUEST_CODE);

        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {

        }
    }

    /**
     * Retrieves the information that the user selected in the Google Places fragment above
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // check if requestCode came from a button
        if (requestCode == PLACE_AUTOCOMPLETE_BUTTON_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // retrieve information about the selected location
                Place place = PlaceAutocomplete.getPlace(this, data);

                // check which button it came from and set TextView accordingly
                switch (PLACE_AUTOCOMPLETE_BUTTON_REQUEST_CODE) {
                    case CURRENT_BUTTON_REQUEST_CODE:
                        startLatLngDept = place.getLatLng();
                        TextView currentText = findViewById(R.id.currentLocText);
                        currentText.setText(place.getName());
                        break;
                    case DEST_BUTTON_REQUEST_CODE:
                        destLatLngDest = place.getLatLng();
                        TextView destText = findViewById(R.id.destLocText);
                        destText.setText(place.getName());
                        break;
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i(TAG, status.getStatusMessage());
            }
        }
    }

    /**
     * Select the date with a DatePickerDialog, store it, and display selected date to user
     *
     * @param view
     */
    public void DateSet(View view) {
        int buttonVal;
        // check which button was tapped
        if (view.getId() == R.id.depDateButton) {
            buttonVal = 1;
        } else {
            buttonVal = 2;
        }
        final Calendar myCalendar = Calendar.getInstance();
        final int finalButtonVal = buttonVal;
        // create DatePickerDialog listener to retrieve information once user selects the date
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, month);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                monthDate = month + 1;
                yearDate = year;
                dayDate = dayOfMonth;
                // display the text underneath the button depending on which button is selected

                if (finalButtonVal == 1) {
                    deptDate = String.valueOf(yearDate) + "-" + String.valueOf(monthDate) + "-" + String.valueOf(dayDate);
                    String displayText = "Date: " + monthDate + "/" + dayDate + "/" + yearDate;
                    TextView fromDateText = findViewById(R.id.fromDateText);
                    fromDateText.setText(displayText);
                }
                if (finalButtonVal == 2) {
                    destDate = String.valueOf(yearDate) + "-" + String.valueOf(monthDate) + "-" + String.valueOf(dayDate);
                    String displayText = "Date: " + monthDate + "/" + dayDate + "/" + yearDate;
                    TextView returnDateText = findViewById(R.id.returnDateText);
                    returnDateText.setText(displayText);
                }
            }
        };
        // create DatePickerDialog
        new DatePickerDialog(this, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();


    }

    /**
     * Calculate the cost of the trip using Google's QPX Express API and the Lyft API
     *
     * @param view
     */
    public void calculateCost(View view) {
        // retrieve airport code from user
        EditText deptAirportCodeText = findViewById(R.id.deptAirportEdit);
        deptAirportCode = deptAirportCodeText.getText().toString();
        EditText destAirportCodeText = findViewById(R.id.destAirportEdit);
        destAirportCode = destAirportCodeText.getText().toString();
        // hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // execute networking tasks in the background
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // use Google's QPX Express API to do a search as per documentation
                    // set up URL connection, create JSON request body, and send POST response
                    // JSON body is constructed as per QPX Express API documentation
                    URL searchPoint = new URL("https://www.googleapis.com/qpxExpress/v1/trips/search?key=" + BuildConfig.GOOGLE_QPXEXPRESS_APIKEY);
                    HttpsURLConnection myConnection = (HttpsURLConnection) searchPoint.openConnection();
                    myConnection.setDoOutput(true);
                    JSONObject request = new JSONObject();
                    JSONObject requestHolder = new JSONObject();
                    JSONObject passengers = new JSONObject();
                    passengers.put("adultCount", 1);
                    JSONArray data = new JSONArray();
                    JSONObject departure = new JSONObject();
                    departure.put("origin", deptAirportCode);
                    departure.put("destination", destAirportCode);
                    departure.put("date", deptDate);
                    departure.put("maxStops", 0);
                    JSONObject returnTrip = new JSONObject();
                    returnTrip.put("origin", destAirportCode);
                    returnTrip.put("destination", deptAirportCode);
                    returnTrip.put("date", destDate);
                    returnTrip.put("maxStops", 0);
                    data.put(departure);
                    data.put(returnTrip);
                    requestHolder.put("passengers", passengers);
                    requestHolder.put("slice", data);
                    requestHolder.put("solutions", "1");
                    request.put("request", requestHolder);
                    myConnection.setRequestMethod("POST");
                    myConnection.setRequestProperty("Content-Type", "application/json");
                    // write data to URLConnection
                    myConnection.getOutputStream().write(request.toString().getBytes());
                    Log.d("JSONRequest", request.toString());
                    // if request is OK
                    if (myConnection.getResponseCode() == 200) {
                        // Retrieve the JSON response using Jackson library to map json to a HashMap
                        InputStream responseBody = myConnection.getInputStream();
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> jsonMap = mapper.readValue(responseBody, Map.class);
                        // traverse through JSON body and retrieve saleTotal for the total cost of the trip
                        Map map = ((Map) jsonMap.get("trips"));
                        ArrayList tripOption = ((ArrayList) map.get("tripOption"));
                        Map trip = (Map) tripOption.get(0);
                        flightCost = Float.parseFloat(((String) trip.get("saleTotal")).substring(3));
                    } else {
                        Log.d("ConnectionError", myConnection.getErrorStream().toString());
                    }
                    myConnection.disconnect();

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }

                try {
                    // given the airport code retrieve from user input, find Lat/Long with Geocoding API
                    GeocodingResult[] startLatLngGeocode = GeocodingApi.newRequest(geoApiContext)
                            .address(deptAirportCode)
                            .await();
                    GeocodingResult[] deptLatLngGeocode = GeocodingApi.newRequest(geoApiContext)
                            .address(destAirportCode)
                            .await();
                    startLatLngAirport = startLatLngGeocode[0].geometry.location;
                    destLatLngAirport = deptLatLngGeocode[0].geometry.location;
                } catch (ApiException | InterruptedException | IOException e) {
                    e.printStackTrace();
                }
                // Using Lyft API as per Lyft API Wrapper documentation to search for a ride
                LyftPublicApi lyftPublicApi = new LyftApiFactory(apiConfig).getLyftPublicApi();
                Call<CostEstimateResponse> costEstimateCallDept = lyftPublicApi.getCosts(startLatLngDept.latitude, startLatLngDept.longitude, RideTypeEnum.CLASSIC.toString(), startLatLngAirport.lat, startLatLngAirport.lng);
                costEstimateCallDept.enqueue(new Callback<CostEstimateResponse>() {
                    @Override
                    public void onResponse(Call<CostEstimateResponse> call, Response<CostEstimateResponse> response) {
                        CostEstimateResponse result = response.body();
                        for (CostEstimate costEstimate : result.cost_estimates) {
                            // store average cost of ride from departing address to the airport
                            lyftDeptCost = (float) (((costEstimate.estimated_cost_cents_min / 100) + (costEstimate.estimated_cost_cents_max / 100)) / 2);
                            //Log.d("MyApp", "Min: " + String.valueOf(costEstimate.estimated_cost_cents_min / 100) + "$");
                            //Log.d("MyApp", "Max: " + String.valueOf(costEstimate.estimated_cost_cents_max / 100) + "$");
                            //Log.d("MyApp", "Distance: " + String.valueOf(costEstimate.estimated_distance_miles) + " miles");
                            //Log.d("MyApp", "Duration: " + String.valueOf(costEstimate.estimated_duration_seconds / 60) + " minutes");
                        }
                    }

                    @Override
                    public void onFailure(Call<CostEstimateResponse> call, Throwable t) {
                        Log.d("MyApp", t.toString());
                    }
                });
                // Using Lyft API as per Lyft API Wrapper documentation to search for a ride
                Call<CostEstimateResponse> costEstimateCallDest = lyftPublicApi.getCosts(destLatLngAirport.lat, destLatLngAirport.lng, RideTypeEnum.CLASSIC.toString(), destLatLngDest.latitude, destLatLngDest.longitude);
                costEstimateCallDest.enqueue(new Callback<CostEstimateResponse>() {
                    @Override
                    public void onResponse(Call<CostEstimateResponse> call, Response<CostEstimateResponse> response) {
                        CostEstimateResponse result = response.body();
                        for (CostEstimate costEstimate : result.cost_estimates) {
                            // store average cost of ride from the destination airport to the destination address
                            lyftDestCost = (float) (((costEstimate.estimated_cost_cents_min / 100) + (costEstimate.estimated_cost_cents_max / 100)) / 2);
                            Log.d("MyApp", "Min: " + String.valueOf(costEstimate.estimated_cost_cents_min / 100) + "$");
                            Log.d("MyApp", "Max: " + String.valueOf(costEstimate.estimated_cost_cents_max / 100) + "$");
                            Log.d("MyApp", "Distance: " + String.valueOf(costEstimate.estimated_distance_miles) + " miles");
                            Log.d("MyApp", "Duration: " + String.valueOf(costEstimate.estimated_duration_seconds / 60) + " minutes");
                        }
                    }

                    @Override
                    public void onFailure(Call<CostEstimateResponse> call, Throwable t) {
                        Log.d("MyApp", t.toString());
                    }
                });
                // HACK: wait until the Lyft API actually returns a value, assumes that the user has put in proper inputs
                // The Lyft API returns zero if the ride is out of range, so this may cause an infinite loop
                // Fix is to redo the AsyncTask portion of the code and the code below can be moved into onPostExecute() of AsyncTask
                while (lyftDeptCost == 0 || lyftDestCost == 0) {

                }
                // calculate final cost and show the user
                float floatCost = (float) (flightCost + (lyftDeptCost * 2.0) + (lyftDestCost * 2.0));
                finalCost = "Total Cost: $" + String.format(Locale.US, "%.2f", floatCost);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView costFinal = findViewById(R.id.finalCostView);
                        costFinal.setText(finalCost);
                    }
                });
            }
        });
        // scroll to the bottom of the view
        ScrollView scroll = findViewById(R.id.scrollView);
        scroll.fullScroll(View.FOCUS_DOWN);
    }
}
