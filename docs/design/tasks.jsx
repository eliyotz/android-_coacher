// tasks.jsx — Tasks tab. Google Tasks integration, overdue tasks, escalation states.

const TaskCard = ({ title, due, dueLabel, overdue, notes, state, delays }) => {
  const stateLabel = {
    'working':    { txt: 'you\'re on it',           color: TOK.sage },
    'delayed':    { txt: 'delayed by coach',        color: TOK.gold },
    'prompted':   { txt: 'waiting on you',          color: TOK.accent },
    'dismissed':  { txt: 'punished · still due',    color: TOK.alert },
    'judged_done':{ txt: 'marked done',             color: TOK.inkMute },
  }[state] || { txt: '', color: TOK.inkMute };

  return (
    <div className="pc-card" style={{ padding: '14px 16px' }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <div style={{
          width: 18, height: 18, borderRadius: 5,
          border: `1.5px solid ${overdue ? TOK.alert : TOK.rule}`,
          marginTop: 2, flexShrink: 0,
          background: state === 'judged_done' ? TOK.sage : 'transparent',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          {state === 'judged_done' && <Icon name="check" size={12} color={TOK.card} />}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontWeight: 600, fontSize: 14.5, color: state === 'judged_done' ? TOK.inkMute : TOK.ink,
            textDecoration: state === 'judged_done' ? 'line-through' : 'none' }}>
            {title}
          </div>
          <div style={{ marginTop: 3, display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <span className="pc-meta" style={{
              color: overdue ? TOK.alert : TOK.inkMute,
              fontFamily: 'Geist Mono, monospace',
              display: 'inline-flex', alignItems: 'center', gap: 4,
            }}>
              {overdue && <Icon name="overdue" size={11} color={TOK.alert} />}
              {dueLabel}
            </span>
            {stateLabel.txt && (
              <span className="pc-meta" style={{ color: stateLabel.color }}>
                · {stateLabel.txt}
              </span>
            )}
          </div>
          {notes && (
            <div className="pc-meta" style={{ marginTop: 6, color: TOK.ink2, lineHeight: 1.5 }}>
              {notes}
            </div>
          )}
          {delays > 0 && (
            <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
              <Icon name="eye" size={11} color={TOK.gold} />
              <span className="pc-meta" style={{ color: TOK.gold }}>
                Coach granted {delays} delay{delays > 1 ? 's' : ''} on this.
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const PunishmentBanner = () => (
  <div style={{
    background: '#2A2720',
    color: TOK.paperOnDeep,
    borderRadius: 14,
    padding: '14px 16px',
    display: 'flex',
    alignItems: 'flex-start',
    gap: 12,
  }}>
    <Icon name="lock" size={18} color={TOK.accent} style={{ marginTop: 2 }} />
    <div style={{ flex: 1 }}>
      <div className="pc-eyebrow" style={{ color: TOK.accent, marginBottom: 3 }}>Coach intervention</div>
      <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>
        Instagram + TikTok blocked for 2h 40m.
      </div>
      <div style={{ fontSize: 12.5, color: TOK.paperOnDeepMute, lineHeight: 1.5 }}>
        You ignored "File Q1 expenses" twice today. Block ends when the task is done,
        or at 6:30pm — whichever comes first.
      </div>
    </div>
  </div>
);

const TasksScreen = () => {
  const tasks = [
    { title: 'File Q1 expenses',         dueLabel: '2d overdue',    overdue: true,  state: 'dismissed', delays: 2,
      notes: 'Receipts in Drive → Expenses 2026.' },
    { title: 'Call mom',                  dueLabel: 'due today',     state: 'prompted' },
    { title: 'Renew gym membership',      dueLabel: '1d overdue',    overdue: true, state: 'delayed', delays: 1,
      notes: 'Login: gold@gymapp.com.' },
    { title: 'Draft retrospective notes', dueLabel: 'due in 2d',     state: 'working' },
    { title: 'Book dentist',              dueLabel: 'due in 4d',     state: '' },
    { title: 'Reply to Maya',             dueLabel: 'due yesterday', state: 'judged_done' },
  ];

  return (
    <div className="pc-screen">
      <ScreenHeader
        eyebrow="Tasks · Google"
        title="Open loops."
        right={
          <div className="pc-pill" style={{ borderColor: TOK.ruleSoft }}>
            <span style={{
              width: 6, height: 6, borderRadius: '50%', background: TOK.sage, display: 'inline-block',
            }} />
            <span>eli@gmail.com</span>
          </div>
        }
      />

      <div style={{ padding: '8px 16px 0' }}>
        <PunishmentBanner />
      </div>

      <div style={{ padding: '14px 16px 4px' }}>
        <div className="pc-eyebrow" style={{ padding: '0 4px 6px' }}>
          Overdue · <span style={{ color: TOK.alert }}>2</span>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {tasks.filter(t => t.overdue).map((t, i) => <TaskCard key={i} {...t} />)}
        </div>
      </div>

      <div style={{ padding: '12px 16px 4px' }}>
        <div className="pc-eyebrow" style={{ padding: '0 4px 6px' }}>Today</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {tasks.filter(t => !t.overdue && t.dueLabel.includes('today')).map((t, i) => <TaskCard key={i} {...t} />)}
        </div>
      </div>

      <div style={{ padding: '12px 16px 4px' }}>
        <div className="pc-eyebrow" style={{ padding: '0 4px 6px' }}>Upcoming</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {tasks.filter(t => t.dueLabel.startsWith('due in')).map((t, i) => <TaskCard key={i} {...t} />)}
        </div>
      </div>

      <div style={{ padding: '12px 16px 4px' }}>
        <div className="pc-eyebrow" style={{ padding: '0 4px 6px' }}>Closed today</div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {tasks.filter(t => t.state === 'judged_done').map((t, i) => <TaskCard key={i} {...t} />)}
        </div>
      </div>

      <div style={{ height: 12 }} />
      <TabBar active="tasks" />
    </div>
  );
};

window.TasksScreen = TasksScreen;
