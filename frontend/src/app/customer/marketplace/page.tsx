"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";

type Farmer = {
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

export default function CustomerMarketplacePage() {
  const [farmers, setFarmers] = useState<Farmer[] | null>(null);
  const [search, setSearch] = useState("");
  const [radius, setRadius] = useState<number | null>(null);
  const [sort, setSort] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [usingBrowserLocation, setUsingBrowserLocation] = useState(false);
  const [customerLocation, setCustomerLocation] = useState<{ lat: number; lng: number } | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams();
    if (search) params.set("search", search);
    if (radius) params.set("radiusKm", radius.toString());
    if (sort) params.set("sort", sort);
    if (customerLocation) {
      params.set("lat", customerLocation.lat.toString());
      params.set("lng", customerLocation.lng.toString());
    }
    
    const queryString = params.toString();
    const url = `/api/customer/farmers${queryString ? '?' + queryString : ''}`;
    
    api
      .get<Farmer[]>(url)
      .then((data) => setFarmers(data))
      .catch((e) => {
        console.error('Error loading farmers:', e);
        setError(e instanceof Error ? e.message : 'Failed to load farmers');
      })
      .finally(() => setLoading(false));
  }, [search, radius, sort, customerLocation]);

  const getCurrentLocation = useCallback(() => {
    setLocationLoading(true);
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setCustomerLocation({ lat: position.coords.latitude, lng: position.coords.longitude });
          setUsingBrowserLocation(true);
          setLocationLoading(false);
        },
        (error) => {
          console.warn('Geolocation error:', error);
          setLocationLoading(false);
          setCustomerLocation(null);
          setUsingBrowserLocation(false);
        }
      );
    } else {
      setLocationLoading(false);
    }
  }, []);

  useEffect(() => {
    getCurrentLocation();
  }, [getCurrentLocation]);

  useEffect(() => {
    load();
  }, [load]);

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
      <p className="text-red-600 font-semibold">Error loading farmers</p>
      <p className="text-red-500 text-sm mt-1">{error}</p>
      <button 
        onClick={load}
        className="mt-3 rounded-lg bg-red-100 px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-200"
      >
        Retry
      </button>
    </div>
  );

  return (
    <div>
      <PageHeader title="Farmer Marketplace" subtitle="Discover local farmers and their fresh produce" />

      <div className="mb-6 flex flex-wrap gap-3">
        <input
          className="w-64 rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none"
          placeholder="Search farmers..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className="rounded-xl border border-line bg-white px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
          value={radius ?? ""}
          onChange={(e) => setRadius(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">All distances</option>
          <option value={10}>Within 10 km</option>
          <option value={20}>Within 20 km</option>
          <option value={50}>Within 50 km</option>
          <option value={100}>Within 100 km</option>
        </select>
        <select
          className="rounded-xl border border-line bg-white px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
          value={sort}
          onChange={(e) => setSort(e.target.value)}
        >
          <option value="">Sort by</option>
          <option value="nearest">Nearest first</option>
          <option value="top-rated">Top rated</option>
          <option value="lowest">Lowest prices</option>
        </select>
        {!usingBrowserLocation && (
          <button
            onClick={getCurrentLocation}
            disabled={locationLoading}
            className="rounded-lg bg-brand-light px-4 py-2.5 text-sm font-semibold text-brand-dark hover:bg-brand disabled:opacity-60"
          >
            {locationLoading ? "Getting location..." : "Use my location"}
          </button>
        )}
        {usingBrowserLocation && (
          <span className="text-sm text-muted self-center">Using your current location</span>
        )}
      </div>

      {locationLoading ? (
        <Spinner label="Finding farmers near you..." />
      ) : loading ? (
        <Spinner label="Loading farmers..." />
      ) : farmers === null ? (
        <Spinner label="Loading marketplace..." />
      ) : farmers.length === 0 ? (
        <EmptyState 
          icon="👨‍🌾" 
          title="No farmers found" 
          hint={
            <div className="flex flex-col gap-2">
              <p>Try adjusting your search radius or search terms.</p>
              {!usingBrowserLocation && (
                <button
                  onClick={getCurrentLocation}
                  className="text-brand hover:underline"
                >
                  Use your current location for better results
                </button>
              )}
            </div>
          } 
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {farmers.map((farmer) => (
            <div key={farmer.farmerId} className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-5">
              <div className="flex items-start gap-4">
                {farmer.avatarUrl ? (
                  <img 
                    src={farmer.avatarUrl} 
                    alt={farmer.fullName} 
                    className="h-14 w-14 rounded-full object-cover border border-line flex-shrink-0"
                  />
                ) : (
                  <div className="h-14 w-14 rounded-full bg-brand text-white flex items-center justify-center text-xl font-bold flex-shrink-0">
                    {farmer.fullName.charAt(0).toUpperCase()}
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <h3 className="font-bold text-ink text-lg truncate">{farmer.fullName}</h3>
                  <p className="text-sm text-muted mt-1">
                    📍 {formatLocation(farmer.city, farmer.state)}
                  </p>
                  {farmer.distanceKm !== null && (
                    <p className="text-sm text-muted mt-1">
                      📏 {formatDistance(farmer.distanceKm)}
                    </p>
                  )}
                  <p className="text-sm text-muted mt-1">
                    {formatRating(farmer.avgRating, farmer.totalReviews)}
                  </p>
                </div>
              </div>

              <div className="mt-4 border-t border-line pt-4">
                <p className="text-sm font-medium text-ink">
                  {farmer.cropCount} active listings
                </p>
                {farmer.categories.length > 0 && (
                  <p className="mt-2 text-sm text-muted">
                    {farmer.categories.slice(0, 3).join(" · ")}
                    {farmer.categories.length > 3 && " · ..."}
                  </p>
                )}
              </div>

              <a
                href={`/customer/farmers/${farmer.farmerId}`}
                className="mt-4 rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark text-center"
              >
                View farmer →
              </a>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}