// app-icon.jsx — eye-mindful adaptive icon mark.

const PCAppIcon = ({ size = 192, withGrid = false, rounded = 0.222 }) => {
  // Compose the foreground in a 108×108 viewBox to match Android adaptive icon spec.
  // Safe zone is ~66×66 centered. Background is a circle gradient on paper-warm.
  const id = 'pc' + Math.floor(Math.random() * 99999);
  return (
    <div style={{ position: 'relative', width: size, height: size, borderRadius: size * rounded, overflow: 'hidden' }}>
      <svg viewBox="0 0 108 108" width={size} height={size} style={{ display: 'block' }}>
        <defs>
          <radialGradient id={id + 'bg'} cx="50%" cy="40%" r="70%">
            <stop offset="0" stopColor="#F5EFE0" />
            <stop offset="0.55" stopColor="#EFE9DC" />
            <stop offset="1" stopColor="#D9CFB6" />
          </radialGradient>
          <linearGradient id={id + 'iris'} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#C25A33" />
            <stop offset="1" stopColor="#7A2E14" />
          </linearGradient>
        </defs>
        {/* Background fill */}
        <rect x="0" y="0" width="108" height="108" fill={`url(#${id}bg)`} />

        {/* faint horizon lines */}
        <g opacity="0.18" stroke="#1A1916" strokeWidth="0.4">
          <line x1="14" y1="62" x2="94" y2="62" />
          <line x1="22" y1="70" x2="86" y2="70" />
          <line x1="30" y1="78" x2="78" y2="78" />
        </g>

        {/* the eye / mindful aperture — almond shape with leaf-like ends */}
        <g transform="translate(54 50)">
          <path
            d="M -28 0
               C -22 -16, 22 -16, 28 0
               C 22 16, -22 16, -28 0 Z"
            fill="#1A1916"
          />
          {/* inner highlight (eyelid) */}
          <path
            d="M -26 -0.3
               C -20 -14.5, 20 -14.5, 26 -0.3"
            fill="none" stroke="#3D3A33" strokeWidth="0.6"
          />
          {/* iris */}
          <circle cx="0" cy="0" r="11.5" fill={`url(#${id}iris)`} />
          {/* pupil */}
          <circle cx="0" cy="0" r="4.2" fill="#14130F" />
          {/* catchlight */}
          <circle cx="-3.4" cy="-3.4" r="1.7" fill="#F5EFE0" />
          {/* upper lash bracket */}
          <path d="M -27 -1 C -22 -16, 22 -16, 27 -1" fill="none" stroke="#1A1916" strokeWidth="2.4" strokeLinecap="round" />
        </g>

        {/* meditation halo — three faint arcs above the eye */}
        <g transform="translate(54 50)" fill="none" stroke="#C25A33" strokeLinecap="round">
          <path d="M -22 -18 C -12 -28, 12 -28, 22 -18" strokeWidth="1.2" opacity="0.85" />
          <path d="M -16 -24 C -8  -32,  8 -32, 16 -24" strokeWidth="1.0" opacity="0.55" />
          <path d="M -10 -29 C -4  -35,  4 -35, 10 -29" strokeWidth="0.8" opacity="0.30" />
        </g>

        {/* wordmark stripe at bottom */}
        <text x="54" y="92" textAnchor="middle"
          fontFamily="Instrument Serif, Georgia, serif" fontStyle="italic" fontSize="9.5"
          fill="#34302A" letterSpacing="2">
          c · o · a · c · h
        </text>

        {/* safe zone grid (debug) */}
        {withGrid && (
          <g fill="none" stroke="#C25A33" strokeDasharray="2 2" opacity="0.5">
            <circle cx="54" cy="54" r="33" />
            <rect x="21" y="21" width="66" height="66" />
          </g>
        )}
      </svg>
    </div>
  );
};

// Variants (monochrome, dark theme, themed icon API)
const PCAppIconMono = ({ size = 192 }) => (
  <div style={{ width: size, height: size, borderRadius: size * 0.222, background: '#1A1916', overflow: 'hidden', display:'flex', alignItems:'center', justifyContent:'center' }}>
    <svg viewBox="0 0 108 108" width={size} height={size}>
      <g transform="translate(54 50)" fill="none" stroke="#F5EFE0" strokeLinecap="round">
        <path d="M -28 0 C -22 -16, 22 -16, 28 0 C 22 16, -22 16, -28 0 Z" strokeWidth="2.4" />
        <circle cx="0" cy="0" r="11.5" strokeWidth="2.2" />
        <circle cx="0" cy="0" r="4.2" fill="#F5EFE0" />
        <path d="M -22 -18 C -12 -28, 12 -28, 22 -18" strokeWidth="1.4" opacity="0.85" />
        <path d="M -16 -24 C -8 -32, 8 -32, 16 -24" strokeWidth="1.0" opacity="0.55" />
      </g>
    </svg>
  </div>
);

const PCNotificationIcon = ({ size = 64 }) => (
  <svg viewBox="0 0 24 24" width={size} height={size}>
    <g fill="none" stroke="#1A1916" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" fill="#1A1916" stroke="none" />
    </g>
  </svg>
);

Object.assign(window, { PCAppIcon, PCAppIconMono, PCNotificationIcon });
