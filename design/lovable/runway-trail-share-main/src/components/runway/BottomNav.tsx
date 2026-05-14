import { Home, Compass, Trophy, User } from "lucide-react";

export function BottomNav({ active = "home" }: { active?: "home" | "discover" | "leaderboard" | "profile" }) {
  const items = [
    { id: "home", icon: Home, label: "Home" },
    { id: "discover", icon: Compass, label: "Discover" },
    { id: "leaderboard", icon: Trophy, label: "Ranks" },
    { id: "profile", icon: User, label: "Me" },
  ] as const;
  return (
    <div className="absolute bottom-0 left-0 right-0 bg-card/90 backdrop-blur border-t border-border px-4 py-2 flex justify-around">
      {items.map((it) => {
        const Icon = it.icon;
        const isActive = active === it.id;
        return (
          <div key={it.id} className="flex flex-col items-center gap-0.5 py-1.5 px-3">
            <Icon className={`w-5 h-5 ${isActive ? "text-primary" : "text-muted-foreground"}`} />
            <span className={`text-[10px] ${isActive ? "text-primary font-medium" : "text-muted-foreground"}`}>{it.label}</span>
          </div>
        );
      })}
    </div>
  );
}
