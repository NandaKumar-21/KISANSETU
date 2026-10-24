"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { PageHeader } from "@/components/ui";

const CATEGORIES = ["Vegetables", "Fruits", "Grains", "Pulses", "Spices", "Dairy", "Others"];

export default function NewListingPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "Vegetables",
    price: "",
    quantity: "",
    unit: "kg",
    location: "",
    variety: "",
    farmingMethod: "conventional",
    harvestDate: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }));

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post("/api/farmer/listings", {
        title: form.title,
        description: form.description || null,
        category: form.category,
        price: parseFloat(form.price),
        quantity: parseFloat(form.quantity),
        unit: form.unit,
        location: form.location || null,
        variety: form.variety || null,
        farmingMethod: form.farmingMethod,
        harvestDate: form.harvestDate || null,
      });
      router.push("/farmer/listings");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create listing");
      setBusy(false);
    }
  }

  const input =
    "w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm text-ink placeholder:text-muted/70 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20";

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="New Listing" subtitle="List your produce on the marketplace" />
      <form onSubmit={submit} className="ks-shadow space-y-4 rounded-2xl border border-line bg-white p-6">
        <div>
          <label className="mb-1.5 block text-sm font-medium text-ink">Title *</label>
          <input
            className={input}
            value={form.title}
            onChange={(e) => set("title", e.target.value)}
            placeholder="e.g. Fresh Tomatoes"
            required
          />
        </div>
        <div>
          <label className="mb-1.5 block text-sm font-medium text-ink">Description</label>
          <textarea
            className={`${input} min-h-24`}
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            placeholder="Describe quality, harvest details..."
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">Category *</label>
            <select className={input} value={form.category} onChange={(e) => set("category", e.target.value)}>
              {CATEGORIES.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">Variety</label>
            <input
              className={input}
              value={form.variety}
              onChange={(e) => set("variety", e.target.value)}
              placeholder="e.g. Hybrid-470"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">Price (₹) *</label>
            <input
              className={input}
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={(e) => set("price", e.target.value)}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Quantity *</label>
              <input
                className={input}
                type="number"
                min="0"
                step="0.01"
                value={form.quantity}
                onChange={(e) => set("quantity", e.target.value)}
                required
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Unit</label>
              <select className={input} value={form.unit} onChange={(e) => set("unit", e.target.value)}>
                {["kg", "quintal", "tonne", "dozen", "bag", "litre"].map((u) => (
                  <option key={u}>{u}</option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">Location</label>
            <input
              className={input}
              value={form.location}
              onChange={(e) => set("location", e.target.value)}
              placeholder="e.g. Kolhapur, Maharashtra"
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-ink">Harvest date</label>
            <input
              className={input}
              type="date"
              value={form.harvestDate}
              onChange={(e) => set("harvestDate", e.target.value)}
            />
          </div>
        </div>
        <div>
          <label className="mb-1.5 block text-sm font-medium text-ink">Farming method</label>
          <div className="flex gap-2">
            {["conventional", "organic"].map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => set("farmingMethod", m)}
                className={`flex-1 rounded-xl border px-3 py-2.5 text-sm font-medium capitalize ${
                  form.farmingMethod === m
                    ? "border-brand bg-brand-light text-brand-dark"
                    : "border-line text-muted"
                }`}
              >
                {m}
              </button>
            ))}
          </div>
        </div>
        {error && <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
        <div className="flex gap-2 pt-2">
          <button
            type="button"
            onClick={() => router.back()}
            className="rounded-xl border border-line px-4 py-2.5 text-sm font-semibold text-muted hover:bg-surface"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={busy}
            className="flex-1 rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {busy ? "Creating..." : "Create listing"}
          </button>
        </div>
      </form>
    </div>
  );
}