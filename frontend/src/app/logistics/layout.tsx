import { AppShell } from "@/components/AppShell";
import { RoleGate } from "@/components/ui";

export default function LogisticsLayout({ children }: { children: React.ReactNode }) {
  return (
    <RoleGate roles={["LOGISTICS"]}>
      <AppShell role="LOGISTICS">{children}</AppShell>
    </RoleGate>
  );
}