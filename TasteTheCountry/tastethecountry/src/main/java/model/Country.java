package model;

import org.json.JSONObject;

public class Country {
    private String name;
    private String region;
    private String capital;
    private long population;
    private String flagUrl;
    private String demonym;
    private String fact;

    public Country(String name, String region, String capital, long population, String flagUrl, String demonym, String fact) {
        this.name = name;
        this.region = region;
        this.capital = capital;
        this.population = population;
        this.flagUrl = flagUrl;
        this.demonym = demonym;
        this.fact = fact;
    }


    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", new JSONObject().put("common", name));
        json.put("region", region);
        json.put("capital", new org.json.JSONArray().put(capital));
        json.put("population", population);
        json.put("flags", new JSONObject().put("svg", flagUrl));
        json.put("fact", fact);
        return json;
    }

    public String getCapital() { return capital; }
    public String getDemonym() { return demonym; }
    public String getName() { return name; }
}