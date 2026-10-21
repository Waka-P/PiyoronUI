// Kotlin のテストで比べる入力(プリセット、極端な色、上書きあり)

// 入力の並びを作る
export function fixtureCases(t) {
  const extra = ['#000000', '#FFFFFF', '#808080', '#FF0000', '#FFFF00', '#00C853', '#0000FF', '#7FD8D0', '#5B3A29'];
  return [
    ...Object.values(t.presets),
    ...extra.map((seed) => ({ seed, overrides: {} })),
    { seed: '#2293C7', overrides: { danger: '#B00020', ok: '#00875A', warn: '#B35C00' } },
  ];
}
