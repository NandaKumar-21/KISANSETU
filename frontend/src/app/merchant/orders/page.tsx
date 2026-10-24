"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, timeAgo } from "@/lib/format";

type Order = {
  id: string;
  farmerName?: string;
  productName?: string;
  productUnit?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
  notes?: string;
  createdAt: string;
};

export default function MerchantOrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .get<Order[]>("/api/merchant/orders")
      .then(setOrders)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  async function act(id: string, action: string) {
    try {
      await api.post(`/api/merchant/orders/${id}/${action}`);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed");
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader title="Orders" subtitle="Input orders placed by farmers" />
      {!orders ? (
        <Spinner />
      ) : orders.length === 0 ? (
        <EmptyState icon="📦" title="No orders yet" hint="When farmers order your products, they will appear here." />
      ) : (
        <div className="space-y-3">
          {orders.map((o) => (
            <div key={o.id} className="ks-shadow rounded-2xl border border-line bg-white p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-ink">
                    {o.productName ?? "Order"}
                    <span className="ml-2 text-sm font-normal text-muted">
                      {o.quantity} {o.productUnit ?? ""} × {formatINR(o.unitPrice)}
                    </span>
                  </p>
                  <p className="mt-0.5 text-xs text-muted">
                    {o.farmerName ?? "Farmer"} · {timeAgo(o.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <p className="font-bold text-brand-dark">{formatINR(o.totalPrice)}</p>
                  <StatusBadge status={o.status} />
                </div>
              </div>
              {o.notes && (
                <p className="mt-2 rounded-lg bg-surface px-3 py-2 text-sm text-muted">📝 {o.notes}</p>
              )}
              <div className="mt-3 flex flex-wrap gap-2 border-t border-line pt-3">
                {o.status === "PENDING" && (
                  <>
                    <button
                      onClick={() => act(o.id, "accept")}
                      className="rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-dark"
                    >
                      Accept order
                    </button>
                    <button
                      onClick={() => act(o.id, "reject")}
                      className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-xs font-semibold text-red-700"
                    >
                      Reject
                    </button>
                  </>
                )}
                {o.status === "ACCEPTED" && (
                  <button
                    onClick={() => act(o.id, "advance")}
                    className="rounded-lg bg-brand-light px-4 py-2 text-xs font-semibold text-brand-dark"
                  >
                    Start processing
                  </button>
                )}
                {o.status === "PROCESSING" && (
                  <button
                    onClick={() => act(o.id, "advance")}
                    className="rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-dark"
                  >
                    Mark completed
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}