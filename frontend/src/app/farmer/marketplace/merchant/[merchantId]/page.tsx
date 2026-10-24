"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatINR } from "@/lib/format";

type MerchantProfile = {
  merchantId: string;
  fullName: string;
  city: string | null;
  state: string | null;
  avatarUrl?: string;
  latitude: number | null;
  longitude: number | null;
  distanceKm: number | null | undefined;
  itemCount: number;
  categories: string[];
};

type Product = {
  id: string;
  merchantId: string;
  name: string;
  description?: string;
  category?: string;
  price: number;
  quantity: number;
  unit: string;
  imageUrl?: string;
};

export default function MerchantDetailPage() {
  const params = useParams();
  const merchantId = params.merchantId as string;
  
  const [merchant, setMerchant] = useState<MerchantProfile | null>(null);
  const [products, setProducts] = useState<Product[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [quantity, setQuantity] = useState<Record<string, string>>({});
  const [busyId, setBusyId] = useState<string | null>(null);

  const loadMerchant = useCallback(() => {
    setError(null);
    api
      .get<MerchantProfile>(`/api/merchant/marketplace/${merchantId}/profile`)
      .then((data) => setMerchant(data))
      .catch((e) => {
        console.error('Error loading merchant profile:', e);
        setError(e instanceof Error ? e.message : 'Failed to load merchant profile');
      });
  }, [merchantId]);

  const loadProducts = useCallback(() => {
    setError(null);
    api
      .get<Product[]>(`/api/merchant/marketplace/${merchantId}`)
      .then((data) => setProducts(data))
      .catch((e) => {
        console.error('Error loading merchant products:', e);
        setError(e instanceof Error ? e.message : 'Failed to load merchant products');
      });
  }, [merchantId]);

  useEffect(() => {
    loadMerchant();
    loadProducts();
  }, [loadMerchant, loadProducts]);

  async function order(product: Product) {
    const q = parseInt(quantity[product.id] ?? "");
    if (!q || q <= 0) return;
    setBusyId(product.id);
    try {
      await api.post("/api/merchant/orders", {
        productId: product.id,
        quantity: q,
        notes: "",
      });
      alert(`Order placed for ${q} ${product.unit} of ${product.name}`);
      setQuantity((q) => ({ ...q, [product.id]: "" }));
      loadProducts();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to place order");
    } finally {
      setBusyId(null);
    }
  }

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
      <p className="text-red-600 font-semibold">Error loading merchant store</p>
      <p className="text-red-500 text-sm mt-1">{error}</p>
      <a
        href="/farmer/marketplace"
        className="mt-3 inline-block rounded-lg bg-red-100 px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-200"
      >
        Back to Marketplace
      </a>
    </div>
  );

  return (
    <div>
      <PageHeader
        title="Merchant Store"
        subtitle="Browse products from this merchant"
        action={
          <a
            href="/farmer/marketplace"
            className="rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-surface"
          >
            Back to Marketplace
          </a>
        }
      />

      {!merchant ? (
        <Spinner label="Loading merchant profile..." />
      ) : (
        <div className="mb-6 rounded-2xl border border-line bg-white p-6">
          <div className="flex items-start gap-4">
            {merchant.avatarUrl ? (
              <img 
                src={merchant.avatarUrl} 
                alt={merchant.fullName} 
                className="h-16 w-16 rounded-full object-cover border border-line flex-shrink-0"
              />
            ) : (
              <div className="h-16 w-16 rounded-full bg-brand text-white flex items-center justify-center text-2xl font-bold flex-shrink-0">
                {merchant.fullName.charAt(0).toUpperCase()}
              </div>
            )}
            <div className="flex-1">
              <h2 className="text-2xl font-bold text-ink">{merchant.fullName}</h2>
              <p className="text-muted mt-1">
                📍 {formatLocation(merchant.city, merchant.state)}
              </p>
              {merchant.distanceKm !== null && (
                <p className="text-muted mt-1">
                  📏 {formatDistance(merchant.distanceKm)}
                </p>
              )}
              <p className="text-muted mt-2">
                {merchant.itemCount} products available
              </p>
            </div>
          </div>
        </div>
      )}

      <h3 className="text-xl font-bold text-ink mb-4">
        {merchant ? `Products from ${merchant.fullName}` : "Products"}
      </h3>

      {!products ? (
        <Spinner label="Loading products..." />
      ) : products.length === 0 ? (
        <EmptyState 
          icon="📦" 
          title="No products available" 
          hint="This merchant currently has no products in stock." 
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {products.map((p) => (
            <div key={p.id} className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-5">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-bold text-ink">{p.name}</h3>
                {p.imageUrl && (
                  <img src={p.imageUrl} alt={p.name} className="h-12 w-12 rounded-lg object-cover" />
                )}
              </div>
              <p className="text-sm text-muted">
                {p.category || "General"}
              </p>
              <p className="mt-2 line-clamp-2 flex-1 text-sm text-muted">{p.description || "No description available"}</p>
              <div className="mt-3 flex items-end justify-between">
                <div>
                  <p className="text-xl font-bold text-brand-dark">
                    {formatINR(p.price)}
                    <span className="text-sm font-medium text-muted">/{p.unit}</span>
                  </p>
                  <p className="text-xs text-muted">
                    {p.quantity} {p.unit} available
                  </p>
                </div>
              </div>
              <div className="mt-3 flex gap-2 border-t border-line pt-3">
                <input
                  type="number"
                  min="1"
                  step="1"
                  placeholder={`Qty (${p.unit})`}
                  value={quantity[p.id] ?? ""}
                  onChange={(e) => setQuantity((q) => ({ ...q, [p.id]: e.target.value }))}
                  className="w-24 rounded-lg border border-line px-2 py-2 text-sm focus:border-brand focus:outline-none"
                />
                <button
                  onClick={() => order(p)}
                  disabled={busyId === p.id || p.quantity <= 0}
                  className="flex-1 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
                >
                  {busyId === p.id ? "Placing..." : p.quantity <= 0 ? "Out of stock" : "Place order"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}