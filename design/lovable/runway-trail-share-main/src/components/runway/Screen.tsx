import { ReactNode } from "react";

export function Screen({ children }: { children: ReactNode }) {
  return (
    <div className="w-full max-w-[420px] mx-auto bg-background min-h-[760px] relative overflow-hidden border-x border-border">
      {children}
    </div>
  );
}

export function ScreenLabel({ n, title }: { n: number; title: string }) {
  return (
    <div className="w-full max-w-[420px] mx-auto py-6 px-5 flex items-baseline gap-3">
      <span className="font-mono text-xs text-primary">{String(n).padStart(2, "0")}</span>
      <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
      <div className="flex-1 h-px bg-border" />
    </div>
  );
}
