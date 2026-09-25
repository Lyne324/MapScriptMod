-- Run with: /minimap script run demo
Minimap.setIcon("spike", 0, 0, "spike", {
  scale = 1.2,
  visibleTo = "all"
})

Minimap.drawShape("arena", "rect", {
  { x = -32, z = -32 },
  { x = 32, z = -32 },
  { x = 32, z = 32 },
  { x = -32, z = 32 },
  { x = -32, z = -32 }
}, {
  color = "#55AAFF",
  lineWidth = 2,
  visibleTo = "all"
})
