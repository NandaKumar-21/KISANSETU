"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, StatCard, StatusBadge } from "@/components/ui";
import { formatINR, timeAgo } from "@/lib/format";

type RecentOrder = {
  id: string;
  listingTitle?: string;
  totalPrice: number;
  status: string;
  createdAt: string;
};

type Dashboard = {
  cartCount: number;
  wishlistCount: number;
  activeOrders: number;
  deliveredOrders: number;
  recentOrders: RecentOrder[];
};

export default function CustomerDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Dashboard>("/api/customer/dashboard")
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;
  if (!data) return <Spinner label="Loading dashboard..." />;

  return (
    <div>
      <PageHeader
        title="Customer Dashboard"
        subtitle="Fresh produce, straight from farms"
        action={
          <Link
            href="/customer/marketplace"
            className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            Browse marketplace
          </Link>
        }
      />

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Cart Items" value={data.cartCount} accent="brand" />
        <StatCard label="Wishlist" value={data.wishlistCount} accent="amber" />
        <StatCard label="Active Orders" value={data.activeOrders} accent="sky" />
        <StatCard label="Delivered" value={data.deliveredOrders} accent="yellow" />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-ink">Recent orders</h3>
            <Link href="/customer/orders" className="text-sm font-medium text-brand hover:underline">
              View all
            </Link>
          </div>
          <div className="mt-3 space-y-2">
            {data.recentOrders.length === 0 ? (
              <p className="text-sm text-muted">No orders yet. Visit the marketplace!</p>
            ) : (
              data.recentOrders.map((o) => (
                <div key={o.id} className="flex items-center justify-between rounded-xl bg-surface px-3 py-2.5">
                  <div>
                    <p className="text-sm font-semibold text-ink">{o.listingTitle ?? "Order"}</p>
                    <p className="text-xs text-muted">{timeAgo(o.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-brand-dark">{formatINR(o.totalPrice)}</span>
                    <StatusBadge status={o.status} />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
          <h3 className="font-bold text-ink">Quick actions</h3>
          <div className="mt-3 grid gap-2">
            {[
              ["/customer/marketplace", "🥬", "Browse produce", "Fresh listings from farmers"],
              ["/customer/compare", "⚖️", "Compare prices", "Best price across farmers"],
              ["/customer/cart", "🛒", "My cart", `${data.cartCount} item(s) ready`],
              ["/customer/orders", "📦", "Track orders", `${data.activeOrders} active`],
            ].map(([href, icon, title, desc]) => (
              <Link
                key={href as string}
                href={href as string}
                className="flex items-center gap-3 rounded-xl border border-line p-3 hover:border-brand/30 hover:bg-surface"
              >
                <span className="text-xl">{icon}</span>
                <div>
                  <p className="text-sm font-semibold text-ink">{title}</p>
                  <p className="text-xs text-muted">{desc}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}