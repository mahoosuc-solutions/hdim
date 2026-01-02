local cjson = require "cjson.safe"
local lrucache = require "resty.lrucache"
local kong = kong

local kafka_ok, kafka_producer = pcall(require, "resty.kafka.producer")

local plugin = {
  PRIORITY = 900,
  VERSION = "0.2.0",
}

-- cache producers per worker
local prod_cache = (function()
  local cache, err = lrucache.new(16)
  if not cache then
    kong.log.err("failed to create lrucache for kafka producers: ", err)
  end
  return cache
end)()

local function normalize_brokers(brokers)
  if type(brokers) ~= "table" then
    return {}
  end
  if #brokers == 1 and brokers[1]:find(",") then
    local list = {}
    for b in brokers[1]:gmatch("([^,]+)") do
      table.insert(list, b)
    end
    return list
  end
  return brokers
end

local function get_producer(brokers, conf)
  if not kafka_ok then
    return nil, "resty.kafka.producer not available in runtime image"
  end

  local cache_key = table.concat(brokers, ",") .. "|" .. (conf.kafka.topic or "")
  if prod_cache then
    local cached = prod_cache:get(cache_key)
    if cached then
      return cached
    end
  end

  local p, err = kafka_producer:new(brokers, {
    producer_type = "async",
    flush_time = conf.kafka.flush_time,
    batch_num = conf.kafka.batch_num,
    required_acks = conf.kafka.required_acks,
    socket_timeout = conf.kafka.timeout_ms,
    compression = conf.kafka.compression,
  })
  if not p then
    return nil, "kafka producer init failed: " .. tostring(err)
  end

  if prod_cache then
    prod_cache:set(cache_key, p, 60)
  end

  return p
end

local function dedupe_patients(patients)
  local seen = {}
  local out = {}
  for _, p in ipairs(patients) do
    local key = (p.patient_id or "") .. "|" .. (p.assigning_authority or "")
    if not seen[key] then
      table.insert(out, p)
      seen[key] = true
    end
  end
  return out
end

local function parse_patient_header(raw, src)
  if not raw or raw == "" then
    return {}
  end

  if src.format == "json_array" then
    local arr = cjson.decode(raw)
    if type(arr) ~= "table" then
      return {}
    end
    local patients = {}
    for _, item in ipairs(arr) do
      if type(item) == "table" then
        local pid = item[src.fields.id_key] or item.patient_id
        local aa = item[src.fields.assigning_key] or item.assigning_authority or src.assigning_authority
        if pid then
          table.insert(patients, { patient_id = pid, assigning_authority = aa })
        end
      end
    end
    return patients
  elseif src.format == "csv" then
    local patients = {}
    for token in raw:gmatch("([^,]+)") do
      table.insert(patients, { patient_id = kong.text.trim(token), assigning_authority = src.assigning_authority })
    end
    return patients
  else
    return { { patient_id = raw, assigning_authority = src.assigning_authority } }
  end
end

local function extract_patients(conf)
  local patients = {}

  for _, src in ipairs(conf.patient_sources or {}) do
    if src.type == "header" and src.name then
      if src.format == "json_array" and src.allow_repeated_headers then
        local raws = kong.request.get_headers()[src.name]
        if type(raws) == "table" then
          for _, v in ipairs(raws) do
            for _, p in ipairs(parse_patient_header(v, src)) do
              table.insert(patients, p)
            end
          end
        end
      end
      local raw = kong.request.get_header(src.name)
      for _, p in ipairs(parse_patient_header(raw, src)) do
        table.insert(patients, p)
      end
    elseif src.type == "path_regex" and src.regex then
      local m, err = ngx.re.match(kong.request.get_path(), src.regex, "jo")
      if m and m["patient_id"] then
        table.insert(patients, { patient_id = m["patient_id"], assigning_authority = src.assigning_authority })
      elseif err then
        kong.log.warn("path_regex error: ", err)
      end
    elseif src.type == "query" and src.param then
      local pid = kong.request.get_query_arg(src.param)
      if pid then
        table.insert(patients, { patient_id = pid, assigning_authority = src.assigning_authority })
      end
    end
  end

  return dedupe_patients(patients)
end

local function extract_user(conf)
  local user
  local tenant

  if kong.client.get_authenticated_claims then
    local jwt_claims = kong.client.get_authenticated_claims()
    if jwt_claims then
      user = jwt_claims[conf.user_claim]
      tenant = jwt_claims[conf.tenant_claim]
    end
  end

  if not user then
    user = kong.request.get_header(conf.user_header_fallback)
  end
  if not tenant then
    tenant = kong.request.get_header(conf.tenant_header_fallback)
  end

  return user, tenant
end

local function build_events(conf, user, tenant, patients)
  local events = {}
  local req = kong.request
  local res = kong.response
  local latency = kong.ctx.core.latencies

  local base = {
    timestamp = ngx.utctime(),
    request_id = req.get_header(conf.request_id_header) or req.get_header("X-Correlation-ID"),
    user_id = user,
    tenant_id = tenant,
    service = (kong.router.get_service() or {}).name,
    route = (kong.router.get_route() or {}).name,
    method = req.get_method(),
    path = req.get_path(),
    query = req.get_raw_query(),
    remote_addr = kong.client.get_forwarded_ip(),
    status = res.get_status(),
    latency = latency and latency.request or nil,
    kong_latency = latency and latency.kong or nil,
    upstream_latency = latency and latency.proxy or nil,
  }

  if #patients == 0 then
    table.insert(events, base)
  else
    for _, p in ipairs(patients) do
      local ev = {}
      for k, v in pairs(base) do
        ev[k] = v
      end
      ev.patient_id = p.patient_id
      ev.assigning_authority = p.assigning_authority
      table.insert(events, ev)
    end
  end

  return events
end

function plugin:access(conf)
  local patients = extract_patients(conf)
  if conf.require_patient and #patients == 0 then
    return kong.response.exit(400, { message = "Patient context required" })
  end

  kong.ctx.plugin.patients = patients
end

function plugin:log(conf)
  local patients = kong.ctx.plugin.patients or extract_patients(conf)
  local user, tenant = extract_user(conf)
  local brokers = normalize_brokers(conf.kafka.brokers)
  local prod, perr = get_producer(brokers, conf)

  local events = build_events(conf, user, tenant, patients)

  if not prod then
    kong.log.err("Kafka producer unavailable: ", perr)
    return
  end

  for _, ev in ipairs(events) do
    local payload = cjson.encode(ev)
    local ok, err = prod:send(conf.kafka.topic, ev.patient_id or "none", payload)
    if not ok then
      kong.log.err("failed to send to Kafka: ", err)
    end
  end
end

return plugin
