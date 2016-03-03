package site.petrumugurel.weatherapp;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String OWM_API_KEY           = "&APPID=3c74a8bc2fd594a33ed13471ca2ad580";
    private static final String WEATHER_QUERY_URL     = "http://api.openweathermap.org/data/2" +
                                                        ".5/weather?q=";
    private static final int    GET_FORECAST_NOTIF_ID = 0x0101;

    private Handler mHandler = new Handler();
    private NotificationManager mNotifManager;
    NotificationCompat.Builder mNotifBuilder;

    EditText mEditText;
    TextView mTextView;
//    Button   mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mNotifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifBuilder = new NotificationCompat.Builder(this);
        mNotifBuilder.setContentTitle(getString(R.string.app_name));

        mEditText = (EditText) findViewById(R.id.mainA_RL_ET_enterCity);
        mTextView = (TextView) findViewById(R.id.mainA_RL_TV_forecast);
//        mButton = (Button) findViewById(R.id.mainA_RL_BTN_go);

        // To allow starting the query when user presses "Enter" on the softkey keyboard.
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_DONE)
                    || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        && (event.getAction() == KeyEvent.ACTION_DOWN))) {

                    // To hide the soft keyboard when user presses "Enter"
                    InputMethodManager imm
                            = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    String city = mEditText.getText().toString();
                    if (isValidCity(city)) {
                        JSONObject forecast = downloadForecastForCity(city);
                        ArrayList<String> weatherInfo
                                = getWeatherInfo(forecast,
                                                 new String[]{"name"},
                                                 new String[]{"sys", "country"},
                                                 new String[]{"weather", "description"},
                                                 new String[]{"main", "temp"},
                                                 new String[]{"main", "pressure"});

                        StringBuilder sb = new StringBuilder(weatherInfo.get(2))
                                .append(" in ").append(weatherInfo.get(0)).append(" - ")
                                .append(weatherInfo.get(1)).append(" with a temperature of ")
                                .append(String.format
                                        ("%.1f", Float.parseFloat(weatherInfo.get(3))))
                                .append(" °C");
                        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                        mTextView.setText(sb.toString());
                    }
                }
                return true;
            }
        });

    }


    /**
     * Cool method which allows to query a JSONObject based on various fields - basically any
     * name-value pair that is contained in the result from
     * {@code http://api.openweathermap.org/data/2.5 ...}.
     *
     * @param forecast JSONObject representing the response get from interrogating
     *                                  {@code http://api.openweathermap.org/data/2.5...}
     *                                  for the weather in a specific city.<br><br>
     * @param forecastQueriedProperties varying number of Strings representing name-value pairs
     *                                  for which to query the {@code forecast} JSONObject
     * @return The values for the queried fields, in the query order.
     */
    private ArrayList<String> getWeatherInfo(JSONObject forecast, String[]...
            forecastQueriedProperties) {

        /** This will store the forecast to be returned */
        ArrayList<String> weather = new ArrayList<>(forecastQueriedProperties.length);
//        Log.e("Forecast:", forecast.toString());

        try {
            for (String[] properties : forecastQueriedProperties) {
                JSONObject tempJSON = forecast;
                String queriedProperty = "";
                for (int i = 0; i < properties.length; i++) {
//                    Log.e("Querying JSON for ", properties[i]);
                    if (i + 1 < properties.length) {
//                        Log.e("Current JSON", tempJSON.getString(properties[i]));
                        // weather is a JSONArray with just one element
                        if (properties[i].equals("weather")) {
                            String wS = tempJSON.getString(properties[i]);
                            tempJSON = new JSONArray(wS).getJSONObject(0);
                        }
                        else {
                            tempJSON = new JSONObject(tempJSON.getString(properties[i]));
                        }
                    }
                    else {  // we have the queried forecast property
                        queriedProperty = tempJSON.getString(properties[i]);
                    }

                    if (properties[i].equals("temp")) {
                        queriedProperty = getCelsiusFromKelvin(queriedProperty);
                    }
                }
                weather.add(queriedProperty);
//                Log.d("Current weather is ", weather.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return weather;
    }

    private String getCelsiusFromKelvin(String kelvinTemp) {
        double kelvins = Double.parseDouble(kelvinTemp.toString());
        double celsius = kelvins - 273.15;

        return (Double.toString(celsius));
    }

    /**
     * Simple check to mitigate invalid queries for invalid cities. Can be made more complex later.
     * @param city name of the city to get the weather for, as entered by the user.
     * @return
     */
    private boolean isValidCity(String city) {
        if (city.length() <= 2) {
            Snackbar.make(findViewById(android.R.id.content),
                          "Too few characters for the city name", Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Will use an AsyncTask to download the response from
     * {@code http://api.openweathermap.org/data/2.5...}
     * representing the forecast for a specific city.
     * <br>It assumes that static fields {@link #WEATHER_QUERY_URL} and {@link #OWM_API_KEY}
     * contain valid data for constructing the URL for the {@code openweathermap} query.
     * @param city Name of the city for which the user wants to get current weather.
     * @return The forecast returned by {@code http://api.openweathermap.org/data/2.5...} .
     */
    private JSONObject downloadForecastForCity(String city) {
        // An ASCII space in the city will break the query on openweathermap
        city = city.replaceAll(" ", "%20");
        String forecast = "";
        try {
            forecast = new DownloadCurrentWeather()
                    .execute(WEATHER_QUERY_URL + city + OWM_API_KEY).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        try {
            return new JSONObject(forecast);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * To be used for downloading the response from interrogating
     * {@code http://api.openweathermap.org/data/2.5...}
     * for the weather of a specific city.
     */
    private class DownloadCurrentWeather extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                String result = "";
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();
                while (data != -1) {
                    result += ((char) data);
                    data = reader.read();
                }

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            mNotifBuilder.setContentText("Getting latest forecast ...")
                         .setSmallIcon(R.drawable.ic_file_download_white_24dp)
                         .setProgress(100, 0, true)
                         .setOngoing(true);
            mNotifManager.notify(GET_FORECAST_NOTIF_ID, mNotifBuilder.build());
        }

        @Override
        protected void onPostExecute(String result) {
            mNotifBuilder.setContentText("latest forecast prepared ...")
                         .setProgress(1, 1, false);
            mNotifManager.notify(GET_FORECAST_NOTIF_ID, mNotifBuilder.build());
            cancelNotifications(1000 * 5, GET_FORECAST_NOTIF_ID);

            Log.i(MainActivity.class.getSimpleName(), result);

        }
    }

    /**
     * Helper function which allows our notifications to auto-cancel after a specified period of
     * time.
     * @param millisDelay delay after which to cancel the indicated notification(s).
     * @param notificationIDs which notification to cancel.
     */
    private void cancelNotifications(long millisDelay, final Integer... notificationIDs) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                StatusBarNotification[] notifications = mNotifManager.getActiveNotifications();
                for (StatusBarNotification activeNotif : notifications) {
                    for (int notifID : notificationIDs) {
                        if (activeNotif.getId() == notifID) {
                            mNotifManager.cancel(notifID);
                        }
                    }
                }
            }
        }, millisDelay);
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;    // we've built the menu, want it displayed
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem menuItem) {
//        super.onOptionsItemSelected(menuItem);
//
//
//        switch (menuItem.getItemId()) {
//            case R.id.mainM_I_settyings:
//                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
//                                                  "No available settyings atm",
//                                                  Snackbar.LENGTH_LONG);
//
//                View snackbarView = snackbar.setActionTextColor(Color.MAGENTA).getView();
//                snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//                snackbarView.animate().rotationX(-360).setDuration(700);
//                snackbar.setAction("Ok", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                    }
//                });
//                snackbar.show();
//        }
//
//        return true;    // we've handled the menuItem, don't need other listeners to check it
//    }
}
