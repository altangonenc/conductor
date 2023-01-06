const { createProxyMiddleware } = require("http-proxy-middleware");
const target = process.env.WF_SERVER || "http://localhost:8081";

module.exports = function (app) {
  app.use(
    "/api",
    createProxyMiddleware({
      target: target,
      //pathRewrite: { "^/api/": "/" },
      changeOrigin: true,
    })
  );
};
