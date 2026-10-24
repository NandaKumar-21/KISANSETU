"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatINR } from "@/lib/format";

type Address = {
  id: string;
  label: string;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  phone?: string;
  isDefault: boolean;
};

type CartItem = { id: string; listingId: string; quantity: number };
type Listing = { id: string; title: string; price: number; unit: string };

const EMPTY_ADDRESS = {
  label: "Home",
  addressLine: "",
  city: "",
  state: "",
  pincode: "",
  phone: "",
};

export default function CustomerCheckoutPage() {
  const router = useRouter();
  const [addresses, setAddresses] = useState<Address[] | null>(null);
  const [lines, setLines] = useState<Array<CartItem & { listing?: Listing }>>([]);
  const [selected, setSelected] = useState<string>("");
  const [deliveryPreference, setDeliveryPreference] = useState("any");
  const [notes, setNotes] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_ADDRESS);
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [addr, items] = await Promise.all([
        api.get<Address[]>("/api/customer/addresses"),
        api.get<CartItem[]>("/api/customer/cart"),
      ]);
      setAddresses(addr);
      if (addr.length > 0 && !addr.some((a) => a.id === selected)) {
        setSelected(addr.find((a) => a.isDefault)?.id ?? addr[0].id);
      }
      if (items.length === 0) {
        setLines([]);
        return;
      }
      const produce = await api.get<{ content: Listing[] }>("/api/customer/produce?page=0&size=200");
      const byId = new Map(produce.content.map((l) => [l.id, l]));
      setLines(items.map((i) => ({ ...i, listing: byId.get(i.listingId) })));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load checkout");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function addAddress(e: React.FormEvent) {
    e.preventDefault();
    const created = await api.post<Address>("/api/customer/addresses", {
      addressLine1: form.addressLine,
      addressLine2: null,
      city: form.city,
      state: form.state,
      pincode: form.pincode,
      phone: form.phone,
      isDefault: true,
    });
    setShowForm(false);
    setForm(EMPTY_ADDRESS);
    setSelected(created.id);
    load();
  }

  async function placeOrder() {
    if (!selected) {
      alert("Please choose a delivery address");
      return;
    }
    setPlacing(true);
    try {
      const res = await api.post<{ ordersCreated: number; grandTotal: number; message: string }>("/api/customer/checkout", {
        deliveryAddressId: selected,
        deliveryPreference,
        notes: notes || null,
      });
      alert(`${res.message}\n${res.ordersCreated} order(s) · Total ${formatINR(res.grandTotal)}`);
      router.push("/customer/orders");
    } catch (e) {
      alert(e instanceof Error ? e.message : "Checkout failed");
      setPlacing(false);
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;
  if (!addresses) return <Spinner label="Loading checkout..." />;

  const total = lines.reduce((sum, l) => sum + (l.listing?.price ?? 0) * l.quantity, 0);

  if (lines.length === 0) {
    return (
      <div>
        <PageHeader title="Checkout" />
        <EmptyState icon="🛒" title="Nothing to check out" hint="Your cart is empty." />
      </div>
    );
  }

  const input =
    "w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none";

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title="Checkout" subtitle="Confirm your delivery details" />

      <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-ink">Delivery address</h3>
          <button onClick={() => setShowForm(!showForm)} className="text-sm font-semibold text-brand hover:underline">
            {showForm ? "Cancel" : "+ New address"}
          </button>
        </div>

        {showForm && (
          <form onSubmit={addAddress} className="mt-3 grid gap-3 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-semibold text-muted uppercase">Label</label>
              <select className={input} value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })}>
                {["Home", "Farm", "Office", "Other"].map((l) => (
                  <option key={l}>{l}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-muted uppercase">Phone *</label>
              <input className={input} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-semibold text-muted uppercase">Address *</label>
              <input className={input} value={form.addressLine} onChange={(e) => setForm({ ...form, addressLine: e.target.value })} required />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-muted uppercase">City *</label>
              <input className={input} value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} required />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-muted uppercase">State *</label>
              <input className={input} value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} required />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-muted uppercase">Pincode *</label>
              <input className={input} value={form.pincode} onChange={(e) => setForm({ ...form, pincode: e.target.value })} required />
            </div>
            <div className="flex items-end">
              <button type="submit" className="w-full rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark">
                Save address
              </button>
            </div>
          </form>
        )}

        {addresses.length === 0 && !showForm ? (
          <p className="mt-3 text-sm text-muted">No saved addresses. Add one to continue.</p>
        ) : (
          <div className="mt-3 space-y-2">
            {addresses.map((a) => (
              <label
                key={a.id}
                className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3 ${
                  selected === a.id ? "border-brand bg-brand-light" : "border-line hover:bg-surface"
                }`}
              >
                <input
                  type="radio"
                  name="address"
                  checked={selected === a.id}
                  onChange={() => setSelected(a.id)}
                  className="mt-1 accent-[#16803C]"
                />
                <span>
                  <span className="text-sm font-semibold text-ink">
                    {a.label} {a.isDefault && <span className="text-xs font-medium text-brand">· Default</span>}
                  </span>
                  <span className="block text-sm text-muted">
                    {a.addressLine}, {a.city}, {a.state} — {a.pincode}
                  </span>
                </span>
              </label>
            ))}
          </div>
        )}
      </div>

      <div className="ks-shadow mt-4 rounded-2xl border border-line bg-white p-5">
        <h3 className="font-bold text-ink">Order summary</h3>
        <div className="mt-3 space-y-2">
          {lines.map((l) => (
            <div key={l.id} className="flex justify-between text-sm">
              <span className="text-ink">
                {l.listing?.title ?? "Item"} <span className="text-muted">× {l.quantity}</span>
              </span>
              <span className="font-semibold text-ink">
                {formatINR((l.listing?.price ?? 0) * l.quantity)}
              </span>
            </div>
          ))}
          <div className="flex justify-between border-t border-line pt-2 text-base font-bold text-brand-dark">
            <span>Total</span>
            <span>{formatINR(total)}</span>
          </div>
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Delivery preference</label>
            <select
              className={input}
              value={deliveryPreference}
              onChange={(e) => setDeliveryPreference(e.target.value)}
            >
              <option value="any">Any preference</option>
              <option value="morning">Morning delivery</option>
              <option value="evening">Evening delivery</option>
              <option value="pickup">Farm pickup</option>
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Notes for farmer</label>
            <input className={input} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Optional" />
          </div>
        </div>

        <button
          onClick={placeOrder}
          disabled={placing || !selected}
          className="mt-5 w-full rounded-xl bg-brand py-3 text-sm font-bold text-white hover:bg-brand-dark disabled:opacity-50"
        >
          {placing ? "Placing order..." : `Place order · ${formatINR(total)}`}
        </button>
      </div>
    </div>
  );
}