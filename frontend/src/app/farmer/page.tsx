"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, StatCard, EmptyState } from "@/components/ui";
import { formatINR, formatDate } from "@/lib/format";

type Dashboard = {
  activeListings: number;
  myListings: number;
  pendingCustomerOrders: number;
  totalCustomerOrders: number;
  unreadNotifications: number;
  upcomingCalendar: Array<{
    id: string;
    title: string;
    eventType: string;
    eventDate: string;
  }>;
  weather?: {
    temperature?: number;
    condition?: string;
    humidity?: number;
    windSpeed?: number;
    city?: string;
  };
};

export default function FarmerDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Dashboard>("/api/farmer/dashboard")
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;
  if (!data) return <Spinner label="Loading dashboard..." />;

  return (
    <div>
      <PageHeader
        title="Farmer Dashboard"
        subtitle="Your farm at a glance"
        action={
          <Link
            href="/farmer/listings"
            className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            + New Listing
          </Link>
        }
      />

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Active Listings" value={data.activeListings} accent="brand" />
        <StatCard label="Total Listings" value={data.myListings} accent="yellow" />
        <StatCard label="Pending Orders" value={data.pendingCustomerOrders} accent="amber" />
        <StatCard label="Total Orders" value={data.totalCustomerOrders} accent="sky" />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        {data.weather && (
          <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
            <h3 className="font-bold text-ink">Weather</h3>
            <p className="text-sm text-muted">{data.weather.city ?? "Your region"}</p>
            <div className="mt-4 flex items-center gap-6">
              <span className="text-5xl font-bold text-brand-dark">
                {data.weather.temperature != null ? `${Math.round(data.weather.temperature)}°C` : "—"}
              </span>
              <div className="text-sm text-muted">
                <p className="capitalize">{data.weather.condition ?? "—"}</p>
                {data.weather.humidity != null && <p>Humidity {data.weather.humidity}%</p>}
                {data.weather.windSpeed != null && (
                  <p>Wind {data.weather.windSpeed} km/h</p>
                )}
              </div>
            </div>
          </div>
        )}

        <div className="ks-shadow rounded-2xl border border-line bg-white p-5">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-ink">Upcoming Calendar</h3>
            <Link href="/farmer/calendar" className="text-sm font-medium text-brand hover:underline">
              View all
            </Link>
          </div>
          <div className="mt-3 space-y-2">
            {data.upcomingCalendar.length === 0 ? (
              <p className="text-sm text-muted">No upcoming events.</p>
            ) : (
              data.upcomingCalendar.slice(0, 4).map((ev) => (
                <div key={ev.id} className="flex items-center justify-between rounded-xl bg-surface px-3 py-2.5">
                  <div>
                    <p className="text-sm font-medium text-ink">{ev.title}</p>
                    <p className="text-xs text-muted capitalize">{ev.eventType}</p>
                  </div>
                  <span className="text-xs font-semibold text-brand-dark">{formatDate(ev.eventDate)}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <QuickCard href="/farmer/marketplace" icon="🏪" title="Marketplace" desc="Browse and order from other farmers" />
        <QuickCard href="/farmer/crop-planner" icon="🧭" title="Crop Planner" desc="Get AI crop recommendations" />
        <QuickCard href="/farmer/disease-detection" icon="🔬" title="Disease Detection" desc="Diagnose crop issues with AI" />
        <QuickCard href="/farmer/customer-orders" icon="📦" title="Customer Orders" desc={`${data.pendingCustomerOrders} awaiting action`} />
        <QuickCard href="/farmer/merchant-orders" icon="🌱" title="Input Orders" desc="Buy seeds, fertilizers and more" />
        <QuickCard href="/farmer/ai-chat" icon="💬" title="Kisan Assistant" desc="Ask anything about farming" />
      </div>

      {data.unreadNotifications > 0 && (
        <div className="mt-6 rounded-2xl border border-accent/40 bg-accent-light px-4 py-3 text-sm font-medium text-amber-800">
          🔔 You have {data.unreadNotifications} unread notification(s).
        </div>
      )}
    </div>
  );
}

function QuickCard({ href, icon, title, desc }: { href: string; icon: string; title: string; desc: string }) {
  return (
    <Link href={href} className="ks-shadow rounded-2xl border border-line bg-white p-5 hover:border-brand/30">
      <span className="text-2xl">{icon}</span>
      <h3 className="mt-2 font-bold text-ink">{title}</h3>
      <p className="mt-1 text-sm text-muted">{desc}</p>
    </Link>
  );
}