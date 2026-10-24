"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { roleLabel, type Role } from "@/lib/types";
import { Logo } from "./Logo";
import { Spinner } from "./ui";

type NavItem = { href: string; label: string; icon: string };

const NAV: Record<string, NavItem[]> = {
  FARMER: [
    { href: "/farmer", label: "Dashboard", icon: "📊" },
    { href: "/farmer/listings", label: "My Listings", icon: "🧺" },
    { href: "/farmer/marketplace", label: "Marketplace", icon: "🏪" },
    { href: "/farmer/customer-orders", label: "Customer Orders", icon: "📦" },
    { href: "/farmer/merchant-orders", label: "Input Orders", icon: "🌱" },
    { href: "/farmer/crop-planner", label: "Crop Planner", icon: "🧭" },
    { href: "/farmer/disease-detection", label: "Disease Detection", icon: "🔬" },
    { href: "/farmer/calendar", label: "Calendar", icon: "📅" },
    { href: "/farmer/ai-chat", label: "Kisan Assistant", icon: "💬" },
    { href: "/farmer/profile", label: "Profile", icon: "👤" },
  ],
  MERCHANT: [
    { href: "/merchant", label: "Dashboard", icon: "📊" },
    { href: "/merchant/products", label: "Products", icon: "🧾" },
    { href: "/merchant/orders", label: "Orders", icon: "📦" },
    { href: "/merchant/profile", label: "Profile", icon: "👤" },
  ],
  CUSTOMER: [
    { href: "/customer", label: "Dashboard", icon: "📊" },
    { href: "/customer/marketplace", label: "Marketplace", icon: "🏪" },
    { href: "/customer/cart", label: "Cart", icon: "🛒" },
    { href: "/customer/orders", label: "My Orders", icon: "📦" },
    { href: "/customer/wishlist", label: "Wishlist", icon: "❤️" },
    { href: "/customer/compare", label: "Price Compare", icon: "⚖️" },
    { href: "/customer/profile", label: "Profile", icon: "👤" },
  ],
  LOGISTICS: [
    { href: "/logistics", label: "Dashboard", icon: "📊" },
    { href: "/logistics/orders", label: "Assigned Orders", icon: "📦" },
    { href: "/logistics/deliveries", label: "Deliveries", icon: "🚚" },
    { href: "/logistics/routes", label: "Routes", icon: "🗺️" },
    { href: "/logistics/earnings", label: "Earnings", icon: "💰" },
    { href: "/logistics/profile", label: "Profile", icon: "👤" },
  ],
};

function ShellInner({ children, role }: { children: React.ReactNode; role: Role }) {
  const { user, signOut, ready, loading } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  // Debug logging
  useEffect(() => {
    console.log("[APPSHELL] State:", { loading, ready, hasUser: !!user, pathname, role });
  }, [loading, ready, user, pathname, role]);

  // Handle navigation in useEffect to prevent React error
  useEffect(() => {
    console.log("[APPSHELL] Checking redirect conditions:", { ready, loading, hasUser: !!user, pathname });
    if (!ready || loading) {
      console.log("[APPSHELL] Skipping redirect - not ready or loading");
      return;
    }

    // CRITICAL: Check authentication BEFORE any other logic
    // If no user exists, the user is unauthenticated - redirect to /auth
    if (!user) {
      console.log("[APPSHELL] REDIRECTING to /auth (no user)");
      router.replace("/auth");
    }
  }, [user, ready, loading, router, pathname]);

  // Show loading state while auth is initializing
  if (!ready || loading) {
    return (
      <div className="min-h-screen bg-surface">
        <Spinner label="Loading..." />
      </div>
    );
  }

  // CRITICAL: Check authentication BEFORE any other logic
  // If no user exists, the user is unauthenticated - redirect to /auth
  if (!user) {
    return null;
  }

  const items = NAV[role] ?? NAV.FARMER;

  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="fixed inset-y-0 left-0 z-30 flex w-60 flex-col border-r border-line bg-white">
        <div className="flex h-16 items-center border-b border-line px-5">
          <Logo size="sm" />
        </div>
        <nav className="flex-1 overflow-y-auto p-3 no-scrollbar">
          {items.map((item) => {
            const active =
              item.href === `/${role.toLowerCase()}`
                ? pathname === item.href
                : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`mb-1 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
                  active ? "bg-brand-light text-brand-dark" : "text-muted hover:bg-surface hover:text-ink"
                }`}
              >
                <span className="text-base">{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-line p-4">
          <div className="mb-3 flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand text-sm font-bold text-white">
              {(user.profile?.fullName ?? "U").charAt(0)}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">
                {user.profile?.fullName ?? user.email}
              </p>
              <p className="text-xs text-muted">{roleLabel(role)}</p>
            </div>
          </div>
          <button
            onClick={async () => {
              await signOut();
              router.push("/auth");
            }}
            className="w-full rounded-xl border border-line px-3 py-2 text-sm font-medium text-muted hover:bg-surface hover:text-ink"
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className="ml-60 flex-1 px-6 py-6">{children}</main>
    </div>
  );
}

export function AppShell({ children, role }: { children: React.ReactNode; role: Role }) {
  return <ShellInner role={role}>{children}</ShellInner>;
}