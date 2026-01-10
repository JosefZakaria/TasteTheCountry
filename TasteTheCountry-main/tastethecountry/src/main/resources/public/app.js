(() => {
    "use strict";
  
    // ----- Elements -----
    const form = document.querySelector("#searchForm");
    const input = document.querySelector("#countryInput");
  
    const countryNameEl = document.querySelector("#countryName");
    const regionEl = document.querySelector("#region");
    const capitalEl = document.querySelector("#capital");
    const populationEl = document.querySelector("#population");
    const flagImgEl = document.querySelector("#flagImg");
    const countryFactEl = document.querySelector("#countryFact");
  
    const mealTagEl = document.querySelector("#mealTag"); // kan vara null om inte finns i HTML
    const mealImgEl = document.querySelector("#mealImg");
    const mealNameEl = document.querySelector("#mealName");
    const mealLinkEl = document.querySelector("#mealLink");
    const mealInstructionsEl = document.querySelector("#mealInstructions");
  
    const tempEl = document.querySelector("#temp");
    const descEl = document.querySelector("#desc");
  
    const lastSearchEl = document.querySelector("#lastSearch");
    const statusEl = document.querySelector("#status");
    const errorEl = document.querySelector("#error");
  
    // ----- State -----
    let currentController = null;
    let requestSeq = 0;

    // ----- Defaults -----
    const BUTTON_TEXT = {
        idle: "Search",
        loading: "Loading…"
      };

    const DEFAULTS = {
      mealInstructions: "Search for a country to see recipes...",
      mealName: "—",
      mealTag: "—",
      weatherDesc: "—"
    };

    function setLink(el, url) {
        if (!el) return;
        const safeUrl = (url && typeof url === "string") ? url.trim() : "";
        const disabled = !safeUrl || safeUrl === "#";

        el.href = disabled ? "#" : safeUrl;
        el.classList.toggle("is-disabled", disabled);
        el.setAttribute("aria-disabled", String(disabled));
        el.tabIndex = disabled ? -1 : 0;
  }

    // ----- UI helpers -----
    function setStatus(text) {
      if (statusEl) statusEl.textContent = text;
    }
  
    function showError(message) {
      if (!errorEl) return;
      errorEl.textContent = message;
      errorEl.hidden = false;
      setStatus("Error");
    }
  
    function clearError() {
      if (!errorEl) return;
      errorEl.textContent = "";
      errorEl.hidden = true;
    }
  
    function setLoading(isLoading) {
      if (!form) return;
      const btn = form.querySelector("button[type='submit']");
      if (btn) {
       btn.disabled = isLoading;
       btn.textContent = isLoading ? BUTTON_TEXT.loading : BUTTON_TEXT.idle;
      }
      if (input) input.disabled = isLoading;
      setStatus(isLoading ? "Loading…" : "Ready");
    }
  
    function normalizeCountry(value) {
      return value.trim().replace(/\s+/g, " ");
    }
  
    function formatNumber(n) {
      try {
        return new Intl.NumberFormat("sv-SE").format(n);
      } catch {
        return String(n);
      }
    }
  
    function resetUI() {
      if (countryNameEl) countryNameEl.textContent = "—";
      if (regionEl) regionEl.textContent = "—";
      if (capitalEl) capitalEl.textContent = "—";
      if (populationEl) populationEl.textContent = "—";
      if (countryFactEl) countryFactEl.textContent = "—";
  
      if (flagImgEl) {
        flagImgEl.removeAttribute("src");
        flagImgEl.alt = "Flag";
      }
  
      // Meal
      if (mealTagEl) mealTagEl.textContent = DEFAULTS.mealTag;
      if (mealNameEl) mealNameEl.textContent = DEFAULTS.mealName;
      if (mealImgEl) mealImgEl.removeAttribute("src");
      setLink(mealLinkEl, "#");
      if (mealInstructionsEl) mealInstructionsEl.textContent = DEFAULTS.mealInstructions;
  
      // Weather
      if (tempEl) tempEl.textContent = "—";
      if (descEl) descEl.textContent = DEFAULTS.weatherDesc;
    }
  
    // ----- Parsing helpers (country) -----
    function pickCountryObject(data) {
      // Ex: RestCountries direct array response
      if (Array.isArray(data) && data.length > 0) return data[0];
  
      // Common mashup shapes
      if (data && typeof data === "object") {
        if (data.country) return data.country;
        if (data.countryData) return data.countryData;
        if (data.data) return data.data;
        if (data.result) return data.result;
      }
  
      return null;
    }
  
    function extractCountryFields(countryObj) {
      const name =
        countryObj?.name?.common ??
        countryObj?.name ??
        countryObj?.countryName ??
        "—";
  
      const region =
        countryObj?.region ??
        countryObj?.continents?.[0] ??
        "—";
  
      const capital =
        (Array.isArray(countryObj?.capital) ? countryObj.capital[0] : countryObj?.capital) ??
        "—";
  
      const population =
        typeof countryObj?.population === "number"
          ? formatNumber(countryObj.population)
          : (countryObj?.population ?? "—");
  
      const flagUrl =
        countryObj?.flags?.png ??
        countryObj?.flags?.svg ??
        countryObj?.flagUrl ??
        "";

      const fact = countryObj?.fact ?? "—";
  
      return { name, region, capital, population, flagUrl, fact };
    }
  
    function renderCountry(fields) {
      const { name, region, capital, population, flagUrl, fact } = fields;
  
      if (countryNameEl) countryNameEl.textContent = name ?? "—";
      if (regionEl) regionEl.textContent = region ?? "—";
      if (countryFactEl) countryFactEl.textContent = fact ?? "—";
      if (capitalEl) capitalEl.textContent = capital ?? "—";
      if (populationEl) populationEl.textContent = population ?? "—";
  
      if (flagImgEl) {
        if (flagUrl) {
          flagImgEl.src = flagUrl;
          flagImgEl.alt = `Flag: ${name}`;
        } else {
          flagImgEl.removeAttribute("src");
          flagImgEl.alt = "Flag not available";
        }
      }
    }
  
    // ----- Parsing helpers (meal) -----
    function pickMealObject(data) {
      // app.js 2 style: data.meals.meals[0]
      const m1 = data?.meals?.meals?.[0];
      if (m1) return m1;
  
      // app.js 1 "maybe" style: data.meal / data.recipe / etc
      const m2 = data?.meal ?? data?.recipe ?? data?.mealData ?? data?.themealdb ?? null;
      if (!m2) return null;
  
      // If it's already a list from some API wrapper
      if (Array.isArray(m2) && m2.length > 0) return m2[0];
  
      // If it's a single object
      if (m2 && typeof m2 === "object") return m2;
  
      return null;
    }
  
    function renderMealMaybe(data) {
      const meal = pickMealObject(data);
      if (!meal) {
        if (mealTagEl) mealTagEl.textContent = DEFAULTS.mealTag;
        if (mealNameEl) mealNameEl.textContent = "No recipe found";
        if (mealImgEl) mealImgEl.removeAttribute("src");
        setLink(mealLinkEl, "#");
        if (mealInstructionsEl) mealInstructionsEl.textContent = "Try another country (e.g. Italy or Turkey).";
        return;
      }
  
      const mealName = meal?.strMeal ?? meal?.name ?? "—";
      const mealImg = meal?.strMealThumb ?? meal?.image ?? "";
      const area = meal?.strArea ?? meal?.area ?? "";
      const category = meal?.strCategory ?? meal?.category ?? "";
      const tag = [area, category].filter(Boolean).join(" · ") || "—";
  
      // Länk: app.js 2 använder themealdb-id
      const mealLink =
        meal?.strSource ??
        meal?.source ??
        (meal?.idMeal ? `https://www.themealdb.com/api/json/v1/1/lookup.php?i=${meal.idMeal}` : "#");
  
      const instructions = meal?.strInstructions ?? meal?.instructions ?? "";
  
      if (mealTagEl) mealTagEl.textContent = tag;
      if (mealNameEl) mealNameEl.textContent = mealName;
      if (mealImgEl) {
        if (mealImg) mealImgEl.src = mealImg;
        else mealImgEl.removeAttribute("src");
      }
      setLink(mealLinkEl, mealLink || "#");
  
      if (mealInstructionsEl) {
        if (!instructions) {
            mealInstructionsEl.textContent = "No instructions available.";
        } else {
            // Escape HTML for safety
            let safeText = instructions
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;");

            // Format "Step X" and newlines
            safeText = safeText.replace(/\r\n|\r|\n/g, "<br>");
            // Make "step X" into a bold header on a new line
            safeText = safeText.replace(/(step\s+\d+)/gi, "<br/><br/><strong style='color: white; text-transform: capitalize;'>$1</strong><br/>");
            
            // Clean up potentially excessive breaks
            safeText = safeText.replace(/^(<br\/>)+/g, "");

            mealInstructionsEl.innerHTML = safeText;
        }
      }
    }
  
    // ----- Parsing helpers (weather) -----
    function pickWeatherObject(data) {
      // app.js 2 style: data.weather.main
      if (data?.weather?.main) return data.weather;
  
      // app.js 1 "maybe" style
      if (data?.openWeather) return data.openWeather;
      if (data?.weatherData) return data.weatherData;
  
      // Sometimes weather might already be the object
      if (data?.weather && typeof data.weather === "object") return data.weather;
  
      return null;
    }
  
    function renderWeatherMaybe(data) {
      const weather = pickWeatherObject(data);
      if (!weather) return;
  
      // OpenWeather typical:
      // weather.main.temp, weather.weather[0].description
      const temp =
        typeof weather?.temp === "number"
          ? weather.temp
          : typeof weather?.main?.temp === "number"
            ? weather.main.temp
            : null;
  
      const desc =
        weather?.description ??
        weather?.weather?.[0]?.description ??
        "—";
  
      if (tempEl) tempEl.textContent = temp !== null ? String(Math.round(temp)) : "—";
      if (descEl) descEl.textContent = desc;
    }
  
    // ----- Main action -----
    async function fetchCountryAndRender(country) {
      // Abort previous request
      if (currentController) currentController.abort();
      currentController = new AbortController();
  
      const seq = ++requestSeq;
  
      clearError();
      resetUI();
      setLoading(true);
  
      try {
        const url = `/api/country/${encodeURIComponent(country)}`;
        const res = await fetch(url, { signal: currentController.signal });
  
        if (!res.ok) {
            if (res.status === 404) throw new Error("Country not found (check the country name or the endpoint).");
            throw new Error(`Server error (${res.status}).`);
        }
  
        const data = await res.json();
  
        // If a newer request started, ignore this result
        if (seq !== requestSeq) return;
  
        const countryObj = pickCountryObject(data);
        if (!countryObj) throw new Error("Could not parse country data from the server.");
  
        renderCountry(extractCountryFields(countryObj));
        renderMealMaybe(data);
        renderWeatherMaybe(data);
  
        setStatus("Ready");
      } catch (err) {
        if (err?.name === "AbortError") return;
        showError(err?.message || "Something went wrong.");
        resetUI();
      } finally {
        if (seq === requestSeq) setLoading(false);
      } 
    }
  
    // ----- Init -----
    setStatus("Ready");
    clearError();
    setLoading(false);
  
    form?.addEventListener("submit", (e) => {
      e.preventDefault();
      clearError();
  
      const country = normalizeCountry(input?.value ?? "");
      if (!country) {
        showError("Type a country before searching.");
        input?.focus();
        return;
      }
  
      if (lastSearchEl) lastSearchEl.textContent = country;
  
      fetchCountryAndRender(country);
    });
  })();