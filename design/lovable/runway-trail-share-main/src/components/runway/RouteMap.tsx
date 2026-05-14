export function RouteMap({ className = "", variant = "a" }: { className?: string; variant?: "a" | "b" | "c" }) {
  const paths = {
    a: "M20,180 C40,120 80,140 110,100 S180,60 220,90 S280,160 300,120",
    b: "M30,60 C70,80 90,160 140,170 S220,120 250,180 S290,200 310,150",
    c: "M20,100 Q80,20 140,80 T260,100 T310,180",
  };
  return (
    <div className={`relative w-full overflow-hidden bg-secondary ${className}`}>
      <div className="absolute inset-0 bg-grid opacity-60" />
      <svg viewBox="0 0 320 220" className="w-full h-full relative">
        <defs>
          <linearGradient id={`g-${variant}`} x1="0" x2="1">
            <stop offset="0%" stopColor="oklch(0.85 0.22 130)" />
            <stop offset="100%" stopColor="oklch(0.78 0.2 160)" />
          </linearGradient>
        </defs>
        <path d={paths[variant]} stroke={`url(#g-${variant})`} strokeWidth="3.5" fill="none" strokeLinecap="round" />
        <circle cx="20" cy={variant === "a" ? 180 : variant === "b" ? 60 : 100} r="5" fill="oklch(0.85 0.22 130)" />
        <circle cx="300" cy={variant === "a" ? 120 : variant === "b" ? 150 : 180} r="5" fill="oklch(0.98 0 0)" stroke="oklch(0.85 0.22 130)" strokeWidth="2" />
      </svg>
    </div>
  );
}
