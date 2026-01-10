package model;

import org.json.JSONObject;

public class Meal {
    private String name;
    private String imageUrl;
    private String id;
    private String instructions;
    private String sourceUrl;

    public Meal(String name, String imageUrl, String id, String instructions, String sourceUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.id = id;
        this.instructions = instructions;
        this.sourceUrl = sourceUrl;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("strMeal", name);
        json.put("strMealThumb", imageUrl);
        json.put("idMeal", id);
        json.put("strInstructions", instructions);
        json.put("strSource", sourceUrl);
        return json;
    }
}