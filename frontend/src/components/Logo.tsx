import Link from "next/link";

export function Logo({ size = "md" }: { size?: "sm" | "md" | "lg" }) {
  const cls =
    size === "lg" ? "h-12 w-12 text-2xl" : size === "sm" ? "h-8 w-8 text-base" : "h-10 w-10 text-xl";
  const text = size === "lg" ? "text-3xl" : size === "sm" ? "text-lg" : "text-2xl";
  return (
    <Link href="/" className="flex items-center gap-2.5">
      <span
        className={`${cls} flex items-center justify-center rounded-xl bg-brand text-white font-bold`}
      >
        KS
      </span>
      <span className={`${text} font-bold tracking-tight`}>
        Kisan<span className="text-brand">Setu</span>
      </span>
    </Link>
  );
}