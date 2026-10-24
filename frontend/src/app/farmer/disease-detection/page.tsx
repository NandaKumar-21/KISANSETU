"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { PageHeader, Spinner, EmptyState, StatusBadge } from "@/components/ui";
import { timeAgo } from "@/lib/format";

type Analysis = {
  diseaseName: string;
  confidence: string;
  severity: string;
  description: string;
  symptoms: string[];
  treatments: Array<{ name: string; dosagePerAcre: string; description: string }>;
  applicationGuide: Array<{ step: string; timing: string }>;
  preventionTips: string[];
};

type HistoryRecord = {
  id: string;
  diseaseName?: string;
  confidence?: string;
  severity?: string;
  description?: string;
  detectionMethod: string;
  symptoms?: string[];
  createdAt: string;
};

const SEVERITY_COLOR: Record<string, string> = {
  low: "bg-green-50 text-green-700 border-green-200",
  medium: "bg-amber-50 text-amber-700 border-amber-200",
  high: "bg-red-50 text-red-700 border-red-200",
};

export default function DiseaseDetectionPage() {
  const [mode, setMode] = useState<"symptom" | "image">("symptom");
  const [symptoms, setSymptoms] = useState("");
  const [image, setImage] = useState<string | null>(null);
  const [result, setResult] = useState<Analysis | null>(null);
  const [history, setHistory] = useState<HistoryRecord[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const loadHistory = useCallback(() => {
    api
      .get<HistoryRecord[]>("/api/disease/history")
      .then(setHistory)
      .catch(() => undefined);
  }, []);

  useEffect(loadHistory, [loadHistory]);

  function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setImage(reader.result as string);
    reader.readAsDataURL(file);
  }

  async function submit() {
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const body =
        mode === "symptom"
          ? {
              method: "symptom",
              symptoms: symptoms
                .split(",")
                .map((s) => s.trim())
                .filter(Boolean),
            }
          : { method: "image", image };
      const data = await api.post<{ analysis: Analysis }>("/api/disease/detect", body);
      setResult(data.analysis);
      loadHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Detection failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader title="Disease Detection" subtitle="Identify crop diseases with AI" />
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="ks-shadow rounded-2xl border border-line bg-white p-6">
          <div className="mb-4 grid grid-cols-2 rounded-xl bg-surface p-1">
            {(["symptom", "image"] as const).map((m) => (
              <button
                key={m}
                onClick={() => {
                  setMode(m);
                  setResult(null);
                  setError(null);
                }}
                className={`rounded-lg py-2 text-sm font-semibold ${
                  mode === m ? "bg-white text-brand-dark ks-shadow" : "text-muted"
                }`}
              >
                {m === "symptom" ? "Describe symptoms" : "Upload photo"}
              </button>
            ))}
          </div>

          {mode === "symptom" ? (
            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">Symptoms (comma separated)</label>
              <textarea
                className="min-h-28 w-full rounded-xl border border-line bg-white px-4 py-2.5 text-sm focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
                placeholder="e.g. yellow spots on leaves, wilting, white powder on stem"
                value={symptoms}
                onChange={(e) => setSymptoms(e.target.value)}
              />
            </div>
          ) : (
            <div>
              <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" className="hidden" onChange={onFile} />
              <button
                onClick={() => fileRef.current?.click()}
                className="flex h-44 w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-line bg-surface text-muted hover:border-brand"
              >
                {image ? (
                  <>
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={image} alt="Selected" className="max-h-32 max-w-full rounded-lg object-contain" />
                    <span className="text-xs">Click to change</span>
                  </>
                ) : (
                  <>
                    <span className="text-3xl">📷</span>
                    <span className="text-sm">Click to upload a leaf photo (JPEG/PNG/WebP)</span>
                  </>
                )}
              </button>
            </div>
          )}

          {error && <p className="mt-3 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}

          <button
            onClick={submit}
            disabled={busy || (mode === "symptom" ? !symptoms.trim() : !image)}
            className="mt-4 w-full rounded-xl bg-brand py-2.5 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
          >
            {busy ? "Analyzing..." : "Detect disease"}
          </button>
        </div>

        <div>
          {busy ? (
            <Spinner label="AI is analyzing your crop..." />
          ) : result ? (
            <div className="ks-shadow rounded-2xl border border-line bg-white p-6">
              <div className="flex items-center justify-between">
                <h3 className="text-xl font-bold text-ink">🔬 {result.diseaseName}</h3>
                <span className={`rounded-full border px-3 py-1 text-xs font-bold ${SEVERITY_COLOR[result.severity] ?? "bg-stone-100 text-stone-600"}`}>
                  {result.severity}
                </span>
              </div>
              <p className="mt-1 text-xs text-muted">Confidence: {result.confidence}</p>
              <p className="mt-3 text-sm text-muted">{result.description}</p>

              {result.symptoms.length > 0 && (
                <div className="mt-4">
                  <p className="text-xs font-semibold text-muted uppercase">Detected symptoms</p>
                  <div className="mt-1.5 flex flex-wrap gap-1.5">
                    {result.symptoms.map((s) => (
                      <span key={s} className="rounded-full border border-line bg-surface px-2.5 py-0.5 text-xs text-muted">{s}</span>
                    ))}
                  </div>
                </div>
              )}

              {result.treatments.length > 0 && (
                <div className="mt-4">
                  <p className="text-xs font-semibold text-muted uppercase">Treatments</p>
                  <div className="mt-1.5 space-y-2">
                    {result.treatments.map((t) => (
                      <div key={t.name} className="rounded-xl border border-line bg-surface p-3">
                        <p className="text-sm font-semibold text-ink">{t.name}</p>
                        <p className="text-xs text-muted">Dosage: {t.dosagePerAcre}</p>
                        {t.description && <p className="mt-1 text-xs text-muted">{t.description}</p>}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {result.applicationGuide.length > 0 && (
                <div className="mt-4">
                  <p className="text-xs font-semibold text-muted uppercase">Application guide</p>
                  <div className="mt-1.5 space-y-1.5">
                    {result.applicationGuide.map((s, i) => (
                      <div key={i} className="flex items-start gap-2 text-sm">
                        <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-light text-xs font-bold text-brand-dark">{i + 1}</span>
                        <p className="text-muted">{s.step} <span className="text-xs">({s.timing})</span></p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {result.preventionTips.length > 0 && (
                <div className="mt-4">
                  <p className="text-xs font-semibold text-muted uppercase">Prevention</p>
                  <ul className="mt-1.5 list-inside list-disc space-y-1 text-sm text-muted">
                    {result.preventionTips.map((t) => (
                      <li key={t}>{t}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          ) : (
            <div className="flex h-full items-center justify-center rounded-2xl border border-dashed border-line bg-surface p-10 text-center">
              <p className="max-w-xs text-sm text-muted">
                Describe symptoms or upload a photo of the affected plant to get an instant AI diagnosis with treatments.
              </p>
            </div>
          )}
        </div>
      </div>

      <div className="mt-8">
        <h3 className="mb-3 text-sm font-semibold text-muted uppercase">Detection history</h3>
        {!history ? (
          <Spinner />
        ) : history.length === 0 ? (
          <EmptyState icon="🔬" title="No detections yet" />
        ) : (
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {history.map((h) => (
              <div key={h.id} className="ks-shadow rounded-2xl border border-line bg-white p-4">
                <div className="flex items-center justify-between">
                  <p className="font-semibold text-ink">{h.diseaseName ?? "Analysis"}</p>
                  {h.severity && <StatusBadge status={h.severity.toUpperCase()} />}
                </div>
                <p className="mt-1 text-xs text-muted">
                  {h.detectionMethod} · {h.confidence ?? "—"} · {timeAgo(h.createdAt)}
                </p>
                {h.symptoms && h.symptoms.length > 0 && (
                  <p className="mt-2 text-xs text-muted">{h.symptoms.join(", ")}</p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}