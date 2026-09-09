#!/bin/sh
set -e

extract_id() {
    grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4 || true
}

echo "Waiting for Keycloak..."
until curl -s http://keycloak:8080/ > /dev/null 2>&1; do sleep 2; done
echo "Keycloak is up"

TOKEN=$(curl -s -X POST http://keycloak:8080/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli" -d "username=admin" -d "password=admin" \
  -d "grant_type=password" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

# --- Realm ---
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" http://keycloak:8080/admin/realms/poc)
if [ "$HTTP_CODE" != "200" ]; then
    echo "Creating realm..."
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d '{"realm":"poc","enabled":true}' http://keycloak:8080/admin/realms > /dev/null
else
    echo "Realm poc already exists"
fi

# --- Client ---
CLIENT_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "http://keycloak:8080/admin/realms/poc/clients?clientId=poc-api")
CLIENT_ID=$(echo "$CLIENT_JSON" | extract_id)

if [ -z "$CLIENT_ID" ]; then
    echo "Creating client..."
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d '{"clientId":"poc-api","secret":"poc-api-secret","directAccessGrantsEnabled":true,"standardFlowEnabled":true,"publicClient":false,"protocol":"openid-connect","redirectUris":["http://localhost:3000/*","http://localhost:3001/*","http://localhost:5173/*"],"webOrigins":["http://localhost:3000","http://localhost:3001","http://localhost:5173"]}' \
      http://keycloak:8080/admin/realms/poc/clients > /dev/null
    CLIENT_ID=$(curl -s -H "Authorization: Bearer $TOKEN" "http://keycloak:8080/admin/realms/poc/clients?clientId=poc-api" | extract_id)
fi

echo "Updating client settings..."
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  "http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID" \
  -d '{"clientId":"poc-api","secret":"poc-api-secret","protocol":"openid-connect","publicClient":false,"standardFlowEnabled":true,"implicitFlowEnabled":false,"directAccessGrantsEnabled":true,"serviceAccountsEnabled":false,"redirectUris":["http://localhost:3000/*","http://localhost:3001/*","http://localhost:5173/*"],"webOrigins":["http://localhost:3000","http://localhost:3001","http://localhost:5173"]}' \
  > /dev/null

# --- Admin User ---
USER_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "http://keycloak:8080/admin/realms/poc/users?username=admin")
UID_ADMIN=$(echo "$USER_JSON" | extract_id)

if [ -z "$UID_ADMIN" ]; then
    echo "Creating admin user..."
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d '{"username":"admin","firstName":"Admin","lastName":"User","email":"admin@poc.local","emailVerified":true,"enabled":true,"requiredActions":[]}' \
      http://keycloak:8080/admin/realms/poc/users > /dev/null
    UID_ADMIN=$(curl -s -H "Authorization: Bearer $TOKEN" "http://keycloak:8080/admin/realms/poc/users?username=admin" | extract_id)

    echo "Setting admin password..."
    curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d '{"type":"password","value":"admin","temporary":false}' \
      "http://keycloak:8080/admin/realms/poc/users/$UID_ADMIN/reset-password" > /dev/null
fi

# --- Roles ---
get_role_id() {
    curl -s -H "Authorization: Bearer $TOKEN" "http://keycloak:8080/admin/realms/poc/roles/$1" \
      | extract_id
}

create_role() {
    if [ -n "$(get_role_id "$1")" ]; then
        echo "Role $1 already exists"
    else
        curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
          -d "{\"name\":\"$1\"}" http://keycloak:8080/admin/realms/poc/roles > /dev/null
        echo "Role $1 created"
    fi
}

get_client_role_id() {
    curl -s -H "Authorization: Bearer $TOKEN" \
      "http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID/roles/$1" \
      | extract_id
}

create_client_role() {
    if [ -n "$(get_client_role_id "$1")" ]; then
        echo "Client role $1 already exists"
    else
        curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
          -d "{\"name\":\"$1\",\"clientRole\":true}" \
          "http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID/roles" > /dev/null
        echo "Client role $1 created"
    fi
}

add_composites() {
    ROLENAME=$1
    shift
    IDS=""
    for entry in "$@"; do
        case "$entry" in
            realm:*) pid=$(get_role_id "${entry#realm:}");;
            client:*) pid=$(get_client_role_id "${entry#client:}");;
            *) echo "ERROR: composite entry must be realm: or client: prefixed: $entry"; exit 1;;
        esac
        IDS="${IDS}{\"id\":\"$pid\",\"name\":\"${entry#*:}\"},"
    done
    IDS="[${IDS%,}]"
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d "$IDS" "http://keycloak:8080/admin/realms/poc/roles/$ROLENAME/composites" > /dev/null
    echo "Composites added to $ROLENAME"
}

echo "Creating realm roles..."
for role in ADMIN USER; do
    create_role "$role"
done

echo "Creating client roles (fine-grained permissions)..."
for role in user:read user:write notification:read notification:write; do
    create_client_role "$role"
done

echo "Wiring role composites..."
add_composites USER client:notification:read client:notification:write
add_composites ADMIN realm:USER client:user:read client:user:write

echo "Assigning ADMIN role to admin user..."
ADMIN_ROLE_ID=$(get_role_id "ADMIN")
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "[{\"id\":\"$ADMIN_ROLE_ID\",\"name\":\"ADMIN\"}]" \
  "http://keycloak:8080/admin/realms/poc/users/$UID_ADMIN/role-mappings/realm" > /dev/null

echo "Adding protocol mapper (realm roles -> top-level 'roles' claim)..."
MAPPER_EXISTS=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID/protocol-mappers/models" | grep -c '"name":"realm-roles"' || true)
if [ "$MAPPER_EXISTS" = "0" ]; then
    curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d '{"name":"realm-roles","protocol":"openid-connect","protocolMapper":"oidc-usermodel-realm-role-mapper","config":{"claim.name":"roles","jsonType.label":"String","multivalued":"true","id.token.claim":"true","access.token.claim":"true","userinfo.token.claim":"true"}}' \
      "http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID/protocol-mappers/models" > /dev/null
    echo "Protocol mapper created"
else
    echo "Protocol mapper already exists"
fi

# --- Remove legacy Authorization Services ---
echo "Removing legacy Authorization Services..."
AUTHZ_BASE="http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID/authz/resource-server"
for id in $(curl -s -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/permission?first=0&max=200" | grep -o '"id":"[^"]*"' | cut -d'"' -f4); do
    curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/policy/$id" > /dev/null || true
done
for id in $(curl -s -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/policy?first=0&max=200" | grep -o '"id":"[^"]*"' | cut -d'"' -f4); do
    curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/policy/$id" > /dev/null || true
done
for id in $(curl -s -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/resource?first=0&max=200" | grep -o '"_id":"[^"]*"' | cut -d'"' -f4); do
    curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/resource/$id" > /dev/null || true
done
for id in $(curl -s -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/scope?first=0&max=200" | grep -o '"id":"[^"]*"' | cut -d'"' -f4); do
    curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "$AUTHZ_BASE/scope/$id" > /dev/null || true
done
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"clientId":"poc-api","secret":"poc-api-secret","protocol":"openid-connect","publicClient":false,"standardFlowEnabled":true,"implicitFlowEnabled":false,"directAccessGrantsEnabled":true,"serviceAccountsEnabled":false,"authorizationServicesEnabled":false,"redirectUris":["http://localhost:3000/*","http://localhost:3001/*","http://localhost:5173/*"],"webOrigins":["http://localhost:3000","http://localhost:3001","http://localhost:5173"]}' \
  "http://keycloak:8080/admin/realms/poc/clients/$CLIENT_ID" > /dev/null

echo "Keycloak init complete"
