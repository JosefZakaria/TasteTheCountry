const searchForm = document.getElementById('searchForm');
const countryInput = document.getElementById('countryInput');

const countryNameEl = document.getElementById('countryName');
const regionEl = document.getElementById('region');
const capitalEl = document.getElementById('capital');
const populationEl = document.getElementById('population');
const flagImgEl = document.getElementById('flagImg');

// New elements for Food and Weather
const mealNameEl = document.getElementById('mealName');
const mealImgEl = document.getElementById('mealImg');
const mealLinkEl = document.getElementById('mealLink');
const tempEl = document.getElementById('temp'); // Make sure you have this ID in HTML
const weatherDescEl = document.getElementById('desc'); // Make sure you have this ID in HTML
const mealInstructionsEl = document.getElementById('mealInstructions');

searchForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    console.log("Search submitted...");

    const query = countryInput.value.trim();
    if (!query) {
        console.log("Empty query");
        return;
    }

    try {
        console.log(`Fetching: /api/country/${query}`);
        const response = await fetch(`/api/country/${query}`);
        console.log("Response status:", response.status);
        
        if (!response.ok) {
            const msg = await response.text();
            console.warn("Response not OK:", msg);
            alert(`Country not found! (Status: ${response.status})`);
            return;
        }

        const data = await response.json();
        console.log("Data received:", data); // Debugging
        updateUI(data);
    } catch (error) {
        console.error('Error fetching data:', error);
        alert('Something went wrong! Check console for details.');
    }
});



function updateUI(data) {
    // 1. LAND-DATA
    const c = data.country;
    if (c) {
        countryNameEl.textContent = c.name.common;
        regionEl.textContent = c.region;
        capitalEl.textContent = c.capital ? c.capital[0] : 'N/A';
        populationEl.textContent = c.population.toLocaleString();
        flagImgEl.src = c.flags.svg;
    }

    // 2. RECEPT-DATA
    // Vi kollar om data.meals finns OCH om listan inuti (meals) har innehåll
    if (data.meals && data.meals.meals && data.meals.meals.length > 0) {
        const meal = data.meals.meals[0]; // Vi tar första receptet

        if(mealNameEl) mealNameEl.textContent = meal.strMeal;
        if(mealImgEl) mealImgEl.src = meal.strMealThumb;
        
        if(mealInstructionsEl) {
            // Om instruktioner finns, visa dem (max 200 tecken)
            const text = meal.strInstructions || "No instructions available.";
            mealInstructionsEl.textContent = text.substring(0, 200) + "...";
        }
        
        if(mealLinkEl) mealLinkEl.href = `https://www.themealdb.com/meal/${meal.idMeal}`;
    } else {
        // Om inga recept hittades i JSON-svaret
        if(mealNameEl) mealNameEl.textContent = "No recipes found";
        if(mealImgEl) mealImgEl.src = ""; 
        if(mealInstructionsEl) mealInstructionsEl.textContent = "Try another country like 'Italy' or 'Turkey'!";
    }

    if (data.weather && data.weather.main) {
        const temp = Math.round(data.weather.main.temp * 10) / 10;
        if(tempEl) tempEl.textContent = temp;
        if(weatherDescEl) weatherDescEl.textContent = data.weather.weather[0].description;
    }
}