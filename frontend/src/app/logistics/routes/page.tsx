"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";

type Stop = {
  sequence: number;
  orderId: string;
  title: string;
  quantity: string;
  unit: string;
  status: string;
  createdAt: string;
};

type Routes = { stops: Stop[]; totalStops: number };

export default function LogisticsRoutesPage() {
  const [routes, setRoutes] = useState<Routes | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Routes>("/api/logistics/routes")
      .then(setRoutes)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="Delivery Route" subtitle="Optimized stop order for active deliveries" />
      {!routes ? (
        <Spinner />
      ) : routes.totalStops === 0 ? (
        <EmptyState icon="🗺️" title="No active route" hint="Your route will appear here once deliveries are in progress." />
      ) : (
        <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
          <p className="mb-4 text-sm text-muted">
            {routes.totalStops} stop(s) · Deliver in the order below
          </p>
          <div className="space-y-0">
            {routes.stops.map((s, i) => (
              <div key={s.orderId} className="relative flex gap-4 pb-6 last:pb-0">
                {i < routes.stops.length - 1 && (
                  <span className="absolute top-8 left-[17px] h-[calc(100%-2rem)] w-0.5 bg-line" />
                )}
                <span className="z-10 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand text-sm font-bold text-white">
                  {s.sequence}
                </span>
                <div className="flex flex-1 items-center justify-between rounded-xl border border-line px-4 py-3">
                  <div>
                    <p className="text-sm font-semibold text-ink">{s.title}</p>
                    <p className="text-xs text-muted">
                      {s.quantity} {s.unit} · {s.status.replace("_", " ")}
                    </p>
                  </div>
                  <Link
                    href="/logistics/deliveries"
                    className="rounded-lg bg-brand-light px-3 py-1.5 text-xs font-semibold text-brand-dark"
                  >
                    Manage
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}