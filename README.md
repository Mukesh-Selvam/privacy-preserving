# Privacy-Preserving Data Sharing Gateway — PS26SCS211

A complete, working implementation: every request for a patient field is
checked against **both** an organization-level OPA policy **and** the
patient's own consent. Only when both agree is the field returned —
encrypted for reversible fields, masked otherwise. Every transaction is
logged, and there's a live dashboard to demo it visually.

## What's in this project

```
src/main/java/com/hackathon/gateway/
  GatewayApplication.java          entry point
  controller/                      REST endpoints
  service/
    OpaService.java                calls OPA for each field decision
    VaultKeyService.java           fetches the FPE key from Vault
    FpeService.java                format-preserving encrypt/decrypt
    GatewayService.java            orchestrates the whole flow
    AuditService.java              writes/reads audit_log
  entity/                          JPA entities
  repository/                      Spring Data repositories
  dto/                             OPA request/response payloads
src/main/resources/static/dashboard.html   live demo UI
policies/gateway.rego              the org + consent policy
sql/schema.sql                     full schema + seed data (Docker auto-load)
sql/seed-data.sql                  seed data only (for manual/native setup)
docker-compose.yml                 one-command infrastructure
postman-collection.json            10 pre-built demo requests
```

## Option A — one-command infrastructure (recommended)

This avoids any "tables created in the wrong database" issue, since the
schema loads automatically into a fresh, purpose-built container.

**Important:** your native PostgreSQL is already using port 5432. Before
running this, either:
- Stop the native Postgres Windows service (Services app → `postgresql-x64-18` → Stop), or
- Edit `docker-compose.yml`'s Postgres port mapping to `"5433:5432"` and update the datasource URL in `application.yml` to `localhost:5433` to match.

From the project root:

```
docker-compose up -d
```

Verify all four are running:

```
docker ps
```

You should see `gateway-postgres`, `gateway-redis`, `gateway-vault`,
`gateway-opa`, all "Up".

Push the OPA policy explicitly once (docker-compose mounts it, but this
confirms it's actually loaded):

```
curl.exe -X PUT --data-binary "@policies/gateway.rego" http://localhost:8181/v1/policies/gateway
```

Seed the Vault FPE key:

```
docker exec -it gateway-vault vault kv put secret/gateway/fpe-key permutation=3781495260
```

## Option B — keep using your existing manual setup

If you'd rather use the Postgres/Redis/Vault/OPA you already configured
by hand:

1. Connect specifically to `gateway_db` (not the default `postgres` database) and run the four `CREATE TABLE` statements from earlier, then:
   ```
   psql -U postgres -h localhost -d gateway_db -f sql/seed-data.sql
   ```
2. Load the policy and seed the Vault key exactly as shown in Option A.

## Set your Postgres password (if not `postgres`)

Edit `src/main/resources/application.yml`, or:
```
set DB_PASSWORD=your_actual_password
```

## Run the application

```
mvn spring-boot:run
```

Check it's alive:
```
curl http://localhost:8080/
```

## The dashboard — your main demo surface

Open in a browser:
```
http://localhost:8080/dashboard.html
```

From here, without typing a single command, you can:
- Pick a patient + requesting organization and send a live request through the gateway
- Load that patient's consent switches and flip them on/off in real time
- Watch the returned JSON change instantly — same org, same policy, only consent changed
- See the audit log update after every request

This is the strongest thing to have on screen when judges walk up.

## Postman collection (backup / narrated demo)

Import `postman-collection.json` into Postman. It has 10 pre-built
requests in the exact order of a full demo narrative — health check,
insurer view, the consent-gate proof, a live consent flip, research-org
view, and the audit log. Use this if the dashboard isn't available on stage.

## Demo script (either surface)

1. **Insurer sees Rahul (patient 1), full consent** → name, age, disease
   plain; Aadhaar encrypted (a different 12-digit number); phone/address removed.
2. **Insurer sees Priya (patient 2)** → `disease` is masked even though the
   org policy allows it for every other patient — because Priya's consent
   for that specific field is off. **This is the pitch.**
3. **Flip Priya's disease consent on** (dashboard toggle or Postman
   request 5) → re-request → disease now appears. Flip back → disappears.
4. **Research org sees Rahul** → only age + disease, a narrower slice of
   the exact same patient.
5. **Audit log** → every call above is recorded: who, what org, what was
   masked/removed, when.

## Notes for the pitch

- **FPE is simplified for the hackathon**: a keyed digit-substitution
  cipher (a permutation of 0–9 stored in Vault), not a full FF3-1
  implementation. It's genuinely reversible and format-preserving, which
  is what matters for the demo — say this plainly if asked, it shows you
  understand the tradeoff rather than papering over it.
- **Fail-closed design**: if OPA is unreachable, `OpaService` returns
  `"hidden"` rather than defaulting open — a field is never accidentally
  exposed by a downstream failure.
- **Default-deny consent**: if a patient has no consent row on file for a
  field, that's treated as `false`, not `true` — no consent means no sharing.
