// CloudFront Function (viewer request) for the default (S3) behavior.
// Client-side routes load their app's index.html; real files (/assets/*.js,
// /images/*.webp, /admin/assets/*) pass through. Attach it only to the default
// behavior, not to /api/*, so API 404s stay API 404s.
function handler(event) {
  var request = event.request;
  var uri = request.uri;
  if (uri === '/admin' || (uri.indexOf('/admin/') === 0 && uri.indexOf('.') === -1)) {
    request.uri = '/admin/index.html';
  } else if (uri.indexOf('.') === -1) {
    request.uri = '/index.html';
  }
  return request;
}
