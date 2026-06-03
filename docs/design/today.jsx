// today.jsx — Today tab. Adherence ring + day total + categories + apps.

const TodayAdherenceRing = ({ pct = 0.74, size = 168 }) => {
  const r = size / 2 - 14;
  const c = 2 * Math.PI * r;
  const offset = c * (1 - pct);
  const color = pct >= 0.8 ? TOK.sage : pct >= 0.5 ? TOK.gold : TOK.accent;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ display: 'block' }}>
      <circle cx={size/2} cy={size/2} r={r} fill="none" stroke={TOK.ruleSoft} strokeWidth="10" />
      <circle cx={size/2} cy={size/2} r={r}
        fill="none" stroke={color} strokeWidth="10" strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={offset}
        transform={`rotate(-90 ${size/2} ${size/2})`} />
      <text x="50%" y="50%" textAnchor="middle" dominantBaseline="central"
        fontFamily="Instrument Serif, serif" fontSize="48" fill={TOK.ink}>
        {Math.round(pct * 100)}<tspan fontSize="22" dy="-16">%</tspan>
      </text>
    </svg>
  );
};

const formatMin = (m) => m < 60 ? `${m}m` : `${Math.floor(m/60)}h ${m%60 ? (m%60)+'m' : ''}`.trim();

const AppGlyph = ({ name, size = 34 }) => {
  const swatches = {
    'Instagram':  { bg: '#C25A33', label: 'Ig' },
    'TikTok':     { bg: '#1A1916', label: 'Tk' },
    'X':          { bg: '#3D3A33', label: 'X'  },
    'Reddit':     { bg: '#A24923', label: 'Rd' },
    'YouTube':    { bg: '#A52E1A', label: 'Yt' },
    'Slack':      { bg: '#6E7F5E', label: 'Sl' },
    'Gmail':      { bg: '#C58A2E', label: 'Gm' },
    'Chrome':     { bg: '#5E6B7A', label: 'Cr' },
    'WhatsApp':   { bg: '#6E7F5E', label: 'Wa' },
    'Maps':       { bg: '#7A7466', label: 'Mp' },
    'Linkedin':   { bg: '#3D3A33', label: 'In' },
  };
  const s = swatches[name] || { bg: TOK.inkMute, label: name.slice(0,2) };
  return (
    <div style={{
      width: size, height: size, borderRadius: size * 0.3,
      background: s.bg, color: '#FBF7EE',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: 'Instrument Serif, serif', fontSize: size * 0.46,
      flexShrink: 0,
    }}>{s.label}</div>
  );
};

const AppRow = ({ name, mins, opens, cap, delta, lock }) => {
  const over = cap && mins > cap;
  return (
    <div style={{ padding: '11px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <AppGlyph name={name} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
            <span style={{ fontWeight: 600, fontSize: 14.5 }}>{name}</span>
            {lock && <Icon name="lock" size={11} color={TOK.alert} />}
          </div>
          <div className="pc-meta" style={{ marginTop: 1 }}>
            <span className="pc-mono">{formatMin(mins)}</span>
            {cap ? <span> · cap {formatMin(cap)}</span> : null}
            <span> · {opens} opens</span>
          </div>
        </div>
        {delta !== undefined && delta !== 0 && (
          <span className="pc-mono" style={{
            fontSize: 11,
            color: delta > 0 ? TOK.accent : TOK.sage,
            display: 'inline-flex', alignItems: 'center', gap: 2,
          }}>
            <Icon name={delta > 0 ? 'arrowUp' : 'arrowDown'} size={11} />
            {Math.abs(delta)}%
          </span>
        )}
      </div>
      {cap ? <UsageBar used={mins} cap={cap} lock={lock} /> : null}
    </div>
  );
};

const CategoryRow = ({ name, mins, cap, opens }) => {
  const over = mins > cap;
  return (
    <div style={{ padding: '12px 16px' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 8, gap: 8 }}>
        <span style={{ fontWeight: 600, fontSize: 14 }}>{name}</span>
        <span className="pc-mono" style={{ fontSize: 12, color: over ? TOK.accent : TOK.ink2, whiteSpace: 'nowrap' }}>
          {formatMin(mins)} <span style={{ color: TOK.inkMute }}>/ {formatMin(cap)}</span>
        </span>
      </div>
      <UsageBar used={mins} cap={cap} />
      <div className="pc-meta" style={{ marginTop: 6 }}>
        {opens} opens · {over
          ? <span style={{ color: TOK.accent }}>over by {formatMin(mins - cap)}</span>
          : 'on track'}
      </div>
    </div>
  );
};

const TodayScreen = () => {
  const apps = [
    { name: 'Instagram', mins: 47, opens: 28, cap: 30, delta: 18 },
    { name: 'TikTok',    mins: 38, opens: 19, cap: 45, delta: -22, lock: true },
    { name: 'X',         mins: 22, opens: 31, cap: 20, delta: 9 },
    { name: 'Reddit',    mins: 14, opens: 6,  cap: 30, delta: -40 },
    { name: 'YouTube',   mins: 11, opens: 3,  cap: 60, delta: -5 },
    { name: 'Slack',     mins: 28, opens: 14 },
    { name: 'Gmail',     mins: 9,  opens: 11 },
    { name: 'WhatsApp',  mins: 16, opens: 22 },
  ];
  const cats = [
    { name: 'Social',    mins: 107, cap: 90,  opens: 78 },
    { name: 'Video',     mins: 49,  cap: 60,  opens: 22 },
    { name: 'Messaging', mins: 44,  cap: 60,  opens: 47 },
  ];

  return (
    <div className="pc-screen">
      <div style={{ padding: '14px 20px 6px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ minWidth: 0 }}>
          <div className="pc-eyebrow" style={{ marginBottom: 4, whiteSpace: 'nowrap' }}>Tuesday · May 14</div>
          <div className="pc-h2" style={{ whiteSpace: 'nowrap' }}>Morning, Eli.</div>
        </div>
        <div className="pc-pill" style={{ borderColor: TOK.ruleSoft, flexShrink: 0 }}>
          <Icon name="focus" size={12} /> <span style={{fontWeight:500}}>Focus</span>
        </div>
      </div>

      <div style={{ padding: '12px 16px 16px' }}>
        {/* Hero card */}
        <div className="pc-card" style={{ padding: '18px 16px 16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <TodayAdherenceRing pct={0.74} size={132} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="pc-eyebrow" style={{ marginBottom: 4 }}>So far today</div>
              <div className="pc-serif" style={{ fontSize: 36, lineHeight: 1, whiteSpace: 'nowrap' }}>
                3h 22m
              </div>
              <div className="pc-meta" style={{ marginTop: 6 }}>219 opens · goal 2h 30m</div>
              <div style={{ marginTop: 12, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                <Pill icon="streak" color={TOK.accent2}>
                  <span style={{ fontWeight: 600, whiteSpace: 'nowrap' }}>4-day streak</span>
                </Pill>
              </div>
              <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                <Pill><span style={{ whiteSpace: 'nowrap' }}>Worst hour <span className="pc-mono" style={{ marginLeft: 4 }}>22:00</span></span></Pill>
              </div>
            </div>
          </div>
        </div>

        {/* Coach line */}
        <div style={{ padding: '18px 4px 6px', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
          <div style={{
            width: 22, height: 22, borderRadius: '50%', background: TOK.ink, color: TOK.paper,
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <Icon name="eye" size={12} />
          </div>
          <div style={{ flex: 1 }}>
            <div className="pc-eyebrow" style={{ marginBottom: 2 }}>Coach</div>
            <div className="pc-serif" style={{ fontSize: 17, lineHeight: 1.35 }}>
              You're <span style={{ fontStyle: 'italic' }}>17 minutes over</span> on Social,
              and it's only 11am. Tomorrow's report will note this.
            </div>
          </div>
        </div>

        {/* Categories */}
        <div style={{ marginTop: 14 }}>
          <div className="pc-eyebrow" style={{ padding: '0 4px 8px' }}>Categories</div>
          <div className="pc-card" style={{ overflow: 'hidden' }}>
            {cats.map((c, i) => (
              <React.Fragment key={c.name}>
                {i > 0 && <Divider />}
                <CategoryRow {...c} />
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Apps */}
        <div style={{ marginTop: 18 }}>
          <div className="pc-eyebrow" style={{ padding: '0 4px 8px', display:'flex', justifyContent:'space-between' }}>
            <span>Apps · top today</span>
            <span style={{ color: TOK.inkMute }}>{apps.length}</span>
          </div>
          <div className="pc-card" style={{ overflow: 'hidden' }}>
            {apps.map((a, i) => (
              <React.Fragment key={a.name}>
                {i > 0 && <Divider />}
                <AppRow {...a} />
              </React.Fragment>
            ))}
          </div>
        </div>

        <div style={{ height: 12 }} />
      </div>

      <TabBar active="today" />
    </div>
  );
};

window.TodayScreen = TodayScreen;
window.AppGlyph = AppGlyph;
window.formatMin = formatMin;
