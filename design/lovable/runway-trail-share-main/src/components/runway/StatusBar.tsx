import { Signal, Wifi, BatteryFull } from "lucide-react";

export function StatusBar() {
  return (
    <div className="flex items-center justify-between px-6 pt-3 pb-1 text-xs font-mono text-foreground">
      <span>9:41</span>
      <div className="flex items-center gap-1">
        <Signal className="w-3 h-3" />
        <Wifi className="w-3 h-3" />
        <BatteryFull className="w-4 h-4" />
      </div>
    </div>
  );
}
