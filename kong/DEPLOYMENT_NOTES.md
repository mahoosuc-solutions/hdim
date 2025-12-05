Kong user-patient-kafka plugin deployment notes
================================================

Goal
----
Emit per-user/per-tenant/per-patient request events from Kong to Kafka with a supported Kong 3.x plugin and deployable Helm configuration.

Plugin packaging
----------------
1) Build a custom Kong image that includes:
   - `kong/plugins/user-patient-kafka/handler.lua`
   - `kong/plugins/user-patient-kafka/schema.lua`
   - Dependencies: `lua-resty-kafka` (and its lua-resty-* deps) installed in the Lua path.
2) Example Dockerfile:
   ```Dockerfile
   FROM kong:3.6
   USER root
   RUN luarocks install lua-resty-kafka
   COPY kong/plugins/user-patient-kafka /usr/local/share/lua/5.1/kong/plugins/user-patient-kafka
   USER kong
   ENV KONG_PLUGINS=bundled,user-patient-kafka
   ENV KONG_LUA_PACKAGE_PATH=/usr/local/share/lua/5.1/?.lua;;/opt/kong/plugins/?.lua;;
   ```
3) Push the image (e.g., `ghcr.io/your-org/kong-user-patient:latest`).

Helm values (outline)
---------------------
- Point to the custom image built above.
- Mount declarative config for services/routes (`kong/kong.yaml`) via ConfigMap or deck sync.
- Example snippet (values.yaml):
```yaml
image:
  repository: ghcr.io/your-org/kong-user-patient
  tag: latest
env:
  database: "off"
  plugins: "bundled,user-patient-kafka"
  lua_package_path: "/usr/local/share/lua/5.1/?.lua;;/opt/kong/plugins/?.lua;;"
proxy:
  type: ClusterIP
deck:
  enabled: true
  files:
    - /opt/kong/kong.yaml
extraVolumes:
  - name: kong-declarative-config
    configMap:
      name: kong-declarative-config
extraVolumeMounts:
  - name: kong-declarative-config
    mountPath: /opt/kong/kong.yaml
    subPath: kong.yaml
```

Declarative config
------------------
- Use `kong/kong.yaml` in this repo as the base. Deck syncs it into Kong.
- Adjust broker list, topic, patient extraction, and require_patient per route as needed.

Runtime expectations
--------------------
- `resty.kafka.producer` must be available (hence the custom image).
- The plugin emits one Kafka message per patient per request; ensure topic/partition capacity.
- Correlation IDs are provided by the `correlation-id` plugin (`X-Request-ID`).
- If `require_patient` is true and no patient is extracted, requests are rejected with 400.

Next refinements
----------------
- Add Prometheus counters for send failures/missing patient (via an additional Kong prometheus plugin or extending this plugin).
- Consider batching multiple patients into a single Kafka record keyed by request_id if fan-out is large.
