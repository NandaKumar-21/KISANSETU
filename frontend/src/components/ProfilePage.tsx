"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { roleLabel } from "@/lib/types";
import { PageHeader, Spinner } from "@/components/ui";

export function ProfilePage({ role }: { role: string }) {
  const { user } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [notifications, setNotifications] = useState<number>(0);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    fullName: "",
    phone: "",
    address: "",
    city: "",
    state: "",
    pincode: "",
    avatarUrl: "",
    latitude: null as number | null,
    longitude: null as number | null,
  });
  const [locationLoading, setLocationLoading] = useState(false);

  useEffect(() => {
    api
      .get<{ count: number }>("/api/notifications/unread-count")
      .then((r) => setNotifications(r.count))
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    if (user?.profile) {
      setFormData({
        fullName: user.profile.fullName || "",
        phone: user.profile.phone || "",
        address: user.profile.address || "",
        city: user.profile.city || "",
        state: user.profile.state || "",
        pincode: user.profile.pincode || "",
        avatarUrl: user.profile.avatarUrl || "",
        latitude: user.profile.latitude || null,
        longitude: user.profile.longitude || null,
      });
    }
  }, [user]);

  const getCurrentLocation = () => {
    setLocationLoading(true);
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setFormData(prev => ({
            ...prev,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          }));
          setLocationLoading(false);
        },
        (error) => {
          console.warn('Geolocation error:', error);
          setLocationLoading(false);
          alert('Could not get your location. Please enable location services.');
        }
      );
    } else {
      setLocationLoading(false);
      alert('Geolocation is not supported by your browser.');
    }
  };

  const handleSave = async () => {
    try {
      await api.put("/api/profiles/me", {
        fullName: formData.fullName,
        phone: formData.phone,
        address: formData.address,
        city: formData.city,
        state: formData.state,
        pincode: formData.pincode,
        avatarUrl: formData.avatarUrl,
        latitude: formData.latitude,
        longitude: formData.longitude,
      });
      setIsEditing(false);
      alert("Profile updated successfully!");
      // Reload the page to refresh data
      window.location.reload();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to update profile");
    }
  };

  if (error) return <p className="text-red-600">{error}</p>;
  if (!user) return <Spinner label="Loading profile..." />;

  const p = user.profile;

  if (isEditing) {
    return (
      <div className="mx-auto max-w-2xl">
        <PageHeader title="Edit Profile" subtitle={`${roleLabel(role)} account`} />
        <div className="ks-shadow rounded-2xl border border-line bg-white p-6">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-ink mb-1">Full Name</label>
              <input
                type="text"
                value={formData.fullName}
                onChange={(e) => setFormData({...formData, fullName: e.target.value})}
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink mb-1">Phone</label>
              <input
                type="text"
                value={formData.phone}
                onChange={(e) => setFormData({...formData, phone: e.target.value})}
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink mb-1">Address</label>
              <input
                type="text"
                value={formData.address}
                onChange={(e) => setFormData({...formData, address: e.target.value})}
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-ink mb-1">City</label>
                <input
                  type="text"
                  value={formData.city}
                  onChange={(e) => setFormData({...formData, city: e.target.value})}
                  className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-ink mb-1">State</label>
                <input
                  type="text"
                  value={formData.state}
                  onChange={(e) => setFormData({...formData, state: e.target.value})}
                  className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-ink mb-1">Pincode</label>
              <input
                type="text"
                value={formData.pincode}
                onChange={(e) => setFormData({...formData, pincode: e.target.value})}
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink mb-1">Profile Photo URL</label>
              <input
                type="text"
                value={formData.avatarUrl}
                onChange={(e) => setFormData({...formData, avatarUrl: e.target.value})}
                placeholder="https://..."
                className="w-full rounded-lg border border-line px-3 py-2 text-sm focus:border-brand focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink mb-1">Location</label>
              <div className="flex gap-2">
                <div className="flex-1">
                  {formData.latitude !== null && formData.longitude !== null ? (
                    <p className="text-sm text-muted py-2">
                      📍 {formData.latitude.toFixed(4)}, {formData.longitude.toFixed(4)}
                    </p>
                  ) : (
                    <p className="text-sm text-muted py-2">No location set</p>
                  )}
                </div>
                <button
                  onClick={getCurrentLocation}
                  disabled={locationLoading}
                  className="rounded-lg bg-brand-light px-4 py-2 text-sm font-semibold text-brand-dark hover:bg-brand disabled:opacity-60"
                >
                  {locationLoading ? "Getting location..." : "Use my current location"}
                </button>
              </div>
            </div>
            <div className="flex gap-3 pt-4">
              <button
                onClick={handleSave}
                className="flex-1 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark"
              >
                Save Changes
              </button>
              <button
                onClick={() => setIsEditing(false)}
                className="rounded-lg border border-line px-4 py-2 text-sm font-semibold text-ink hover:bg-surface"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const rows: Array<[string, string | undefined]> = [
    ["Full name", p?.fullName],
    ["Email", user.email],
    ["Phone", p?.phone],
    ["Address", p?.address],
    ["City", p?.city],
    ["State", p?.state],
    ["Pincode", p?.pincode],
  ];

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader 
        title="My Profile" 
        subtitle={`${roleLabel(role)} account`}
        action={
          <button
            onClick={() => setIsEditing(true)}
            className="rounded-xl border border-line bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-surface"
          >
            Edit Profile
          </button>
        }
      />
      <div className="ks-shadow rounded-2xl border border-line bg-white p-6">
        <div className="flex items-center gap-4 border-b border-line pb-5">
          {p?.avatarUrl ? (
            <img 
              src={p.avatarUrl} 
              alt={p.fullName} 
              className="h-16 w-16 rounded-full object-cover border border-line"
            />
          ) : (
            <span className="flex h-16 w-16 items-center justify-center rounded-2xl bg-brand text-2xl font-bold text-white">
              {(p?.fullName ?? "U").charAt(0)}
            </span>
          )}
          <div>
            <p className="text-xl font-bold text-ink">{p?.fullName ?? "—"}</p>
            <p className="text-sm text-muted">{user.email}</p>
            <span className="mt-1 inline-block rounded-full bg-brand-light px-3 py-0.5 text-xs font-semibold text-brand-dark">
              {(user.roles ?? []).map((r) => roleLabel(r)).join(", ") || "User"}
            </span>
          </div>
        </div>
        <dl className="mt-5 grid gap-x-6 gap-y-4 sm:grid-cols-2">
          {rows.map(([label, value]) => (
            <div key={label}>
              <dt className="text-xs font-semibold text-muted uppercase">{label}</dt>
              <dd className="mt-0.5 text-sm font-medium text-ink">{value || "—"}</dd>
            </div>
          ))}
        </dl>
        {p?.latitude != null && p?.longitude != null && (
          <p className="mt-5 rounded-xl bg-surface px-3 py-2 text-xs text-muted">
            📍 Location: {p.latitude.toFixed(4)}, {p.longitude.toFixed(4)}
          </p>
        )}
        <p className="mt-3 text-xs text-muted">
          Profile details are managed by your account.
        </p>
      </div>
    </div>
  );
}