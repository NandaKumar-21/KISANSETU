"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR } from "@/lib/format";

type Product = {
  id: string;
  name: string;
  description?: string;
  category: string;
  price: number;
  quantity: number;
  unit: string;
  imageUrl?: string;
  stockThreshold: number;
  lowStock: boolean;
  inStock: boolean;
};

const EMPTY_FORM = {
  name: "",
  description: "",
  category: "Seeds",
  price: "",
  quantity: "",
  unit: "kg",
  stockThreshold: "10",
};

export default function MerchantProductsPage() {
  const [products, setProducts] = useState<Product[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const load = useCallback(() => {
    api
      .get<Product[]>("/api/merchant/products")
      .then(setProducts)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  function openNew() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
  }

  function openEdit(p: Product) {
    setEditing(p);
    setForm({
      name: p.name,
      description: p.description ?? "",
      category: p.category,
      price: String(p.price),
      quantity: String(p.quantity),
      unit: p.unit,
      stockThreshold: String(p.stockThreshold),
    });
    setShowForm(true);
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    const body = {
      name: form.name,
      description: form.description || null,
      category: form.category,
      price: parseFloat(form.price),
      quantity: parseInt(form.quantity),
      unit: form.unit,
      stockThreshold: parseInt(form.stockThreshold),
    };
    try {
      if (editing) await api.put(`/api/merchant/products/${editing.id}`, body);
      else await api.post("/api/merchant/products", body);
      setShowForm(false);
      load();
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed to save");
    }
  }

  async function remove(p: Product) {
    if (!confirm(`Delete "${p.name}"?`)) return;
    await api.del(`/api/merchant/products/${p.id}`);
    load();
  }

  if (error) return <p className="text-red-600">{error}</p>;

  const input =
    "w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none";

  return (
    <div>
      <PageHeader
        title="Products"
        subtitle="Seeds, fertilizers and equipment you sell"
        action={
          <button onClick={openNew} className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark">
            + New Product
          </button>
        }
      />

      {showForm && (
        <form onSubmit={save} className="ks-shadow mb-6 grid gap-3 rounded-2xl border border-line bg-white p-5 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Name *</label>
            <input className={input} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Category</label>
            <select className={input} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {["Seeds", "Fertilizers", "Pesticides", "Tools", "Equipment", "Fodder", "Others"].map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Unit</label>
            <select className={input} value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })}>
              {["kg", "bag", "litre", "piece", "dozen", "quintal"].map((u) => (
                <option key={u}>{u}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Price (₹) *</label>
            <input className={input} type="number" min="0" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Quantity *</label>
            <input className={input} type="number" min="0" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} required />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Low-stock alert at</label>
            <input className={input} type="number" min="1" value={form.stockThreshold} onChange={(e) => setForm({ ...form, stockThreshold: e.target.value })} />
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Description</label>
            <textarea className={`${input} min-h-16`} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="flex gap-2 sm:col-span-2">
            <button type="button" onClick={() => setShowForm(false)} className="rounded-xl border border-line px-4 py-2.5 text-sm font-semibold text-muted">
              Cancel
            </button>
            <button type="submit" className="flex-1 rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark">
              {editing ? "Save changes" : "Add product"}
            </button>
          </div>
        </form>
      )}

      {!products ? (
        <Spinner />
      ) : products.length === 0 ? (
        <EmptyState icon="🧾" title="No products yet" hint="Add seeds, fertilizers or equipment to start selling." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {products.map((p) => (
            <div key={p.id} className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-5">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h3 className="font-bold text-ink">{p.name}</h3>
                  <p className="text-sm text-muted">{p.category}</p>
                </div>
                <StatusBadge status={p.lowStock ? "LOW STOCK" : "IN STOCK"} />
              </div>
              <p className="mt-2 line-clamp-2 flex-1 text-sm text-muted">{p.description}</p>
              <div className="mt-3 flex items-end justify-between">
                <p className="text-xl font-bold text-brand-dark">
                  {formatINR(p.price)}
                  <span className="text-sm font-medium text-muted">/{p.unit}</span>
                </p>
                <p className="text-xs text-muted">
                  Stock: <span className={p.lowStock ? "font-bold text-amber-600" : "font-semibold text-ink"}>{p.quantity}</span>
                </p>
              </div>
              <div className="mt-3 flex gap-2 border-t border-line pt-3">
                <button onClick={() => openEdit(p)} className="flex-1 rounded-lg border border-line px-3 py-2 text-xs font-semibold text-muted hover:bg-surface">
                  Edit
                </button>
                <button onClick={() => remove(p)} className="rounded-lg border border-red-100 px-3 py-2 text-xs font-semibold text-red-600 hover:bg-red-50">
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}