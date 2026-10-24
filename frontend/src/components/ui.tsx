"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { roleLabel, roleHome, type Role } from "@/lib/types";

const STATUS_STYLES: Record<string, string> = {
  PENDING: "bg-amber-50 text-amber-700 border-amber-200",
  ACCEPTED: "bg-sky-50 text-sky-700 border-sky-200",
  PROCESSING: "bg-sky-50 text-sky-700 border-sky-200",
  CONFIRMED: "bg-sky-50 text-sky-700 border-sky-200",
  PACKED: "bg-indigo-50 text-indigo-700 border-indigo-200",
  DISPATCHED: "bg-indigo-50 text-indigo-700 border-indigo-200",
  ASSIGNED: "bg-sky-50 text-sky-700 border-sky-200",
  PICKUP_SCHEDULED: "bg-indigo-50 text-indigo-700 border-indigo-200",
  PICKED_UP: "bg-indigo-50 text-indigo-700 border-indigo-200",
  IN_TRANSIT: "bg-violet-50 text-violet-700 border-violet-200",
  SHIPPED: "bg-indigo-50 text-indigo-700 border-indigo-200",
  DELIVERED: "bg-green-50 text-green-700 border-green-200",
  COMPLETED: "bg-green-50 text-green-700 border-green-200",
  ACTIVE: "bg-green-50 text-green-700 border-green-200",
  AVAILABLE: "bg-green-50 text-green-700 border-green-200",
  SOLD: "bg-stone-100 text-stone-600 border-stone-200",
  EXPIRED: "bg-stone-100 text-stone-600 border-stone-200",
  PAUSED: "bg-amber-50 text-amber-700 border-amber-200",
  BUSY: "bg-amber-50 text-amber-700 border-amber-200",
  OFFLINE: "bg-stone-100 text-stone-600 border-stone-200",
  REJECTED: "bg-red-50 text-red-700 border-red-200",
  CANCELLED: "bg-red-50 text-red-700 border-red-200",
};

export function StatusBadge({ status }: { status: string | null | undefined }) {
  const key = (status ?? "").toUpperCase();
  const cls = STATUS_STYLES[key] ?? "bg-stone-100 text-stone-600 border-stone-200";
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${cls}`}
    >
      {(status ?? "—").replace(/_/g, " ")}
    </span>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-muted">
      <span className="h-8 w-8 animate-spin rounded-full border-[3px] border-brand border-t-transparent" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  );
}

export function EmptyState({ icon, title, hint }: { icon?: string; title: string; hint?: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-2xl border border-dashed border-line bg-white px-6 py-14 text-center">
      <span className="text-4xl">{icon ?? "🌾"}</span>
      <p className="font-semibold text-ink">{title}</p>
      {hint && <p className="max-w-sm text-sm text-muted">{hint}</p>}
    </div>
  );
}

export function StatCard({
  label,
  value,
  sub,
  accent = "brand",
}: {
  label: string;
  value: string | number;
  sub?: string;
  accent?: "brand" | "yellow" | "sky" | "amber" | "violet";
}) {
  const colors: Record<string, string> = {
    brand: "bg-brand-light text-brand-dark",
    yellow: "bg-accent-light text-amber-800",
    sky: "bg-sky-50 text-sky-700",
    amber: "bg-amber-50 text-amber-700",
    violet: "bg-violet-50 text-violet-700",
  };
  return (
    <div className="ks-shadow rounded-2xl border border-line bg-white p-4">
      <p className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</p>
      <p className="mt-2 text-2xl font-bold text-ink">{value}</p>
      {sub && <p className="mt-1 text-xs text-muted">{sub}</p>}
      <span className={`mt-3 block h-1 w-10 rounded-full ${colors[accent]}`} />
    </div>
  );
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: React.ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold text-ink">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function AppLink({ href, children, variant = "primary" }: { href: string; children: React.ReactNode; variant?: "primary" | "ghost" }) {
  const cls =
    variant === "primary"
      ? "rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark transition-colors"
      : "rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-surface transition-colors";
  return (
    <Link href={href} className={`${cls} inline-flex items-center gap-2`}>
      {children}
    </Link>
  );
}

export function RoleGate({ roles, children }: { roles: Role[]; children: React.ReactNode }) {
  const { user, loading, ready } = useAuth();
  const router = useRouter();

  // Debug logging
  useEffect(() => {
    console.log("[ROLE GATE] State:", { loading, ready, hasUser: !!user, roles, userRoles: user?.roles });
  }, [loading, ready, user, roles]);

  // Handle navigation in useEffect to prevent React error
  useEffect(() => {
    console.log("[ROLE GATE] Checking redirect conditions:", { ready, loading, hasUser: !!user });
    if (!ready || loading) {
      console.log("[ROLE GATE] Skipping redirect - not ready or loading");
      return;
    }

    // CRITICAL: Check authentication BEFORE checking role
    // If no user exists, the user is unauthenticated - redirect to /auth
    if (!user) {
      console.log("[ROLE GATE] REDIRECTING to /auth (no user)");
      router.replace("/auth");
      return;
    }

    const userRoles = user.roles ?? [];

    // If user has no roles at all, redirect to auth (this shouldn't happen normally)
    if (userRoles.length === 0) {
      console.log("[ROLE GATE] REDIRECTING to /auth (no roles)");
      router.replace("/auth");
      return;
    }

    console.log("[ROLE GATE] User authenticated with roles:", userRoles);
  }, [user, loading, ready, router]);

  // Show loading state while auth is initializing
  if (!ready || loading) {
    return (
      <div className="min-h-screen bg-surface">
        <Spinner label="Loading..." />
      </div>
    );
  }

  // CRITICAL: Check authentication BEFORE checking role
  // If no user exists, the user is unauthenticated - redirect to /auth
  if (!user) {
    return null;
  }

  const userRoles = user.roles ?? [];

  // If user has no roles at all, redirect to auth (this shouldn't happen normally)
  if (userRoles.length === 0) {
    return null;
  }

  // Only check role AFTER authentication is confirmed
  const has = userRoles.some((r) => roles.includes(((r ?? "").toUpperCase()) as Role));
  if (!has) {
    const firstRole = userRoles[0] ?? "";
    return (
      <div className="rounded-2xl border border-line bg-white p-8 text-center">
        <p className="font-semibold">You do not have access to this section.</p>
        <p className="mt-1 text-sm text-muted">
          Your role is {roleLabel(firstRole)}.{" "}
          <Link className="text-brand hover:underline" href={roleHome(firstRole)}>
            Go to your dashboard
          </Link>
        </p>
      </div>
    );
  }
  return <>{children}</>;
}