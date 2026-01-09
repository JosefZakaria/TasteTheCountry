package main;

import io.github.cdimascio.dotenv.Dotenv;
import model.Country;
import model.Meal;
import model.Weather;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CountryClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String countryApiUrl;
    private final String mealDbUrl;
    private final String weatherApiUrl;
    private final String weatherApiKey;

    public CountryClient() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        this.countryApiUrl = env.get("COUNTRY_API_URL");
        this.mealDbUrl = env.get("MEAL_DB_URL");
        this.weatherApiUrl = env.get("WEATHER_API_URL");
        this.weatherApiKey = env.get("WEATHER_API_KEY");
    }

    public JSONObject getMashup(String searchName) {
        Country country = fetchCountry(searchName);
        if (country == null) return null;

        List<Meal> meals = fetchMeals(country.getDemonym(), country.getName());
        
        if (country.getName().equalsIgnoreCase("Sweden")) {
            meals = new ArrayList<>();
            meals.add(new Meal(
                "Swedish Meatballs", 
                "https://images.services.kitchenstories.io/hP04DDCA2zQ-oTBkgfZDNJ52CHw=/3840x0/filters:quality(85)/images.kitchenstories.io/wagtailOriginalImages/R2854-photo-final-1.jpg", // Exempelbild
                "manual_1",
                "1. Mix meat and spices. 2. Fry in butter. 3. Serve with lingonberries. Smaklig måltid! :)"
            ));
        } else {
            meals = fetchMeals(country.getDemonym(), country.getName());
        }

        if (country == null) return null;

        Weather weather = fetchWeather(country.getCapital());


        JSONObject response = new JSONObject();
        response.put("country", country.toJson());

        if (weather != null) {
            response.put("weather", weather.toJson());
        }

        if (meals != null && !meals.isEmpty()) {
            JSONArray mealsArray = new JSONArray();
            for (Meal m : meals) {
                mealsArray.put(m.toJson());
            }
            response.put("meals", new JSONObject().put("meals", mealsArray));
        }

        return response;
    }


    private Country fetchCountry(String name) {
        String json = makeRequest(countryApiUrl + "/name/" + name);
        if (json == null) return null;

        JSONObject data = new JSONArray(json).getJSONObject(0);

        String commonName = data.getJSONObject("name").getString("common");
        String region = data.getString("region");
        String flag = data.getJSONObject("flags").getString("svg");

        String capital = "London";
        if (data.has("capital")) capital = data.getJSONArray("capital").getString(0);

        String demonym = "Canadian";
        if (data.has("demonyms") && data.getJSONObject("demonyms").has("eng")) {
            demonym = data.getJSONObject("demonyms").getJSONObject("eng").getString("m");
        }

        return new Country(commonName, region, capital, data.getLong("population"), flag, demonym);
    }

    private Weather fetchWeather(String city) {
        String url = weatherApiUrl + "?q=" + city + "&appid=" + weatherApiKey + "&units=metric";
        String json = makeRequest(url);
        if (json == null) return null;

        JSONObject data = new JSONObject(json);
        double temp = data.getJSONObject("main").getDouble("temp");
        String desc = data.getJSONArray("weather").getJSONObject(0).getString("description");

        return new Weather(temp, desc);
    }

    private List<Meal> fetchMeals(String area, String countryName) {
        String cleanArea = area.trim();
        String cleanCountry = countryName.trim();
        String json = null;

        json = makeRequest(mealDbUrl + "filter.php?a=" + cleanArea);

        if (json == null || new JSONObject(json).isNull("meals")) {
            json = makeRequest(mealDbUrl + "search.php?s=" + cleanArea);
        }

        if (json == null || new JSONObject(json).isNull("meals")) {
            json = makeRequest(mealDbUrl + "search.php?s=" + cleanCountry);
        }

        if (json == null || new JSONObject(json).isNull("meals")) return null;

        JSONObject data = new JSONObject(json);
        JSONArray arr = data.getJSONArray("meals");
        JSONObject firstMeal = arr.getJSONObject(0);
        
        String detailsJson = makeRequest(mealDbUrl + "lookup.php?i=" + firstMeal.getString("idMeal"));
        List<Meal> mealList = new ArrayList<>();
        
        if (detailsJson != null) {
            JSONObject m = new JSONObject(detailsJson).getJSONArray("meals").getJSONObject(0);
            mealList.add(new Meal(
                m.getString("strMeal"),
                m.getString("strMealThumb"),
                m.getString("idMeal"),
                m.has("strInstructions") ? m.getString("strInstructions") : ""
            ));
        }
        return mealList;
    }

    private String makeRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.replace(" ", "%20")))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return (response.statusCode() == 200) ? response.body() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}