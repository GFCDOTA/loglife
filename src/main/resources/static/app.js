"use strict";

// API base: empty string = same origin (so the iPhone hitting http://<machine-ip>:8080 just works).
// Override for advanced setups via: localStorage.setItem('loglife.apiBase', 'http://192.168.0.10:8080')
const API_BASE = localStorage.getItem("loglife.apiBase") || "";

const MEAL_LABELS = {
    BREAKFAST: "Café", LUNCH: "Almoço", DINNER: "Jantar", SNACK: "Lanche", OTHER: "Outro",
};

let selectedMeal = "LUNCH";

const el = (id) => document.getElementById(id);
const num = (v) => Math.round(Number(v) || 0);

function todayIso() {
    const d = new Date();
    const off = d.getTimezoneOffset();
    return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
}

function currentDate() {
    return el("dayInput").value || todayIso();
}

async function api(path, options) {
    const res = await fetch(API_BASE + path, options);
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        const err = new Error((data && (data.message || data.error)) || ("HTTP " + res.status));
        err.payload = data;
        throw err;
    }
    return data;
}

function toast(message, isError) {
    const t = el("toast");
    t.textContent = message;
    t.classList.toggle("error", !!isError);
    t.classList.add("show");
    setTimeout(() => t.classList.remove("show"), 2600);
}

async function loadDay() {
    const date = currentDate();
    try {
        const [logs, summary, frequent, trend] = await Promise.all([
            api("/api/v1/food-logs?date=" + date),
            api("/api/v1/nutrition/daily-summary?date=" + date),
            api("/api/v1/food-logs/frequent?date=" + date),
            api("/api/v1/nutrition/trend?date=" + date),
        ]);
        renderSummary(summary);
        renderList(logs);
        renderFrequent(frequent);
        renderTrend(trend, date);
    } catch (e) {
        toast("Falha ao carregar: " + e.message, true);
    }
}

function renderTrend(trend, selectedDate) {
    const card = el("trendCard");
    card.hidden = !trend || trend.daysWithLogs === 0;
    if (card.hidden) return;

    const bars = el("trendBars");
    bars.innerHTML = "";
    const max = Math.max(1, ...trend.days.map((d) => Number(d.calories) || 0));

    for (const day of trend.days) {
        const cal = Number(day.calories) || 0;
        const col = document.createElement("div");
        col.className = "trend-col";
        col.title = `${day.date} · ${num(cal)} kcal (${day.totalLogs} registro(s))`;

        const bar = document.createElement("div");
        bar.className = "trend-bar" + (day.date === selectedDate ? " today" : "");
        bar.style.height = Math.max(4, Math.round((cal / max) * 56)) + "px";

        const label = document.createElement("small");
        label.textContent = day.date.slice(8); // day of month

        col.append(bar, label);
        bars.appendChild(col);
    }

    el("trendAvg").textContent = num(trend.averageCalories);
    el("trendDays").textContent = trend.daysWithLogs;
}

function renderFrequent(foods) {
    const card = el("frequentCard");
    const chips = el("frequentChips");
    chips.innerHTML = "";
    card.hidden = !foods || foods.length === 0;
    if (card.hidden) return;

    for (const food of foods) {
        const chip = document.createElement("button");
        chip.type = "button";
        chip.className = "chip freq-chip";
        chip.textContent = `${food.name} · ${num(food.calories)} kcal`;
        chip.title = `Registrado ${food.timesLogged}x nos últimos 30 dias`;
        chip.onclick = () => repeatFood(food);
        chips.appendChild(chip);
    }
}

async function repeatFood(food) {
    try {
        await api("/api/v1/food-logs/" + food.logId + "/repeat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ date: currentDate(), mealType: selectedMeal }),
        });
        toast(`Repetido: ${food.name} · ${num(food.calories)} kcal (sem re-estimar)`);
        loadDay();
    } catch (e) {
        toast("Falha ao repetir: " + e.message, true);
    }
}

function renderSummary(s) {
    el("totalCalories").textContent = num(s.totalCalories);
    el("totalProtein").textContent = num(s.totalProteinGrams);
    el("totalCarbs").textContent = num(s.totalCarbsGrams);
    el("totalFat").textContent = num(s.totalFatGrams);
    el("totalLogs").textContent = s.totalLogs;
    renderGoal(s.goal);
}

function renderGoal(goal) {
    const hasGoal = !!goal;
    el("goalBlock").hidden = !hasGoal;
    el("goalUnset").hidden = hasGoal;
    if (!hasGoal) return;

    const percent = Number(goal.percentOfCalories) || 0;
    const over = percent > 100;
    const fill = el("goalFill");
    fill.style.width = Math.min(percent, 100) + "%";
    fill.classList.toggle("over", over);

    el("goalPercent").textContent = percent + "%";
    el("goalCalories").textContent = num(goal.calories);
    el("goalRemaining").textContent = over
        ? num(-goal.remainingCalories) + " kcal acima"
        : num(goal.remainingCalories) + " kcal restantes";
}

async function editGoal() {
    const current = el("goalCalories").textContent;
    const answer = prompt("Meta diária de calorias (kcal):", current !== "0" ? current : "2000");
    if (answer === null) return;
    const calories = Number(answer);
    if (!calories || calories <= 0) {
        toast("Informe um número de calorias positivo", true);
        return;
    }
    try {
        await api("/api/v1/nutrition/goal", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ calories }),
        });
        toast("Meta diária: " + num(calories) + " kcal");
        loadDay();
    } catch (e) {
        toast("Falha ao salvar a meta: " + e.message, true);
    }
}

function renderList(logs) {
    const list = el("logList");
    list.innerHTML = "";
    el("emptyState").hidden = logs.length > 0;

    for (const log of logs) {
        const item = document.createElement("div");
        item.className = "log-item";

        const main = document.createElement("div");
        main.className = "log-main";

        const meal = document.createElement("div");
        meal.className = "log-meal";
        meal.textContent = MEAL_LABELS[log.mealType] || log.mealType;

        const name = document.createElement("div");
        name.className = "log-name";
        name.textContent = log.normalizedFoodName || log.descriptionOriginal;

        const macros = document.createElement("div");
        macros.className = "log-macros";
        macros.textContent = `P ${num(log.proteinGrams)}g · C ${num(log.carbsGrams)}g · G ${num(log.fatGrams)}g`;

        main.append(meal, name, macros);
        if (log.source === "MOCK") {
            const badge = document.createElement("span");
            badge.className = "badge-mock";
            badge.textContent = "ESTIMATIVA MOCK";
            main.appendChild(badge);

            const redo = document.createElement("button");
            redo.type = "button";
            redo.className = "link-btn reestimate-btn";
            redo.textContent = "re-estimar";
            redo.onclick = () => reestimateLog(log.id, redo);
            main.appendChild(redo);
        }

        const cal = document.createElement("div");
        cal.className = "log-cal";
        cal.textContent = num(log.calories) + " kcal";

        const del = document.createElement("button");
        del.className = "del-btn";
        del.setAttribute("aria-label", "Excluir");
        del.textContent = "🗑";
        del.onclick = () => deleteLog(log.id);

        item.append(main, cal, del);
        list.appendChild(item);
    }
}

async function reestimateLog(id, btn) {
    btn.disabled = true;
    btn.textContent = "re-estimando...";
    try {
        const updated = await api("/api/v1/food-logs/" + id + "/re-estimate", { method: "POST" });
        toast(`Re-estimado: ${num(updated.calories)} kcal (${updated.source})`);
        loadDay();
    } catch (e) {
        toast("Falha ao re-estimar: " + e.message, true);
        btn.disabled = false;
        btn.textContent = "re-estimar";
    }
}

async function deleteLog(id) {
    if (!confirm("Excluir este registro?")) return;
    try {
        await api("/api/v1/food-logs/" + id, { method: "DELETE" });
        toast("Registro excluído");
        loadDay();
    } catch (e) {
        toast("Falha ao excluir: " + e.message, true);
    }
}

async function submitFood(event) {
    event.preventDefault();
    const description = el("description").value.trim();
    if (description.length < 2) {
        toast("Descreva o alimento (mín. 2 caracteres)", true);
        return;
    }
    const payload = {
        date: currentDate(),
        mealType: selectedMeal,
        description,
        quantity: el("quantity").value ? Number(el("quantity").value) : null,
        unit: el("unit").value.trim() || null,
        notes: el("notes").value.trim() || null,
        language: "pt-BR",
    };

    const btn = el("submitBtn");
    btn.disabled = true;
    btn.textContent = "Estimando...";
    try {
        const created = await api("/api/v1/food-logs", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        // The API may split one entry into several logs (one per food item).
        const items = Array.isArray(created) ? created : [created];
        const totalCal = items.reduce((sum, x) => sum + (Number(x.calories) || 0), 0);
        const src = items.length ? items[0].source : "";
        const label = items.length === 1 ? "item" : "itens";
        toast(`Registrado: ${items.length} ${label} · ${num(totalCal)} kcal (${src})`);
        el("description").value = "";
        el("quantity").value = "";
        el("unit").value = "";
        el("notes").value = "";
        loadDay();
    } catch (e) {
        const fields = e.payload && e.payload.fieldErrors;
        const detail = fields && fields.length ? fields.map((f) => f.message).join("; ") : e.message;
        toast("Não foi possível registrar: " + detail, true);
    } finally {
        btn.disabled = false;
        btn.textContent = "Estimar e registrar";
    }
}

function selectMeal(meal) {
    selectedMeal = meal;
    document.querySelectorAll(".chip").forEach((c) =>
        c.classList.toggle("active", c.dataset.meal === meal));
}

function shiftDay(days) {
    const d = new Date(currentDate() + "T00:00:00");
    d.setDate(d.getDate() + days);
    el("dayInput").value = d.toISOString().slice(0, 10);
    loadDay();
}

function init() {
    el("dayInput").value = todayIso();
    selectMeal(selectedMeal);

    el("mealChips").addEventListener("click", (e) => {
        const chip = e.target.closest(".chip");
        if (chip) selectMeal(chip.dataset.meal);
    });
    el("foodForm").addEventListener("submit", submitFood);
    el("goalSetBtn").addEventListener("click", editGoal);
    el("goalEditBtn").addEventListener("click", editGoal);
    el("dayInput").addEventListener("change", loadDay);
    el("prevDay").addEventListener("click", () => shiftDay(-1));
    el("nextDay").addEventListener("click", () => shiftDay(1));
    el("todayBtn").addEventListener("click", () => { el("dayInput").value = todayIso(); loadDay(); });

    loadDay();

    if ("serviceWorker" in navigator) {
        navigator.serviceWorker.register("service-worker.js").catch(() => { /* offline cache is optional */ });
    }
}

document.addEventListener("DOMContentLoaded", init);
