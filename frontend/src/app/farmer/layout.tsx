import { AppShell } from "@/components/AppShell";
import { RoleGate } from "@/components/ui";

export default function FarmerLayout({ children }: { children: React.ReactNode }) {
  return (
    <RoleGate roles={["FARMER"]}>
      <AppShell role="FARMER">{children}</AppShell>
    </RoleGate>
  );
}