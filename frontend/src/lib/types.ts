export type Role = "FARMER" | "MERCHANT" | "CUSTOMER" | "LOGISTICS";

export type Profile = {
  userId: string;
  fullName: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  avatarUrl?: string;
  latitude?: number;
  longitude?: number;
  roles?: string[];
};

export type MeResponse = {
  id: string;
  email: string;
  roles?: string[];
  profile: Profile;
};

export function roleLabel(role: string | null | undefined): string {
  switch ((role ?? "").toUpperCase()) {
    case "FARMER":
      return "Farmer";
    case "MERCHANT":
      return "Merchant";
    case "CUSTOMER":
      return "Customer";
    case "LOGISTICS":
      return "Logistics Partner";
    default:
      return "User";
  }
}

export function roleHome(role: string | null | undefined): string {
  switch ((role ?? "").toUpperCase()) {
    case "FARMER":
      return "/farmer";
    case "MERCHANT":
      return "/merchant";
    case "CUSTOMER":
      return "/customer";
    case "LOGISTICS":
      return "/logistics";
    default:
      return "/";
  }
}