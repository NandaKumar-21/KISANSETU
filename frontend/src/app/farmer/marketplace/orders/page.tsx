"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, timeAgo } from "@/lib/format";

type MpOrder = {
  id: string;
  listingTitle?: string;
  listingUnit?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
  notes?: string;
  createdAt: string;
};

export default function FarmerMarketplaceOrdersPage() {
  const [tab, setTab] = useState<"bought" | "sold">("bought");
  const [orders, setOrders] = useState<MpOrder[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .get<MpOrder[]>(`/api/farmer/marketplace/orders/${tab}`)
      .then(setOrders)
      .catch((e) => setError(e.message));
  }, [tab]);

  useEffect(load, [load]);

  async function updateStatus(id: string, status: string) {
    try {
      await api.post(`/api/farmer/marketplace/orders/${id}/status?status=${status}`);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed");
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader title="Marketplace Orders" subtitle="Orders between farmers" />
      <div className="mb-4 inline-flex rounded-xl bg-surface p-1">
        {(["bought", "sold"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`rounded-lg px-4 py-2 text-sm font-semibold ${
              tab === t ? "bg-white text-brand-dark ks-shadow" : "text-muted"
            }`}
          >
            {t === "bought" ? "I bought" : "Sold to me"}
          </button>
        ))}
      </div>

      {!orders ? (
        <Spinner />
      ) : orders.length === 0 ? (
        <EmptyState icon="📦" title="No orders here yet" />
      ) : (
        <div className="space-y-3">
          {orders.map((o) => (
            <div key={o.id} className="ks-shadow flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-line bg-white p-4">
              <div>
                <p className="font-semibold text-ink">
                  {o.listingTitle ?? "Marketplace order"}
                  <span className="ml-2 text-sm font-normal text-muted">
                    {o.quantity} {o.listingUnit ?? ""} × {formatINR(o.unitPrice)}
                  </span>
                </p>
                <p className="mt-1 text-xs text-muted">
                  {timeAgo(o.createdAt)}
                  {o.notes ? ` · "${o.notes}"` : ""}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <p className="font-bold text-brand-dark">{formatINR(o.totalPrice)}</p>
                <StatusBadge status={o.status} />
                {tab === "sold" && (
                  <div className="flex gap-2">
                    {o.status === "PENDING" && (
                      <button
                        onClick={() => updateStatus(o.id, "CONFIRMED")}
                        className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-dark"
                      >
                        Accept
                      </button>
                    )}
                    {o.status === "CONFIRMED" && (
                      <button
                        onClick={() => updateStatus(o.id, "SHIPPED")}
                        className="rounded-lg bg-brand-light px-3 py-1.5 text-xs font-semibold text-brand-dark"
                      >
                        Ship
                      </button>
                    )}
                    {o.status === "SHIPPED" && (
                      <button
                        onClick={() => updateStatus(o.id, "DELIVERED")}
                        className="rounded-lg bg-brand-light px-3 py-1.5 text-xs font-semibold text-brand-dark"
                      >
                        Deliver
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}