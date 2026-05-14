import { StatusBar } from "@/components/runway/StatusBar";
import { RouteMap } from "@/components/runway/RouteMap";
import { BottomNav } from "@/components/runway/BottomNav";
import { Screen, ScreenLabel } from "@/components/runway/Screen";
import {
  ArrowRight, ChevronLeft, Pause, Square, Play, MapPin, Search,
  Flame, Footprints, Timer, Activity, Users, Trophy, Plus, Medal,
} from "lucide-react";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  component: Showcase,
  head: () => ({
    meta: [
      { title: "RunWay — Run. Share. Compete." },
      { name: "description", content: "Mobile UI concept for RunWay, a location-based running course sharing and competition app." },
    ],
  }),
});

function Splash() {
  return (
    <Screen>
      <div className="absolute inset-0 bg-grid opacity-40" />
      <div className="absolute -top-20 -left-20 w-72 h-72 rounded-full bg-primary/20 blur-3xl" />
      <div className="absolute bottom-0 -right-20 w-80 h-80 rounded-full bg-primary/10 blur-3xl" />
      <div className="relative h-full min-h-[760px] flex flex-col items-center justify-center px-8">
        <div className="flex items-center gap-2">
          <div className="w-12 h-12 rounded-2xl bg-primary flex items-center justify-center glow-primary">
            <Footprints className="w-6 h-6 text-primary-foreground" />
          </div>
          <span className="text-5xl font-bold tracking-tight">
            Run<span className="text-gradient-primary">Way</span>
          </span>
        </div>
        <p className="mt-5 text-sm text-muted-foreground tracking-wide">
          Run your way. Share the route. Beat the clock.
        </p>
        <div className="absolute bottom-12 flex flex-col items-center gap-3">
          <div className="w-8 h-1 rounded-full bg-primary animate-pulse" />
          <span className="font-mono text-[10px] text-muted-foreground uppercase">Loading routes…</span>
        </div>
      </div>
    </Screen>
  );
}

function Login() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-7 pt-10 pb-8">
        <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center mb-8">
          <Footprints className="w-5 h-5 text-primary-foreground" />
        </div>
        <h1 className="text-3xl font-bold tracking-tight">Welcome back.</h1>
        <p className="text-sm text-muted-foreground mt-1.5">Lace up and pick up where you left off.</p>
      </div>
      <div className="px-7 space-y-4">
        <Field label="Email" value="alex.runner@runway.app" />
        <Field label="Password" value="••••••••••" />
        <button className="text-xs text-muted-foreground ml-auto block">Forgot password?</button>
      </div>
      <div className="px-7 mt-8 space-y-3">
        <PrimaryBtn>Log in</PrimaryBtn>
        <p className="text-center text-sm text-muted-foreground">
          New here? <span className="text-primary font-medium">Create an account</span>
        </p>
      </div>
    </Screen>
  );
}

function Signup() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-7 pt-10 pb-6">
        <button className="w-9 h-9 rounded-full bg-card border border-border flex items-center justify-center mb-6">
          <ChevronLeft className="w-4 h-4" />
        </button>
        <h1 className="text-3xl font-bold tracking-tight">Join the run.</h1>
        <p className="text-sm text-muted-foreground mt-1.5">Build your profile in under a minute.</p>
      </div>
      <div className="px-7 space-y-4">
        <Field label="Email" value="jamie.swift@runway.app" />
        <Field label="Password" value="••••••••••" />
        <Field label="Nickname" value="jamie_swift" />
      </div>
      <div className="px-7 mt-8 space-y-3">
        <PrimaryBtn>Create account</PrimaryBtn>
        <p className="text-[11px] text-center text-muted-foreground leading-relaxed">
          By continuing you agree to RunWay's Terms and Privacy Policy.
        </p>
      </div>
    </Screen>
  );
}

function HomeScreen() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-5 pt-4 pb-3 flex items-center justify-between">
        <div>
          <p className="text-xs text-muted-foreground">Good evening,</p>
          <h2 className="text-xl font-bold tracking-tight">Alex 👋</h2>
        </div>
        <div className="w-10 h-10 rounded-full bg-secondary border border-border flex items-center justify-center text-sm font-semibold">A</div>
      </div>

      <div className="px-5 mt-3">
        <div className="rounded-3xl bg-primary text-primary-foreground p-5 relative overflow-hidden glow-primary">
          <div className="absolute -right-6 -top-6 w-32 h-32 rounded-full bg-black/10" />
          <p className="text-xs font-mono uppercase tracking-widest opacity-70">This week</p>
          <div className="flex items-end gap-2 mt-1">
            <span className="text-4xl font-bold">24.6</span>
            <span className="text-sm mb-1.5 opacity-80">km</span>
          </div>
          <div className="flex gap-5 mt-4 text-xs">
            <Stat dark label="Runs" value="4" />
            <Stat dark label="Pace" value={`5'12"`} />
            <Stat dark label="Cal" value="1,820" />
          </div>
        </div>
      </div>

      <div className="px-5 mt-4">
        <button className="w-full rounded-2xl bg-card border border-border py-4 flex items-center justify-between px-5">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center">
              <Play className="w-4 h-4 text-primary-foreground fill-current" />
            </div>
            <div className="text-left">
              <p className="text-sm font-semibold">Start running</p>
              <p className="text-[11px] text-muted-foreground">Free run · GPS ready</p>
            </div>
          </div>
          <ArrowRight className="w-4 h-4 text-muted-foreground" />
        </button>
      </div>

      <SectionHeader title="Nearby courses" cta="See all" />
      <div className="px-5 flex gap-3 overflow-x-auto pb-1">
        {[
          { name: "Riverside Loop", dist: "0.4 km", len: "5.2 km", v: "a" as const },
          { name: "Hillcrest Sprint", dist: "1.1 km", len: "3.0 km", v: "b" as const },
        ].map((c) => (
          <div key={c.name} className="min-w-[180px] rounded-2xl bg-card border border-border overflow-hidden">
            <RouteMap variant={c.v} className="h-20" />
            <div className="p-3">
              <p className="text-sm font-semibold truncate">{c.name}</p>
              <p className="text-[11px] text-muted-foreground mt-0.5 flex items-center gap-1">
                <MapPin className="w-3 h-3" /> {c.dist} away · {c.len}
              </p>
            </div>
          </div>
        ))}
      </div>

      <SectionHeader title="Recent runs" />
      <div className="px-5 space-y-2 pb-24">
        {[
          { d: "Today", km: "5.20", pace: "5'08\"", t: "26:43" },
          { d: "Tue", km: "8.10", pace: "5'22\"", t: "43:30" },
        ].map((r) => (
          <div key={r.d} className="rounded-2xl bg-card border border-border p-3 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-secondary flex items-center justify-center">
              <Activity className="w-4 h-4 text-primary" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-semibold">{r.km} km · {r.t}</p>
              <p className="text-[11px] text-muted-foreground">{r.d} · Pace {r.pace}/km</p>
            </div>
            <ChevronLeft className="w-4 h-4 rotate-180 text-muted-foreground" />
          </div>
        ))}
      </div>

      <BottomNav active="home" />
    </Screen>
  );
}

function Tracking() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-5 pt-3 flex items-center justify-between">
        <div className="px-3 py-1.5 rounded-full bg-card border border-border text-[11px] font-mono uppercase tracking-widest text-primary flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" /> Recording
        </div>
        <span className="text-[11px] font-mono text-muted-foreground">GPS · Strong</span>
      </div>

      <div className="px-6 pt-10 text-center">
        <p className="text-xs font-mono uppercase tracking-widest text-muted-foreground">Time</p>
        <p className="text-7xl font-bold tracking-tighter font-mono mt-1">28:14</p>
      </div>

      <div className="mx-5 mt-8 grid grid-cols-3 rounded-3xl bg-card border border-border overflow-hidden">
        <Metric label="Distance" value="5.42" unit="km" />
        <Metric label="Pace" value={`5'12"`} unit="/km" border />
        <Metric label="Speed" value="11.5" unit="km/h" />
      </div>

      <div className="mx-5 mt-5 rounded-3xl overflow-hidden border border-border">
        <RouteMap variant="a" className="h-44" />
      </div>

      <div className="absolute bottom-8 left-0 right-0 px-8 flex items-center justify-center gap-5">
        <button className="w-14 h-14 rounded-full bg-card border border-border flex items-center justify-center">
          <Square className="w-5 h-5 fill-destructive text-destructive" />
        </button>
        <button className="w-20 h-20 rounded-full bg-primary flex items-center justify-center glow-primary">
          <Pause className="w-7 h-7 text-primary-foreground fill-current" />
        </button>
        <button className="w-14 h-14 rounded-full bg-card border border-border flex items-center justify-center">
          <Play className="w-5 h-5 text-primary fill-current" />
        </button>
      </div>
    </Screen>
  );
}

function Result() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-5 pt-3 flex items-center justify-between">
        <button className="w-9 h-9 rounded-full bg-card border border-border flex items-center justify-center">
          <ChevronLeft className="w-4 h-4" />
        </button>
        <p className="text-sm font-semibold">Run complete</p>
        <div className="w-9" />
      </div>

      <div className="px-6 pt-6 text-center">
        <p className="text-xs font-mono uppercase tracking-widest text-primary">Saturday Evening Run</p>
        <p className="text-6xl font-bold tracking-tighter mt-2">5.42<span className="text-2xl text-muted-foreground"> km</span></p>
        <p className="text-xs text-muted-foreground mt-1">Riverside Park · 8:42 PM</p>
      </div>

      <div className="mx-5 mt-6 grid grid-cols-2 gap-3">
        <ResultCard icon={<Timer className="w-4 h-4" />} label="Time" value="28:14" />
        <ResultCard icon={<Activity className="w-4 h-4" />} label="Pace" value={`5'12"/km`} />
        <ResultCard icon={<Flame className="w-4 h-4" />} label="Calories" value="412 kcal" />
        <ResultCard icon={<Footprints className="w-4 h-4" />} label="Steps" value="6,840" />
      </div>

      <div className="mx-5 mt-4 rounded-3xl overflow-hidden border border-border">
        <RouteMap variant="a" className="h-32" />
      </div>

      <div className="absolute bottom-6 left-0 right-0 px-5 space-y-2">
        <PrimaryBtn>
          <Plus className="w-4 h-4" /> Create course from this run
        </PrimaryBtn>
        <button className="w-full py-3 text-sm text-muted-foreground">Save & exit</button>
      </div>
    </Screen>
  );
}

function Discover() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-5 pt-3 pb-3">
        <h1 className="text-2xl font-bold tracking-tight">Discover courses</h1>
        <p className="text-xs text-muted-foreground mt-1">23 courses within 5 km</p>
      </div>

      <div className="px-5">
        <div className="flex items-center gap-2 rounded-2xl bg-card border border-border px-4 py-3">
          <Search className="w-4 h-4 text-muted-foreground" />
          <input
            readOnly
            value="Search nearby courses"
            className="bg-transparent text-sm flex-1 outline-none text-muted-foreground"
          />
        </div>
        <div className="flex gap-2 mt-3 overflow-x-auto">
          {["Nearby", "Under 5 km", "Flat", "Trail", "Popular"].map((t, i) => (
            <span key={t} className={`shrink-0 px-3 py-1.5 rounded-full text-xs font-medium border ${i === 0 ? "bg-primary text-primary-foreground border-primary" : "border-border text-muted-foreground"}`}>
              {t}
            </span>
          ))}
        </div>
      </div>

      <div className="px-5 mt-4 space-y-3 pb-24">
        {[
          { name: "Riverside Loop", away: "0.4 km", len: "5.2 km", att: 312, comp: 248, v: "a" as const },
          { name: "Hillcrest Sprint", away: "1.1 km", len: "3.0 km", att: 178, comp: 96, v: "b" as const },
          { name: "Old Bridge Circuit", away: "2.3 km", len: "7.8 km", att: 524, comp: 401, v: "c" as const },
        ].map((c) => (
          <div key={c.name} className="rounded-2xl bg-card border border-border overflow-hidden">
            <RouteMap variant={c.v} className="h-28" />
            <div className="p-4">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-base font-semibold">{c.name}</p>
                  <p className="text-[11px] text-muted-foreground mt-0.5 flex items-center gap-1">
                    <MapPin className="w-3 h-3" /> {c.away} away
                  </p>
                </div>
                <span className="text-xs font-mono text-primary">{c.len}</span>
              </div>
              <div className="flex gap-4 mt-3 text-[11px] text-muted-foreground">
                <span className="flex items-center gap-1"><Users className="w-3 h-3" /> {c.att} attempts</span>
                <span className="flex items-center gap-1"><Trophy className="w-3 h-3" /> {c.comp} completed</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <BottomNav active="discover" />
    </Screen>
  );
}

function CourseDetail() {
  return (
    <Screen>
      <div className="relative">
        <RouteMap variant="a" className="h-60" />
        <div className="absolute inset-0 bg-gradient-to-b from-background/60 via-transparent to-background" />
        <button className="absolute top-12 left-5 w-9 h-9 rounded-full bg-card/80 backdrop-blur border border-border flex items-center justify-center">
          <ChevronLeft className="w-4 h-4" />
        </button>
      </div>

      <div className="px-5 -mt-8 relative">
        <span className="text-xs font-mono uppercase tracking-widest text-primary">Course</span>
        <h1 className="text-2xl font-bold tracking-tight mt-1">Riverside Loop</h1>
        <div className="flex gap-4 mt-2 text-xs text-muted-foreground">
          <span className="flex items-center gap-1"><MapPin className="w-3 h-3" /> 0.4 km away</span>
          <span>· 5.2 km</span>
          <span>· Flat</span>
        </div>
        <p className="text-sm text-muted-foreground mt-3 leading-relaxed">
          A scenic loop along the river path. Mostly flat with two short climbs near the boathouse.
        </p>
      </div>

      <div className="px-5 mt-5">
        <div className="flex items-center justify-between mb-2">
          <p className="text-sm font-semibold">Leaderboard</p>
          <span className="text-xs text-muted-foreground">Top 3</span>
        </div>
        <div className="rounded-2xl bg-card border border-border divide-y divide-border">
          {[
            { r: 1, n: "miles_max", t: "21:08" },
            { r: 2, n: "sara_runs", t: "22:34" },
            { r: 3, n: "alex (you)", t: "23:12" },
          ].map((u) => (
            <div key={u.r} className="flex items-center gap-3 px-4 py-3">
              <span className={`w-6 h-6 rounded-full grid place-items-center text-[11px] font-bold ${u.r === 1 ? "bg-primary text-primary-foreground" : "bg-secondary text-foreground"}`}>{u.r}</span>
              <span className="text-sm flex-1">{u.n}</span>
              <span className="text-sm font-mono text-primary">{u.t}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="absolute bottom-6 left-0 right-0 px-5">
        <PrimaryBtn>Attempt course</PrimaryBtn>
      </div>
    </Screen>
  );
}

function Attempt() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-5 pt-3 flex items-center justify-between">
        <button className="w-9 h-9 rounded-full bg-card border border-border flex items-center justify-center">
          <ChevronLeft className="w-4 h-4" />
        </button>
        <div className="text-center">
          <p className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">Attempting</p>
          <p className="text-sm font-semibold">Riverside Loop</p>
        </div>
        <div className="w-9" />
      </div>

      <div className="px-6 pt-8 text-center">
        <p className="text-xs font-mono uppercase tracking-widest text-primary">Elapsed</p>
        <p className="text-6xl font-bold tracking-tighter font-mono mt-1">14:32</p>
        <p className="text-xs text-muted-foreground mt-1">vs best 21:08 · on pace +0:42</p>
      </div>

      <div className="px-5 mt-6">
        <div className="flex items-center justify-between text-[11px] text-muted-foreground mb-2">
          <span>Progress</span>
          <span className="font-mono text-primary">2.84 / 5.20 km</span>
        </div>
        <div className="h-2 rounded-full bg-secondary overflow-hidden">
          <div className="h-full w-[55%] bg-primary rounded-full" />
        </div>
      </div>

      <div className="mx-5 mt-5 grid grid-cols-3 rounded-3xl bg-card border border-border overflow-hidden">
        <Metric label="Pace" value={`5'06"`} unit="/km" />
        <Metric label="Speed" value="11.8" unit="km/h" border />
        <Metric label="Heart" value="162" unit="bpm" />
      </div>

      <div className="mx-5 mt-4 rounded-3xl overflow-hidden border border-border relative">
        <RouteMap variant="a" className="h-32" />
        <div className="absolute top-2 right-2 px-2 py-1 rounded-full bg-background/80 backdrop-blur text-[10px] font-mono">
          Following course
        </div>
      </div>

      <div className="absolute bottom-8 left-0 right-0 px-5">
        <button className="w-full rounded-2xl bg-destructive text-destructive-foreground py-4 font-semibold flex items-center justify-center gap-2">
          <Square className="w-4 h-4 fill-current" /> Finish attempt
        </button>
      </div>
    </Screen>
  );
}

function Leaderboard() {
  return (
    <Screen>
      <StatusBar />
      <div className="px-5 pt-3 pb-2">
        <h1 className="text-2xl font-bold tracking-tight">Leaderboard</h1>
        <p className="text-xs text-muted-foreground mt-1">Riverside Loop · 5.2 km</p>
      </div>

      <div className="px-5 flex gap-2 mt-2">
        {["All time", "This month", "Friends"].map((t, i) => (
          <span key={t} className={`px-3 py-1.5 rounded-full text-xs font-medium border ${i === 0 ? "bg-primary text-primary-foreground border-primary" : "border-border text-muted-foreground"}`}>{t}</span>
        ))}
      </div>

      <div className="px-5 mt-5 grid grid-cols-3 gap-2 items-end">
        <Podium rank={2} name="sara_runs" time="22:34" h="h-20" />
        <Podium rank={1} name="miles_max" time="21:08" h="h-28" highlight />
        <Podium rank={3} name="kai.j" time="23:01" h="h-16" />
      </div>

      <div className="px-5 mt-6 space-y-2 pb-24">
        {[
          { r: 4, n: "alex (you)", t: "23:12", c: 8, you: true },
          { r: 5, n: "lena.k", t: "23:48", c: 5 },
          { r: 6, n: "tomas_p", t: "24:02", c: 12 },
          { r: 7, n: "noor.r", t: "24:30", c: 3 },
          { r: 8, n: "iris.f", t: "24:55", c: 7 },
        ].map((u) => (
          <div key={u.r} className={`flex items-center gap-3 px-4 py-3 rounded-2xl border ${u.you ? "bg-primary/10 border-primary/40" : "bg-card border-border"}`}>
            <span className="w-6 text-center text-xs font-mono text-muted-foreground">{u.r}</span>
            <div className="w-8 h-8 rounded-full bg-secondary grid place-items-center text-xs font-semibold">{u.n[0].toUpperCase()}</div>
            <div className="flex-1">
              <p className="text-sm font-medium">{u.n}</p>
              <p className="text-[11px] text-muted-foreground">{u.c} completions</p>
            </div>
            <span className="text-sm font-mono text-primary">{u.t}</span>
          </div>
        ))}
      </div>

      <BottomNav active="leaderboard" />
    </Screen>
  );
}

/* ---------- helpers ---------- */

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-card border border-border px-4 py-3">
      <p className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">{label}</p>
      <p className="text-sm mt-0.5">{value}</p>
    </div>
  );
}

function PrimaryBtn({ children }: { children: React.ReactNode }) {
  return (
    <button className="w-full rounded-2xl bg-primary text-primary-foreground py-4 font-semibold flex items-center justify-center gap-2 glow-primary">
      {children}
    </button>
  );
}

function Stat({ label, value, dark }: { label: string; value: string; dark?: boolean }) {
  return (
    <div>
      <p className={`text-[10px] uppercase tracking-widest ${dark ? "opacity-70" : "text-muted-foreground"}`}>{label}</p>
      <p className="text-base font-semibold mt-0.5">{value}</p>
    </div>
  );
}

function SectionHeader({ title, cta }: { title: string; cta?: string }) {
  return (
    <div className="px-5 pt-5 pb-2 flex items-center justify-between">
      <h3 className="text-sm font-semibold">{title}</h3>
      {cta && <span className="text-xs text-primary">{cta}</span>}
    </div>
  );
}

function Metric({ label, value, unit, border }: { label: string; value: string; unit: string; border?: boolean }) {
  return (
    <div className={`py-4 px-2 text-center ${border ? "border-x border-border" : ""}`}>
      <p className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">{label}</p>
      <p className="text-xl font-bold mt-1">{value}</p>
      <p className="text-[10px] text-muted-foreground">{unit}</p>
    </div>
  );
}

function ResultCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-card border border-border p-4">
      <div className="flex items-center gap-1.5 text-muted-foreground">
        {icon}
        <p className="text-[10px] uppercase tracking-widest">{label}</p>
      </div>
      <p className="text-lg font-bold mt-1">{value}</p>
    </div>
  );
}

function Podium({ rank, name, time, h, highlight }: { rank: number; name: string; time: string; h: string; highlight?: boolean }) {
  return (
    <div className="flex flex-col items-center gap-2">
      <div className={`w-12 h-12 rounded-full grid place-items-center text-base font-semibold ${highlight ? "bg-primary text-primary-foreground" : "bg-secondary"}`}>
        {name[0].toUpperCase()}
      </div>
      <p className="text-xs font-medium truncate max-w-full">{name}</p>
      <p className="text-[11px] font-mono text-primary">{time}</p>
      <div className={`w-full ${h} rounded-t-2xl flex items-start justify-center pt-2 ${highlight ? "bg-primary/20 border border-primary/40" : "bg-card border border-border"}`}>
        <Medal className={`w-4 h-4 ${highlight ? "text-primary" : "text-muted-foreground"}`} />
        <span className="ml-1 text-xs font-bold">{rank}</span>
      </div>
    </div>
  );
}

const SCREENS = [
  { n: 1, title: "Splash", C: Splash },
  { n: 2, title: "Login", C: Login },
  { n: 3, title: "Sign up", C: Signup },
  { n: 4, title: "Home", C: HomeScreen },
  { n: 5, title: "Running tracking", C: Tracking },
  { n: 6, title: "Run result", C: Result },
  { n: 7, title: "Course discovery", C: Discover },
  { n: 8, title: "Course detail", C: CourseDetail },
  { n: 9, title: "Course attempt", C: Attempt },
  { n: 10, title: "Leaderboard", C: Leaderboard },
];

function Showcase() {
  return (
    <main className="min-h-screen bg-background pb-12">
      <header className="max-w-[420px] mx-auto px-5 pt-10 pb-6">
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-primary grid place-items-center">
            <Footprints className="w-4 h-4 text-primary-foreground" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">
            Run<span className="text-gradient-primary">Way</span>
          </h1>
        </div>
        <p className="text-sm text-muted-foreground mt-2">
          Mobile UI concept · 10 screens · dark sporty theme
        </p>
      </header>

      {SCREENS.map(({ n, title, C }) => (
        <section key={n}>
          <ScreenLabel n={n} title={title} />
          <C />
        </section>
      ))}

      <footer className="max-w-[420px] mx-auto px-5 pt-10 text-center">
        <p className="text-xs font-mono uppercase tracking-widest text-muted-foreground">
          End of concept · ready for Jetpack Compose
        </p>
      </footer>
    </main>
  );
}
