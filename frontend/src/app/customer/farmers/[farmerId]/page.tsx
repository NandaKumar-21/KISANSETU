"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatINR } from "@/lib/format";

type FarmerProfile = {
  farmerId: string;
  fullName: string;
  city: string | null;
  state: string | null;
  avatarUrl?: string;
  distanceKm: number | null;
  cropCount: number;
  avgRating: number;
  totalReviews: number;
  categories: string[];
};

type Listing = {
  id: string;
  farmerId: string;
  farmerName: string;
  title: string;
  description?: string;
  category: string;
  price: number;
  quantity: number;
  unit: string;
  imageUrl?: string;
  location?: string;
  variety?: string;
  farmingMethod?: string;
  harvestDate?: string;
};

export default function FarmerDetailPage() {
  const params = useParams();
  const farmerId = params.farmerId as string;
  
  const [farmer, setFarmer] = useState<FarmerProfile | null>(null);
  const [listings, setListings] = useState<Listing[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadFarmer = useCallback(() => {
    setError(null);
    // Get farmer details from the farmer profile API
    api
      .get<FarmerProfile>(`/api/customer/farmers/${farmerId}/profile`)
      .then((data) => setFarmer(data))
      .catch((e) => {
        console.error('Error loading farmer profile:', e);
        setError(e instanceof Error ? e.message : 'Failed to load farmer profile');
      });
  }, [farmerId]);

  const loadListings = useCallback(() => {
    setError(null);
    api
      .get<Listing[]>(`/api/customer/farmers/${farmerId}`)
      .then((data) => setListings(data))
      .catch((e) => {
        console.error('Error loading farmer listings:', e);
        setError(e instanceof Error ? e.message : 'Failed to load farmer listings');
      });
  }, [farmerId]);

  useEffect(() => {
    loadFarmer();
    loadListings();
  }, [loadFarmer, loadListings]);

  async function addToCart(listing: Listing) {
    try {
      await api.post(`/api/customer/cart?listingId=${listing.id}&quantity=1`);
      setNotice(`Added ${listing.title} to cart`);
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to add to cart");
    }
  }

  const formatDistance = (distance: number | null) => {
    if (distance == null) return "Location not available";
    return `${distance.toFixed(1)} km away`;
  };

  const formatLocation = (city: string | null, state: string | null) => {
    if (city && state) return `${city}, ${state}`;
    if (city) return city;
    if (state) return state;
    return "Location not available";
  };

  const formatRating = (rating: number, totalReviews: number) => {
    if (totalReviews === 0) return "New farmer";
    return `⭐ ${rating.toFixed(1)} (${totalReviews} ${totalReviews === 1 ? 'review' : 'reviews'})`;
  };

  if (error) return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4">
      <p className="text-red-600 font-semibold">Error loading farmer store</p>
      <p className="text-red-500 text-sm mt-1">{error}</p>
      <a
        href="/customer/marketplace"
        className="mt-3 inline-block rounded-lg bg-red-100 px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-200"
      >
        Back to Marketplace
      </a>
    </div>
  );

  return (
    <div>
      <PageHeader
        title="Farmer Store"
        subtitle="Browse fresh produce from this farmer"
        action={
          <a
            href="/customer/marketplace"
            className="rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-surface"
          >
            Back to Marketplace
          </a>
        }
      />
      {notice && (
        <div className="mb-4 rounded-xl border border-brand/30 bg-brand-light px-4 py-2.5 text-sm font-medium text-brand-dark">
          ✓ {notice}
        </div>
      )}

      {!farmer ? (
        <Spinner label="Loading farmer profile..." />
      ) : (
        <div className="mb-6 rounded-2xl border border-line bg-white p-6">
          <div className="flex items-start gap-4">
            {farmer.avatarUrl ? (
              <img 
                src={farmer.avatarUrl} 
                alt={farmer.fullName} 
                className="h-16 w-16 rounded-full object-cover border border-line flex-shrink-0"
              />
            ) : (
              <div className="h-16 w-16 rounded-full bg-brand text-white flex items-center justify-center text-2xl font-bold flex-shrink-0">
                {farmer.fullName.charAt(0).toUpperCase()}
              </div>
            )}
            <div className="flex-1">
              <h2 className="text-2xl font-bold text-ink">{farmer.fullName}</h2>
              <p className="text-muted mt-1">
                📍 {formatLocation(farmer.city, farmer.state)}
              </p>
              {farmer.distanceKm !== null && (
                <p className="text-muted mt-1">
                  📏 {formatDistance(farmer.distanceKm)}
                </p>
              )}
              <p className="text-muted mt-1">
                {formatRating(farmer.avgRating, farmer.totalReviews)}
              </p>
              <p className="text-muted mt-2">
                {farmer.cropCount} active listings
              </p>
              {farmer.categories.length > 0 && (
                <p className="mt-1 text-sm text-muted">
                  {farmer.categories.slice(0, 3).join(" · ")}
                  {farmer.categories.length > 3 && " · ..."}
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      <h3 className="text-xl font-bold text-ink mb-4">
        {farmer ? `Available Produce from ${farmer.fullName}` : "Available Produce"}
      </h3>

      {!listings ? (
        <Spinner label="Loading produce..." />
      ) : listings.length === 0 ? (
        <EmptyState 
          icon="🥬" 
          title="No produce available" 
          hint="This farmer currently has no active listings." 
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {listings.map((l) => (
            <div key={l.id} className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-5">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-bold text-ink">{l.title}</h3>
                {l.variety && <span className="shrink-0 rounded-full bg-surface px-2 py-0.5 text-xs text-muted">{l.variety}</span>}
              </div>
              <p className="mt-0.5 text-xs text-muted">
                {l.category}
              </p>
              <p className="mt-2 line-clamp-2 flex-1 text-sm text-muted">{l.description || "No description available"}</p>
              {l.farmingMethod && (
                <span className="mt-2 inline-block w-fit rounded-full bg-yellow-50 px-2 py-0.5 text-[11px] font-medium text-yellow-800">
                  🌱 {l.farmingMethod}
                </span>
              )}
              {l.harvestDate && (
                <p className="mt-2 text-xs text-muted">
                  📅 Harvested: {new Date(l.harvestDate).toLocaleDateString()}
                </p>
              )}
              <div className="mt-3 flex items-end justify-between">
                <div>
                  <p className="text-xl font-bold text-brand-dark">
                    {formatINR(l.price)}
                    <span className="text-sm font-medium text-muted">/{l.unit}</span>
                  </p>
                  <p className="text-xs text-muted">
                    {l.quantity} {l.unit} available
                  </p>
                </div>
              </div>
              <button
                onClick={() => addToCart(l)}
                className="mt-3 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark"
              >
                Add to cart
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}