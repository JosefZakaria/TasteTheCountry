# API Reference: Taste The Country
This API provides a "mashup" of information about countries, including geographic data, current weather in the capital city, and popular recipes from the region.

## Endpoints
Get Country Data
Returns a combination of country information, weather, and recipes based on the country's name.

* URL: /api/country/{name}

* Method: GET

* URL Parameters:

    * name (Required): The name of the country in English (e.g., sweden or turkey).

## Response Body (JSON)
Upon a successful request (Status 200), a JSON object is returned with the following structure:
```
{
  "country": {
    "name": { "common": "Turkey" },
    "region": "Asia",
    "capital": ["Ankara"],
    "population": 85664944,
    "flags": { "svg": "https://restcountries.com/data/tur.svg" },
    "fact": "The flag of Turkey has a red field..."
  },
  "weather": {
    "main": { "temp": 0.5 },
    "weather": [
      { "description": "overcast clouds" }
    ]
  },
  "meals": {
    "meals": [
      {
        "strMeal": "Adana kebab",
        "strMealThumb": "https://www.themealdb.com/images/...",
        "idMeal": "52808",
        "strInstructions": "Step 1: Finely chop the peppers..."
      }
    ]
  }
}
```

## Error Handling
The API uses standard HTTP status codes and returns error messages in JSON format to assist external developers.

### 404 Not Found
Returned if the country could not be found in the database.

```
{
  "error": "Country not found"
}
```
### 500 Internal Server Error
Returned in case of unexpected server errors or issues with external API calls.

```
{
  "error": "Internal Server Error: [Detailed message]"
}
```
