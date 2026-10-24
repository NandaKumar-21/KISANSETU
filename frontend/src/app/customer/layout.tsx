import { AppShell } from "@/components/AppShell";
import { RoleGate } from "@/components/ui";

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  return (
    <RoleGate roles={["CUSTOMER"]}>
      <AppShell role="CUSTOMER">{children}</AppShell>
    </RoleGate>
  );
}