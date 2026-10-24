"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState } from "@/components/ui";
import { formatDate } from "@/lib/format";

type CalendarEvent = {
  id: string;
  title: string;
  description?: string;
  eventType: string;
  eventDate: string;
  reminderEnabled: boolean;
  completed: boolean;
  cropType?: string;
  weatherDependent?: boolean;
  suggestedByAi?: boolean;
};

const EVENT_TYPES = ["planting", "irrigation", "fertilizing", "harvesting", "pest_control", "other"];

export default function FarmerCalendarPage() {
  const [events, setEvents] = useState<CalendarEvent[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    title: "",
    description: "",
    eventType: "planting",
    eventDate: new Date().toISOString().slice(0, 10),
    cropType: "",
    reminderEnabled: true,
  });

  const load = useCallback(() => {
    api
      .get<CalendarEvent[]>("/api/farmer/calendar")
      .then(setEvents)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    try {
      await api.post("/api/farmer/calendar", {
        ...form,
        description: form.description || null,
        cropType: form.cropType || null,
      });
      setShowForm(false);
      setForm({ title: "", description: "", eventType: "planting", eventDate: new Date().toISOString().slice(0, 10), cropType: "", reminderEnabled: true });
      load();
    } catch (err) {
      alert(err instanceof Error ? err.message : "Failed");
    }
  }

  async function toggle(id: string) {
    await api.patch(`/api/farmer/calendar/${id}/toggle`);
    load();
  }

  async function remove(id: string) {
    if (!confirm("Delete this event?")) return;
    await api.del(`/api/farmer/calendar/${id}`);
    load();
  }

  if (error) return <p className="text-red-600">{error}</p>;

  const input =
    "w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none";

  return (
    <div>
      <PageHeader
        title="Farm Calendar"
        subtitle="Plan sowing, irrigation, fertilizer and harvest"
        action={
          <button
            onClick={() => setShowForm((s) => !s)}
            className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            + Add event
          </button>
        }
      />

      {showForm && (
        <form onSubmit={create} className="ks-shadow mb-6 grid gap-3 rounded-2xl border border-line bg-white p-5 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Title *</label>
            <input className={input} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g. Transplant tomato seedlings" required />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Type</label>
            <select className={input} value={form.eventType} onChange={(e) => setForm({ ...form, eventType: e.target.value })}>
              {EVENT_TYPES.map((t) => (
                <option key={t} value={t} className="capitalize">
                  {t.replace("_", " ")}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Date</label>
            <input className={input} type="date" value={form.eventDate} onChange={(e) => setForm({ ...form, eventDate: e.target.value })} required />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Crop</label>
            <input className={input} value={form.cropType} onChange={(e) => setForm({ ...form, cropType: e.target.value })} placeholder="e.g. Tomato" />
          </div>
          <div className="flex items-end">
            <label className="flex items-center gap-2 pb-2 text-sm font-medium text-ink">
              <input type="checkbox" checked={form.reminderEnabled} onChange={(e) => setForm({ ...form, reminderEnabled: e.target.checked })} className="h-4 w-4 accent-brand" />
              Remind me
            </label>
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-semibold text-muted uppercase">Notes</label>
            <textarea className={`${input} min-h-16`} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="flex gap-2 sm:col-span-2">
            <button type="submit" className="flex-1 rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark">
              Add to calendar
            </button>
          </div>
        </form>
      )}

      {!events ? (
        <Spinner />
      ) : events.length === 0 ? (
        <EmptyState icon="📅" title="No farm events yet" hint="Add planting, irrigation and harvest reminders." />
      ) : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {events.map((ev) => (
            <div
              key={ev.id}
              className={`ks-shadow rounded-2xl border bg-white p-4 ${
                ev.completed ? "border-line opacity-60" : "border-line"
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-ink">{ev.title}</p>
                  <p className="text-xs text-muted capitalize">
                    {ev.eventType.replace("_", " ")}
                    {ev.cropType ? ` · ${ev.cropType}` : ""}
                    {ev.suggestedByAi ? " · 🤖 AI" : ""}
                  </p>
                </div>
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold ${ev.completed ? "bg-green-50 text-green-700" : "bg-brand-light text-brand-dark"}`}>
                  {formatDate(ev.eventDate)}
                </span>
              </div>
              {ev.description && <p className="mt-2 text-sm text-muted">{ev.description}</p>}
              <div className="mt-3 flex items-center gap-2 border-t border-line pt-3">
                <button
                  onClick={() => toggle(ev.id)}
                  className="flex-1 rounded-lg bg-brand-light px-3 py-1.5 text-xs font-semibold text-brand-dark"
                >
                  {ev.completed ? "Mark pending" : "Mark done"}
                </button>
                <button
                  onClick={() => toggle(ev.id)}
                  className="rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-muted"
                >
                  🔔 {ev.reminderEnabled ? "On" : "Off"}
                </button>
                <button onClick={() => remove(ev.id)} className="rounded-lg border border-red-100 px-3 py-1.5 text-xs font-semibold text-red-600">
                  ✕
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}