#!/bin/bash
set -e

KONG_YML="docker/kong/kong.yml"
KEYCLOAK_HOST="${KEYCLOAK_HOST:-localhost:8081}"
KEYCLOAK_INTERNAL_HOST="${KEYCLOAK_INTERNAL_HOST:-poc-keycloak:8080}"
REALM="${KEYCLOAK_REALM:-poc}"
BACKEND_URL="${BACKEND_URL:-http://host.docker.internal:8080}"
FRONTEND_ORIGINS="${FRONTEND_ORIGINS:-http://localhost:3000,http://localhost:5173}"
FRONTEND_ORIGINS_YAML=$(echo "$FRONTEND_ORIGINS" | sed 's/,/", "/g; s/^/"/; s/$/"/')

# Wait for Keycloak
echo "Waiting for Keycloak at http://$KEYCLOAK_HOST ..."
until curl -s "http://$KEYCLOAK_HOST/realms/$REALM" > /dev/null 2>&1; do sleep 2; done

echo "Extracting RSA public key from JWKS..."
CERT_PEM=$(curl -s "http://$KEYCLOAK_HOST/realms/$REALM/protocol/openid-connect/certs" | python3 -c "
import json, sys, base64
keys = json.load(sys.stdin)
if not keys.get('keys'):
    print('ERROR: No keys in JWKS response', file=sys.stderr)
    sys.exit(1)
jwk = next((k for k in keys['keys'] if k.get('use') == 'sig'), None)
if not jwk:
    print('ERROR: No signing key in JWKS response', file=sys.stderr)
    sys.exit(1)
cert_b64 = jwk['x5c'][0]
cert_pem = '-----BEGIN CERTIFICATE-----\n' + '\n'.join(cert_b64[i:i+64] for i in range(0, len(cert_b64), 64)) + '\n-----END CERTIFICATE-----'
print(cert_pem)
")

RSA_PUB_KEY=$(echo "$CERT_PEM" | openssl x509 -pubkey -noout)
RSA_PUB_KEY_INDENTED=$(echo "$RSA_PUB_KEY" | sed 's/^/          /')

if [ -z "$RSA_PUB_KEY" ]; then
    echo "ERROR: Failed to extract public key"
    exit 1
fi

# Build the post-function Lua for one route.
make_lua() {
    local allowed="$1"
    cat << LUAEOF
local ALLOWED = { $allowed }
local function extract(str, key)
  local _, _, val = str:find('"' .. key .. '":"([^"]*)"')
  return val
end
local function extract_array(str, key)
  local _, _, arr = str:find('"' .. key .. '":%[([^%]]*)%]')
  if arr then return arr:gsub('"', ''):gsub('%s+', '') end
  return ""
end
local auth_header = kong.request.get_header("Authorization")
if auth_header then
  local jwt_str = auth_header:match("^Bearer%s+(.+)$")
  if jwt_str then
    local _, payload_b64, _ = jwt_str:match("^([^.]+)%.([^.]+)%.([^.]+)$")
    if payload_b64 then
      payload_b64 = payload_b64:gsub("-", "+"):gsub("_", "/")
      local pad = 4 - (#payload_b64 % 4)
      if pad < 4 then payload_b64 = payload_b64 .. string.rep("=", pad) end
      local payload = ngx.decode_base64(payload_b64)
      if payload then
        local permissions = {}
        local permitted = false
        local ra_match = payload:match('"resource_access":%{.-"roles":%[([^%]]*)%]')
        if ra_match then
          for perm in (ra_match:gsub('"', ''):gsub('%s+', '') .. ","):gmatch("([^,]*),") do
            if perm ~= "" and not permissions[perm] then
              permissions[perm] = true
              if ALLOWED[perm] then permitted = true end
            end
          end
        end
        if not permitted then
          return kong.response.exit(403, { message = "Forbidden: missing required permission" })
        end
        local permission_list = ""
        for perm in pairs(permissions) do
          if permission_list ~= "" then permission_list = permission_list .. "," end
          permission_list = permission_list .. perm
        end
        local roles = extract_array(payload, "roles")
        kong.service.request.clear_header("Authorization")
        kong.service.request.clear_header("X-User-Id")
        kong.service.request.clear_header("X-User-Roles")
        kong.service.request.clear_header("X-User-Permissions")
        kong.service.request.clear_header("X-User-Name")
        kong.service.request.set_header("X-User-Id", extract(payload, "sub"))
        kong.service.request.set_header("X-User-Roles", roles)
        kong.service.request.set_header("X-User-Permissions", permission_list)
        kong.service.request.set_header("X-User-Name", extract(payload, "preferred_username"))
      end
    end
  end
end
LUAEOF
}

indent_lua() {
    sed 's/^/                  /'
}

USERS_LUA=$(make_lua '["user:read"] = true, ["user:write"] = true' | indent_lua)
NOTIFICATIONS_LUA=$(make_lua '["notification:read"] = true, ["notification:write"] = true' | indent_lua)

echo "Generating $KONG_YML..."

cat > "$KONG_YML" << YMLEOF
_format_version: "3.0"
_transform: true

consumers:
  - username: keycloak-poc
    jwt_secrets:
      - key: "http://${KEYCLOAK_HOST}/realms/${REALM}"
        algorithm: RS256
        rsa_public_key: |
${RSA_PUB_KEY_INDENTED}
      - key: "http://${KEYCLOAK_INTERNAL_HOST}/realms/${REALM}"
        algorithm: RS256
        rsa_public_key: |
${RSA_PUB_KEY_INDENTED}

services:
  - name: poc-api
    url: ${BACKEND_URL}
    routes:
      - name: auth-register
        paths: ["/api/auth/register"]
        strip_path: false
        plugins:
          - name: cors
            config:
              origins: [${FRONTEND_ORIGINS_YAML}]
              methods: [POST, OPTIONS]
              headers: [Content-Type]
              exposed_headers: []
              credentials: true
              preflight_continue: false
              max_age: 3600
      - name: users
        paths: ["/api/users"]
        strip_path: false
        plugins:
          - name: cors
            config:
              origins: [${FRONTEND_ORIGINS_YAML}]
              methods: [GET, HEAD, PUT, PATCH, POST, DELETE, OPTIONS]
              headers: [Authorization, Content-Type]
              exposed_headers: []
              credentials: true
              preflight_continue: false
              max_age: 3600
          - name: jwt
            config:
              claims_to_verify: ["exp"]
              run_on_preflight: false
          - name: post-function
            config:
              access:
                - |
${USERS_LUA}
      - name: notification-templates
        paths: ["/api/notification-templates"]
        strip_path: false
        plugins:
          - name: cors
            config:
              origins: [${FRONTEND_ORIGINS_YAML}]
              methods: [GET, HEAD, PUT, PATCH, POST, DELETE, OPTIONS]
              headers: [Authorization, Content-Type]
              exposed_headers: []
              credentials: true
              preflight_continue: false
              max_age: 3600
          - name: jwt
            config:
              claims_to_verify: ["exp"]
              run_on_preflight: false
          - name: post-function
            config:
              access:
                - |
${NOTIFICATIONS_LUA}
      - name: notifications
        paths: ["/api/notifications"]
        strip_path: false
        plugins:
          - name: cors
            config:
              origins: [${FRONTEND_ORIGINS_YAML}]
              methods: [GET, HEAD, PUT, PATCH, POST, DELETE, OPTIONS]
              headers: [Authorization, Content-Type]
              exposed_headers: []
              credentials: true
              preflight_continue: false
              max_age: 3600
          - name: jwt
            config:
              claims_to_verify: ["exp"]
              run_on_preflight: false
          - name: post-function
            config:
              access:
                - |
${NOTIFICATIONS_LUA}
YMLEOF

echo "Done. Kong config generated with public key from Keycloak."
echo "Keycloak iss: http://${KEYCLOAK_HOST}/realms/${REALM}"
