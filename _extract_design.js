const fs = require("fs");
const p = "c:/Users/TF/Downloads/Voice Lock_ full app flow.html";
const t = fs.readFileSync(p, "utf8");
console.log("chars", t.length);

fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_head.html",
  t.slice(0, 40000)
);

const classNames = new Map();
for (const m of t.matchAll(/class="([^"]+)"/g)) {
  for (const c of m[1].split(/\s+/)) {
    if (!c) continue;
    classNames.set(c, (classNames.get(c) || 0) + 1);
  }
}
const sorted = [...classNames.entries()].sort((a, b) => b[1] - a[1]);
fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_classes.txt",
  sorted.map(([k, v]) => v + "\t" + k).join("\n")
);

const ids = [...t.matchAll(/id="([^"]+)"/g)].map((m) => m[1]);
fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_ids.txt",
  ids.join("\n")
);

const dataScreens = [...t.matchAll(/data-[a-z-]+="([^"]+)"/g)].slice(0, 200);
fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_data.txt",
  dataScreens.map((m) => m[0]).join("\n")
);

const headings = [...t.matchAll(/<(h[1-4]|button|p|span|div)[^>]*>\s*([^<]{2,60})\s*</gi)]
  .map((m) => m[2].trim())
  .filter((s) => s && !s.startsWith("{") && s.length < 50);
const uniqueHead = [...new Set(headings)];
fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_text.txt",
  uniqueHead.join("\n")
);

console.log("ids", ids.length, "classes", classNames.size, "texts", uniqueHead.length);
