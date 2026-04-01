export default {
  async fetch(request, env) {
    const assetResponse = await env.ASSETS.fetch(request);
    if (assetResponse.status !== 404) {
      return assetResponse;
    }

    if (request.method !== "GET") {
      return assetResponse;
    }

    const accept = request.headers.get("accept") || "";
    const expectsHtml = accept.includes("text/html");
    if (!expectsHtml) {
      return assetResponse;
    }

    const url = new URL(request.url);
    const fallbackRequest = new Request(new URL("/index.html", url), request);
    return env.ASSETS.fetch(fallbackRequest);
  }
};