"use client";

import { useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner } from "@/components/ui";
import { formatINR } from "@/lib/format";

type Recommendation = {
  crop: string;
  expectedProfit: string;
  expectedProfitValue: number;
  expectedPriceRange: string;
  fertilizers: string[];
  waterNeed: string;
  whyRecommended: string;
};

export default function CropPlannerPage() {
  const [form, setForm] = useState({
    soilType: "loamy",
    region: "",
    season: "Kharif",
    waterAvailability: "Medium",
    budget: "",
    farmSize: "",
    previousCrop: "",
    preferredCrop: "",
  });
  const [result, setResult] = useState<{ recommendations: Recommendation[] } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const input =
    "w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20";

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const data = await api.post<{ recommendations: Recommendation[] }>("/api/ai/crop-planner", {
        soilType: form.soilType,
        region: form.region || null,
        season: form.season,
        waterAvailability: form.waterAvailability,
        budget: form.budget ? parseFloat(form.budget) : null,
        farmSize: form.farmSize ? parseFloat(form.farmSize) : null,
        previousCrop: form.previousCrop || null,
        preferredCrop: form.preferredCrop || null,
      });
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader title="Crop Planner" subtitle="AI-powered crop recommendations for your farm" />
      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={submit} className="ks-shadow space-y-4 rounded-2xl border border-line bg-white p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Soil type</label>
              <select className={input} value={form.soilType} onChange={(e) => setForm({ ...form, soilType: e.target.value })}>
                {["loamy", "red", "black", "clay", "sandy"].map((s) => (
                  <option key={s} className="capitalize">{s}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Region</label>
              <input className={input} value={form.region} onChange={(e) => setForm({ ...form, region: e.target.value })} placeholder="e.g. Maharashtra" />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Season</label>
              <select className={input} value={form.season} onChange={(e) => setForm({ ...form, season: e.target.value })}>
                {["Kharif", "Rabi", "Summer"].map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Water availability</label>
              <select className={input} value={form.waterAvailability} onChange={(e) => setForm({ ...form, waterAvailability: e.target.value })}>
                {["Low", "Medium", "High"].map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Budget (₹)</label>
              <input className={input} type="number" min="0" value={form.budget} onChange={(e) => setForm({ ...form, budget: e.target.value })} placeholder="e.g. 100000" />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Farm size (acres)</label>
              <input className={input} type="number" min="0" step="0.1" value={form.farmSize} onChange={(e) => setForm({ ...form, farmSize: e.target.value })} placeholder="e.g. 2" />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Previous crop</label>
              <input className={input} value={form.previousCrop} onChange={(e) => setForm({ ...form, previousCrop: e.target.value })} placeholder="e.g. Paddy" />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Preferred crop</label>
              <input className={input} value={form.preferredCrop} onChange={(e) => setForm({ ...form, preferredCrop: e.target.value })} placeholder="e.g. Tomato" />
            </div>
          </div>
          {error && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {busy ? "Analyzing..." : "Get recommendations"}
          </button>
        </form>

        <div>
          {busy ? (
            <Spinner label="Analyzing soil, season and budget..." />
          ) : result ? (
            <div className="space-y-3">
              {result.recommendations.map((r, i) => (
                <div key={i} className="ks-shadow rounded-2xl border border-line bg-white p-5">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h3 className="text-lg font-bold text-ink">🌾 {r.crop}</h3>
                      <p className="text-xs text-muted">
                        Water need: {r.waterNeed} · Market price: {r.expectedPriceRange || "—"}
                      </p>
                    </div>
                    <span className="rounded-full bg-brand-light px-3 py-1 text-sm font-bold text-brand-dark">
                      {formatINR(r.expectedProfitValue)}
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-muted">{r.whyRecommended}</p>
                  {r.fertilizers.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {r.fertilizers.map((f) => (
                        <span key={f} className="rounded-full border border-line bg-surface px-2.5 py-0.5 text-xs text-muted">
                          {f}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="flex h-full items-center justify-center rounded-2xl border border-dashed border-line bg-surface p-10 text-center">
              <p className="max-w-xs text-sm text-muted">
                Fill in your farm details to get AI crop recommendations with expected profits, fertilizers and guidance.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}