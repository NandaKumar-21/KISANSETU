"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, formatDate } from "@/lib/format";

type Listing = {
  id: string;
  title: string;
  description?: string;
  category: string;
  price: number;
  quantity: number;
  unit: string;
  location?: string;
  status: string;
  variety?: string;
  farmingMethod?: string;
  harvestDate?: string;
};

export default function FarmerListingsPage() {
  const [listings, setListings] = useState<Listing[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api
      .get<Listing[]>("/api/farmer/listings")
      .then(setListings)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  async function setStatus(id: string, status: string) {
    await api.patch(`/api/farmer/listings/${id}/status?status=${status}`);
    load();
  }

  if (error) return <p className="text-red-600">{error}</p>;
  if (!listings) return <Spinner label="Loading listings..." />;

  return (
    <div>
      <PageHeader
        title="My Listings"
        subtitle="Produce you are selling on the marketplace"
        action={
          <button
            onClick={() => (window.location.href = "/farmer/listings/new")}
            className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            + New Listing
          </button>
        }
      />

      {listings.length === 0 ? (
        <EmptyState
          icon="🧺"
          title="No listings yet"
          hint="Create your first listing to start selling produce on the marketplace."
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {listings.map((l) => (
            <div key={l.id} className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-5">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h3 className="font-bold text-ink">{l.title}</h3>
                  <p className="text-sm text-muted">
                    {l.category}
                    {l.variety ? ` · ${l.variety}` : ""}
                    {l.farmingMethod ? ` · ${l.farmingMethod}` : ""}
                  </p>
                </div>
                <StatusBadge status={l.status} />
              </div>
              <p className="mt-3 line-clamp-2 text-sm text-muted">{l.description}</p>
              <div className="mt-4 flex items-end justify-between">
                <div>
                  <p className="text-xl font-bold text-brand-dark">
                    {formatINR(l.price)}
                    <span className="text-sm font-medium text-muted">/{l.unit}</span>
                  </p>
                  <p className="text-xs text-muted">
                    {l.quantity} {l.unit} available{l.harvestDate ? ` · harvest ${formatDate(l.harvestDate)}` : ""}
                  </p>
                </div>
              </div>
              <div className="mt-4 flex gap-2 border-t border-line pt-3">
                {l.status === "ACTIVE" ? (
                  <button
                    onClick={() => setStatus(l.id, "PAUSED")}
                    className="flex-1 rounded-lg border border-line px-3 py-2 text-xs font-semibold text-muted hover:bg-surface"
                  >
                    Pause
                  </button>
                ) : (
                  <button
                    onClick={() => setStatus(l.id, "ACTIVE")}
                    className="flex-1 rounded-lg bg-brand-light px-3 py-2 text-xs font-semibold text-brand-dark hover:bg-brand/10"
                  >
                    Activate
                  </button>
                )}
                <button
                  onClick={() => setStatus(l.id, "SOLD")}
                  className="flex-1 rounded-lg border border-line px-3 py-2 text-xs font-semibold text-muted hover:bg-surface"
                >
                  Mark sold
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}