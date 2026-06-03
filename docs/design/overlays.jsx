// overlays.jsx — three intervention overlays + focus mode activity.
// Mindfulness pause (full-screen calm), AI negotiation (bottom sheet w/ keyboard),
// Hard lock (full-screen, dark, no escape), Focus mode activity (full-screen picker).

// ─────────────────────────────────────────────────────────────
// Mindfulness pause — soft, full-bleed breathing screen
// ─────────────────────────────────────────────────────────────
const MindfulnessOverlay = () => (
  <div className="pc-screen" style={{
    background: '#1A2421',
    color: TOK.paperOnDeep,
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
    position: 'relative',
  }}>
    <div style={{ padding: '20px 24px 0', display: 'flex', justifyContent: 'space-between' }}>
      <div className="pc-eyebrow" style={{ color: '#7C8F6E' }}>Pause</div>
      <div className="pc-mono" style={{ fontSize: 11, color: '#7C8F6E' }}>opening · Instagram</div>
    </div>

    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '40px 32px', position: 'relative' }}>
      {/* Concentric breathing rings */}
      <div style={{ position: 'relative', width: 260, height: 260, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {[1, 0.78, 0.56, 0.34].map((s, i) => (
          <div key={i} style={{
            position: 'absolute',
            width: 260 * s, height: 260 * s,
            borderRadius: '50%',
            border: `1px solid rgba(124,143,110,${0.15 + (1-s) * 0.25})`,
          }} />
        ))}
        <div style={{
          width: 92, height: 92, borderRadius: '50%',
          background: '#2D3A33',
          border: '1px solid rgba(124,143,110,0.45)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <span className="pc-serif" style={{ fontSize: 52, color: '#E6EFE0' }}>3</span>
        </div>
      </div>

      <div className="pc-serif" style={{
        marginTop: 48, fontSize: 26, lineHeight: 1.3, textAlign: 'center', maxWidth: 280, fontStyle: 'italic',
      }}>
        Breathe in, then ask: what am I actually looking for here?
      </div>

      <div className="pc-meta" style={{ marginTop: 16, color: '#9CAA8C', textAlign: 'center' }}>
        3 seconds left · 28 opens today
      </div>
    </div>

    <div style={{ padding: '0 24px 28px' }}>
      <div style={{
        width: '100%', height: 3, borderRadius: 2,
        background: 'rgba(124,143,110,0.2)',
        overflow: 'hidden',
      }}>
        <div style={{ width: '60%', height: '100%', background: '#7C8F6E', borderRadius: 2 }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12, fontSize: 11, color: '#7C8F6E', fontFamily: 'Geist Mono, monospace' }}>
        <span>auto-continues</span>
        <span>tap & hold to skip</span>
      </div>
    </div>
  </div>
);

// ─────────────────────────────────────────────────────────────
// AI Negotiation — bottom sheet, phone visible above, keyboard
// ─────────────────────────────────────────────────────────────
const NegotiationOverlay = ({ phase = 'typing' }) => {
  // phase: 'typing' | 'judging' | 'accept' | 'reject'
  return (
    <div className="pc-screen" style={{
      background: 'rgba(20,19,15,0.62)',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'flex-end',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Faint Instagram-ish app behind */}
      <div style={{
        position: 'absolute', inset: 0,
        backgroundImage: 'linear-gradient(180deg, #C25A33 0%, #1A1916 60%)',
        opacity: 0.18,
        pointerEvents: 'none',
      }}/>

      <div style={{ flex: 1, display: 'flex', alignItems: 'flex-start', justifyContent: 'center', padding: '24px 16px 0' }}>
        <div className="pc-pill" style={{
          background: 'rgba(20,19,15,0.72)',
          color: TOK.paperOnDeep,
          border: '1px solid rgba(241,236,222,0.18)',
          backdropFilter: 'blur(8px)',
        }}>
          <Icon name="lock" size={12} color={TOK.accent} />
          <span style={{ fontWeight: 500 }}>Instagram · over 30m cap</span>
        </div>
      </div>

      {/* sheet */}
      <div style={{
        background: TOK.card,
        borderTopLeftRadius: 24,
        borderTopRightRadius: 24,
        padding: '14px 20px 18px',
        boxShadow: '0 -8px 32px rgba(0,0,0,0.25)',
      }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: TOK.rule, margin: '0 auto 14px' }} />

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <div style={{
            width: 28, height: 28, borderRadius: '50%', background: TOK.ink,
            color: TOK.paper, display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name="eye" size={14} />
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 14 }}>The coach is listening.</div>
            <div className="pc-meta">Tell it why you need more time.</div>
          </div>
        </div>

        {phase === 'reject' && (
          <div style={{
            padding: '12px 14px',
            background: '#F1D6CC55',
            border: `1px solid ${TOK.alertSoft}`,
            borderRadius: 12,
            marginBottom: 12,
          }}>
            <div className="pc-eyebrow" style={{ color: TOK.alert, marginBottom: 4 }}>Verdict · denied</div>
            <div className="pc-serif" style={{ fontSize: 16, lineHeight: 1.4, fontStyle: 'italic' }}>
              "You told me the same thing on Friday at 11pm.
              The pattern is the answer. Try again at 5pm."
            </div>
          </div>
        )}

        {phase === 'accept' && (
          <div style={{
            padding: '12px 14px',
            background: '#E5ECDB',
            border: `1px solid #C7D4B5`,
            borderRadius: 12,
            marginBottom: 12,
          }}>
            <div className="pc-eyebrow" style={{ color: TOK.sage, marginBottom: 4 }}>Verdict · 10 more minutes</div>
            <div className="pc-serif" style={{ fontSize: 16, lineHeight: 1.4, fontStyle: 'italic' }}>
              "Fair. Reply to the DM, then put it down. I'll re-prompt at 11:30."
            </div>
          </div>
        )}

        <div style={{
          border: `1px solid ${TOK.rule}`,
          borderRadius: 14,
          padding: '12px 14px',
          background: TOK.card2,
          minHeight: 96,
          display: 'flex', flexDirection: 'column',
        }}>
          <div style={{ fontSize: 14.5, lineHeight: 1.5, color: TOK.ink, flex: 1 }}>
            I need to reply to a DM from work about tomorrow's
            standup<span style={{ color: TOK.accent, borderLeft: `2px solid ${TOK.accent}`, marginLeft: 1, animation: 'blink 1s steps(1) infinite' }}>&nbsp;</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 10 }}>
            <span className="pc-meta">62 / 280</span>
            <button className="pc-btn" style={{ padding: '8px 14px', fontSize: 13, background: TOK.accent, borderColor: TOK.accent, color: TOK.card }}>
              {phase === 'judging' ? 'Thinking…' : 'Send to coach'}
              {phase !== 'judging' && <Icon name="send" size={13} />}
            </button>
          </div>
        </div>

        <div className="pc-meta" style={{ marginTop: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
          <Icon name="bell" size={11} color={TOK.inkMute} />
          Coach has memory. It's seen your last 5 reasons today.
        </div>
      </div>

      {/* fake keyboard strip */}
      <FakeKeyboard />
    </div>
  );
};

const FakeKeyboard = () => {
  const k = (l, w = 1) => (
    <div style={{
      flex: w, height: 38, borderRadius: 6,
      background: '#FFFFFF',
      boxShadow: '0 1px 0 rgba(0,0,0,.08)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: 'Roboto, system-ui, sans-serif', fontSize: 15, color: '#1A1916',
    }}>{l}</div>
  );
  const row = (keys) => (
    <div style={{ display: 'flex', gap: 5 }}>{keys.map((x, i) => <React.Fragment key={i}>{x}</React.Fragment>)}</div>
  );
  return (
    <div style={{ background: '#D5D2CB', padding: '8px 6px', display: 'flex', flexDirection: 'column', gap: 6 }}>
      {row('qwertyuiop'.split('').map(c => k(c)))}
      <div style={{ padding: '0 14px' }}>{row('asdfghjkl'.split('').map(c => k(c)))}</div>
      {row([k('⇧', 1.3), ...'zxcvbnm'.split('').map(c => k(c)), k('⌫', 1.3)])}
      {row([k('?123', 1.4), k(',', 1), k('space', 4.5), k('.', 1), k('↵', 1.4)])}
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Hard lock — Ulysses contract enforced
// ─────────────────────────────────────────────────────────────
const HardLockOverlay = () => (
  <div className="pc-screen dark" style={{
    background: TOK.inkDeep,
    color: TOK.paperOnDeep,
    display: 'flex', flexDirection: 'column',
    padding: '24px 28px 32px',
  }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <div className="pc-eyebrow" style={{ color: TOK.accent }}>Hard lock · TikTok</div>
      <Icon name="lock" size={18} color={TOK.accent} />
    </div>

    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', maxWidth: 320 }}>
      <div className="pc-serif" style={{ fontSize: 28, lineHeight: 1.15, marginBottom: 18, maxWidth: 280 }}>
        You signed a contract with yourself.
      </div>

      <div style={{ fontSize: 14.5, lineHeight: 1.65, color: TOK.paperOnDeepMute }}>
        On <span className="pc-mono" style={{ color: TOK.paperOnDeep }}>Sun, May 4 · 21:14</span> you
        enabled hard-lock on TikTok at a 45-minute daily cap.
        You're at <span className="pc-mono" style={{ color: TOK.accent }}>47 min</span> today.
        <br /><br />
        No negotiation. No extension. The lock disables 24 hours after you
        toggle it off — and the toggle's in <em>Limits</em>.
      </div>
    </div>

    <div>
      <div style={{
        padding: '14px 16px',
        background: TOK.inkDeep2,
        border: `1px solid ${TOK.inkDeep3}`,
        borderRadius: 14,
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <div style={{ flex: 1 }}>
          <div className="pc-eyebrow" style={{ color: TOK.paperOnDeepMute }}>Lock ends</div>
          <div className="pc-mono" style={{ fontSize: 22, marginTop: 2 }}>tomorrow · 00:00</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="pc-eyebrow" style={{ color: TOK.paperOnDeepMute }}>Streak protected</div>
          <div className="pc-serif" style={{ fontSize: 22, marginTop: 2 }}>4 days</div>
        </div>
      </div>

      <button className="pc-btn" style={{
        marginTop: 14, width: '100%',
        background: TOK.accent, borderColor: TOK.accent, color: TOK.card,
      }}>
        <Icon name="home" size={14} /> Take me home
      </button>
      <div className="pc-meta" style={{ marginTop: 10, textAlign: 'center', color: TOK.paperOnDeepMute }}>
        Pressing back also goes home. Same destination.
      </div>
    </div>
  </div>
);

// ─────────────────────────────────────────────────────────────
// Focus mode — full-screen activity for picking duration
// ─────────────────────────────────────────────────────────────
const FocusDuration = ({ mins, label, sub }) => (
  <div className="pc-card" style={{
    padding: '16px 18px',
    display: 'flex', alignItems: 'center', gap: 14,
    cursor: 'pointer',
  }}>
    <div style={{
      width: 56, height: 56, borderRadius: 14,
      background: TOK.card2,
      border: `1px solid ${TOK.rule}`,
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
    }}>
      <span className="pc-serif" style={{ fontSize: 22, lineHeight: 1 }}>{mins}</span>
      <span className="pc-meta" style={{ fontSize: 9, letterSpacing: 1, marginTop: 2 }}>MIN</span>
    </div>
    <div style={{ flex: 1 }}>
      <div style={{ fontWeight: 600, fontSize: 15 }}>{label}</div>
      <div className="pc-meta" style={{ marginTop: 2 }}>{sub}</div>
    </div>
    <Icon name="chevron" size={16} color={TOK.inkMute} />
  </div>
);

const FocusModeScreen = () => (
  <div className="pc-screen">
    <div style={{ padding: '18px 20px 8px', display: 'flex', alignItems: 'center', gap: 14 }}>
      <Icon name="back" size={20} />
      <div className="pc-eyebrow">Focus mode</div>
    </div>

    <div style={{ padding: '8px 20px 0' }}>
      <div className="pc-serif" style={{ fontSize: 30, lineHeight: 1.1, marginBottom: 10 }}>
        Hard-lock every flagged app.
      </div>
      <div className="pc-body" style={{ color: TOK.ink2, maxWidth: 320 }}>
        No mindfulness pause. No AI negotiation. Until the timer ends,
        the answer is no.
      </div>
    </div>

    <div style={{ padding: '24px 20px 0', display: 'flex', flexDirection: 'column', gap: 10 }}>
      <FocusDuration mins={30} label="Quick reset" sub="Lunch break, walk, single deep task" />
      <FocusDuration mins={60} label="One hour" sub="Default for most sessions" />
      <FocusDuration mins={120} label="Half a workday" sub="2 hours" />
    </div>

    <div style={{ padding: '24px 20px 0' }}>
      <div className="pc-eyebrow" style={{ marginBottom: 8 }}>Custom</div>
      <div className="pc-card" style={{
        padding: '12px 16px',
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <Icon name="focus" size={18} />
        <input
          defaultValue="45"
          style={{
            flex: 1, border: 'none', background: 'transparent',
            fontFamily: 'Geist Mono, monospace', fontSize: 22, color: TOK.ink, outline: 'none',
            minWidth: 0,
          }}
        />
        <span className="pc-meta">minutes</span>
        <button className="pc-btn accent" style={{ padding: '8px 14px', fontSize: 13 }}>Start</button>
      </div>
    </div>

    <div style={{ padding: '28px 20px 0' }}>
      <div className="pc-card" style={{
        padding: '14px 16px', background: TOK.card2,
        display: 'flex', alignItems: 'flex-start', gap: 10,
      }}>
        <Icon name="bell" size={14} style={{ marginTop: 2 }} color={TOK.inkMute} />
        <div className="pc-meta" style={{ lineHeight: 1.55 }}>
          Persistent notification will show while focus is active.
          You can end it from the quick settings tile or this screen.
        </div>
      </div>
    </div>

    <div style={{ height: 32 }} />
  </div>
);

Object.assign(window, { MindfulnessOverlay, NegotiationOverlay, HardLockOverlay, FocusModeScreen });
