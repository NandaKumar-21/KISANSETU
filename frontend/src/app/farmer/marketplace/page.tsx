"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";

type Merchant = {
  merchantId: string;
  fullName: string;
  city: string | null;
  state: string | null;
  avatarUrl?: string;
  latitude: number | null;
  longitude: number | null;
  distanceKm: number | null;
  itemCount: number;
  categories: string[];
};

export default function FarmerMarketplacePage() {
  const [merchants, setMerchants] = useState<Merchant[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [radius, setRadius] = useState<number>(20);
  const [loading, setLoading] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [usingBrowserLocation, setUsingBrowserLocation] = useState(false);
  const [farmerLocation, setFarmerLocation] = useState<{ lat: number; lng: number } | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams();
    if (search) params.set("search", search);
    if (radius) params.set("radiusKm", radius.toString());
    if (farmerLocation) {
      params.set("lat", farmerLocation.lat.toString());
      params.set("lng", farmerLocation.lng.toString());
    }
    
    const queryString = params.toString();
    const url = `/api/merchant/marketplace${queryString ? '?' + queryString : ''}`;
    
    api
      .get<Merchant[]>(url)
      .then((data) => setMerchants(data))
      .catch((e) => {
        console.error('Error loading merchants:', e);
        setError(e instanceof Error ? e.message : 'Failed to load merchants');
      })
      .finally(() => setLoading(false));
  }, [search, radius, farmerLocation]);

  const getCurrentLocation = useCallback(() => {
    setLocationLoading(true);
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setFarmerLocation({ lat: position.coords.latitude, lng: position.coords.longitude });
          setUsingBrowserLocation(true);
          setLocationLoading(false);
        },
        (error) => {
          console.warn('Geolocation error:', error);
          setLocationLoading(false);
          // Fall back to nearby endpoint which uses saved profile location
          setFarmerLocation(null);
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

  const formatDistance = (distance: number | null | undefined) => {
    if (distance == null) return "Location unknown";
    return `${distance.toFixed(1)} km away`;
  };

  const formatLocation = (city: string | null, state: string | null) => {
    if (city && state) return `${city}, ${state}`;
    if (city) return city;
    if (state) return state;
    return "Location not available";
  };

  if (error) return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-4">
      <p className="text-red-600 font-semibold">Error loading marketplace</p>
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
      <PageHeader
        title="Marketplace"
        subtitle="Find agricultural suppliers near you"
        action={
          <a
            href="/farmer/merchant-orders"
            className="rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-surface"
          >
            My orders
          </a>
        }
      />

      <div className="mb-6 flex flex-wrap gap-3">
        <input
          className="w-64 rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none"
          placeholder="Search merchants..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className="rounded-xl border border-line bg-white px-3 py-2.5 text-sm focus:border-brand focus:outline-none"
          value={radius}
          onChange={(e) => setRadius(Number(e.target.value))}
        >
          <option value={5}>Within 5 km</option>
          <option value={10}>Within 10 km</option>
          <option value={20}>Within 20 km</option>
          <option value={50}>Within 50 km</option>
          <option value={100}>Within 100 km</option>
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
        <Spinner label="Finding merchants near you..." />
      ) : loading ? (
        <Spinner label="Loading nearby merchants..." />
      ) : merchants === null ? (
        <Spinner label="Loading marketplace..." />
      ) : merchants.length === 0 ? (
        <EmptyState 
          icon="🏪" 
          title="No merchants found within your selected radius" 
          hint={
            <div className="flex flex-col gap-2">
              <p>Try increasing your search radius or check your location settings.</p>
              <button
                onClick={() => setRadius(50)}
                className="text-brand hover:underline"
              >
                Try 50 km radius
              </button>
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
          {merchants.map((merchant) => (
            <div key={merchant.merchantId} className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-5">
              <div className="flex items-start gap-4">
                {merchant.avatarUrl ? (
                  <img 
                    src={merchant.avatarUrl} 
                    alt={merchant.fullName} 
                    className="h-14 w-14 rounded-full object-cover border border-line flex-shrink-0"
                  />
                ) : (
                  <div className="h-14 w-14 rounded-full bg-brand text-white flex items-center justify-center text-xl font-bold flex-shrink-0">
                    {merchant.fullName.charAt(0).toUpperCase()}
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <h3 className="font-bold text-ink text-lg truncate">{merchant.fullName}</h3>
                  <p className="text-sm text-muted mt-1">
                    📍 {formatLocation(merchant.city, merchant.state)}
                  </p>
                  {merchant.distanceKm !== null && (
                    <p className="text-sm text-muted mt-1">
                      📏 {formatDistance(merchant.distanceKm)}
                    </p>
                  )}
                </div>
              </div>

              <div className="mt-4 border-t border-line pt-4">
                <p className="text-sm font-medium text-ink">
                  {merchant.itemCount} products available
                </p>
                {merchant.categories.length > 0 && (
                  <p className="mt-2 text-sm text-muted">
                    {merchant.categories.slice(0, 3).join(" · ")}
                    {merchant.categories.length > 3 && " · ..."}
                  </p>
                )}
              </div>

              <a
                href={`/farmer/marketplace/merchant/${merchant.merchantId}`}
                className="mt-4 rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark text-center"
              >
                View Store
              </a>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}