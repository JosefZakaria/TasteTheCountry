package model;

import org.json.JSONObject;

public class Weather {
    private double temp;
    private String description;

    public Weather(double temp, String description) {
        this.temp = temp;
        this.description = description;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JSONObject main = new JSONObject().put("temp", temp);
        JSONObject weatherObj = new JSONObject().put("description", description);

        json.put("main", main);
        json.put("weather", new org.json.JSONArray().put(weatherObj));
        return json;
    }
}