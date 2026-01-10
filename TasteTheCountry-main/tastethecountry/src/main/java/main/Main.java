package main;

import io.javalin.Javalin;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        try {
            CountryClient client = new CountryClient();

            Javalin app = Javalin.create(config -> {
                config.staticFiles.add("/public");
            }).start(7070);

            app.get("/api/country/{name}", ctx -> {
                try {
                String name = ctx.pathParam("name");

                JSONObject response = client.getMashup(name);

                if (response != null) {
                    ctx.contentType("application/json").result(response.toString());
                } else {
                    ctx.status(404).json(new JSONObject().put("error", "Country not found"));
                }
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.status(500).json(new JSONObject().put("error", "Internal Server Error: " + e.getMessage()));
                }
            });

            System.out.println("Server started on http://localhost:7070");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR DURING STARTUP:");
            e.printStackTrace();
        }
    }
}