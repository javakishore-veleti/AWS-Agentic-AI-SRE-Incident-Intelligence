const scenes: Record<string, { sky: string; left: string; right: string; accent: string }> = {
  FIRSTLIGHT: { sky: "#f6d7b0", left: "#e07a4c", right: "#fff6ea", accent: "#c4552a" },
  DESKHOURS: { sky: "#d7efe8", left: "#d7a15a", right: "#1f6f62", accent: "#f4e2c4" },
  HOSTING: { sky: "#efe2c9", left: "#c9844a", right: "#6e8f71", accent: "#f7f1e6" },
  KITCHEN: { sky: "#f8e3d4", left: "#efcfb4", right: "#7eb8c9", accent: "#e36b45" },
  DINNER: { sky: "#f3d5cf", left: "#f2c14e", right: "#e7b2a2", accent: "#c4524a" },
  READING: { sky: "#e7def8", left: "#f0b429", right: "#6d5bd0", accent: "#fff4d8" },
}

export function OfferArt({ code }: { code: string }) {
  const scene = scenes[code] ?? { sky: "#ffe0cc", left: "#ff8a3d", right: "#fff7f1", accent: "#14b8c9" }
  return (
    <svg viewBox="0 0 320 180" role="img" aria-label="">
      <rect width="320" height="180" fill={scene.sky} />
      <ellipse cx="160" cy="150" rx="110" ry="16" fill="rgba(90,40,20,0.08)" />
      <rect x="46" y="78" width="70" height="62" rx="18" fill={scene.left} />
      <rect x="118" y="48" width="36" height="92" rx="16" fill={scene.accent} />
      <circle cx="214" cy="92" r="36" fill={scene.right} />
      <rect x="196" y="92" width="36" height="48" rx="8" fill={scene.right} />
      <path d="M70 78c8-24 28-36 48-28" stroke="white" strokeWidth="6" fill="none" strokeLinecap="round" opacity="0.7" />
    </svg>
  )
}
