package site.petrumugurel.weatherapp;

import android.app.NotificationManager;
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
import android.view.View;
import android.widget.Button;
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
    Button   mButton;

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
        mButton = (Button) findViewById(R.id.mainA_RL_BTN_go);

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    String city = mEditText.getText().toString();
                    if (isValidCity(city)) {
                        getForecastForCity(city);
                    }
                }
                return true;
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = mEditText.getText().toString();
                if (isValidCity(city)) {
                    getForecastForCity(city);
                }
            }
        });

    }

    private boolean isValidCity(String city) {
        if (city.length() <= 2) {
            Snackbar.make(findViewById(android.R.id.content),
                          "Too few characters for the city", Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void getForecastForCity(String city) {
        new DownloadCurrentWeather().execute(WEATHER_QUERY_URL + city + OWM_API_KEY);
    }


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

            String weather = "";

            // To extract the weather data
            try {
                JSONObject jsonObject = new JSONObject(result);
                // extract the weather part of the JSON Object
                String weatherInfo = jsonObject.getString("weather");
                Log.i(MainActivity.class.getSimpleName(), weatherInfo);

                // to loop through the weatherInfo mini JSON
                JSONArray jsonArray = new JSONArray(weatherInfo);
                for (int i = 0; i < jsonArray.length(); i++) {
                    // the weatherInfo (a String) can be seen as an array with 2 elements - 2 jsons.
                    JSONObject jsonPart = jsonArray.getJSONObject(i);
                    System.out.println("Arr " + i + " : "+ jsonPart.toString());
                    Log.i("MAIN: ", jsonPart.getString("main") + " - "
                                    + jsonPart.getString("description"));

                    weather = jsonPart.getString("main") + " - "
                              + jsonPart.getString("description");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mTextView.setText(weather);

            mEditText.animate().alpha(0).setDuration(300).start();
            mButton.animate().alpha(0).setDuration(300).start();
            mTextView.animate().alpha(1).setDuration(400).start();

        }
    }

    private void cancelNotifications(long millisDelay, final Integer... notificationID) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                StatusBarNotification[] notifications = mNotifManager.getActiveNotifications();
                for (StatusBarNotification activeNotif : notifications) {
                    for (int notifID : notificationID) {
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
