"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatCard } from "@/components/ui";
import { formatINR, formatDateTime } from "@/lib/format";

type HistoryItem = {
  orderId: string;
  amount: number;
  deliveredAt: string;
  orderValue: number;
};

type Earnings = {
  today: number;
  week: number;
  month: number;
  todayDeliveries: number;
  weekDeliveries: number;
  monthDeliveries: number;
  earningPerDelivery: string;
  history: HistoryItem[];
};

export default function LogisticsEarningsPage() {
  const [data, setData] = useState<Earnings | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Earnings>("/api/logistics/earnings")
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;

  return (
    <div>
      <PageHeader title="Earnings" subtitle="You earn 5% of every delivered order value" />
      {!data ? (
        <Spinner />
      ) : (
        <>
          <div className="grid grid-cols-3 gap-4">
            <StatCard label="Today" value={formatINR(data.today)} accent="brand" />
            <StatCard label="This Week" value={formatINR(data.week)} accent="sky" />
            <StatCard label="This Month" value={formatINR(data.month)} accent="amber" />
          </div>
          <p className="mt-3 text-xs text-muted">
            {data.todayDeliveries} delivered today · {data.weekDeliveries} this week · {data.monthDeliveries} this month
          </p>

          <div className="ks-shadow mt-6 rounded-2xl border border-line bg-white p-5">
            <h3 className="font-bold text-ink">Earnings history</h3>
            {data.history.length === 0 ? (
              <EmptyState icon="💰" title="No earnings yet" hint="Deliveries you complete will appear here." />
            ) : (
              <div className="mt-3 space-y-2">
                {data.history.map((h) => (
                  <div key={h.orderId} className="flex items-center justify-between rounded-xl bg-surface px-3 py-2.5">
                    <div>
                      <p className="text-sm font-semibold text-ink">Order {h.orderId.slice(0, 8)}</p>
                      <p className="text-xs text-muted">Order value {formatINR(h.orderValue)} · {formatDateTime(h.deliveredAt)}</p>
                    </div>
                    <p className="text-sm font-bold text-brand-dark">+{formatINR(h.amount)}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}