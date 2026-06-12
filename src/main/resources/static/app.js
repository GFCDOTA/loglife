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
        const [logs, summary] = await Promise.all([
            api("/api/v1/food-logs?date=" + date),
            api("/api/v1/nutrition/daily-summary?date=" + date),
        ]);
        renderSummary(summary);
        renderList(logs);
    } catch (e) {
        toast("Falha ao carregar: " + e.message, true);
    }
}

function renderSummary(s) {
    el("totalCalories").textContent = num(s.totalCalories);
    el("totalProtein").textContent = num(s.totalProteinGrams);
    el("totalCarbs").textContent = num(s.totalCarbsGrams);
    el("totalFat").textContent = num(s.totalFatGrams);
    el("totalLogs").textContent = s.totalLogs;
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
        toast(`Registrado: ${num(created.calories)} kcal (${created.source})`);
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
