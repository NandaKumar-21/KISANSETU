"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { PageHeader, Spinner, StatCard } from "@/components/ui";
import { formatINR } from "@/lib/format";

type Dashboard = {
  assignedDeliveries: number;
  activeDeliveries: number;
  completedToday: number;
  earningsToday: number;
  completionRate: number;
  totalDeliveries: number;
  completed: number;
  inProgress: number;
  inTransit: number;
};

export default function LogisticsDashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [availability, setAvailability] = useState("offline");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Dashboard>("/api/logistics/dashboard")
      .then(setData)
      .catch((e) => setError(e.message));
    api
      .get<{ status: string }>("/api/logistics/availability")
      .then((r) => setAvailability(r.status))
      .catch(() => undefined);
  }, []);

  async function setStatus(status: string) {
    setAvailability(status);
    try {
      await api.put(`/api/logistics/availability?status=${status}`);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to update status");
      setAvailability("offline");
    }
  }

  if (error) return <p className="text-red-600">{error}</p>;
  if (!data) return <Spinner label="Loading dashboard..." />;

  const statusStyles: Record<string, string> = {
    available: "bg-emerald-500 text-white",
    busy: "bg-amber-500 text-white",
    offline: "bg-gray-300 text-gray-700",
  };

  return (
    <div>
      <PageHeader
        title="Delivery Dashboard"
        subtitle="Your deliveries at a glance"
        action={
          <div className="flex items-center gap-1 rounded-xl border border-line bg-white p-1">
            {["available", "busy", "offline"].map((s) => (
              <button
                key={s}
                onClick={() => setStatus(s)}
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold capitalize ${
                  availability === s ? statusStyles[s] : "text-muted hover:bg-surface"
                }`}
              >
                {s}
              </button>
            ))}
          </div>
        }
      />

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <StatCard label="Assigned" value={data.assignedDeliveries} accent="sky" />
        <StatCard label="Active" value={data.activeDeliveries} accent="brand" />
        <StatCard label="Completed Today" value={data.completedToday} accent="yellow" />
        <StatCard label="Earnings Today" value={formatINR(data.earningsToday)} accent="amber" />
      </div>

      <div className="ks-shadow mt-6 rounded-2xl border border-line bg-white p-5">
        <h3 className="font-bold text-ink">Performance</h3>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <div>
            <p className="text-xs font-semibold text-muted uppercase">Completion rate</p>
            <p className="mt-1 text-2xl font-bold text-brand-dark">{data.completionRate}%</p>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-surface">
              <div className="h-full rounded-full bg-brand" style={{ width: `${data.completionRate}%` }} />
            </div>
          </div>
          <div>
            <p className="text-xs font-semibold text-muted uppercase">Total deliveries</p>
            <p className="mt-1 text-2xl font-bold text-ink">{data.totalDeliveries}</p>
            <p className="text-xs text-muted">{data.completed} completed</p>
          </div>
          <div>
            <p className="text-xs font-semibold text-muted uppercase">In transit</p>
            <p className="mt-1 text-2xl font-bold text-ink">{data.inTransit}</p>
            <p className="text-xs text-muted">{data.inProgress} in progress</p>
          </div>
        </div>
      </div>

      <div className="ks-shadow mt-4 rounded-2xl border border-line bg-white p-5">
        <h3 className="font-bold text-ink">Quick actions</h3>
        <div className="mt-3 grid gap-2 sm:grid-cols-3">
          {[
            ["/logistics/deliveries", "🚚", "Active deliveries", `${data.activeDeliveries} in progress`],
            ["/logistics/routes", "🗺️", "View route", "Optimized stop order"],
            ["/logistics/earnings", "💰", "Earnings", "5% of order value"],
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
  );
}