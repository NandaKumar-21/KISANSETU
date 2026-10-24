"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, formatDateTime, timeAgo } from "@/lib/format";

type Order = {
  id: string;
  farmerId: string;
  farmerName?: string;
  listingTitle?: string;
  listingUnit?: string;
  quantity: number;
  totalPrice: number;
  status: string;
  deliveryPreference?: string;
  notes?: string;
  estimatedDelivery?: string;
  createdAt: string;
  deliveryStatus?: string;
};

export default function CustomerOrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rating, setRating] = useState<{ orderId: string; score: number; review: string } | null>(null);

  const load = useCallback(() => {
    api
      .get<Order[]>("/api/customer/orders")
      .then(setOrders)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  async function cancel(o: Order) {
    if (!confirm(`Cancel your order of ${o.listingTitle ?? "produce"}?`)) return;
    try {
      await api.post(`/api/customer/orders/${o.id}/cancel`);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Cannot cancel this order");
    }
  }

  async function submitRating() {
    if (!rating) return;
    try {
      await api.post(`/api/customer/orders/${rating.orderId}/rate`, {
        rating: rating.score,
        review: rating.review || null,
      });
      setRating(null);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to rate");
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader title="My Orders" subtitle="Track and manage your produce orders" />
      {!orders ? (
        <Spinner />
      ) : orders.length === 0 ? (
        <EmptyState icon="📦" title="No orders yet" hint="Browse the marketplace and place your first order!" />
      ) : (
        <div className="space-y-3">
          {orders.map((o) => (
            <div key={o.id} className="ks-shadow rounded-2xl border border-line bg-white p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-ink">
                    {o.listingTitle ?? "Order"}
                    <span className="ml-2 text-sm font-normal text-muted">
                      {o.quantity} {o.listingUnit ?? ""}
                    </span>
                  </p>
                  <p className="mt-0.5 text-xs text-muted">
                    {o.farmerName ?? "Farmer"} · placed {timeAgo(o.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <p className="font-bold text-brand-dark">{formatINR(o.totalPrice)}</p>
                  <StatusBadge status={o.status} />
                </div>
              </div>

              <div className="mt-3 grid gap-2 rounded-xl bg-surface px-3 py-2.5 text-xs text-muted sm:grid-cols-3">
                <span>🚚 Delivery: {o.deliveryPreference ?? "any"}</span>
                {o.estimatedDelivery && <span>🗓️ ETA: {formatDateTime(o.estimatedDelivery)}</span>}
                {o.deliveryStatus && <span>📦 Logistics: {o.deliveryStatus.replace("_", " ")}</span>}
              </div>

              {o.notes && <p className="mt-2 text-sm text-muted">📝 {o.notes}</p>}

              <div className="mt-3 flex flex-wrap gap-2 border-t border-line pt-3">
                {(o.status === "pending" || o.status === "confirmed") && (
                  <button
                    onClick={() => cancel(o)}
                    className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-xs font-semibold text-red-700"
                  >
                    Cancel order
                  </button>
                )}
                {o.status === "delivered" && (
                  <button
                    onClick={() => setRating({ orderId: o.id, score: 5, review: "" })}
                    className="rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-dark"
                  >
                    ⭐ Rate farmer
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {rating && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setRating(null)}>
          <div
            className="ks-shadow w-full max-w-md rounded-2xl bg-white p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-bold text-ink">Rate the farmer</h3>
            <div className="mt-3 flex gap-1 text-3xl">
              {[1, 2, 3, 4, 5].map((n) => (
                <button
                  key={n}
                  onClick={() => setRating({ ...rating, score: n })}
                  className={n <= rating.score ? "" : "opacity-30 grayscale"}
                >
                  ⭐
                </button>
              ))}
            </div>
            <textarea
              value={rating.review}
              onChange={(e) => setRating({ ...rating, review: e.target.value })}
              placeholder="Write a review (optional)"
              className="mt-4 w-full rounded-xl border border-line px-4 py-2.5 text-sm focus:border-brand focus:outline-none"
            />
            <div className="mt-4 flex gap-2">
              <button onClick={() => setRating(null)} className="flex-1 rounded-xl border border-line py-2.5 text-sm font-semibold text-muted">
                Cancel
              </button>
              <button onClick={submitRating} className="flex-1 rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark">
                Submit rating
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}