module.exports = function apiKeyAuth(req, res, next) {
  const key = (req.header("x-api-key") || "").trim();
  const expected = (process.env.API_KEY || "").trim();

  if (!expected) {
    console.warn("[auth] API_KEY not set in env");
    return res.status(401).json({ ok: false, error: "Unauthorized" });
  }
  if (key !== expected) {
    console.warn("[auth] Bad API key", { gotLen: key.length, expLen: expected.length });
    return res.status(401).json({ ok: false, error: "Unauthorized" });
  }
  next();
};
