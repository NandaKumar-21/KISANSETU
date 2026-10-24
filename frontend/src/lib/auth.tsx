"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { createClient, type SupabaseClient } from "@supabase/supabase-js";
import { api, setToken } from "./api";
import type { MeResponse } from "./types";

export type AuthState = {
  supabase: SupabaseClient | null;
  user: MeResponse | null;
  loading: boolean;
  ready: boolean;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

// Singleton pattern to prevent multiple Supabase clients
let supabaseClient: SupabaseClient | null = null;

function makeClient(): SupabaseClient | null {
  if (supabaseClient) return supabaseClient;

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !key || url.includes("your-project")) return null;
  try {
    supabaseClient = createClient(url, key);
    return supabaseClient;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [supabase] = useState<SupabaseClient | null>(() => makeClient());
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [ready, setReady] = useState(false);

  // Debug logging
  useEffect(() => {
    console.log("[AUTH PROVIDER] State changed:", { loading, ready, hasUser: !!user, userRoles: user?.roles });
  }, [loading, ready, user]);

  const refreshUser = useCallback(async (token: string) => {
    setToken(token);
    try {
      const me = await api.get<MeResponse>("/api/auth/me");
      setUser(me);
      return true;
    } catch {
      setUser(null);
      return false;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      console.log("[AUTH PROVIDER] Starting session check...");
      if (!supabase) {
        console.log("[AUTH PROVIDER] No supabase client, setting ready=true, loading=false");
        setReady(true);
        setLoading(false);
        return;
      }
      const { data } = await supabase.auth.getSession();
      if (cancelled) return;
      console.log("[AUTH PROVIDER] Session check result:", { hasSession: !!data.session, hasToken: !!data.session?.access_token });
      const token = data.session?.access_token ?? null;
      if (token) {
        console.log("[AUTH PROVIDER] Token found, refreshing user...");
        const success = await refreshUser(token);
        if (success) {
          console.log("[AUTH PROVIDER] User loaded successfully, setting ready=true");
        } else {
          console.log("[AUTH PROVIDER] Failed to load user, but setting ready=true anyway");
        }
      } else {
        console.log("[AUTH PROVIDER] No token, user remains null, setting ready=true");
      }
      console.log("[AUTH PROVIDER] Setting loading=false");
      setLoading(false);
      supabase.auth.onAuthStateChange((_event, session) => {
        console.log("[AUTH PROVIDER] Auth state changed:", { event: _event, hasSession: !!session, hasToken: !!session?.access_token });
        const t = session?.access_token ?? null;
        if (t) {
          setLoading(true);
          refreshUser(t).finally(() => setLoading(false));
        } else {
          console.log("[AUTH PROVIDER] Session cleared, clearing user and token");
          setToken(null);
          setUser(null);
        }
      });
      console.log("[AUTH PROVIDER] Setting ready=true");
      setReady(true);
    })();
    return () => {
      cancelled = true;
    };
  }, [supabase, refreshUser]);

  const signOut = useCallback(async () => {
    setLoading(true);
    setToken(null);
    setUser(null);
    if (supabase) await supabase.auth.signOut();
    setLoading(false);
  }, [supabase]);

  return (
    <AuthContext.Provider value={{ supabase, user, loading, ready, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}