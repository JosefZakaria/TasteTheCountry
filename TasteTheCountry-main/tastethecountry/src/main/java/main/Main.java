package main;

import io.javalin.Javalin;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {

        CountryClient client = new CountryClient();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(7070);

        app.get("/api/country/{name}", ctx -> {
            String name = ctx.pathParam("name");

            JSONObject response = client.getMashup(name);

            if (response != null) {
                ctx.contentType("application/json").result(response.toString());
            } else {
                ctx.status(404).result("Country not found");
            }
        });

        System.out.println("Server started on http://localhost:7070");
    }
}