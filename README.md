Everybody has to have a weather app under it's belt, right?

Here's mine.

[![Android - weather app](http://oi66.tinypic.com/34eb32g.jpg)](https://www.youtube.com/watch?v=EhTMw2BPakU "Android - weather app")



Simple app (can be built a lot on top of it, if time) which uses data from
http://api.openweathermap.org/data/2.5  similar to  
`{"coord":{"lon":2.35,"lat":48.85},"weather":[{"id":800,"main":"Clear","description":"clear sky","icon":"02n"}],"base":"cmc stations","main":{"temp":277.571,"pressure":1009.4,"humidity":67,"temp_min":277.571,"temp_max":277.571,"sea_level":1021.74,"grnd_level":1009.4},"wind":{"speed":2.18,"deg":286.001},"clouds":{"all":8},"dt":1457032380,"sys":{"message":0.006,"country":"FR","sunrise":1456986378,"sunset":1457026741},"id":2988507,"name":"Paris","cod":200}`

from which with simple JSON methods various weather properties are extracted.

Nothing too complicated but I'm really proud of a method I've built which allows to easily query  the openweathermap forecast JSON to get any property(es) desired.
```java
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
```


As background image used a photo by Ales Krivec - https://unsplash.com/photos/ywtbSuCSjhM

