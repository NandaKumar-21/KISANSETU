"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatINR } from "@/lib/format";

type CompareRow = {
  listingId: string;
  farmerId: string;
  farmerName: string;
  price: number;
  quantity: number;
  unit: string;
  location?: string;
  farmingMethod?: string;
  avgRating: number;
};

type CompareGroup = {
  displayName: string;
  key: string;
  rows: CompareRow[];
};

export default function CustomerComparePage() {
  const [groups, setGroups] = useState<CompareGroup[] | null>(null);
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (search) params.set("search", search);
    api
      .get<CompareGroup[]>(`/api/customer/compare?${params.toString()}`)
      .then(setGroups)
      .catch((e) => setError(e.message));
  }, [search]);

  useEffect(load, [load]);

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader title="Price Comparison" subtitle="Same produce, different farmers — pick the best deal" />

      <div className="mb-5 flex max-w-md gap-2">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Filter by produce..."
          className="flex-1 rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none"
        />
        <button onClick={load} className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark">
          Filter
        </button>
      </div>

      {!groups ? (
        <Spinner />
      ) : groups.length === 0 ? (
        <EmptyState icon="⚖️" title="Nothing to compare" hint="Produce listed by multiple farmers will appear here." />
      ) : (
        <div className="space-y-5">
          {groups.map((g) => {
            const sorted = [...g.rows].sort((a, b) => a.price - b.price);
            const cheapest = sorted[0];
            return (
              <div key={g.key} className="ks-shadow overflow-hidden rounded-2xl border border-line bg-white">
                <div className="flex items-center justify-between bg-brand-light px-4 py-3">
                  <h3 className="font-bold text-brand-dark">{g.displayName}</h3>
                  <span className="rounded-full bg-brand px-2.5 py-0.5 text-xs font-bold text-white">
                    from {formatINR(cheapest.price)}/{cheapest.unit}
                  </span>
                </div>
                <div className="divide-y divide-line">
                  {sorted.map((r) => (
                    <div key={r.listingId} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-ink">
                          {r.farmerName}
                          {r.avgRating > 0 && <span className="ml-1.5 text-xs text-amber-600">⭐ {r.avgRating.toFixed(1)}</span>}
                        </p>
                        <p className="text-xs text-muted">
                          {[r.location, r.farmingMethod].filter(Boolean).join(" · ")}
                        </p>
                      </div>
                      <div className="flex items-center gap-3">
                        <p className="text-sm font-bold text-brand-dark">
                          {formatINR(r.price)}
                          <span className="text-xs font-medium text-muted">/{r.unit}</span>
                        </p>
                        <p className="text-xs text-muted">{r.quantity} {r.unit} available</p>
                        {r.listingId === cheapest.listingId && (
                          <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-[11px] font-bold text-yellow-800">
                            BEST PRICE
                          </span>
                        )}
                        <Link
                          href="/customer/marketplace"
                          className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-dark"
                        >
                          Order
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}