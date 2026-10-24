"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner } from "@/components/ui";
import { timeAgo } from "@/lib/format";

type Conversation = {
  id: string;
  title?: string;
  updatedAt?: string;
};

type Message = {
  id: string;
  role: string;
  content: string;
  createdAt: string;
};

export default function AiChatPage() {
  const [conversations, setConversations] = useState<Conversation[] | null>(null);
  const [active, setActive] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[] | null>(null);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const loadConversations = useCallback(() => {
    api
      .get<Conversation[]>("/api/ai/conversations")
      .then(setConversations)
      .catch(() => undefined);
  }, []);

  const loadMessages = useCallback((id: string) => {
    api
      .get<Message[]>(`/api/ai/conversations/${id}/messages`)
      .then(setMessages)
      .catch(() => setMessages([]));
  }, []);

  useEffect(loadConversations, [loadConversations]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, busy]);

  async function startNew() {
    const conv = await api.post<Conversation>("/api/ai/conversations", {});
    setActive(conv.id);
    setMessages([]);
    loadConversations();
  }

  async function send() {
    if (!input.trim() || busy) return;
    setBusy(true);
    const text = input;
    setInput("");
    try {
      if (!active) {
        const conv = await api.post<Conversation>("/api/ai/conversations", {});
        setActive(conv.id);
        await api.post(`/api/ai/conversations/${conv.id}/messages`, { message: text });
        loadConversations();
        loadMessages(conv.id);
      } else {
        await api.post(`/api/ai/conversations/${active}/messages`, { message: text });
        loadMessages(active);
      }
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to send");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Kisan Assistant"
        subtitle="Ask anything about farming — powered by AI"
        action={
          <button
            onClick={startNew}
            className="rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            + New chat
          </button>
        }
      />

      <div className="ks-shadow flex h-[calc(100vh-220px)] overflow-hidden rounded-2xl border border-line bg-white">
        <aside className="hidden w-60 flex-col border-r border-line bg-surface/60 md:flex">
          <div className="border-b border-line p-3">
            <p className="text-xs font-semibold text-muted uppercase">Conversations</p>
          </div>
          <div className="flex-1 overflow-y-auto p-2">
            {!conversations ? (
              <Spinner />
            ) : conversations.length === 0 ? (
              <p className="p-3 text-xs text-muted">No conversations yet.</p>
            ) : (
              conversations.map((c) => (
                <button
                  key={c.id}
                  onClick={() => {
                    setActive(c.id);
                    loadMessages(c.id);
                  }}
                  className={`mb-1 w-full rounded-xl px-3 py-2 text-left text-sm ${
                    active === c.id ? "bg-brand-light text-brand-dark" : "text-muted hover:bg-white"
                  }`}
                >
                  <p className="truncate font-medium">{c.title ?? "New conversation"}</p>
                  <p className="text-[10px] opacity-70">{timeAgo(c.updatedAt)}</p>
                </button>
              ))
            )}
          </div>
        </aside>

        <div className="flex flex-1 flex-col">
          <div className="flex-1 overflow-y-auto p-5">
            {!active ? (
              <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
                <span className="text-5xl">💬</span>
                <p className="font-semibold text-ink">Ask your Kisan Assistant anything</p>
                <p className="max-w-sm text-sm text-muted">
                  "How to control aphids on chillies?", "Best time to sow wheat?", "Why are my tomato leaves yellowing?"
                </p>
                <button
                  onClick={startNew}
                  className="mt-2 rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark"
                >
                  Start chatting
                </button>
              </div>
            ) : !messages ? (
              <Spinner />
            ) : messages.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-muted">
                Send your first message to begin.
              </div>
            ) : (
              <div className="space-y-4">
                {messages.map((m) => (
                  <div key={m.id} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
                    <div
                      className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm ${
                        m.role === "user"
                          ? "rounded-br-md bg-brand text-white"
                          : "rounded-bl-md border border-line bg-surface text-ink"
                      }`}
                    >
                      <p className="whitespace-pre-wrap">{m.content}</p>
                      <p className={`mt-1 text-[10px] ${m.role === "user" ? "text-white/70" : "text-muted"}`}>
                        {timeAgo(m.createdAt)}
                      </p>
                    </div>
                  </div>
                ))}
                {busy && (
                  <div className="flex justify-start">
                    <div className="rounded-2xl rounded-bl-md border border-line bg-surface px-4 py-3">
                      <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-brand border-t-transparent align-middle" />
                    </div>
                  </div>
                )}
                <div ref={bottomRef} />
              </div>
            )}
          </div>

          <div className="border-t border-line p-3">
            <div className="flex gap-2">
              <input
                className="flex-1 rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
                placeholder="Type your farming question..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && send()}
                disabled={busy}
              />
              <button
                onClick={send}
                disabled={busy || !input.trim()}
                className="rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
              >
                Send
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}