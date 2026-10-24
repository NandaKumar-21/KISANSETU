import { AppShell } from "@/components/AppShell";
import { RoleGate } from "@/components/ui";

export default function MerchantLayout({ children }: { children: React.ReactNode }) {
  return (
    <RoleGate roles={["MERCHANT"]}>
      <AppShell role="MERCHANT">{children}</AppShell>
    </RoleGate>
  );
}