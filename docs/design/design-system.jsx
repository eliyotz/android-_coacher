// design-system.jsx — shared tokens, icons, and tiny atoms used across screens.

const TOK = {
  paper: '#EFE9DC',
  paper2: '#E8E1D0',
  card: '#FBF7EE',
  card2: '#F5EFE0',
  ink: '#1A1916',
  ink2: '#34302A',
  inkMute: '#7A7466',
  rule: '#D8D1BD',
  ruleSoft: '#E5DECC',
  accent: '#C25A33',
  accent2: '#A24923',
  sage: '#6E7F5E',
  gold: '#C58A2E',
  alert: '#A52E1A',
  alertSoft: '#F1D6CC',
  inkDeep: '#14130F',
  inkDeep2: '#1F1D17',
  inkDeep3: '#2A2720',
  paperOnDeep: '#F1ECDE',
  paperOnDeepMute: '#A39C8B',
};

// ─────────────────────────────────────────────────────────────
// Inline-SVG icon set. 24×24 stroke icons, currentColor.
// ─────────────────────────────────────────────────────────────
const Icon = ({ name, size = 18, color, style }) => {
  const s = size;
  const c = color || 'currentColor';
  const stroke = { fill: 'none', stroke: c, strokeWidth: 1.6, strokeLinecap: 'round', strokeLinejoin: 'round' };
  const fill = { fill: c };
  const svgProps = { width: s, height: s, viewBox: '0 0 24 24', style: { display: 'block', ...style } };

  switch (name) {
    case 'today':    return <svg {...svgProps}><rect x="4" y="5" width="16" height="15" rx="2" {...stroke}/><path d="M4 9h16M8 3v4M16 3v4" {...stroke}/></svg>;
    case 'tasks':    return <svg {...svgProps}><path d="M5 12l4 4 10-10" {...stroke}/><path d="M5 19h14" {...stroke}/></svg>;
    case 'insights': return <svg {...svgProps}><path d="M4 19V9m6 10V5m6 14v-7" {...stroke}/></svg>;
    case 'limits':   return <svg {...svgProps}><circle cx="12" cy="12" r="8" {...stroke}/><path d="M12 8v4l3 2" {...stroke}/></svg>;
    case 'settings': return <svg {...svgProps}><circle cx="12" cy="12" r="2.5" {...stroke}/><path d="M19 13.3v-2.6l1.7-1.1-1.8-3.1-2 .6-2.3-1.3L14 4h-4l-.6 1.8-2.3 1.3-2-.6-1.8 3.1L5 10.7v2.6L3.3 14.4l1.8 3.1 2-.6 2.3 1.3L10 20h4l.6-1.8 2.3-1.3 2 .6 1.8-3.1L19 13.3z" {...stroke}/></svg>;
    case 'streak':   return <svg {...svgProps}><path d="M12 3c1.5 3 4 4 4 7a4 4 0 1 1-8 0c0-1.5.5-2.5 1.5-3.5C10 8 11 6 12 3z" {...stroke}/></svg>;
    case 'arrowUp':  return <svg {...svgProps}><path d="M12 19V5M6 11l6-6 6 6" {...stroke}/></svg>;
    case 'arrowDown':return <svg {...svgProps}><path d="M12 5v14M6 13l6 6 6-6" {...stroke}/></svg>;
    case 'plus':     return <svg {...svgProps}><path d="M12 5v14M5 12h14" {...stroke}/></svg>;
    case 'check':    return <svg {...svgProps}><path d="M5 12l4 4 10-10" {...stroke}/></svg>;
    case 'x':        return <svg {...svgProps}><path d="M6 6l12 12M18 6L6 18" {...stroke}/></svg>;
    case 'chevron':  return <svg {...svgProps}><path d="M9 6l6 6-6 6" {...stroke}/></svg>;
    case 'chevronD': return <svg {...svgProps}><path d="M6 9l6 6 6-6" {...stroke}/></svg>;
    case 'search':   return <svg {...svgProps}><circle cx="11" cy="11" r="6" {...stroke}/><path d="M16 16l4 4" {...stroke}/></svg>;
    case 'bell':     return <svg {...svgProps}><path d="M6 16h12l-1.5-2V11a4.5 4.5 0 0 0-9 0v3L6 16zM10 19a2 2 0 0 0 4 0" {...stroke}/></svg>;
    case 'lock':     return <svg {...svgProps}><rect x="5" y="11" width="14" height="9" rx="2" {...stroke}/><path d="M8 11V8a4 4 0 0 1 8 0v3" {...stroke}/></svg>;
    case 'pause':    return <svg {...svgProps}><rect x="7" y="5" width="3" height="14" rx="1" {...fill}/><rect x="14" y="5" width="3" height="14" rx="1" {...fill}/></svg>;
    case 'play':     return <svg {...svgProps}><path d="M7 5l12 7-12 7V5z" {...fill}/></svg>;
    case 'send':     return <svg {...svgProps}><path d="M4 12l16-8-7 18-2-8-7-2z" {...stroke}/></svg>;
    case 'eye':      return <svg {...svgProps}><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" {...stroke}/><circle cx="12" cy="12" r="3" {...stroke}/></svg>;
    case 'home':     return <svg {...svgProps}><path d="M4 11l8-7 8 7v9a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1v-9z" {...stroke}/></svg>;
    case 'leaf':     return <svg {...svgProps}><path d="M5 19c0-9 6-14 14-14 0 9-5 14-14 14zM5 19l6-6" {...stroke}/></svg>;
    case 'spark':    return <svg {...svgProps}><path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M6 18l2.5-2.5M15.5 8.5L18 6" {...stroke}/></svg>;
    case 'focus':    return <svg {...svgProps}><circle cx="12" cy="12" r="3" {...stroke}/><path d="M3 12h3M18 12h3M12 3v3M12 18v3" {...stroke}/></svg>;
    case 'overdue':  return <svg {...svgProps}><circle cx="12" cy="12" r="8" {...stroke}/><path d="M12 8v5M12 16v.5" {...stroke}/></svg>;
    case 'dot':      return <svg {...svgProps}><circle cx="12" cy="12" r="3" {...fill}/></svg>;
    case 'kebab':    return <svg {...svgProps}><circle cx="12" cy="6"  r="1.4" {...fill}/><circle cx="12" cy="12" r="1.4" {...fill}/><circle cx="12" cy="18" r="1.4" {...fill}/></svg>;
    case 'back':     return <svg {...svgProps}><path d="M15 6l-6 6 6 6" {...stroke}/></svg>;
    default: return null;
  }
};

// ─────────────────────────────────────────────────────────────
// Bottom tab bar (matches existing nav: Today/Tasks/Insights/Limits/Settings)
// ─────────────────────────────────────────────────────────────
const TabBar = ({ active = 'today' }) => {
  const tabs = [
    { id: 'today',    label: 'Today',    icon: 'today' },
    { id: 'tasks',    label: 'Tasks',    icon: 'tasks' },
    { id: 'insights', label: 'Insights', icon: 'insights' },
    { id: 'limits',   label: 'Limits',   icon: 'limits' },
    { id: 'settings', label: 'Settings', icon: 'settings' },
  ];
  return (
    <div className="pc-tabbar">
      {tabs.map(t => (
        <div key={t.id} className={'tab' + (t.id === active ? ' active' : '')}>
          <Icon name={t.icon} size={20} />
          <span>{t.label}</span>
          <span className="dot" />
        </div>
      ))}
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// Screen header (mimics Material top app bar but in our voice)
// ─────────────────────────────────────────────────────────────
const ScreenHeader = ({ eyebrow, title, right }) => (
  <div style={{
    padding: '14px 20px 6px',
    display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 12,
  }}>
    <div style={{ minWidth: 0 }}>
      {eyebrow && <div className="pc-eyebrow" style={{ marginBottom: 4, whiteSpace: 'nowrap' }}>{eyebrow}</div>}
      <div className="pc-h2" style={{ whiteSpace: 'nowrap' }}>{title}</div>
    </div>
    <div style={{ flexShrink: 0 }}>{right}</div>
  </div>
);

// ─────────────────────────────────────────────────────────────
// Small reusable bits
// ─────────────────────────────────────────────────────────────
const Pill = ({ icon, children, color, bg, border }) => (
  <span className="pc-pill" style={{ color, background: bg, borderColor: border }}>
    {icon && <Icon name={icon} size={12} />}
    {children}
  </span>
);

const UsageBar = ({ used, cap, lock }) => {
  const pct = Math.min(used / Math.max(cap, 1), 1);
  const over = used > cap;
  return (
    <div className="pc-bar-track">
      <div
        className={'pc-bar-fill' + (lock ? ' lock' : over ? ' over' : '')}
        style={{ width: `${pct * 100}%` }}
      />
    </div>
  );
};

const Switch = ({ on, accent = TOK.accent }) => (
  <div style={{
    width: 36, height: 20, borderRadius: 999,
    background: on ? accent : TOK.rule,
    position: 'relative',
    transition: 'background .2s',
    flexShrink: 0,
  }}>
    <div style={{
      position: 'absolute',
      top: 2, left: on ? 18 : 2,
      width: 16, height: 16, borderRadius: '50%',
      background: TOK.card,
      transition: 'left .2s',
      boxShadow: '0 1px 2px rgba(0,0,0,.18)',
    }} />
  </div>
);

const Divider = ({ color = TOK.ruleSoft, vert }) => (
  <div style={vert
    ? { width: 1, alignSelf: 'stretch', background: color }
    : { height: 1, background: color, width: '100%' }
  }/>
);

Object.assign(window, { TOK, Icon, TabBar, ScreenHeader, Pill, UsageBar, Switch, Divider });
