"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, timeAgo } from "@/lib/format";

type Order = {
  id: string;
  customerName?: string;
  listingTitle?: string;
  listingUnit?: string;
  quantity: number;
  totalPrice: number;
  status: string;
  deliveryStatus?: string;
  deliveryPreference?: string;
  createdAt: string;
};

export default function LogisticsOrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .get<Order[]>("/api/logistics/orders")
      .then(setOrders)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader title="My Deliveries" subtitle="All orders assigned to you" />
      {!orders ? (
        <Spinner />
      ) : orders.length === 0 ? (
        <EmptyState icon="🚚" title="No deliveries assigned yet" hint="Orders get assigned when farmers dispatch them." />
      ) : (
        <div className="space-y-3">
          {orders.map((o) => (
            <div key={o.id} className="ks-shadow flex items-center justify-between gap-3 rounded-2xl border border-line bg-white p-4">
              <div className="min-w-0">
                <p className="font-semibold text-ink">
                  {o.listingTitle ?? "Produce"}
                  <span className="ml-2 text-sm font-normal text-muted">
                    {o.quantity} {o.listingUnit ?? ""}
                  </span>
                </p>
                <p className="mt-0.5 truncate text-xs text-muted">
                  {o.customerName ?? "Customer"} · placed {timeAgo(o.createdAt)}
                  {o.deliveryPreference && ` · ${o.deliveryPreference}`}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <p className="font-bold text-brand-dark">{formatINR(o.totalPrice)}</p>
                <StatusBadge status={o.deliveryStatus ?? o.status} />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}