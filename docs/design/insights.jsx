// insights.jsx — Insights tab. 7-day chart + heatmap + editorial weekly letter.

const SevenDayChart = () => {
  const days = [
    { d: 'M', m: 198 }, { d: 'T', m: 245 }, { d: 'W', m: 162 },
    { d: 'T', m: 311 }, { d: 'F', m: 289 }, { d: 'S', m: 134 },
    { d: 'S', m: 175 },
  ];
  const max = Math.max(...days.map(d => d.m));
  const goal = 150;
  return (
    <div>
      <div style={{ position: 'relative', height: 140, display: 'flex', alignItems: 'flex-end', gap: 10, padding: '0 6px' }}>
        {/* goal line */}
        <div style={{
          position: 'absolute', left: 4, right: 4,
          bottom: `${(goal / max) * 100}%`,
          borderTop: `1px dashed ${TOK.accent}`,
          opacity: 0.7,
        }}>
          <span style={{
            position: 'absolute', right: 0, top: -16,
            fontSize: 10, color: TOK.accent, fontFamily: 'Geist Mono, monospace',
            background: TOK.card, padding: '0 4px',
          }}>goal 2h 30m</span>
        </div>
        {days.map((day, i) => {
          const h = (day.m / max) * 100;
          const over = day.m > goal;
          return (
            <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, height: '100%' }}>
              <div style={{ flex: 1, width: '100%', display: 'flex', alignItems: 'flex-end' }}>
                <div style={{
                  width: '100%',
                  height: `${h}%`,
                  background: over ? TOK.accent : TOK.sage,
                  borderRadius: '4px 4px 0 0',
                  minHeight: 3,
                  position: 'relative',
                }}>
                  {i === 3 && (
                    <span style={{
                      position: 'absolute', top: -18, left: '50%', transform: 'translateX(-50%)',
                      fontFamily: 'Geist Mono, monospace', fontSize: 10, color: TOK.ink,
                    }}>5h11</span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
      <div style={{ display: 'flex', gap: 10, padding: '8px 6px 0' }}>
        {days.map((day, i) => (
          <div key={i} className="pc-meta" style={{ flex: 1, textAlign: 'center', fontFamily: 'Geist Mono, monospace' }}>{day.d}</div>
        ))}
      </div>
    </div>
  );
};

const Heatmap = () => {
  // 7 days × 24 hours mock data — emphasis evenings/late night
  const rand = (d, h) => {
    const peak = h >= 20 || h <= 1 ? 0.7 : h >= 12 && h <= 14 ? 0.4 : h >= 7 && h <= 9 ? 0.35 : 0.1;
    const noise = ((d * 31 + h * 7) % 13) / 60;
    return Math.min(1, Math.max(0, peak + noise - 0.05));
  };
  const labels = ['Tue','Wed','Thu','Fri','Sat','Sun','Mon'];
  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        <div style={{ width: 28 }} />
        <div style={{ flex: 1, display: 'flex', justifyContent: 'space-between', fontFamily: 'Geist Mono, monospace', fontSize: 9, color: TOK.inkMute }}>
          {[0, 6, 12, 18, 23].map(h => <span key={h}>{h}</span>)}
        </div>
      </div>
      {labels.map((lab, d) => (
        <div key={d} style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 3 }}>
          <div style={{ width: 28, fontFamily: 'Geist Mono, monospace', fontSize: 10, color: TOK.inkMute }}>{lab}</div>
          <div style={{ flex: 1, display: 'flex', gap: 2 }}>
            {Array.from({ length: 24 }).map((_, h) => {
              const v = rand(d, h);
              return (
                <div key={h} style={{
                  flex: 1, height: 14, borderRadius: 2,
                  background: v < 0.1 ? TOK.ruleSoft : `rgba(194, 90, 51, ${0.18 + v * 0.82})`,
                }}/>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
};

const WeeklyLetter = () => (
  <div className="pc-card" style={{ padding: '24px 22px 22px', background: TOK.card2 }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 18 }}>
      <div style={{
        width: 28, height: 28, borderRadius: '50%', background: TOK.ink,
        color: TOK.paper, display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <Icon name="eye" size={14}/>
      </div>
      <div>
        <div className="pc-eyebrow">Week 19 · letter from your coach</div>
        <div className="pc-mono" style={{ fontSize: 10.5, color: TOK.inkMute, marginTop: 2 }}>
          May 6 – May 12 · written Sun 8:04 AM
        </div>
      </div>
    </div>

    <div className="pc-serif" style={{ fontSize: 22, lineHeight: 1.25, marginBottom: 14 }}>
      Eli — last week you spent <span style={{ color: TOK.accent }}>9h 14m</span> on Social,
      and three of those hours arrived after 10pm.
    </div>

    <div className="pc-body" style={{ color: TOK.ink2, fontSize: 13.5, lineHeight: 1.7 }}>
      <p style={{ margin: '0 0 12px' }}>
        The pattern is consistent. Monday through Thursday you stayed
        within the Social cap, and you closed Instagram on the first prompt
        every time. Then Friday happened — a 71-minute session that ran
        straight through the negotiation overlay, and that single evening
        accounted for nearly a third of your weekly total.
      </p>
      <p style={{ margin: '0 0 12px' }}>
        You told me twice this week that you were "just checking one thing".
        That phrase has come up <span className="pc-mono">11×</span> over the last
        four weeks, and the average session length when you use it is
        <span className="pc-mono"> 22 minutes</span>. It's a tell.
      </p>
      <p style={{ margin: '0 0 0' }}>
        One thing for next week: put the phone on the kitchen counter at 9pm.
        Not as a rule — as an experiment. If it changes nothing, we'll know.
      </p>
    </div>

    <div style={{
      marginTop: 18, paddingTop: 16, borderTop: `1px solid ${TOK.rule}`,
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    }}>
      <div className="pc-meta">
        Drawn from 47 sessions, 6 negotiations, 1 hard-lock.
      </div>
      <button className="pc-btn ghost" style={{ padding: '6px 10px', fontSize: 12, border: `1px solid ${TOK.rule}` }}>
        Mark as read
      </button>
    </div>
  </div>
);

const InsightsScreen = () => (
  <div className="pc-screen">
    <ScreenHeader
      eyebrow="Insights"
      title="Patterns"
      right={
        <button className="pc-btn ghost" style={{ padding: '6px 12px', fontSize: 12, border: `1px solid ${TOK.rule}`, borderRadius: 999 }}>
          Run report
        </button>
      }
    />

    <div style={{ padding: '12px 16px 16px' }}>
      <div className="pc-card" style={{ padding: '18px 16px 14px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <div className="pc-eyebrow">Last 7 days</div>
          <div className="pc-mono" style={{ fontSize: 11, color: TOK.ink2 }}>
            <span style={{ color: TOK.accent }}>↑ 12%</span> vs last week
          </div>
        </div>
        <div style={{ marginTop: 4, marginBottom: 12 }}>
          <span className="pc-serif" style={{ fontSize: 30 }}>22h 14m</span>
          <span className="pc-meta" style={{ marginLeft: 8 }}>avg 3h 11m/day</span>
        </div>
        <SevenDayChart />
      </div>

      <div className="pc-card" style={{ padding: '18px 16px 16px', marginTop: 12 }}>
        <div className="pc-eyebrow" style={{ marginBottom: 2 }}>When you reach for it</div>
        <div className="pc-meta" style={{ marginBottom: 12 }}>Hour-of-day heatmap, last 7 days. Darker = more.</div>
        <Heatmap />
      </div>

      <div style={{ marginTop: 18 }}>
        <WeeklyLetter />
      </div>

      <div style={{ marginTop: 12 }}>
        <div className="pc-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: 14 }}>Week 18 · letter</div>
            <div className="pc-meta">"You finally found a quiet evening." · Apr 29 – May 5</div>
          </div>
          <Icon name="chevron" size={16} color={TOK.inkMute} />
        </div>
      </div>
      <div style={{ marginTop: 8 }}>
        <div className="pc-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontWeight: 600, fontSize: 14 }}>Week 17 · letter</div>
            <div className="pc-meta">"The Sunday-night spike returns." · Apr 22 – Apr 28</div>
          </div>
          <Icon name="chevron" size={16} color={TOK.inkMute} />
        </div>
      </div>

      <div style={{ height: 12 }} />
    </div>

    <TabBar active="insights" />
  </div>
);

window.InsightsScreen = InsightsScreen;
