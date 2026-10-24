"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { formatINR, timeAgo } from "@/lib/format";

type Merchant = {
  merchantId: string;
  shopName?: string;
  city?: string;
  state?: string;
  productCount?: number;
  avgPrice?: number;
  distanceKm?: number;
};

type Product = {
  id: string;
  name: string;
  category: string;
  price: number;
  unit: string;
  quantity: number;
  imageUrl?: string;
};

type Order = {
  id: string;
  merchantName?: string;
  productName?: string;
  productUnit?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
  notes?: string;
  createdAt: string;
};

export default function FarmerMerchantOrdersPage() {
  const [merchants, setMerchants] = useState<Merchant[] | null>(null);
  const [products, setProducts] = useState<Product[] | null>(null);
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [selectedMerchant, setSelectedMerchant] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Merchant[]>("/api/merchant/marketplace")
      .then(setMerchants)
      .catch((e) => setError(e.message));
    api
      .get<Order[]>("/api/merchant/orders/mine")
      .then(setOrders)
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!selectedMerchant) return;
    api
      .get<Product[]>(`/api/merchant/marketplace/${selectedMerchant}`)
      .then(setProducts)
      .catch((e) => setError(e.message));
  }, [selectedMerchant]);

  async function order(product: Product, qty: number) {
    try {
      await api.post("/api/merchant/orders", {
        merchantId: selectedMerchant,
        productId: product.id,
        quantity: qty,
      });
      alert(`Order placed for ${qty} ${product.unit} of ${product.name}`);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed");
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader
        title="Input Orders"
        subtitle="Buy seeds, fertilizers and equipment from merchants"
      />

      {!merchants ? (
        <Spinner />
      ) : merchants.length === 0 ? (
        <EmptyState icon="🏬" title="No merchants available" />
      ) : (
        <>
          <h3 className="mb-2 text-sm font-semibold text-muted uppercase">Choose a merchant</h3>
          <div className="grid gap-3 md:grid-cols-3">
            {merchants.map((m) => (
              <button
                key={m.merchantId}
                onClick={() => setSelectedMerchant(m.merchantId)}
                className={`ks-shadow rounded-2xl border bg-white p-4 text-left transition-colors ${
                  selectedMerchant === m.merchantId
                    ? "border-brand bg-brand-light"
                    : "border-line hover:border-brand/30"
                }`}
              >
                <p className="font-bold text-ink">{m.shopName ?? "Shop"}</p>
                <p className="text-sm text-muted">
                  {m.city ?? ""}{m.city && m.state ? ", " : ""}{m.state ?? ""}
                </p>
                <p className="mt-2 text-xs text-muted">
                  {m.productCount ?? 0} products
                  {m.avgPrice != null ? ` · avg ${formatINR(m.avgPrice)}` : ""}
                  {m.distanceKm != null ? ` · ${m.distanceKm.toFixed(1)} km` : ""}
                </p>
              </button>
            ))}
          </div>

          {selectedMerchant && (
            <div className="mt-6">
              <h3 className="mb-2 text-sm font-semibold text-muted uppercase">Products</h3>
              {!products ? (
                <Spinner />
              ) : products.length === 0 ? (
                <EmptyState title="No in-stock products" />
              ) : (
                <div className="grid gap-3 md:grid-cols-3">
                  {products.map((p) => (
                    <OrderRow key={p.id} product={p} onOrder={order} />
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}

      <div className="mt-8">
        <h3 className="mb-3 text-sm font-semibold text-muted uppercase">My input orders</h3>
        {!orders ? (
          <Spinner />
        ) : orders.length === 0 ? (
          <EmptyState icon="🌱" title="No input orders yet" />
        ) : (
          <div className="space-y-3">
            {orders.map((o) => (
              <div key={o.id} className="ks-shadow flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-line bg-white p-4">
                <div>
                  <p className="font-semibold text-ink">
                    {o.productName ?? "Order"}
                    <span className="ml-2 text-sm font-normal text-muted">
                      {o.quantity} {o.productUnit ?? ""} × {formatINR(o.unitPrice)}
                    </span>
                  </p>
                  <p className="mt-0.5 text-xs text-muted">
                    {o.merchantName ?? "Merchant"} · {timeAgo(o.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <p className="font-bold text-brand-dark">{formatINR(o.totalPrice)}</p>
                  <StatusBadge status={o.status} />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function OrderRow({
  product,
  onOrder,
}: {
  product: Product;
  onOrder: (p: Product, qty: number) => void;
}) {
  const [qty, setQty] = useState(1);
  return (
    <div className="ks-shadow flex flex-col rounded-2xl border border-line bg-white p-4">
      <p className="font-semibold text-ink">{product.name}</p>
      <p className="text-sm text-muted">
        {product.category} · {product.quantity} {product.unit} in stock
      </p>
      <p className="mt-2 text-lg font-bold text-brand-dark">
        {formatINR(product.price)}
        <span className="text-sm font-medium text-muted">/{product.unit}</span>
      </p>
      <div className="mt-3 flex items-center gap-2 border-t border-line pt-3">
        <input
          type="number"
          min={1}
          max={Math.max(1, product.quantity)}
          value={qty}
          onChange={(e) => setQty(parseInt(e.target.value) || 1)}
          className="w-16 rounded-lg border border-line px-2 py-1.5 text-sm focus:border-brand focus:outline-none"
        />
        <button
          onClick={() => onOrder(product, qty)}
          className="flex-1 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-dark"
        >
          Order
        </button>
      </div>
    </div>
  );
}