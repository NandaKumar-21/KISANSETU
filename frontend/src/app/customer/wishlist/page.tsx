"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatINR } from "@/lib/format";

type WishlistItem = {
  id: string;
  listingId?: string;
  farmerId?: string;
  createdAt: string;
};

type Listing = { id: string; title: string; price: number; unit: string; farmerName: string };
type Farmer = { farmerId: string; fullName: string; city?: string; state?: string; avgRating: number };

export default function CustomerWishlistPage() {
  const [items, setItems] = useState<WishlistItem[] | null>(null);
  const [listings, setListings] = useState<Listing[]>([]);
  const [farmers, setFarmers] = useState<Farmer[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const wishlist = await api.get<WishlistItem[]>("/api/customer/wishlist");
      setItems(wishlist);
      if (wishlist.length === 0) return;
      const [produce, farmerList] = await Promise.all([
        api.get<{ content: Listing[] }>("/api/customer/produce?page=0&size=200"),
        api.get<Farmer[]>("/api/customer/farmers"),
      ]);
      setListings(produce.content);
      setFarmers(farmerList);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load wishlist");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function remove(item: WishlistItem) {
    await api.del(`/api/customer/wishlist/${item.id}`);
    load();
  }

  if (error) return <p className="text-red-600">{error}</p>;

  const listingOf = (id?: string) => listings.find((l) => l.id === id);
  const farmerOf = (id?: string) => farmers.find((f) => f.farmerId === id);

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title="My Wishlist" subtitle="Saved produce and favourite farmers" />
      {!items ? (
        <Spinner />
      ) : items.length === 0 ? (
        <EmptyState icon="💚" title="Wishlist is empty" hint="Tap the heart on any produce or farmer to save it here." />
      ) : (
        <div className="space-y-3">
          {items.map((it) => {
            const listing = listingOf(it.listingId);
            const farmer = farmerOf(it.farmerId);
            return (
              <div key={it.id} className="ks-shadow flex items-center justify-between gap-3 rounded-2xl border border-line bg-white p-4">
                <div>
                  {listing ? (
                    <>
                      <p className="font-semibold text-ink">{listing.title}</p>
                      <p className="text-xs text-muted">{listing.farmerName} · {formatINR(listing.price)}/{listing.unit}</p>
                    </>
                  ) : farmer ? (
                    <>
                      <p className="font-semibold text-ink">{farmer.fullName}</p>
                      <p className="text-xs text-muted">
                        {farmer.city ?? "Farmer"} {farmer.state ? `, ${farmer.state}` : ""}
                        {farmer.avgRating > 0 && ` · ⭐ ${farmer.avgRating.toFixed(1)}`}
                      </p>
                    </>
                  ) : (
                    <p className="text-sm text-muted">Item no longer available</p>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  {listing && (
                    <Link
                      href="/customer/marketplace"
                      className="rounded-lg bg-brand px-3 py-2 text-xs font-semibold text-white hover:bg-brand-dark"
                    >
                      Buy again
                    </Link>
                  )}
                  <button
                    onClick={() => remove(it)}
                    className="rounded-lg border border-red-100 px-3 py-2 text-xs font-semibold text-red-600 hover:bg-red-50"
                  >
                    Remove
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}