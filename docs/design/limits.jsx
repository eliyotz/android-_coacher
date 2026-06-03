// limits.jsx — Limits tab. Apps vs Categories segmented; flag, cap, hard-lock toggle.

const SegmentedTab = ({ tabs, active }) => (
  <div style={{
    display: 'inline-flex',
    background: TOK.paper2,
    borderRadius: 999,
    padding: 3,
    border: `1px solid ${TOK.ruleSoft}`,
  }}>
    {tabs.map(t => (
      <div key={t} style={{
        padding: '6px 16px',
        borderRadius: 999,
        fontSize: 12.5,
        fontWeight: 500,
        background: t === active ? TOK.ink : 'transparent',
        color: t === active ? TOK.paper : TOK.ink2,
      }}>{t}</div>
    ))}
  </div>
);

const LimitAppCard = ({ name, pkg, flagged, cap, hardLock, lockState, used }) => (
  <div className="pc-card" style={{ padding: '14px 16px' }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <AppGlyph name={name} size={36} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 600, fontSize: 14.5 }}>{name}</div>
        <div className="pc-meta" style={{ fontFamily: 'Geist Mono, monospace', fontSize: 11 }}>{pkg}</div>
      </div>
      <Switch on={flagged} />
    </div>

    {flagged && (
      <div style={{ marginTop: 14, paddingTop: 14, borderTop: `1px dashed ${TOK.rule}` }}>
        <div className="pc-eyebrow" style={{ marginBottom: 6 }}>Daily limit</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            border: `1px solid ${TOK.rule}`,
            background: TOK.card2,
            borderRadius: 10,
            padding: '10px 14px',
            display: 'flex', alignItems: 'baseline', gap: 4,
            flex: 1,
          }}>
            <span className="pc-mono" style={{ fontSize: 22 }}>{cap}</span>
            <span className="pc-meta">minutes / day</span>
          </div>
          {used !== undefined && (
            <div style={{ flex: 1 }}>
              <div className="pc-meta" style={{ marginBottom: 4 }}>Used today</div>
              <UsageBar used={used} cap={cap} lock={hardLock} />
              <div className="pc-meta" style={{ marginTop: 4, fontFamily: 'Geist Mono, monospace' }}>
                {used}m / {cap}m
              </div>
            </div>
          )}
        </div>

        <div style={{
          marginTop: 14,
          padding: '12px 14px',
          borderRadius: 12,
          background: hardLock ? '#F1D6CC55' : TOK.paper2,
          border: `1px solid ${hardLock ? TOK.alertSoft : TOK.ruleSoft}`,
          display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <Icon name="lock" size={16} color={hardLock ? TOK.alert : TOK.inkMute} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 600, fontSize: 13.5, color: hardLock ? TOK.alert : TOK.ink }}>
              Hard lock at limit
            </div>
            <div className="pc-meta" style={{ marginTop: 2 }}>
              {hardLock
                ? <>Can't be turned off for <span className="pc-mono">22h 14m</span>. Ulysses contract.</>
                : lockState === 'cooloff'
                  ? <>Lock still active. Disables in <span className="pc-mono">3h 41m</span>.</>
                  : 'No negotiation. Sends you home when the cap hits.'}
            </div>
          </div>
          <Switch on={hardLock} accent={TOK.alert} />
        </div>
      </div>
    )}
  </div>
);

const LimitsScreen = () => {
  const apps = [
    { name: 'Instagram', pkg: 'com.instagram.android', flagged: true, cap: 30, hardLock: false, used: 47 },
    { name: 'TikTok',    pkg: 'com.zhiliaoapp.musically', flagged: true, cap: 45, hardLock: true, used: 38 },
    { name: 'X',         pkg: 'com.twitter.android', flagged: true, cap: 20, hardLock: false, used: 22 },
    { name: 'Reddit',    pkg: 'com.reddit.frontpage', flagged: true, cap: 30, hardLock: false, lockState: 'cooloff', used: 14 },
    { name: 'YouTube',   pkg: 'com.google.android.youtube', flagged: false },
  ];

  return (
    <div className="pc-screen">
      <ScreenHeader
        eyebrow="Limits"
        title="What you cap."
        right={
          <button className="pc-btn ghost" style={{ padding: '6px 10px', fontSize: 12, border: `1px solid ${TOK.rule}`, borderRadius: 999 }}>
            <Icon name="search" size={14} />
          </button>
        }
      />

      <div style={{ padding: '8px 16px 8px' }}>
        <SegmentedTab tabs={['Apps', 'Categories']} active="Apps" />
      </div>

      {/* Banner: cap recommendation from observe mode */}
      <div style={{ padding: '8px 16px 0' }}>
        <div className="pc-card" style={{
          padding: '14px 16px',
          background: TOK.card2,
          borderColor: TOK.accent,
          borderStyle: 'dashed',
          display: 'flex', alignItems: 'flex-start', gap: 10,
        }}>
          <Icon name="spark" size={16} color={TOK.accent} style={{ marginTop: 2 }} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 600, fontSize: 13.5 }}>Coach has a suggestion.</div>
            <div className="pc-meta" style={{ marginTop: 3, lineHeight: 1.55 }}>
              Based on your last 14 days, a <span className="pc-mono">90-min Social cap</span> covers
              the same behavior as capping Instagram alone — without the whack-a-mole.
            </div>
            <div style={{ display: 'flex', gap: 6, marginTop: 10 }}>
              <button className="pc-btn accent" style={{ padding: '7px 14px', fontSize: 12 }}>Apply</button>
              <button className="pc-btn ghost" style={{ padding: '7px 12px', fontSize: 12, border: `1px solid ${TOK.rule}` }}>Dismiss</button>
            </div>
          </div>
        </div>
      </div>

      <div style={{ padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="pc-eyebrow" style={{ padding: '0 4px' }}>Flagged · {apps.filter(a => a.flagged).length}</div>
        {apps.filter(a => a.flagged).map(a => <LimitAppCard key={a.pkg} {...a} />)}

        <div className="pc-eyebrow" style={{ padding: '12px 4px 0' }}>Not flagged</div>
        {apps.filter(a => !a.flagged).map(a => <LimitAppCard key={a.pkg} {...a} />)}
      </div>

      <div style={{ height: 12 }} />
      <TabBar active="limits" />
    </div>
  );
};

window.LimitsScreen = LimitsScreen;
