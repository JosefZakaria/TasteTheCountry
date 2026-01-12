package main;

import io.github.cdimascio.dotenv.Dotenv;
import model.Country;
import model.Meal;
import model.Weather;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CountryClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String countryApiUrl;
    private final String mealDbUrl;
    private final String weatherApiUrl;
    private final String weatherApiKey;


    public CountryClient() {
        System.out.println("Initializing CountryClient...");
        System.out.println("Working Directory: " + System.getProperty("user.dir"));

        Dotenv env = null;
        try {
            // Try explicit path first (best for Maven projects running from root)
            // We look in current dir, then in ./TasteTheCountry-main/tastethecountry
            
             try {
                env = Dotenv.configure().ignoreIfMissing().load();
            } catch (Exception ignored) {}

            if (env == null || env.get("COUNTRY_API_URL") == null) {
                System.out.println("Trying alternative paths for .env...");
                 try {
                    env = Dotenv.configure()
                        .directory("./TasteTheCountry-main/tastethecountry")
                        .ignoreIfMissing()
                        .load();
                } catch (Exception ignored) {}
            }
            
            if (env == null || env.get("COUNTRY_API_URL") == null) {
                 try {
                    env = Dotenv.configure()
                        .directory("./tastethecountry")
                        .ignoreIfMissing()
                        .load();
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            System.err.println("Error loading .env: " + e.getMessage());
        }

        if (env != null) {
            this.countryApiUrl = env.get("COUNTRY_API_URL");
            this.mealDbUrl = env.get("MEAL_DB_URL");
            this.weatherApiUrl = env.get("WEATHER_API_URL");
            this.weatherApiKey = env.get("WEATHER_API_KEY");
        } else {
             // Fallback to hardcoded or null (logging warning)
             this.countryApiUrl = null;
             this.mealDbUrl = null;
             this.weatherApiUrl = null;
             this.weatherApiKey = null;
        }

        System.out.println("Configuration loaded. API URL: " + this.countryApiUrl);
        
        if (this.countryApiUrl == null) {
            System.err.println("WARNING: COUNTRY_API_URL is missing. App will not work correctly.");
        }
    }

    public JSONObject getMashup(String searchName) {
        Country country = fetchCountry(searchName);
        if (country == null) return null;

        List<Meal> meals;
        
        if (country.getName().equalsIgnoreCase("Sweden")) {
            meals = new ArrayList<>();
            meals.add(new Meal(
                "Swedish Meatballs", 
                "https://images.services.kitchenstories.io/hP04DDCA2zQ-oTBkgfZDNJ52CHw=/3840x0/filters:quality(85)/images.kitchenstories.io/wagtailOriginalImages/R2854-photo-final-1.jpg", // Exempelbild
                "manual_1",
                "1. Mix meat and spices. 2. Fry in butter. 3. Serve with lingonberries. Smaklig måltid! :)",
                "https://kitchenstories.com/en/recipes/traditional-swedish-meatballs"
            ));
        } else {
            meals = fetchMeals(country.getDemonym(), country.getName());
        }

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
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String json = makeRequest(countryApiUrl + "/name/" + encodedName);
        if (json == null) return null;

        JSONArray jsonArray = new JSONArray(json);
        if (jsonArray.isEmpty()) {
            return null;
        }

        JSONObject data = jsonArray.getJSONObject(0);
        String commonName = data.getJSONObject("name").getString("common");
        String region = data.getString("region");
        String flag = data.getJSONObject("flags").getString("svg");

        String capital = "Unknown";
        if (data.has("capital")) capital = data.getJSONArray("capital").getString(0);

        String demonym = "Unknown";
        if (data.has("demonyms") && data.getJSONObject("demonyms").has("eng")) {
            demonym = data.getJSONObject("demonyms").getJSONObject("eng").getString("m");
        }

        String fact = "No specific fact found.";
        JSONObject flags = data.getJSONObject("flags");
        
        if (flags.has("alt") && !flags.getString("alt").isEmpty()) {
            fact = flags.getString("alt"); // Använder beskrivningen av flaggan från API:et
        } else {
            boolean landlocked = data.optBoolean("landlocked", false);
            String carSide = data.getJSONObject("car").optString("side", "right");
            fact = commonName + " is " + (landlocked ? "landlocked" : "not landlocked") + 
                " and they drive on the " + carSide + " side.";
        }

        return new Country(commonName, region, capital, data.getLong("population"), flag, demonym, fact);
    }

    private Weather fetchWeather(String city) {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = weatherApiUrl + "?q=" + encodedCity + "&appid=" + weatherApiKey + "&units=metric";
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
        
        String encodedArea = URLEncoder.encode(cleanArea, StandardCharsets.UTF_8);
        String encodedCountry = URLEncoder.encode(cleanCountry, StandardCharsets.UTF_8);

        String json = null;

        json = makeRequest(mealDbUrl + "filter.php?a=" + encodedArea);

        if (json == null || new JSONObject(json).isNull("meals")) {
            json = makeRequest(mealDbUrl + "search.php?s=" + encodedArea);
        }

        if (json == null || new JSONObject(json).isNull("meals")) {
            json = makeRequest(mealDbUrl + "search.php?s=" + encodedCountry);
        }

        if (json == null || new JSONObject(json).isNull("meals")) return null;

        JSONObject data = new JSONObject(json);
        JSONArray arr = data.getJSONArray("meals");
        JSONObject firstMeal = arr.getJSONObject(0);
        
        String detailsJson = makeRequest(mealDbUrl + "lookup.php?i=" + firstMeal.getString("idMeal"));
        List<Meal> mealList = new ArrayList<>();
        
        if (detailsJson != null) {
            JSONObject m = new JSONObject(detailsJson).getJSONArray("meals").getJSONObject(0);
            
            String source = m.has("strSource") ? m.optString("strSource") : "";
            if (source == null || source.isEmpty()) {
                source = m.has("strYoutube") ? m.optString("strYoutube") : "";
            }

            mealList.add(new Meal(
                m.getString("strMeal"),
                m.getString("strMealThumb"),
                m.getString("idMeal"),
                m.has("strInstructions") ? m.getString("strInstructions") : "",
                source
            ));
        }
        return mealList;
    }

    private String makeRequest(String url) {
        if (url == null || url.contains("null")) {
            System.err.println("Ogiltig URL (innehåller 'null'): " + url);
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.replace(" ", "%20")))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return (response.statusCode() == 200) ? response.body() : null;
        } catch (Exception e) {
            System.err.println("Fel vid anrop till: " + url);
            e.printStackTrace();
            return null;
        }
    }
}