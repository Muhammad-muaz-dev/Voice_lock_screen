const fs = require("fs");
const t = fs.readFileSync("c:/Users/TF/Downloads/Voice Lock_ full app flow.html", "utf8");
const caps = [...t.matchAll(/<div class="cap">([\s\S]*?)<\/div>/g)].map((m) =>
  m[1].replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim()
);
fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_caps.txt",
  caps.map((c, i) => i + 1 + ". " + c).join("\n")
);
const arias = [...t.matchAll(/aria-label="([^"]+)"/g)].map((m) => m[1]);
fs.writeFileSync(
  "c:/Users/TF/AndroidStudioProjects/Voice-Lock-Screen/_design_arias.txt",
  arias.join("\n")
);
console.log("caps", caps.length, "arias", arias.length);
