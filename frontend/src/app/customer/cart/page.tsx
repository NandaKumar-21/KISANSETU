"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatINR } from "@/lib/format";

type CartItem = {
  id: string;
  listingId: string;
  quantity: number;
  createdAt: string;
};

type Listing = {
  id: string;
  title: string;
  price: number;
  unit: string;
};

type CartLine = CartItem & { listing?: Listing };

export default function CustomerCartPage() {
  const [lines, setLines] = useState<CartLine[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const items = await api.get<CartItem[]>("/api/customer/cart");
      if (items.length === 0) {
        setLines([]);
        return;
      }
      const produce = await api.get<{ content: Listing[] }>(
        "/api/customer/produce?page=0&size=200"
      );
      const byId = new Map(produce.content.map((l) => [l.id, l]));
      setLines(items.map((i) => ({ ...i, listing: byId.get(i.listingId) })));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load cart");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function update(itemId: string, quantity: number) {
    if (quantity <= 0) return;
    await api.put(`/api/customer/cart/${itemId}?quantity=${quantity}`);
    load();
  }

  async function remove(itemId: string) {
    await api.del(`/api/customer/cart/${itemId}`);
    load();
  }

  async function clear() {
    if (!confirm("Clear your entire cart?")) return;
    await api.del("/api/customer/cart");
    load();
  }

  if (error) return <p className="text-red-600">{error}</p>;

  const total = (lines ?? []).reduce(
    (sum, l) => sum + (l.listing?.price ?? 0) * l.quantity,
    0
  );

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title="My Cart"
        subtitle={`${lines?.length ?? 0} item(s)`}
        action={
          (lines?.length ?? 0) > 0 ? (
            <button onClick={clear} className="text-sm font-semibold text-red-600 hover:underline">
              Clear cart
            </button>
          ) : undefined
        }
      />

      {!lines ? (
        <Spinner />
      ) : lines.length === 0 ? (
        <EmptyState
          icon="🛒"
          title="Your cart is empty"
          hint={
            <Link href="/customer/marketplace" className="font-semibold text-brand hover:underline">
              Browse the marketplace →
            </Link>
          }
        />
      ) : (
        <>
          <div className="space-y-3">
            {lines.map((l) => (
              <div key={l.id} className="ks-shadow flex items-center justify-between gap-4 rounded-2xl border border-line bg-white p-4">
                <div className="min-w-0">
                  <p className="font-semibold text-ink">{l.listing?.title ?? "Unavailable item"}</p>
                  <p className="text-xs text-muted">
                    {formatINR(l.listing?.price ?? 0)} / {l.listing?.unit ?? "unit"}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => update(l.id, l.quantity - 1)}
                      className="h-8 w-8 rounded-lg border border-line text-sm font-bold text-muted hover:bg-surface"
                    >
                      −
                    </button>
                    <input
                      type="number"
                      min={1}
                      value={l.quantity}
                      onChange={(e) => update(l.id, Math.max(1, parseInt(e.target.value) || 1))}
                      className="h-8 w-16 rounded-lg border border-line text-center text-sm font-semibold focus:border-brand focus:outline-none"
                    />
                    <button
                      onClick={() => update(l.id, l.quantity + 1)}
                      className="h-8 w-8 rounded-lg border border-line text-sm font-bold text-muted hover:bg-surface"
                    >
                      +
                    </button>
                  </div>
                  <p className="w-24 text-right font-bold text-brand-dark">
                    {formatINR((l.listing?.price ?? 0) * l.quantity)}
                  </p>
                  <button
                    onClick={() => remove(l.id)}
                    className="rounded-lg border border-red-100 px-2.5 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="ks-shadow mt-5 rounded-2xl border border-line bg-white p-5">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-muted">Total</p>
              <p className="text-2xl font-bold text-brand-dark">{formatINR(total)}</p>
            </div>
            <Link
              href="/customer/checkout"
              className="mt-4 block rounded-xl bg-brand py-3 text-center text-sm font-bold text-white hover:bg-brand-dark"
            >
              Proceed to checkout →
            </Link>
          </div>
        </>
      )}
    </div>
  );
}