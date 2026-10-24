import Link from "next/link";
import { Logo } from "@/components/Logo";

const ROLES = [
  {
    key: "farmer",
    title: "Farmers",
    icon: "🧑‍🌾",
    points: ["Sell produce on the marketplace", "Buy quality inputs from merchants", "AI crop planning & disease detection"],
  },
  {
    key: "merchant",
    title: "Merchants",
    icon: "🏬",
    points: ["List seeds, fertilizers & equipment", "Receive & manage farmer orders", "Track inventory and sales"],
  },
  {
    key: "customer",
    title: "Customers",
    icon: "🛍️",
    points: ["Buy fresh produce directly from farms", "Compare prices across farmers", "Track orders with live delivery updates"],
  },
  {
    key: "logistics",
    title: "Logistics Partners",
    icon: "🚚",
    points: ["Get matched to nearby orders", "Manage pickup-to-delivery pipeline", "Earn 5% commission per delivery"],
  },
];

export default function HomePage() {
  return (
    <div className="min-h-screen bg-white">
      <header className="border-b border-line bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <Logo />
          <nav className="flex items-center gap-2">
            <Link
              href="/auth"
              className="rounded-xl px-4 py-2 text-sm font-semibold text-ink hover:bg-surface"
            >
              Sign in
            </Link>
            <Link
              href="/auth?tab=signup"
              className="rounded-xl bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark"
            >
              Get started
            </Link>
          </nav>
        </div>
      </header>

      <section className="bg-brand-light">
        <div className="mx-auto max-w-6xl px-4 py-20 text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-brand/20 bg-white px-4 py-1.5 text-sm font-medium text-brand-dark">
            🚜 Direct farm-to-table marketplace
          </span>
          <h1 className="mx-auto mt-6 max-w-3xl text-4xl font-bold leading-tight text-ink md:text-5xl">
            Fresh produce, fair prices —{" "}
            <span className="text-brand">from farm to your doorstep</span>
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-lg text-muted">
            KisanSetu connects farmers, merchants, customers and delivery partners on one
            platform — with AI crop advice, price comparison and live order tracking.
          </p>
          <div className="mt-8 flex justify-center gap-3">
            <Link
              href="/auth?tab=signup"
              className="rounded-xl bg-brand px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-brand/20 hover:bg-brand-dark"
            >
              Join KisanSetu
            </Link>
            <Link
              href="/auth"
              className="rounded-xl border border-line bg-white px-6 py-3 text-sm font-semibold text-ink hover:bg-surface"
            >
              Sign in
            </Link>
          </div>
          <div className="mt-14 grid grid-cols-2 gap-3 md:grid-cols-4">
            {[
              ["250+", "Farmer listings"],
              ["40+", "Partner merchants"],
              ["5,000+", "Happy customers"],
              ["3,000+", "Deliveries done"],
            ].map(([num, label]) => (
              <div key={label} className="rounded-2xl border border-brand/10 bg-white p-4 ks-shadow">
                <p className="text-2xl font-bold text-brand-dark">{num}</p>
                <p className="text-sm text-muted">{label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16">
        <h2 className="text-center text-3xl font-bold text-ink">One platform, four communities</h2>
        <p className="mx-auto mt-2 max-w-xl text-center text-muted">
          Every role gets a dedicated workspace tailored to their daily work.
        </p>
        <div className="mt-10 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {ROLES.map((role) => (
            <div
              key={role.key}
              className="ks-shadow rounded-2xl border border-line bg-white p-5 hover:border-brand/30"
            >
              <span className="text-3xl">{role.icon}</span>
              <h3 className="mt-3 text-lg font-bold text-ink">{role.title}</h3>
              <ul className="mt-3 space-y-2">
                {role.points.map((p) => (
                  <li key={p} className="flex gap-2 text-sm text-muted">
                    <span className="text-brand">✓</span>
                    {p}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-accent-light">
        <div className="mx-auto max-w-6xl px-4 py-16">
          <h2 className="text-center text-3xl font-bold text-ink">
            Smart features for Indian agriculture
          </h2>
          <div className="mt-10 grid gap-4 md:grid-cols-3">
            {[
              ["🧭", "AI Crop Planner", "Get crop recommendations tuned to your soil, water and budget."],
              ["🔬", "Disease Detection", "Upload a leaf photo or describe symptoms for instant diagnosis."],
              ["🗺️", "Smart Delivery", "Orders are auto-matched to nearby logistics partners."],
              ["⚖️", "Price Compare", "Compare produce prices across farmers before you buy."],
              ["📅", "Farm Calendar", "Never miss sowing, irrigation or harvest with reminders."],
              ["💬", "Kisan Assistant", "Ask anything about farming in natural language."],
            ].map(([icon, title, desc]) => (
              <div key={title} className="ks-shadow rounded-2xl border border-line bg-white p-5">
                <span className="text-2xl">{icon}</span>
                <h3 className="mt-2 font-bold text-ink">{title}</h3>
                <p className="mt-1 text-sm text-muted">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <footer className="border-t border-line bg-white">
        <div className="mx-auto flex max-w-6xl flex-col items-center gap-2 px-4 py-8 text-center">
          <Logo size="sm" />
          <p className="text-sm text-muted">© {new Date().getFullYear()} KisanSetu. Bridging farms to families.</p>
        </div>
      </footer>
    </div>
  );
}