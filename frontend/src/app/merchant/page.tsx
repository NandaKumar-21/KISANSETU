"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, StatCard } from "@/components/ui";
import { formatINR } from "@/lib/format";

type Dashboard = {
  totalProducts: number;
  totalStock: number;
  lowStockItems: number;
  stockValue: number;
  orderCounts: Record<string, number>;
  unreadNotifications: number;
};

export default function MerchantDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Dashboard>("/api/merchant/dashboard")
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;
  if (!data) return <Spinner label="Loading dashboard..." />;

  return (
    <div>
      <PageHeader
        title="Merchant Dashboard"
        subtitle="Your shop at a glance"
        action={
          <Link
            href="/merchant/products"
            className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            + New Product
          </Link>
        }
      />

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Products" value={data.totalProducts} accent="brand" />
        <StatCard label="Total Stock" value={data.totalStock} accent="sky" />
        <StatCard label="Low Stock" value={data.lowStockItems} accent="amber" />
        <StatCard label="Stock Value" value={formatINR(data.stockValue)} accent="yellow" />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-ink">Order status</h3>
            <Link href="/merchant/orders" className="text-sm font-medium text-brand hover:underline">
              View all
            </Link>
          </div>
          <div className="mt-3 space-y-2">
            {Object.entries(data.orderCounts).map(([status, count]) => (
              <div key={status} className="flex items-center justify-between rounded-xl bg-surface px-3 py-2.5">
                <span className="text-sm font-medium capitalize text-ink">{status.replace("_", " ")}</span>
                <span className="rounded-full bg-brand-light px-2.5 py-0.5 text-xs font-bold text-brand-dark">{count}</span>
              </div>
            ))}
            {Object.keys(data.orderCounts).length === 0 && (
              <p className="text-sm text-muted">No orders yet.</p>
            )}
          </div>
        </div>

        <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
          <h3 className="font-bold text-ink">Quick actions</h3>
          <div className="mt-3 grid gap-2">
            {[
              ["/merchant/products", "🧾", "Manage products", "Add stock, update prices"],
              ["/merchant/orders", "📦", "Process orders", `${data.orderCounts.pending ?? 0} pending`],
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

      {data.lowStockItems > 0 && (
        <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800">
          ⚠️ {data.lowStockItems} product(s) are running low on stock. Restock soon!
        </div>
      )}
    </div>
  );
}