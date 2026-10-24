"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, formatDateTime, timeAgo } from "@/lib/format";

type Order = {
  id: string;
  customerName?: string;
  listingTitle?: string;
  listingUnit?: string;
  quantity: number;
  totalPrice: number;
  deliveryStatus?: string;
  deliveryPreference?: string;
  pickupTime?: string;
  createdAt: string;
  deliveredAt?: string;
};

const PIPELINE = ["assigned", "accepted", "pickup_scheduled", "picked_up", "in_transit", "delivered", "completed"];

export default function LogisticsDeliveriesPage() {
  const [active, setActive] = useState<Order[] | null>(null);
  const [completed, setCompleted] = useState<Order[] | null>(null);
  const [tab, setTab] = useState<"active" | "completed">("active");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [a, c] = await Promise.all([
        api.get<Order[]>("/api/logistics/deliveries/active"),
        api.get<Order[]>("/api/logistics/deliveries/completed"),
      ]);
      setActive(a);
      setCompleted(c);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load deliveries");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function act(orderId: string, action: "accept" | "reject" | "advance") {
    if (action === "reject" && !confirm("Reject this delivery? It will be reassigned.")) return;
    try {
      await api.post(`/api/logistics/deliveries/${orderId}/${action}`);
      load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Action failed");
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;

  const orders = tab === "active" ? active : completed;

  return (
    <div>
      <PageHeader title="Deliveries" subtitle="Accept, progress and complete deliveries" />
      <div className="mb-5 flex gap-1 rounded-xl border border-line bg-white p-1 sm:w-fit">
        {(["active", "completed"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`flex-1 rounded-lg px-5 py-2 text-sm font-semibold capitalize ${
              tab === t ? "bg-brand text-white" : "text-muted hover:bg-surface"
            }`}
          >
            {t} ({t === "active" ? active?.length ?? 0 : completed?.length ?? 0})
          </button>
        ))}
      </div>

      {!orders ? (
        <Spinner />
      ) : orders.length === 0 ? (
        <EmptyState icon="🚚" title={`No ${tab} deliveries`} hint="New assignments appear here automatically." />
      ) : (
        <div className="space-y-3">
          {orders.map((o) => {
            const idx = o.deliveryStatus ? PIPELINE.indexOf(o.deliveryStatus) : -1;
            return (
              <div key={o.id} className="ks-shadow rounded-2xl border border-line bg-white p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-ink">
                      {o.listingTitle ?? "Produce"}
                      <span className="ml-2 text-sm font-normal text-muted">
                        {o.quantity} {o.listingUnit ?? ""}
                      </span>
                    </p>
                    <p className="mt-0.5 text-xs text-muted">
                      {o.customerName ?? "Customer"} · {timeAgo(o.createdAt)}
                      {o.deliveryPreference && ` · ${o.deliveryPreference}`}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <p className="font-bold text-brand-dark">{formatINR(o.totalPrice)}</p>
                    <StatusBadge status={o.deliveryStatus ?? "assigned"} />
                  </div>
                </div>

                {tab === "active" && o.deliveryStatus && idx >= 0 && (
                  <div className="mt-3 flex items-center gap-1.5">
                    {PIPELINE.slice(0, Math.max(idx + 1, 1)).map((step, i) => (
                      <div key={step} className="flex flex-1 items-center gap-1.5">
                        <div
                          className={`h-2 flex-1 rounded-full ${
                            i < idx ? "bg-brand" : i === idx ? "bg-brand-light" : "bg-surface"
                          }`}
                        />
                      </div>
                    ))}
                    <span className="text-[11px] font-semibold text-muted capitalize">
                      {o.deliveryStatus.replace("_", " ")}
                    </span>
                  </div>
                )}

                {o.pickupTime && (
                  <p className="mt-2 text-xs text-muted">🕑 Pickup at {formatDateTime(o.pickupTime)}</p>
                )}
                {o.deliveredAt && (
                  <p className="mt-2 text-xs text-muted">✅ Delivered {formatDateTime(o.deliveredAt)}</p>
                )}

                <div className="mt-3 flex flex-wrap gap-2 border-t border-line pt-3">
                  {o.deliveryStatus === "assigned" && (
                    <>
                      <button
                        onClick={() => act(o.id, "accept")}
                        className="rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-dark"
                      >
                        Accept delivery
                      </button>
                      <button
                        onClick={() => act(o.id, "reject")}
                        className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-xs font-semibold text-red-700"
                      >
                        Reject
                      </button>
                    </>
                  )}
                  {o.deliveryStatus && ["accepted", "pickup_scheduled", "picked_up", "in_transit"].includes(o.deliveryStatus) && (
                    <button
                      onClick={() => act(o.id, "advance")}
                      className="rounded-lg bg-brand px-4 py-2 text-xs font-semibold text-white hover:bg-brand-dark"
                    >
                      Advance to next stage →
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}