import base64
import datetime
import hmac
import urllib.parse

from flask import request, abort, current_app


def rfc5849_encode(inp):
    # Supplied by ChatGPT
    return urllib.parse.quote(inp, safe='')


def compute_signature(method, url, data, client_key: str, token_key: str = '') -> str:
    normalized = sorted([(rfc5849_encode(k), rfc5849_encode(v)) for k, v in data if k != 'oauth_signature'])
    serialized = "&".join([f"{k}={v}" for k, v in normalized])
    serialized = rfc5849_encode(method.upper()) + "&" + rfc5849_encode(url) + "&" + rfc5849_encode(serialized)
    key = rfc5849_encode(client_key) + '&' + rfc5849_encode(token_key)
    digest = hmac.digest(key.encode(), serialized.encode(), 'sha1')
    return base64.b64encode(digest).decode()


def validate_signature(client_keys, now: int = None):
    # TODO nonce validation -> I think that'd require accessing the DB which I am reluctant to do. ChatGPT suggests redis, but that is yet another dependency.
    key = client_keys[request.form['oauth_consumer_key']]
    if request.headers['content-type'] != 'application/x-www-form-urlencoded':
        abort(400)
    if request.form.get("oauth_version", "1.0") != "1.0":
        current_app.logger.error("Unsupported OAuth version")
        abort(400)
    if request.form["oauth_signature_method"] != "HMAC-SHA1":
        current_app.logger.error("Unsupported OAuth signature method")
        abort(400)
    timestamp = int(request.form["oauth_timestamp"])
    if now is None:
        now = datetime.datetime.now(datetime.timezone.utc).timestamp()
    lifetime = current_app.config.get("OAUTH_SIGNATURE_LIFETIME", None)
    if lifetime is not None and abs(timestamp - now) > lifetime:
        abort(400)
    # This is tailored to the needs of LTI and thus parsing Authorization header is not necessary
    computed = compute_signature(request.method, request.base_url, request.form.items(), key)
    supplied = request.form["oauth_signature"]
    if computed != supplied:
        abort(401)
