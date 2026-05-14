import { ReactNode } from "react";

export function PhoneFrame({ children, label }: { children: ReactNode; label?: string }) {
  return (
    <div className="flex flex-col items-center gap-3">
      {label && (
        <span className="text-xs font-mono uppercase tracking-widest text-muted-foreground">
          {label}
        </span>
      )}
      <div className="relative w-[340px] h-[720px] rounded-[3rem] border border-border bg-card p-2 shadow-2xl">
        <div className="relative w-full h-full overflow-hidden rounded-[2.5rem] bg-background">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 z-50 w-32 h-6 bg-black rounded-b-2xl" />
          {children}
        </div>
      </div>
    </div>
  );
}
